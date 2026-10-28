package pe.upeu.ecorecicla.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.upeu.ecorecicla.domain.model.PuntoAcopio
import pe.upeu.ecorecicla.domain.model.RutaDia

interface RutaRepository {
    fun getRutaHoyFlow(): Flow<RutaDia?>
    fun getPuntosFlow(): Flow<List<PuntoAcopio>>
    suspend fun cerrarRuta(rutaId: Long): Result<Unit>
    suspend fun syncRutaHoy(): Result<RutaDia>
}
