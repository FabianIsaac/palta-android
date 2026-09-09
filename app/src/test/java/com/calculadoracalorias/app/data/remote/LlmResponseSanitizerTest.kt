package com.calculadoracalorias.app.data.remote

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class LlmResponseSanitizerTest {

    @Test
    @DisplayName("Debe limpiar etiquetas de razonamiento think en minusculas y mayusculas")
    fun testStripThinkTags() {
        val input = """
            <think>
            Razonando en varios renglones...
            Paso 1: analizar calorias.
            </think>
            {"name": "test"}
        """.trimIndent()

        val output = LlmResponseSanitizer.sanitizeJsonResponse(input)
        assertEquals("{\"name\": \"test\"}", output)
    }

    @Test
    @DisplayName("Debe limpiar bloques markdown ```json y ```")
    fun testStripMarkdownFences() {
        val input = """
            ```json
            {"key": "value"}
            ```
        """.trimIndent()

        val output = LlmResponseSanitizer.sanitizeJsonResponse(input)
        assertEquals("{\"key\": \"value\"}", output)
    }

    @Test
    @DisplayName("Debe extraer solo el contenido entre llaves cuando hay texto introductorio")
    fun testExtractJsonWithIntroText() {
        val input = """
            Hola! Aquí está el desglose solicitado:
            {"items": []}
            Espero que te sirva.
        """.trimIndent()

        val output = LlmResponseSanitizer.sanitizeJsonResponse(input)
        assertEquals("{\"items\": []}", output)
    }
}
