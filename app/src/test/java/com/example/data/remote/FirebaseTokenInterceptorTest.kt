package com.example.data.remote

import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class FirebaseTokenInterceptorTest {
    private class RecordingChain : Interceptor.Chain {
        var sent: Request? = null
        private val original = Request.Builder().url("https://example.test/api/v1/documents/extract")
            .header("Authorization", "Bearer stale").build()
        override fun request() = original
        override fun proceed(request: Request): Response {
            sent = request
            return Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body("{}".toResponseBody()).build()
        }
        override fun connection(): Connection? = null
        override fun call(): Call = OkHttpClient().newCall(original)
        override fun connectTimeoutMillis() = 1000
        override fun readTimeoutMillis() = 1000
        override fun writeTimeoutMillis() = 1000
        override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
    }

    @Test fun replacesAnyExistingAuthorizationWithCurrentFirebaseToken() {
        val chain = RecordingChain()
        FirebaseTokenInterceptor { "fresh-token" }.intercept(chain).close()
        assertEquals(listOf("Bearer fresh-token"), chain.sent!!.headers.values("Authorization"))
    }
    @Test fun neverSendsAnUnauthenticatedRequest() {
        val chain = RecordingChain()
        assertThrows(SessionAuthenticationException::class.java) { FirebaseTokenInterceptor { "" }.intercept(chain) }
        assertNull(chain.sent)
    }
    @Test fun fetchesTheCurrentAccountTokenForEveryRequest() {
        var token = "user-a"
        val interceptor = FirebaseTokenInterceptor { token }
        val first = RecordingChain(); interceptor.intercept(first).close()
        token = "user-b"
        val second = RecordingChain(); interceptor.intercept(second).close()
        assertEquals("Bearer user-a", first.sent!!.header("Authorization"))
        assertEquals("Bearer user-b", second.sent!!.header("Authorization"))
    }
}
