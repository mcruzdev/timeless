require("dotenv").config()
const { Client, LocalAuth } = require("whatsapp-web.js")
const qrcode = require("qrcode-terminal")
const crypto = require("crypto")
const OpenAI = require("openai")
const fs = require("fs")
const { tmpdir } = require("os")
const { createClient } = require("redis")
const path = require("path")
const { S3Client, PutObjectCommand } = require("@aws-sdk/client-s3")
const { SQSClient, SendMessageCommand } = require("@aws-sdk/client-sqs")
const { Consumer } = require("sqs-consumer")

const timelessApiClient = require("./axios")

const FLAGS = {
    sendMediaToS3: process.env.SEND_MEDIA_TO_S3 || false,
}
const AWS_REGION = "sa-east-1"
const USERS_KEY = "timeless-api:users"
const ALLOWED_MEDIAS = ["image/jpeg", "audio/ogg; codecs=opus"]
const CURRENCY_FORMATTER = Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
    minimumFractionDigits: 2,
})

const awsCredentials = {
    accessKeyId: process.env.AWS_ACCESS_KEY_ID,
    secretAccessKey: process.env.AWS_SECRET_ACCESS_KEY,
}

const s3Client = new S3Client({
    region: AWS_REGION,
    credentials: awsCredentials,
})

const sqsClient = new SQSClient({
    region: AWS_REGION,
    credentials: awsCredentials,
})

const openai = new OpenAI({
    apiKey: process.env.OPENAI_API_KEY,
})

const mimetypeFileMap = {
    "audio/ogg; codecs=opus": {
        name: "audio.mp3",
        kind: "AUDIO",
    },
    "image/jpeg": {
        name: "image.jpg",
        kind: "IMAGE",
    },
}

let redis

async function initializeRedis() {
    const client = createClient({ url: "redis://localhost:6379" })
    await client.connect()
    redis = client
    const allowedUsers = process.env.ALLOW_USERS.split(",").filter(Boolean)
    await redis.set(USERS_KEY, JSON.stringify(allowedUsers))
    console.log(`${USERS_KEY} was set successfully`)
}
initializeRedis()

const client = new Client({
    clientId: "timeless-bot",
    authStrategy: new LocalAuth({ dataPath: "wwebjs-auth" }),
    puppeteer: {
        args: ["--no-sandbox", "--disable-setuid-sandbox"],
    },
})

client.on("qr", (qr) => qrcode.generate(qr, { small: true }))

client.on("ready", () => {
    console.log("client connected")
    consumer.start()
})

client.on("message", async (message) => {
    if (message.isStatus) return

    const users = await getAllowedUsers()
    const sender = (await message.getContact()).id.user
    if (!users.includes(sender)) return

    message.reply("Estamos processando sua mensagem")
    const chat = await message.getChat()
    await chat.sendStateTyping()

    if (message.hasMedia) {
        const media = await message.downloadMedia()
        if (!ALLOWED_MEDIAS.includes(media.mimetype)) {
            console.log("mimetype rejected: ", media.mimetype)
            return
        }
        await handleMediaMessage(message, media, sender)
    } else {
        await handleTextMessage(message, sender)
    }
})

async function getAllowedUsers() {
    const data = await redis.get(USERS_KEY)
    return JSON.parse(data || "[]")
}

function generateSimpleAudioName() {
    return `${crypto.randomUUID().toLowerCase()}.mp3`
}

async function handleTextMessage(message, sender) {
    const messageId = message.id.id
    const chat = await message.getChat()

    await sqsClient.send(
        new SendMessageCommand({
            QueueUrl: process.env.INCOMING_MESSAGE_QUEUE,
            MessageGroupId: "IncomingMessagesFromUser",
            MessageBody: JSON.stringify({
                sender,
                kind: "TEXT",
                messageId,
                status: "READ",
                messageBody: message.body,
            }),
        })
    )
}

