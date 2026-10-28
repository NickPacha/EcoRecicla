package pe.upeu.ecorecicla.backend.seguridad

data class Privilegio(
    val nombre: String
)

data class Rol(
    val nombre: String,
    val privilegios: Set<String>
)

data class UsuarioBackend(
    val id: Long,
    val usuario: String,
    val contrasena: String,
    val nombreCompleto: String,
    val rol: Rol
)
