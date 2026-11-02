package com.telesales.assistant.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BaseUrlNormalizerTest {
    @Test
    fun trimsWhitespaceAndAddsTrailingSlash() {
        assertEquals(
            "http://10.0.2.2:8000/api/v1/",
            BaseUrlNormalizer.normalizeOrNull("  http://10.0.2.2:8000/api/v1  "),
        )
    }

    @Test
    fun keepsExistingTrailingSlash() {
        assertEquals(
            "https://api.example.test/api/v1/",
            BaseUrlNormalizer.normalizeOrNull("https://api.example.test/api/v1/"),
        )
    }

    @Test
    fun rejectsMissingOrUnsupportedScheme() {
        assertNull(BaseUrlNormalizer.normalizeOrNull("api.example.test/api/v1"))
        assertNull(BaseUrlNormalizer.normalizeOrNull("ftp://api.example.test"))
    }

    @Test
    fun rejectsMalformedUrlAndQuery() {
        assertNull(BaseUrlNormalizer.normalizeOrNull("http://"))
        assertNull(BaseUrlNormalizer.normalizeOrNull("https://api.example.test/api/v1?token=secret"))
    }
}
