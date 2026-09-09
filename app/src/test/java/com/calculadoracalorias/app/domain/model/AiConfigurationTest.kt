package com.calculadoracalorias.app.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AiConfigurationTest {

    @Test
    fun `nvidia nim defaults are properly resolved`() {
        val config = AiConfiguration(provider = AiProvider.NVIDIA_NIM, apiKey = "nvapi-test")

        assertEquals("https://integrate.api.nvidia.com/v1/chat/completions", config.effectiveEndpointUrl)
        assertEquals("meta/llama-3.3-70b-instruct", config.effectiveTextModel)
        assertEquals("meta/llama-3.2-11b-vision-instruct", config.effectiveVisionModel)
        assertEquals("nvapi-test", config.apiKey)
        assertTrue(config.provider.isCloud)
    }

    @Test
    fun `gemini defaults are properly resolved`() {
        val config = AiConfiguration(provider = AiProvider.GOOGLE_GEMINI, apiKey = "AIzaSy...")

        assertEquals("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions", config.effectiveEndpointUrl)
        assertEquals("gemini-1.5-flash", config.effectiveTextModel)
        assertEquals("gemini-1.5-flash", config.effectiveVisionModel)
        assertTrue(config.provider.isCloud)
    }

    @Test
    fun `minimax defaults are properly resolved`() {
        val config = AiConfiguration(provider = AiProvider.MINIMAX, apiKey = "mm-key")

        assertEquals("https://api.minimaxi.chat/v1/chat/completions", config.effectiveEndpointUrl)
        assertEquals("MiniMax-Text-01", config.effectiveTextModel)
        assertEquals("MiniMax-M3", config.effectiveVisionModel)
        assertTrue(config.provider.isCloud)
    }

    @Test
    fun `local provider is not cloud`() {
        val config = AiConfiguration(provider = AiProvider.LOCAL)

        assertEquals("", config.effectiveEndpointUrl)
        assertEquals("", config.effectiveTextModel)
        assertEquals("", config.effectiveVisionModel)
        assertFalse(config.provider.isCloud)
    }

    @Test
    fun `custom provider uses custom values when provided`() {
        val config = AiConfiguration(
            provider = AiProvider.CUSTOM,
            apiKey = "sk-custom",
            customEndpointUrl = "http://192.168.1.50:11434/v1/chat/completions",
            customTextModel = "llama3:latest",
            customVisionModel = "llava:latest"
        )

        assertEquals("http://192.168.1.50:11434/v1/chat/completions", config.effectiveEndpointUrl)
        assertEquals("llama3:latest", config.effectiveTextModel)
        assertEquals("llava:latest", config.effectiveVisionModel)
    }

    @Test
    fun `custom provider falls back to defaults when custom fields are blank`() {
        val config = AiConfiguration(
            provider = AiProvider.CUSTOM,
            apiKey = "sk-custom",
            customEndpointUrl = "   ",
            customTextModel = "",
            customVisionModel = "  "
        )

        assertEquals("https://api.openai.com/v1/chat/completions", config.effectiveEndpointUrl)
        assertEquals("gpt-4o-mini", config.effectiveTextModel)
        assertEquals("gpt-4o-mini", config.effectiveVisionModel)
    }

    @Test
    fun `fromId recovers correct provider case-insensitively or defaults to NVIDIA`() {
        assertEquals(AiProvider.NVIDIA_NIM, AiProvider.fromId("nvidia"))
        assertEquals(AiProvider.GOOGLE_GEMINI, AiProvider.fromId("GEMINI"))
        assertEquals(AiProvider.MINIMAX, AiProvider.fromId("minimax"))
        assertEquals(AiProvider.LOCAL, AiProvider.fromId("local"))
        assertEquals(AiProvider.CUSTOM, AiProvider.fromId("custom"))
        assertEquals(AiProvider.NVIDIA_NIM, AiProvider.fromId("unknown_provider"))
    }
}
