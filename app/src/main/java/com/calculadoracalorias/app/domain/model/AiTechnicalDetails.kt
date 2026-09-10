package com.calculadoracalorias.app.domain.model

/**
 * Metadatos técnicos detallados de una interacción con un proveedor de IA.
 */
data class AiTechnicalDetails(
    val provider: AiProvider,
    val model: String,
    val endpointUrl: String,
    val httpStatus: Int? = null,
    val errorBody: String? = null,
    val exceptionMessage: String? = null,
    val durationMs: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Excepción estructurada que preserva los detalles técnicos ante fallos con el proveedor de IA.
 */
class AiServiceException(
    message: String,
    val technicalDetails: AiTechnicalDetails,
    cause: Throwable? = null
) : RuntimeException(message, cause)
