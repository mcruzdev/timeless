const path = require("path")

if (process.env.ENV !== "production") {
    require("dotenv").config({
        path: path.resolve(
            process.cwd(),
            process.env.ENV === "local" ? ".env.local" : ".env"
        ),
    })
} else {
    console.log("Running in production mode, skipping .env loading")
    console.log(
        "Queues are: ",
        process.env.INCOMING_MESSAGE_FIFO_URL,
        process.env.RECOGNIZED_MESSAGE_FIFO_URL
    )
}

const QRCode = require("qrcode")
const { Client, LocalAuth } = require("whatsapp-web.js")
const qrcode = require("qrcode-terminal")
const crypto = require("crypto")
const OpenAI = require("openai")
const fs = require("fs")
const { tmpdir } = require("os")
const { S3Client, PutObjectCommand } = require("@aws-sdk/client-s3")
const { SQSClient, SendMessageCommand } = require("@aws-sdk/client-sqs")
const { Consumer } = require("sqs-consumer")

const timelessApiClient = require("./axios")
const WAWebJS = require("whatsapp-web.js")

const FLAGS = {
    sendMediaToS3: process.env.SEND_MEDIA_TO_S3 || false,
}
const AWS_REGION = process.env.AWS_REGION
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

const additionalConfigs =
    process.env.ENV === "local" ? { endpoint: "http://localhost:4566" } : {}

const sqsClient = new SQSClient({
    region: AWS_REGION,
    credentials: awsCredentials,
    ...additionalConfigs,
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

const messages = {
    sorryNotRegistered:
        "Desculpe-me! Não foi possível cadastrar a sua movimentação",
}

const getAllowedUsers = () =>
    process.env.ALLOWED_PHONE_NUMBERS.split(",").filter(Boolean)

const client = new Client({
    clientId: "timeless-bot",
    authStrategy: new LocalAuth({ dataPath: "wwebjs-auth" }),
    puppeteer: {
        args: ["--no-sandbox", "--disable-setuid-sandbox"],
    },
})

client.on("qr", (qr) => {
    if (process.env.ENV === "production") {
        QRCode.toDataURL(qr, (err, url) => {
            if (err) {
                console.error("Failed to generate QR code data URL:", err)
                throw err
            }

            s3Client
                .send(
                    new PutObjectCommand({
                        Bucket: process.env.ASSETS_BUCKET,
                        Key: `whatsapp-bot/${new Date().toISOString()}.png`,
                        Body: Buffer.from(
                            url.replace(/^data:image\/\w+;base64,/, ""),
                            "base64"
                        ),
                        ContentType: "image/png",
                    })
                )
                .then(() => {
                    console.log("QR code uploaded to S3 successfully!")
                    console.log(`
1. Access the S3 bucket: ${process.env.ASSETS_BUCKET}
2. Locate the QR code image inside the whatsapp-bot folder
3. Scan the QR code with your WhatsApp mobile app to log in
        `)
                })
                .catch((err) => {
                    console.error("Failed to upload QR code to S3:", err)
                })
        })
    } else {
        console.log(`
1. See the terminal for the QR code
2. Scan the QR code with your WhatsApp mobile app to log in
        `)
        qrcode.generate(qr, { small: true })
    }
})

client.on("ready", () => {
    console.log("WhatsApp Bot Running 🥳")
    consumer.start()
})

client.on("message", async (message) => {
    if (message.isStatus) return

    const users = await getAllowedUsers()
    const sender = (await message.getContact()).id.user
    if (!users.includes(sender)) {
        console.log(
            "This number is not authorized to communicate with the bot:",
            sender
        )
        return
    }

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

function generateSimpleAudioName() {
    return `${crypto.randomUUID().toLowerCase()}.mp3`
}

/**
 *
 * @param {WAWebJS.Message} message
 * @param {String} sender
 */
async function handleTextMessage(message, sender) {
    const messageId = message.id.id

    await sqsClient.send(
        new SendMessageCommand({
            QueueUrl: process.env.INCOMING_MESSAGE_FIFO_URL,
            MessageGroupId: "IncomingMessagesFromUser",
            MessageDeduplicationId: crypto.randomUUID().toString(),
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

/**
 * @param {WAWebJS.Message} message
 * @param {WAWebJS.MessageMedia} media
 * @param {String} sender
 */
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

/**
 *
 * @param {WAWebJS.Message} message
 * @param {WAWebJS.MessageMedia} media
 */
async function handleImageMessage(message, media) {
    const contact = await message.getContact()
    const chat = await message.getChat()

    try {
        const { data } = await timelessApiClient.post("/api/messages/image", {
            from: contact.id.user,
            text: message.body,
            base64: media.data,
            mimeType: media.mimetype,
        })

        await sendRecordResult(chat, {
            ...data,
        })
    } catch (err) {
        console.error(err.message)
        await chat.sendMessage(messages.sorryNotRegistered)
    }
}

/**
 *
 * @param {WAWebJS.Message} message
 * @param {WAWebJS.MessageMedia} media
 */
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
        await message.reply("Não foi possível transcrever o seu áudio")
        return
    }

    try {
        const { data } = await timelessApiClient.post("/api/messages", {
            from: contact.id.user,
            message: transcription.text,
        })
        const chat = await message.getChat()

        const content = JSON.parse(data.content)

        await sendRecordResult(chat, {
            amount: content.amount,
            description: content.description,
            type: content.type,
            withError: content.withError,
        })
    } catch (err) {
        console.error(err)
        await chat.sendMessage(messages.sorryNotRegistered)
    }
}

/**
 *
 * @param {WAWebJS.Message} message
 * @param {WAWebJS.MessageMedia} media
 */
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
    queueUrl: process.env.RECOGNIZED_MESSAGE_FIFO_URL,
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
            return {}
        }
    },
})

/**
 * @param {String} userId
 */
async function findChatByUser(userId) {
    const chats = await client.getChats()
    return chats.find((chat) => chat.id.user === userId)
}

/**
 *
 * @param {WAWebJS.Chat} chat
 * @param {Object} data
 * @param {String} data.kind
 * @param {Boolean} data.withError
 * @param {Object} data.content
 * @param {Number} data.content.amount
 * @param {String} data.content.description
 * @param {String} data.content.type
 */
async function handleMessageByCommandName(chat, data) {
    switch (data.kind) {
        case "GET_BALANCE":
            await chat.sendMessage(data.content.message)
            break

        case "ADD_TRANSACTION":
            await sendRecordResult(chat, {
                withError: data.withError,
                amount: data.content.amount,
                description: data.content.description,
                type: data.content.type,
            })
            break

        default:
            console.warn(`Unsupported message kind: ${data.kind}`)
    }
}

/**
 *
 * @param {WAWebJS.Chat} chat
 * @param {Object} metadata
 * @param {String} metadata.description
 * @param {Number} metadata.amount
 * @param {String} metadata.type
 * @param {Boolean} parmetadataam1.withError
 */
async function sendRecordResult(
    chat,
    { description, amount, type, withError }
) {
    if (withError) {
        await chat.sendMessage(messages.sorryNotRegistered)
    } else {
        await chat.sendMessage(
            `Sua movimentação foi cadastrada com sucesso ✅

*Descrição:* ${description}
*Valor:* ${CURRENCY_FORMATTER.format(amount)}
*Tipo:* ${type === "IN" ? "Entrada" : "Saída"}`
        )
    }
}

client.initialize()

// re_dN6xCZKU_KAg5ZAs8ViReNif3kQDBve3j
