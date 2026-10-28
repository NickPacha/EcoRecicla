package pe.upeu.ecorecicla.backend.web

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import pe.upeu.ecorecicla.backend.config.JwtService
import pe.upeu.ecorecicla.backend.servicio.EcoApiService
import pe.upeu.ecorecicla.data.remote.dto.CrearPesajeDto
import pe.upeu.ecorecicla.data.remote.dto.ErrorResponseDto
import pe.upeu.ecorecicla.data.remote.dto.LoginRequestDto
import pe.upeu.ecorecicla.domain.model.ErrorApi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EcoApiServer {

    private val jwtService = JwtService()
    private val ecoApiService = EcoApiService(jwtService)

    fun start(port: Int = 8080) {
        embeddedServer(CIO, port = port) {
            configureServer()
        }.start(wait = false)
    }

    private fun Application.configureServer() {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = true
            })
        }

        install(StatusPages) {
            exception<ErrorApi> { call, cause ->
                val status = when (cause) {
                    is ErrorApi.BadRequest -> HttpStatusCode.BadRequest
                    is ErrorApi.Unauthorized -> HttpStatusCode.Unauthorized
                    is ErrorApi.Forbidden -> HttpStatusCode.Forbidden
                    is ErrorApi.NotFound -> HttpStatusCode.NotFound
                    is ErrorApi.Conflict -> HttpStatusCode.Conflict
                    is ErrorApi.ServerError -> HttpStatusCode.InternalServerError
                    else -> HttpStatusCode.InternalServerError
                }

                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val validationErrors = (cause as? ErrorApi.BadRequest)?.validationErrors

                call.respond(
                    status,
                    ErrorResponseDto(
                        timestamp = isoFormat.format(Date()),
                        status = status.value,
                        error = status.description,
                        message = cause.message ?: "Ocurrió un error en la solicitud.",
                        path = call.request.local.uri,
                        validationErrors = validationErrors
                    )
                )
            }

            exception<Throwable> { call, cause ->
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponseDto(
                        timestamp = isoFormat.format(Date()),
                        status = HttpStatusCode.InternalServerError.value,
                        error = HttpStatusCode.InternalServerError.description,
                        message = cause.message ?: "Error interno del servidor.",
                        path = call.request.local.uri
                    )
                )
            }
        }

        routing {
            route("/api/v1") {
                post("/auth/login") {
                    val request = call.receive<LoginRequestDto>()
                    val response = ecoApiService.login(request.usuario, request.contrasena)
                    call.respond(HttpStatusCode.OK, response)
                }

                get("/auth/me") {
                    val (usuario, _, _) = extractUserContext(call.request.headers["Authorization"])
                    val response = ecoApiService.getMe(usuario)
                    call.respond(HttpStatusCode.OK, response)
                }

                get("/rutas/hoy") {
                    val (usuario, _, privilegios) = extractUserContext(call.request.headers["Authorization"])
                    val response = ecoApiService.getRutaHoy(usuario, privilegios)
                    call.respond(HttpStatusCode.OK, response)
                }

                post("/rutas/{id}/cerrar") {
                    val rutaId = call.parameters["id"]?.toLongOrNull()
                        ?: throw ErrorApi.BadRequest("ID de ruta inválido.")
                    val (usuario, _, privilegios) = extractUserContext(call.request.headers["Authorization"])
                    val response = ecoApiService.cerrarRuta(rutaId, usuario, privilegios)
                    call.respond(HttpStatusCode.OK, response)
                }

                get("/puntos") {
                    val (_, _, privilegios) = extractUserContext(call.request.headers["Authorization"])
                    val zona = call.request.queryParameters["zona"]
                    val pagina = call.request.queryParameters["pagina"]?.toIntOrNull() ?: 0
                    val tamanio = call.request.queryParameters["tamanio"]?.toIntOrNull() ?: 20

                    val response = ecoApiService.getPuntosPaginado(zona, pagina, tamanio, privilegios)
                    call.respond(HttpStatusCode.OK, response)
                }

                get("/puntos/{id}/pesajes") {
                    val puntoId = call.parameters["id"]?.toLongOrNull()
                        ?: throw ErrorApi.BadRequest("ID de punto inválido.")
                    val (_, _, privilegios) = extractUserContext(call.request.headers["Authorization"])
                    val pagina = call.request.queryParameters["pagina"]?.toIntOrNull() ?: 0
                    val tamanio = call.request.queryParameters["tamanio"]?.toIntOrNull() ?: 20

                    val response = ecoApiService.getPesajesByPuntoPaginado(puntoId, pagina, tamanio, privilegios)
                    call.respond(HttpStatusCode.OK, response)
                }

                post("/pesajes") {
                    val (usuario, _, privilegios) = extractUserContext(call.request.headers["Authorization"])
                    val dto = call.receive<CrearPesajeDto>()
                    val response = ecoApiService.registrarPesaje(dto, usuario, privilegios)
                    call.respond(HttpStatusCode.Created, response)
                }

                put("/pesajes/{id}") {
                    val pesajeId = call.parameters["id"]?.toLongOrNull()
                        ?: throw ErrorApi.BadRequest("ID de pesaje inválido.")
                    val (usuario, rol, privilegios) = extractUserContext(call.request.headers["Authorization"])
                    val dto = call.receive<CrearPesajeDto>()
                    val response = ecoApiService.corregirPesaje(pesajeId, dto, usuario, rol, privilegios)
                    call.respond(HttpStatusCode.OK, response)
                }

                delete("/pesajes/{id}") {
                    val pesajeId = call.parameters["id"]?.toLongOrNull()
                        ?: throw ErrorApi.BadRequest("ID de pesaje inválido.")
                    val motivo = call.request.queryParameters["motivo"]
                    val (usuario, _, privilegios) = extractUserContext(call.request.headers["Authorization"])
                    val response = ecoApiService.anularPesaje(pesajeId, motivo, usuario, privilegios)
                    call.respond(HttpStatusCode.OK, response)
                }
            }
        }
    }

    private fun extractUserContext(authHeader: String?): Triple<String, String, Set<String>> {
        if (authHeader.isNullOrBlank() || !authHeader.startsWith("Bearer ")) {
            throw ErrorApi.Unauthorized("Token JWT no proporcionado o formato inválido.")
        }
        val token = authHeader.removePrefix("Bearer ").trim()
        return try {
            val decodedJwt = jwtService.verifier.verify(token)
            val usuario = decodedJwt.subject
            val rol = decodedJwt.getClaim("rol").asString() ?: "OPERARIO"
            val privilegios = decodedJwt.getClaim("privilegios").asList(String::class.java)?.toSet() ?: emptySet()
            Triple(usuario, rol, privilegios)
        } catch (_: Exception) {
            throw ErrorApi.Unauthorized("Token JWT expirado o firma inválida.")
        }
    }
}
