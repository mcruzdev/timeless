require('dotenv').config()
const { Client, LocalAuth } = require('whatsapp-web.js')
const qrcode = require('qrcode-terminal')
const crypto = require('crypto')
const { S3Client, PutObjectCommand } = require("@aws-sdk/client-s3");
const OpenAI = require("openai");


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
});

client.on('ready', () => {
    console.log('client connected')
})

client.on('message', async (msg) => {

    if (!msg.hasMedia) {
        console.log('message rejected')
    } else {
        const media = await msg.downloadMedia()
        if (media.mimetype.startsWith("audio/ogg;")) {
            const buffer = Buffer.from(media.data, 'base64')
            const from = await msg.getContact()
            const audioName = generateAudioName(from.id.user)
            const command = new PutObjectCommand({
                Bucket: process.env.ASSETS_BUCKET,
                Key: audioName,
                Body: buffer,
                ContentType: 'audio/mpeg'
            })

            await s3Client.send(command)

            const transcription = await openai.audio.transcriptions.create({
                file: buffer,
                model: "gpt-4o-transcribe",
            });

            timelessApiClient.post('/api/messages', {
                location: audioName,
                from: from.id.user.toString(),
                message: transcription,
            }).then(response => {
                console.log('sent to timeless-api, status code is ', response.status)
                if (allowUsers.includes(from.id.user)) {
                    msg.reply('Sua movimentação foi cadastrada no sistema 😀')
                    console.log(JSON.stringify(response.data))
                }
            }).catch(err => {
                console.error('error while sending message to timeless-api', err)
                if (allowUsers.includes(from.id.user)) {
                    msg.reply('Desculpe-me! Não foi possível cadastrar a sua movimentação 😔😔😔')
                }
            })
        }
    }
})

const generateAudioName = (userId) => `audios/${userId}/${crypto.randomUUID().toLocaleLowerCase()}.mp3`

client.initialize();