package pe.upeu.ecorecicla.data.remote.dto

import kotlinx.serialization.Serializable
import pe.upeu.ecorecicla.domain.model.Sesion

@Serializable
data class LoginRequestDto(
    val usuario: String,
    val contrasena: String
)

@Serializable
data class LoginResponseDto(
    val token: String,
    val usuario: String,
    val rol: String,
    val privilegios: List<String> = emptyList()
) {
    fun toDomain(): Sesion = Sesion(
        token = token,
        usuario = usuario,
        rol = rol,
        privilegios = privilegios.toSet()
    )
}
