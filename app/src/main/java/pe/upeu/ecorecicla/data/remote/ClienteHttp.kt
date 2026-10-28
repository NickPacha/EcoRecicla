package pe.upeu.ecorecicla.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import pe.upeu.ecorecicla.data.local.AlmacenSeguroAndroid
import pe.upeu.ecorecicla.data.remote.dto.ErrorResponseDto
import pe.upeu.ecorecicla.domain.model.ErrorApi

class ClienteHttp(private val almacenSeguro: AlmacenSeguroAndroid) {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }

        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.BODY
        }

        install(DefaultRequest) {
            contentType(ContentType.Application.Json)
            val token = almacenSeguro.obtenerToken()
            if (!token.isNullOrBlank()) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }

    suspend fun parseErrorResponse(response: HttpResponse): ErrorApi {
        val statusCode = response.status
        val responseBody = try {
            response.bodyAsText()
        } catch (_: Exception) {
            ""
        }

        val errorDto = try {
            json.decodeFromString<ErrorResponseDto>(responseBody)
        } catch (_: Exception) {
            null
        }

        val message = errorDto?.message ?: when (statusCode) {
            HttpStatusCode.BadRequest -> "Datos inválidos en la petición."
            HttpStatusCode.Unauthorized -> "No autorizado. Su sesión puede haber expirado."
            HttpStatusCode.Forbidden -> "No tiene privilegios suficientes para realizar esta acción."
            HttpStatusCode.NotFound -> "Recurso no encontrado."
            HttpStatusCode.Conflict -> "Conflicto con el estado actual del recurso."
            HttpStatusCode.InternalServerError -> "Error interno del servidor."
            else -> "Error en la comunicación con el servidor ($statusCode)."
        }

        return when (statusCode) {
            HttpStatusCode.BadRequest -> ErrorApi.BadRequest(
                message = message,
                validationErrors = errorDto?.validationErrors ?: emptyMap()
            )
            HttpStatusCode.Unauthorized -> ErrorApi.Unauthorized(message)
            HttpStatusCode.Forbidden -> ErrorApi.Forbidden(message)
            HttpStatusCode.NotFound -> ErrorApi.NotFound(message)
            HttpStatusCode.Conflict -> ErrorApi.Conflict(message)
            HttpStatusCode.InternalServerError -> ErrorApi.ServerError(message)
            else -> ErrorApi.UnknownError(message)
        }
    }

    companion object {
        const val BASE_URL = "http://10.0.2.2:8080/api/v1"
    }
}
