package com.calculadoracalorias.app.domain.model

import java.util.UUID

/**
 * Registro de diagnóstico técnico para una interacción con un proveedor de IA.
 */
data class AiCallLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val callType: AiCallType,
    val provider: AiProvider,
    val model: String,
    val endpointUrl: String,
    val promptSummary: String,
    val httpStatus: Int? = null,
    val durationMs: Long = 0L,
    val isSuccess: Boolean,
    val rawResponse: String? = null,
    val errorMessage: String? = null
)
