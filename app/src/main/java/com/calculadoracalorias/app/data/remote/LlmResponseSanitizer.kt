package com.calculadoracalorias.app.data.remote

/**
 * Utilidad para sanitizar respuestas de modelos de lenguaje grande (LLMs).
 * Elimina etiquetas de razonamiento (como <think>...</think> de DeepSeek R1 o Qwen)
 * y bloques de código Markdown (```json ... ```) para extraer JSON válido.
 */
object LlmResponseSanitizer {
    private val THINK_TAG_REGEX = Regex("<think>[\\s\\S]*?</think>", RegexOption.IGNORE_CASE)
    private val THOUGHT_TAG_REGEX = Regex("<thought>[\\s\\S]*?</thought>", RegexOption.IGNORE_CASE)

    fun sanitizeJsonResponse(rawContent: String): String {
        val withoutThinking = rawContent
            .replace(THINK_TAG_REGEX, "")
            .replace(THOUGHT_TAG_REGEX, "")
            .trim()

        val withoutFences = withoutThinking
            .removePrefix("```json")
            .removePrefix("```JSON")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val jsonStartIndex = withoutFences.indexOf('{')
        val jsonEndIndex = withoutFences.lastIndexOf('}')

        return if (jsonStartIndex >= 0 && jsonEndIndex > jsonStartIndex) {
            withoutFences.substring(jsonStartIndex, jsonEndIndex + 1).trim()
        } else {
            withoutFences
        }
    }
}
