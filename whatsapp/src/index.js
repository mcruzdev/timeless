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

const allowedMedias = ["image/jpeg", "audio/ogg; codecs=opus"]
const usersKey = "timeless-api:users"

const currencyFormatter = Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
    minimumFractionDigits: 2,
})

let redis
createClient({
    url: "redis://localhost:6379",
})
    .connect()
    .then((r) => {
        redis = r
        redis
            .set(
                usersKey,
                JSON.stringify(
                    process.env.ALLOW_USERS.split(",").filter(
                        (u) => u.length > 0
                    )
                )
            )
            .then(() => {
                console.log(usersKey, "was set successfully")
            })
    })

const timelessApiClient = require("./axios")
const awsRegion = "sa-east-1"

const awsCredentials = {
    accessKeyId: process.env.ACCESS_KEY,
    secretAccessKey: process.env.SECRET_KEY,
}

const s3Client = new S3Client({
    region: awsRegion,
    credentials: awsCredentials,
})

const sqsClient = new SQSClient({
    region: awsRegion,
    credentials: awsCredentials,
})

const openai = new OpenAI({
    apiKey: process.env.OPENAI_API_KEY,
})

const client = new Client({
    clientId: "timeless-bot",
    authStrategy: new LocalAuth({
        dataPath: "wwebjs-auth",
    }),
    puppeteer: {
        args: ["--no-sandbox", "--disable-setuid-sandbox"],
    },
})

client.on("qr", (qr) => {
    qrcode.generate(qr, { small: true })
})

client.on("ready", () => {
    console.log("client connected")
    consumer.start()
})

client.on("message", async (message) => {
    // there is no need to handle status message
    if (message.isStatus) {
        return
    }

    const data = await redis.get(usersKey)

    const users = JSON.parse([].concat(data))

    const sender = (await message.getContact()).id.user
    if (!users.includes(sender)) {
        return
    }

    let media
    if (message.hasMedia) {
        media = await message.downloadMedia()
    }

    if (media) {
        if (!allowedMedias.includes(media.mimetype)) {
            console.log("mimetype rejected: ", media.mimetype)
            return
        }

        await handleMediaMessage(message, media, sender)
    } else {
        await handleTextMessage(message, sender)
        const chat = await message.getChat()
        await chat.sendMessage("Estamos processando sua mensagem")
        await chat.sendStateTyping()
    }
})

/**
 *
 * @param {WAWebJS.Message} message
 * @param {WAWebJS.MessageMedia} media
 */
const handleImageMessage = async (message, media) => {
    const contact = await message.getContact()

    try {
        await timelessApiClient.post("/api/messages/image", {
            from: contact.id.user,
            text: message.body,
            base64: media.data,
            mimeType: media.mimetype,
        })
        await message.react("✅")
    } catch (err) {
        console.error(err.message)
        await message.react("❌")
    }
}

/**
 *
 * @param {WAWebJS.Message} message
 * @param {WAWebJS.MessageMedia} media
 */
const handleAudioMessage = async (message, media) => {
    const buffer = Buffer.from(media.data, "base64")
    const contact = await message.getContact()
    const p = path.join(tmpdir(), generateSimpleAudioName())
    fs.writeFileSync(p, buffer)
    let transcription
    try {
        transcription = await openai.audio.transcriptions.create({
            file: fs.createReadStream(p),
            model: "whisper-1",
            response_format: "verbose_json",
        })
    } catch (error) {
        console.error("error while getting transcription from OpenAI", error)
    } finally {
        fs.rmSync(p, {
            force: true,
            maxRetries: 3,
        })
    }

    if (!transcription) {
        message.reply("Hmmmm... Não foi possível transcrever o seu áudio")
    } else {
        timelessApiClient
            .post("/api/messages", {
                from: contact.id.user,
                message: transcription.text,
            })
            .then(() => {
                return message.react("✅")
            })
            .catch((err) => {
                console.error(err)
                return message.react("❌")
            })
    }
}

/**
 *
 * @param {WAWebJS.Message} message
 */
const handleTextMessage = async (message, sender) => {
    const messageId = message.id.id
    await sqsClient.send(
        new SendMessageCommand({
            QueueUrl: process.env.INCOMING_MESSAGE_QUEUE,
            MessageGroupId: "IncomingMessagesFromUser",
            MessageBody: JSON.stringify({
                sender,
                kind: "text",
                messageId,
                chat: (await message.getChat()).id,
                status: "READ",
                messageBody: message.body,
            }),
        })
    )
}

/**
 *
 * @param {WAWebJS.Message} message
 * @param {WAWebJS.MessageMedia} media
 */
const handleMediaMessage = async (message, media) => {
    if (!mimetypeFileMap[media.mimetype]) {
        return
    }

    const file = mimetypeFileMap[media.mimetype]

    const key = `messages/${sender}/${message.id.id}/${file.name}`
    await s3Client.send(
        new PutObjectCommand({
            Bucket: process.env.ASSETS_BUCKET,
            Key: key,
            Body: Buffer.from(media.data, "base64"),
            ContentEncoding: "base64",
            ContentType: media.mimetype,
        })
    )

    await sqsClient.send(
        new SendMessageCommand({
            QueueUrl: process.env.INCOMING_MESSAGE_QUEUE,
            MessageGroupId: "IncomingMessagesFromUser",
            MessageBody: JSON.stringify({
                sender,
                mediaLocation: key,
                kind: file.kind,
                messageId: message.id.id,
                chat: (await message.getChat()).id,
                status: "READ",
            }),
        })
    )

    if (media.mimetype === "audio/ogg; codecs=opus") {
        await handleAudioMessage(message, media)
    }

    if (media.mimetype === "image/jpeg") {
        await handleImageMessage(message, media)
    }
}

const mimetypeFileMap = {
    "audio/ogg; codecs=opus": {
        name: "audio.mp3",
        kind: "audio",
    },
    "image/jpeg": {
        name: "image.jpg",
        kind: "image",
    },
}

const generateSimpleAudioName = () =>
    `${crypto.randomUUID().toLocaleLowerCase()}.mp3`

const consumer = Consumer.create({
    queueUrl:
        "https://sqs.sa-east-1.amazonaws.com/405894840898/messages-processed.fifo",
    sqs: sqsClient,
    suppressFifoWarning: true,
    handleMessage: async (message) => {
        const data = JSON.parse(message.Body)
        console.log(data)
        try {
            const chats = await client.getChats()
            chats.forEach(async (chat) => {
                if (chat.id.user === data.chat.user) {
                    if (data.withError) {
                        chat.sendMessage(
                            "Desculpe-me! Não foi possível cadastrar a sua movimentação"
                        )
                    } else {
                        chat.sendMessage(
                            `Sua movimentação foi cadastrada com sucesso ✅

*Descrição:* ${data.record.description}
*Valor:* ${currencyFormatter.format(data.record.amount)}
*Tipo:* ${data.record.type === "IN" ? "Entrada" : "Saída"}
                        `
                        )
                    }
                }
            })

            return message
        } catch (err) {
            console.err(err)
            // do not delete the message
            return {}
        }
    },
})

client.initialize()
