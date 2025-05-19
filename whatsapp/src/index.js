require('dotenv').config()
const { Client, LocalAuth } = require('whatsapp-web.js')
const qrcode = require('qrcode-terminal')
const crypto = require('crypto')
const { S3Client, PutObjectCommand } = require("@aws-sdk/client-s3")
const OpenAI = require("openai")
const fs = require("fs")
const { tmpdir } = require("os")
const { createClient } = require("redis");
const path = require('path')
const WAWebJS = require('whatsapp-web.js')

const allowedMedias = ['image/jpeg', 'audio/ogg; codecs=opus']
const usersKey = 'timeless-api:users'

let redis
createClient({
    url: 'redis://localhost:6379'
}).connect().then(r => {
    redis = r
    redis.set(usersKey, JSON.stringify(process.env.ALLOW_USERS.split(',').filter(u => u.length > 0))).then(r => {
        console.log(usersKey, 'was set successfully')
    })
})

const { processImagePrompt } = require('./prompts')

// timeless-api
const timelessApiClient = require('./axios')

// OpenAI
const openai = new OpenAI({
    apiKey: process.env.OPENAI_API_KEY
})

// AWS S3
const s3Client = new S3Client({
    region: 'sa-east-1',
    credentials: {
        accessKeyId: process.env.ACCESS_KEY,
        secretAccessKey: process.env.SECRET_KEY
    }
})

const client = new Client({
    authStrategy: new LocalAuth({
        dataPath: 'wwebjs-auth'
    }),
    puppeteer: {
        args: ['--no-sandbox', '--disable-setuid-sandbox'],
    }
})

client.on('qr', (qr) => {
    qrcode.generate(qr, { small: true })
})

client.on('ready', () => {
    console.log('client connected')
})

client.on('message', async (message) => {

    // there is no need to handle status message
    if (message.isStatus) {
        return
    }

    const data = await redis.get(usersKey)

    const users = JSON.parse([].concat(data))

    if (!users.includes((await message.getContact()).id.user)) {
        return
    }

    if (message.hasMedia) {
        const media = await message.downloadMedia()
        if (!allowedMedias.includes(media.mimetype)) {
            console.log('mimetype rejected: ', media.mimetype)
            return
        }
        await handleMediaMessage(message, media)
    } else {
        handleTextMessage(message)
    }
})


/**
 * 
 * @param {WAWebJS.Message} message 
 * @param {WAWebJS.MessageMedia} media 
 */
const handleImageMessage = async (message, media) => {
    const contact = await message.getContact()
    const response = await openai.responses.create({
        model: "gpt-4.1-mini",
        input: [
            {
                role: "user",
                content: [
                    {
                        type: "input_text", text: processImagePrompt
                    },
                    {
                        type: "input_image",
                        image_url: `data:image/jpeg;base64,${media.data}`,
                    },
                ],
            },
        ],
    })

    const data = JSON.parse(response.output_text)

    if (data.error) {
        message.reply('Hmmmm... Não foi possível processar sua imagem')
        return
    }

    await timelessApiClient.post('/api/records', {
        from: contact.id.user,
        recordType: data.type,
        amount: data.amount,
        description: data.description
    })
}

/**
 * 
 * @param {WAWebJS.Message} message 
 * @param {WAWebJS.MessageMedia} media 
 */
const handleAudioMessage = async (message, media) => {
    const buffer = Buffer.from(media.data, 'base64')
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
        console.error('error while getting transcription from OpenAI', error)
    } finally {
        fs.rmSync(p, {
            force: true,
            maxRetries: 3
        })
    }

    if (!transcription) {
        message.reply('Hmmmm... Não foi possível transcrever o seu áudio')
    } else {
        timelessApiClient.post('/api/messages', {
            from: contact.id.user,
            message: transcription.text
        }).then(_ => {
            return message.react('✅')
        })
        .catch(err => {
            console.error(err)
            return message.react('❌')
        })
    }
}

/**
 * 
 * @param {WAWebJS.Message} message 
 */
const handleTextMessage = (message) => {
    console.log(message.body)
}

/**
 * 
 * @param {WAWebJS.Message} message 
 * @param {WAWebJS.MessageMedia} media 
 */
const handleMediaMessage = async (message, media) => {
    if (media.mimetype === 'audio/ogg; codecs=opus') {
        await handleAudioMessage(message, media)
    }

    if (media.mimetype === 'image/jpeg') {
        await handleImageMessage(message, media)
    }

}

const generateAudioName = (userId) => `audios/${userId}/${crypto.randomUUID().toLocaleLowerCase()}.mp3`

const generateSimpleAudioName = () => `${crypto.randomUUID().toLocaleLowerCase()}.mp3`

const sendMessageIf = (condition, runnable) => {
    if (condition) {
        runnable()
    }
}

client.initialize()