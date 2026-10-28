package pe.upeu.ecorecicla.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.upeu.ecorecicla.domain.model.Sesion

interface AuthRepository {
    suspend fun login(usuario: String, contrasena: String): Result<Sesion>
    suspend fun getMe(): Result<Sesion>
    fun getSesionFlow(): Flow<Sesion?>
    suspend fun getSesionActual(): Sesion?
    suspend fun logout()
}
