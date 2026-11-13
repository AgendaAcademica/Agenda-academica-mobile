package com.example.ui.viewmodel
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.net.UnknownHostException

class DocumentAnalysisErrorMessageTest {
    private fun quotaError(body: String) = retrofit2.HttpException(
        retrofit2.Response.error<Any>(429, body.toResponseBody("application/json".toMediaType()))
    )

    @Test
    fun `rate limit differs from daily quota and busy service`() {
        val rate = documentAnalysisErrorMessage(quotaError("""{"code":"USER_RATE_LIMIT"}"""))
        org.junit.Assert.assertTrue(rate.contains("1 minuto"))
        assertFalse(rate.contains("diário"))
        for (code in listOf("USER_DAILY_QUOTA", "GLOBAL_DAILY_QUOTA", "USER_AI_QUOTA", "GLOBAL_AI_QUOTA", "USER_COST_LIMIT", "GLOBAL_COST_LIMIT")) {
            val message = documentAnalysisErrorMessage(quotaError("""{"code":"$code"}"""))
            org.junit.Assert.assertTrue(message.contains("diário"))
            org.junit.Assert.assertTrue(message.contains("arquivos continuam selecionados"))
        }
        org.junit.Assert.assertTrue(documentAnalysisErrorMessage(quotaError("""{"code":"SERVER_BUSY"}""")).contains("em andamento"))
        assertFalse(documentAnalysisErrorMessage(quotaError("invalid body")).contains("diário"))
    }
    @Test
    fun `connection error does not expose backend URL`() {
        val backendUrl = "https://private-backend.example.com/api/v1/documents"

        val message = documentAnalysisErrorMessage(UnknownHostException(backendUrl))

        assertEquals(
            "Não foi possível conectar ao serviço de análise. Verifique sua conexão e tente novamente.",
            message
        )
        assertFalse(message.contains(backendUrl))
    }

    @Test
    fun `unexpected error does not expose technical details`() {
        val message = documentAnalysisErrorMessage(
            IllegalStateException("Request failed for https://private-backend.example.com/internal")
        )

        assertEquals(
            "Não foi possível analisar o documento. Verifique o arquivo e tente novamente.",
            message
        )
        assertFalse(message.contains("http"))
    }
}
