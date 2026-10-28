package pe.upeu.ecorecicla.data.remote.dto

import kotlinx.serialization.Serializable
import pe.upeu.ecorecicla.domain.model.EstadoPesaje
import pe.upeu.ecorecicla.domain.model.Pesaje

@Serializable
data class CrearPesajeDto(
    val puntoId: Long,
    val material: String,
    val pesoBruto: Double,
    val tara: Double,
    val observacion: String? = null
)

@Serializable
data class PesajeDto(
    val id: Long,
    val puntoId: Long,
    val fechaHora: String,
    val material: String,
    val pesoBruto: Double,
    val tara: Double,
    val pesoNeto: Double,
    val operario: String,
    val estadoTipo: String, // "Registrado" | "Anulado"
    val motivoAnulacion: String? = null,
    val observacion: String? = null
) {
    fun toDomain(pendiente: Boolean = false): Pesaje {
        val estado = if (estadoTipo.equals("Anulado", ignoreCase = true)) {
            EstadoPesaje.Anulado(motivoAnulacion ?: "")
        } else {
            EstadoPesaje.Registrado
        }
        return Pesaje(
            id = id,
            puntoId = puntoId,
            fechaHora = fechaHora,
            material = material,
            pesoBruto = pesoBruto,
            tara = tara,
            pesoNeto = pesoNeto,
            operario = operario,
            estado = estado,
            pendiente = pendiente,
            observacion = observacion
        )
    }
}
