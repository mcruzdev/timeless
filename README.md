# 🕰️ Timeless

Do you often make purchases and forget to write them down? **Timeless** is a project designed to help you by automatically detecting purchase-related messages on WhatsApp and registering that data in the **Timeless** app — your personal financial organization hub.

## ✨ Overview

This application monitors WhatsApp messages for signs of purchases (such as receipts, audio messages, transfers), extracts relevant information (amounts, dates, descriptions), and automatically sends it to **Timeless**, helping you keep your finances organized — effortlessly.

## 🚀 Features

- 📩 Automatic reading of WhatsApp messages (locally or from backups)
- 🧠 Smart extraction of purchase data using NLP (Natural Language Processing)
- 📤 Automatic registration of purchases in Timeless (via API)

## 🛠️ Technologies Used

- **Java + Quarkus** (timeless-api)
- **Langchain4j + OpenAI Whisper** (audio transcription and analysis)
- **NodeJS + whatsapp-web.js** (whatsapp)

## 💡 How It Works

1. You send or receive a message on WhatsApp:  
   `"Transfer of R$ 120.00 successfully made to Mercadinho da Vila."`

2. The system identifies the transaction, extracts the data, and saves it into your monthly budget.
