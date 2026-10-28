package pe.upeu.ecorecicla.backend.dominio

data class PuntoBackend(
    val id: Long,
    val razonSocial: String,
    val direccion: String,
    val zona: String,
    val materialHabitual: String,
    val secuencia: Int,
    var atendido: Boolean = false
)

data class RutaBackend(
    val id: Long,
    val fecha: String,
    var estado: String, // "abierta" | "cerrada"
    val usuarioAsignado: String,
    val puntos: MutableList<PuntoBackend>
)

data class PesajeBackend(
    val id: Long,
    val puntoId: Long,
    val fechaHora: String,
    val material: String,
    val pesoBruto: Double,
    val tara: Double,
    val pesoNeto: Double,
    val operario: String,
    var estadoTipo: String, // "Registrado" | "Anulado"
    var motivoAnulacion: String? = null,
    val observacion: String? = null
)
