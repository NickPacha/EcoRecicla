package pe.upeu.ecorecicla.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.upeu.ecorecicla.domain.model.Pesaje

interface PesajeRepository {
    fun getPesajesByPuntoFlow(puntoId: Long): Flow<List<Pesaje>>
    suspend fun registrarPesaje(
        puntoId: Long,
        material: String,
        pesoBruto: Double,
        tara: Double,
        observacion: String? = null
    ): Result<Pesaje>

    suspend fun corregirPesaje(
        id: Long,
        material: String,
        pesoBruto: Double,
        tara: Double,
        observacion: String? = null
    ): Result<Pesaje>

    suspend fun anularPesaje(id: Long, motivo: String): Result<Unit>
    suspend fun getPesajesPendientes(): List<Pesaje>
    suspend fun syncPesajes(): Result<Unit>
}
