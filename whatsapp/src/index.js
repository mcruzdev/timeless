require('dotenv').config()
const { Client, LocalAuth } = require('whatsapp-web.js')
const qrcode = require('qrcode-terminal')
const crypto = require('crypto')
const { S3Client, PutObjectCommand } = require("@aws-sdk/client-s3")
const OpenAI = require("openai")
const fs = require("fs")
const { tmpdir } = require("os")
const path = require('path')

const timelessApiClient = require('./axios')

const allowUsers = process.env.ALLOW_USERS.split(',').filter(user => user.length > 0)

const openai = new OpenAI({
    apiKey: process.env.OPENAI_API_KEY
})

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

client.on('message', async (msg) => {

    if (msg.hasMedia) {
        const media = await msg.downloadMedia()
        
        if (media.mimetype.startsWith("audio")) {
            const buffer = Buffer.from(media.data, 'base64')
            const from = await msg.getContact()
            const audioName = generateAudioName(from.id.user)
            const command = new PutObjectCommand({
                Bucket: process.env.ASSETS_BUCKET,
                Key: audioName,
                Body: buffer,
                ContentType: media.mimetype
            })

            const p = path.join(tmpdir(), generateSimpleAudioName())

            fs.writeFileSync(p, buffer)

            await s3Client.send(command)

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
                sendMessageIf(allowUsers.includes(from.id.user), () => {
                    msg.reply('Desculpe-me! Não foi possível cadastrar a sua movimentação 😔')
                })
            } else {
                timelessApiClient.post('/api/messages', {
                    location: audioName,
                    from: from.id.user.toString(),
                    message: transcription.text
                }).then(response => {
                    console.log('sent to timeless-api, status code is ', response.status)
                    sendMessageIf(allowUsers.includes(from.id.user), () => {
                        msg.reply('Sua movimentação foi cadastrada no sistema 😀')
                    })
                }).catch(err => {
                    console.error('error while sending message to timeless-api', err.status)
                    sendMessageIf(allowUsers.includes(from.id.user), () => {
                        msg.reply('Desculpe-me! Não foi possível cadastrar a sua movimentação 😔')
                    })
                })
            }
        }
    }
})

const generateAudioName = (userId) => `audios/${userId}/${crypto.randomUUID().toLocaleLowerCase()}.mp3`

const generateSimpleAudioName = () => `${crypto.randomUUID().toLocaleLowerCase()}.mp3`

const sendMessageIf = (condition, runnable) => {
    if (condition) {
        runnable()
    }
}

client.initialize()