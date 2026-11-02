package com.telesales.assistant.data.network

interface ApiServiceProvider {
    fun create(baseUrl: String, token: String): ApiService
}
