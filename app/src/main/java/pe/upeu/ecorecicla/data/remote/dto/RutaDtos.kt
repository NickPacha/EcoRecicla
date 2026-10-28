package pe.upeu.ecorecicla.data.remote.dto

import kotlinx.serialization.Serializable
import pe.upeu.ecorecicla.domain.model.PuntoAcopio
import pe.upeu.ecorecicla.domain.model.RutaDia

@Serializable
data class PuntoAcopioDto(
    val id: Long,
    val razonSocial: String,
    val direccion: String,
    val zona: String,
    val materialHabitual: String,
    val secuencia: Int,
    val atendido: Boolean = false
) {
    fun toDomain(): PuntoAcopio = PuntoAcopio(
        id = id,
        razonSocial = razonSocial,
        direccion = direccion,
        zona = zona,
        materialHabitual = materialHabitual,
        secuencia = secuencia,
        atendido = atendido
    )
}

@Serializable
data class RutaDiaDto(
    val id: Long,
    val fecha: String,
    val estado: String,
    val puntos: List<PuntoAcopioDto> = emptyList()
) {
    fun toDomain(): RutaDia = RutaDia(
        id = id,
        fecha = fecha,
        estado = estado,
        puntos = puntos.map { it.toDomain() }
    )
}
