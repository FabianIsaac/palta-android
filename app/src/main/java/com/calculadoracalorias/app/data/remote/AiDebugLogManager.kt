package com.calculadoracalorias.app.data.remote

import com.calculadoracalorias.app.domain.model.AiCallLogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val MAX_ENTRIES_DEFAULT = 50

/**
 * Gestor en memoria de registros de diagnóstico técnico de IA.
 * Mantiene un buffer circular FIFO de hasta [maxEntries] elementos expuesto en un [StateFlow].
 * Ofusca automáticamente credenciales y tokens sensibles antes de almacenar.
 */
open class AiDebugLogManager(
    private val maxEntries: Int = MAX_ENTRIES_DEFAULT
) {
    private val _logs = MutableStateFlow<List<AiCallLogEntry>>(emptyList())
    val logs: StateFlow<List<AiCallLogEntry>> = _logs.asStateFlow()

    private val lock = Any()

    fun log(entry: AiCallLogEntry) {
        val sanitized = sanitizeEntry(entry)
        synchronized(lock) {
            val current = _logs.value
            val updated = (listOf(sanitized) + current).take(maxEntries)
            _logs.value = updated
        }
    }

    fun clear() {
        synchronized(lock) {
            _logs.value = emptyList()
        }
    }

    companion object : AiDebugLogManager(50) {
        const val MAX_ENTRIES = 50

        private val BEARER_REGEX = Regex("""(?i)\b(Bearer\s+)[A-Za-z0-9_\-\.]{6,}""")
        private val URL_KEY_REGEX = Regex("""(?i)([?&](?:key|api_key|token|access_token)=)[^&]+""")
        private val NVAPI_KEY_REGEX = Regex("""\bnvapi-[A-Za-z0-9_\-]{8,}""")
        private val OPENAI_KEY_REGEX = Regex("""\bsk-[A-Za-z0-9_\-]{8,}""")
        private val GEMINI_KEY_REGEX = Regex("""\bAIza[0-9A-Za-z\-_]{20,}""")

        fun sanitizeText(text: String?): String? {
            if (text == null) return null
            var sanitized = text
            sanitized = BEARER_REGEX.replace(sanitized, "$1***")
            sanitized = URL_KEY_REGEX.replace(sanitized, "$1***")
            sanitized = NVAPI_KEY_REGEX.replace(sanitized, "nvapi-***")
            sanitized = OPENAI_KEY_REGEX.replace(sanitized, "sk-***")
            sanitized = GEMINI_KEY_REGEX.replace(sanitized, "AIza***")
            return sanitized
        }

        fun sanitizeEntry(entry: AiCallLogEntry): AiCallLogEntry {
            return entry.copy(
                endpointUrl = sanitizeText(entry.endpointUrl) ?: "",
                promptSummary = sanitizeText(entry.promptSummary) ?: "",
                rawResponse = sanitizeText(entry.rawResponse),
                errorMessage = sanitizeText(entry.errorMessage)
            )
        }
    }
}
