const axios = require('axios')

const createAxios = () => {
    return axios.create({
        baseURL: process.env.TIMELESS_API_URL || 'http://localhost:8080'
    })
}

module.exports = createAxios()