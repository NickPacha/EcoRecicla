package pe.upeu.ecorecicla.domain.usecase

import pe.upeu.ecorecicla.domain.repository.PesajeRepository
import pe.upeu.ecorecicla.domain.repository.RutaRepository

class SincronizarUseCase(
    private val pesajeRepository: PesajeRepository,
    private val rutaRepository: RutaRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return try {
            // 1° Subida de registros pendientes al backend
            val resultSyncPesajes = pesajeRepository.syncPesajes()
            if (resultSyncPesajes.isFailure) {
                return resultSyncPesajes
            }
            // 2° Descarga y reemplazo transaccional de datos remotos
            val resultSyncRuta = rutaRepository.syncRutaHoy()
            if (resultSyncRuta.isFailure) {
                return Result.failure(resultSyncRuta.exceptionOrNull() ?: Exception("Error al sincronizar la ruta"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
