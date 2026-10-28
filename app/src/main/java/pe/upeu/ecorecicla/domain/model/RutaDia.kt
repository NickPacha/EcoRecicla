package pe.upeu.ecorecicla.domain.model

data class RutaDia(
    val id: Long,
    val fecha: String,
    val estado: String, // "abierta" | "cerrada"
    val puntos: List<PuntoAcopio>,
    val ultimaSincronizacion: String? = null
)
