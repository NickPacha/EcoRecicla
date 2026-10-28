package pe.upeu.ecorecicla.data.repository

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.upeu.ecorecicla.data.local.AlmacenSeguroAndroid
import pe.upeu.ecorecicla.data.local.EcoDatabase
import pe.upeu.ecorecicla.data.remote.ClienteHttp
import pe.upeu.ecorecicla.data.remote.dto.LoginRequestDto
import pe.upeu.ecorecicla.data.remote.dto.LoginResponseDto
import pe.upeu.ecorecicla.domain.model.ErrorApi
import pe.upeu.ecorecicla.domain.model.Sesion
import pe.upeu.ecorecicla.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val clienteHttp: ClienteHttp,
    private val almacenSeguro: AlmacenSeguroAndroid,
    private val database: EcoDatabase
) : AuthRepository {

    private val _sesionState = MutableStateFlow<Sesion?>(cargarSesionGuardada())

    private fun cargarSesionGuardada(): Sesion? {
        val token = almacenSeguro.obtenerToken() ?: return null
        val usuario = almacenSeguro.obtenerUsuario() ?: ""
        val rol = almacenSeguro.obtenerRol() ?: ""
        val privilegios = almacenSeguro.obtenerPrivilegios()
        return Sesion(token, usuario, rol, privilegios)
    }

    override suspend fun login(usuario: String, contrasena: String): Result<Sesion> {
        return try {
            val response = clienteHttp.httpClient.post("${ClienteHttp.BASE_URL}/auth/login") {
                setBody(LoginRequestDto(usuario, contrasena))
            }
            if (response.status.isSuccess()) {
                val bodyText = response.bodyAsText()
                val loginResponse = clienteHttp.json.decodeFromString<LoginResponseDto>(bodyText)
                val sesion = loginResponse.toDomain()

                almacenSeguro.guardarToken(sesion.token)
                almacenSeguro.guardarUsuario(sesion.usuario)
                almacenSeguro.guardarRol(sesion.rol)
                almacenSeguro.guardarPrivilegios(sesion.privilegios)

                _sesionState.value = sesion
                Result.success(sesion)
            } else {
                Result.failure(clienteHttp.parseErrorResponse(response))
            }
        } catch (e: Exception) {
            Result.failure(if (e is ErrorApi) e else ErrorApi.NetworkError(e.message ?: "Error de red al iniciar sesión"))
        }
    }

    override suspend fun getMe(): Result<Sesion> {
        return try {
            val response = clienteHttp.httpClient.get("${ClienteHttp.BASE_URL}/auth/me")
            if (response.status.isSuccess()) {
                val bodyText = response.bodyAsText()
                val meResponse = clienteHttp.json.decodeFromString<LoginResponseDto>(bodyText)
                val sesion = meResponse.toDomain()

                almacenSeguro.guardarUsuario(sesion.usuario)
                almacenSeguro.guardarRol(sesion.rol)
                almacenSeguro.guardarPrivilegios(sesion.privilegios)

                _sesionState.value = sesion
                Result.success(sesion)
            } else {
                Result.failure(clienteHttp.parseErrorResponse(response))
            }
        } catch (e: Exception) {
            Result.failure(if (e is ErrorApi) e else ErrorApi.NetworkError(e.message ?: "Error de red al obtener usuario"))
        }
    }

    override fun getSesionFlow(): Flow<Sesion?> = _sesionState.asStateFlow()

    override suspend fun getSesionActual(): Sesion? = _sesionState.value ?: cargarSesionGuardada()

    override suspend fun logout() {
        almacenSeguro.limpiar()
        database.ecoDatabaseQueries.clearAllPesajes()
        database.ecoDatabaseQueries.clearPuntos()
        database.ecoDatabaseQueries.clearRuta()
        _sesionState.value = null
    }
}
