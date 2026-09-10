package com.calculadoracalorias.app.data.remote

import com.calculadoracalorias.app.domain.model.AiCallLogEntry
import com.calculadoracalorias.app.domain.model.AiCallType
import com.calculadoracalorias.app.domain.model.AiProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class AiDebugLogManagerTest {

    @Test
    @DisplayName("Debe registrar llamadas y emitir reactivamente en el StateFlow")
    fun testLogRetentionAndFlowEmission() {
        val manager = AiDebugLogManager(maxEntries = 5)
        assertEquals(0, manager.logs.value.size)

        val entry1 = AiCallLogEntry(
            callType = AiCallType.MEAL_TEXT,
            provider = AiProvider.NVIDIA_NIM,
            model = "llama-3.3-70b",
            endpointUrl = "https://integrate.api.nvidia.com/v1/chat/completions",
            promptSummary = "marraqueta con palta",
            httpStatus = 200,
            durationMs = 350,
            isSuccess = true,
            rawResponse = """{"choices":[]}"""
        )

        manager.log(entry1)

        val currentLogs = manager.logs.value
        assertEquals(1, currentLogs.size)
        assertEquals("marraqueta con palta", currentLogs[0].promptSummary)
        assertEquals(200, currentLogs[0].httpStatus)
        assertTrue(currentLogs[0].isSuccess)
    }

    @Test
    @DisplayName("Debe aplicar rotación FIFO manteniendo un máximo de registros")
    fun testFifoBufferRotation() {
        val manager = AiDebugLogManager(maxEntries = 3)

        for (i in 1..5) {
            manager.log(
                AiCallLogEntry(
                    callType = AiCallType.MEAL_TEXT,
                    provider = AiProvider.GOOGLE_GEMINI,
                    model = "gemini-1.5-flash",
                    endpointUrl = "https://api.test/completions",
                    promptSummary = "Llamada $i",
                    isSuccess = true
                )
            )
        }

        val logs = manager.logs.value
        assertEquals(3, logs.size)
        // La lista mantiene los más recientes al principio
        assertEquals("Llamada 5", logs[0].promptSummary)
        assertEquals("Llamada 4", logs[1].promptSummary)
        assertEquals("Llamada 3", logs[2].promptSummary)
    }

    @Test
    @DisplayName("Debe ofuscar credenciales sensibles en URL, prompt y respuestas")
    fun testCredentialObfuscation() {
        val manager = AiDebugLogManager(maxEntries = 5)

        val entryWithSensitiveData = AiCallLogEntry(
            callType = AiCallType.MEAL_TEXT,
            provider = AiProvider.NVIDIA_NIM,
            model = "test-model",
            endpointUrl = "https://api.test/v1/chat?key=AIzaSySecretKey1234567890",
            promptSummary = "Prompt con Authorization: Bearer nvapi-supersecret12345678 y sk-1234567890abcdef",
            httpStatus = 401,
            isSuccess = false,
            errorMessage = "Unauthorized with Bearer nvapi-supersecret12345678",
            rawResponse = "Error response nvapi-supersecret12345678"
        )

        manager.log(entryWithSensitiveData)

        val logged = manager.logs.value.first()
        assertFalse(logged.endpointUrl.contains("AIzaSySecretKey1234567890"))
        assertTrue(logged.endpointUrl.contains("key=***"))

        assertFalse(logged.promptSummary.contains("nvapi-supersecret12345678"))
        assertFalse(logged.promptSummary.contains("sk-1234567890abcdef"))
        assertTrue(logged.promptSummary.contains("Bearer ***"))
        assertTrue(logged.promptSummary.contains("sk-***"))

        assertFalse(logged.errorMessage!!.contains("nvapi-supersecret12345678"))
        assertTrue(logged.errorMessage!!.contains("Bearer ***"))

        assertFalse(logged.rawResponse!!.contains("nvapi-supersecret12345678"))
        assertTrue(logged.rawResponse!!.contains("nvapi-***"))
    }

    @Test
    @DisplayName("Debe vaciar el buffer al llamar a clear")
    fun testClearLogs() {
        val manager = AiDebugLogManager(maxEntries = 5)
        manager.log(
            AiCallLogEntry(
                callType = AiCallType.CONNECTIVITY_TEST,
                provider = AiProvider.MINIMAX,
                model = "MiniMax-M2.7-highspeed",
                endpointUrl = "https://api.minimaxi.chat",
                promptSummary = "Ping",
                isSuccess = true
            )
        )
        assertEquals(1, manager.logs.value.size)

        manager.clear()
        assertEquals(0, manager.logs.value.size)
    }
}
