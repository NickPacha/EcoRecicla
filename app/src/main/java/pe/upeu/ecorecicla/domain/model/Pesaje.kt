package pe.upeu.ecorecicla.domain.model

data class Pesaje(
    val id: Long,            // Negativo mientras esté pendiente de envío
    val puntoId: Long,
    val fechaHora: String,   // ISO-8601
    val material: String,
    val pesoBruto: Double,
    val tara: Double,
    val pesoNeto: Double,    // Lo calcula el servicio
    val operario: String,
    val estado: EstadoPesaje,
    val pendiente: Boolean,
    val observacion: String? = null
)