async function handleMediaMessage(message, media, sender) {
    const { mimetype } = media
    if (!mimetypeFileMap[mimetype]) return

    if (FLAGS.sendMediaToS3) {
        await uploadMediaToS3(message, media, sender)
    }

    if (mimetype === "audio/ogg; codecs=opus") {
        await handleAudioMessage(message, media)
    } else if (mimetype === "image/jpeg") {
        await handleImageMessage(message, media)
    }
}

async function handleImageMessage(message, media) {
    const contact = await message.getContact()
    try {
        await timelessApiClient.post("/api/messages/image", {
            from: contact.id.user,
            text: message.body,
            base64: media.data,
            mimeType: media.mimetype,
        })
    } catch (err) {
        console.error(err.message)
        await message.react("❌")
    }
}

async function handleAudioMessage(message, media) {
    const buffer = Buffer.from(media.data, "base64")
    const contact = await message.getContact()
    const audioPath = path.join(tmpdir(), generateSimpleAudioName())
    let transcription

    try {
        fs.writeFileSync(audioPath, buffer)
        transcription = await openai.audio.transcriptions.create({
            file: fs.createReadStream(audioPath),
            model: "whisper-1",
            response_format: "verbose_json",
        })
    } catch (error) {
        console.error("error while getting transcription from OpenAI", error)
    } finally {
        fs.rmSync(audioPath, { force: true, maxRetries: 3 })
    }

    if (!transcription) {
        await message.reply("Hmmmm... Não foi possível transcrever o seu áudio")
        return
    }

    try {
        await timelessApiClient.post("/api/messages", {
            from: contact.id.user,
            message: transcription.text,
        })
        await message.react("✅")
    } catch (err) {
        console.error(err)
        await message.react("❌")
    }
}

async function uploadMediaToS3(message, media) {
    const file = mimetypeFileMap[media.mimetype]
    const key = `messages/${message.from}/${message.id.id}/${file.name}`
    await s3Client.send(
        new PutObjectCommand({
            Bucket: process.env.ASSETS_BUCKET,
            Key: key,
            Body: Buffer.from(media.data, "base64"),
            ContentEncoding: "base64",
            ContentType: media.mimetype,
        })
    )
}

const consumer = Consumer.create({
    queueUrl: process.env.MESSAGES_PROCESSED_FIFO_URL,
    sqs: sqsClient,
    suppressFifoWarning: true,
    handleMessage: async (sqsMessage) => {
        try {
            const data = JSON.parse(sqsMessage.Body)

            const chat = await findChatByUser(data.user)

            if (!chat) {
                console.warn(`Chat not found for user: ${data.user}`)
                return sqsMessage
            }

            await handleMessageByCommandName(chat, data)
            return sqsMessage
        } catch (error) {
            console.error("Failed to process SQS message:", error)
            return {} // Don't delete message
        }
    },
})

async function findChatByUser(userId) {
    const chats = await client.getChats()
    return chats.find((chat) => chat.id.user === userId)
}

async function handleMessageByCommandName(chat, data) {
    switch (data.kind) {
        case "GET_BALANCE":
            await chat.sendMessage(data.content.message)
            break

        case "ADD_TRANSACTION":
            await sendMovementResult(chat, data)
            break

        default:
            console.warn(`Unsupported message kind: ${data.kind}`)
    }
}

async function sendMovementResult(chat, data) {
    if (data.withError) {
        await chat.sendMessage(
            "Desculpe-me! Não foi possível cadastrar a sua movimentação"
        )
    } else {
        await chat.sendMessage(
            `Sua movimentação foi cadastrada com sucesso ✅

*Descrição:* ${data.content.description}
*Valor:* ${CURRENCY_FORMATTER.format(data.content.amount)}
*Tipo:* ${data.content.type === "IN" ? "Entrada" : "Saída"}`
        )
    }
}

client.initialize()
