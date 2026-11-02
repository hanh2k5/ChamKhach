package com.telesales.assistant.data.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object BaseUrlNormalizer {
    fun normalizeOrNull(input: String): String? {
        val trimmed = input.trim()
        if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return null
        }

        val parsed = trimmed.toHttpUrlOrNull() ?: return null
        if (parsed.scheme != "http" && parsed.scheme != "https") return null
        if (parsed.username.isNotEmpty() || parsed.password.isNotEmpty()) return null
        if (parsed.encodedQuery != null || parsed.encodedFragment != null) return null

        val path = parsed.encodedPath
        return if (path.endsWith('/')) {
            parsed.toString()
        } else {
            parsed.newBuilder().encodedPath("$path/").build().toString()
        }
    }
}
