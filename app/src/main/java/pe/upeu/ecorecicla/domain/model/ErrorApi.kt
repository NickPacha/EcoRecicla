package pe.upeu.ecorecicla.domain.model

sealed class ErrorApi : Exception() {
    data class BadRequest(
        override val message: String,
        val validationErrors: Map<String, String> = emptyMap()
    ) : ErrorApi()

    data class Unauthorized(
        override val message: String = "No autorizado. Inicie sesión nuevamente."
    ) : ErrorApi()

    data class Forbidden(
        override val message: String = "No tiene privilegios suficientes para realizar esta acción."
    ) : ErrorApi()

    data class NotFound(
        override val message: String = "Recurso no encontrado."
    ) : ErrorApi()

    data class Conflict(
        override val message: String
    ) : ErrorApi()

    data class ServerError(
        override val message: String = "Error interno del servidor."
    ) : ErrorApi()

    data class NetworkError(
        override val message: String = "Sin conexión a internet. Verifique su red."
    ) : ErrorApi()

    data class UnknownError(
        override val message: String = "Ocurrió un error inesperado."
    ) : ErrorApi()
}
