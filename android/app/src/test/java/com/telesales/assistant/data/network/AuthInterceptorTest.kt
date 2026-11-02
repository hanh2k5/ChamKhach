package com.telesales.assistant.data.network

import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.lang.reflect.Proxy

class AuthInterceptorTest {
    @Test
    fun addsBearerTokenAndAcceptHeader() {
        val request = intercept(token = "test-secret-token")

        assertEquals("Bearer test-secret-token", request.header("Authorization"))
        assertEquals("application/json", request.header("Accept"))
    }

    @Test
    fun removesExistingAuthorizationWhenTokenIsBlank() {
        val request = intercept(
            token = "",
            originalAuthorization = "Bearer stale-token",
        )

        assertNull(request.header("Authorization"))
        assertEquals("application/json", request.header("Accept"))
    }

    private fun intercept(token: String, originalAuthorization: String? = null): Request {
        val originalRequest = Request.Builder()
            .url("https://api.example.test/api/v1/ping")
            .apply {
                originalAuthorization?.let { header("Authorization", it) }
            }
            .build()
        var proceededRequest: Request? = null
        val response = Response.Builder()
            .request(originalRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .build()
        val chain = Proxy.newProxyInstance(
            Interceptor.Chain::class.java.classLoader,
            arrayOf(Interceptor.Chain::class.java),
        ) { _, method, args ->
            when (method.name) {
                "request" -> originalRequest
                "proceed" -> {
                    proceededRequest = args?.get(0) as Request
                    response.newBuilder().request(proceededRequest!!).build()
                }
                else -> error("Unexpected chain method: ${method.name}")
            }
        } as Interceptor.Chain

        AuthInterceptor { token }.intercept(chain)
        return requireNotNull(proceededRequest)
    }
}
