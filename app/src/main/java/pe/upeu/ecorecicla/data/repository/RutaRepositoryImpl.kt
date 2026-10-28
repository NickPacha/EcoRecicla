package pe.upeu.ecorecicla.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.upeu.ecorecicla.data.local.EcoDatabase
import pe.upeu.ecorecicla.data.remote.ClienteHttp
import pe.upeu.ecorecicla.data.remote.dto.RutaDiaDto
import pe.upeu.ecorecicla.domain.model.ErrorApi
import pe.upeu.ecorecicla.domain.model.PuntoAcopio
import pe.upeu.ecorecicla.domain.model.RutaDia
import pe.upeu.ecorecicla.domain.repository.RutaRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RutaRepositoryImpl(
    private val clienteHttp: ClienteHttp,
    private val database: EcoDatabase
) : RutaRepository {

    private val queries = database.ecoDatabaseQueries

    override fun getRutaHoyFlow(): Flow<RutaDia?> {
        val rutaFlow = queries.getRutaHoy().asFlow().mapToOneOrNull(Dispatchers.IO)

        return rutaFlow.map { rutaEntity ->
            if (rutaEntity == null) null
            else {
                val puntos = queries.selectAllPuntos().executeAsList().map { entity ->
                    PuntoAcopio(
                        id = entity.id,
                        razonSocial = entity.razonSocial,
                        direccion = entity.direccion,
                        zona = entity.zona,
                        materialHabitual = entity.materialHabitual,
                        secuencia = entity.secuencia,
                        atendido = entity.atendido == 1L
                    )
                }
                RutaDia(
                    id = rutaEntity.id,
                    fecha = rutaEntity.fecha,
                    estado = rutaEntity.estado,
                    puntos = puntos,
                    ultimaSincronizacion = rutaEntity.ultimaSincronizacion
                )
            }
        }
    }

    override fun getPuntosFlow(): Flow<List<PuntoAcopio>> {
        return queries.selectAllPuntos().asFlow().mapToList(Dispatchers.IO).map { entities ->
            entities.map { entity ->
                PuntoAcopio(
                    id = entity.id,
                    razonSocial = entity.razonSocial,
                    direccion = entity.direccion,
                    zona = entity.zona,
                    materialHabitual = entity.materialHabitual,
                    secuencia = entity.secuencia,
                    atendido = entity.atendido == 1L
                )
            }
        }
    }

    override suspend fun syncRutaHoy(): Result<RutaDia> {
        return try {
            val response = clienteHttp.httpClient.get("${ClienteHttp.BASE_URL}/rutas/hoy")
            if (response.status.isSuccess()) {
                val bodyText = response.bodyAsText()
                val rutaDto = clienteHttp.json.decodeFromString<RutaDiaDto>(bodyText)
                val fechaHoraActual = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

                database.transaction {
                    queries.clearPuntos()
                    queries.clearRuta()

                    queries.insertRuta(
                        id = rutaDto.id,
                        fecha = rutaDto.fecha,
                        estado = rutaDto.estado,
                        ultimaSincronizacion = fechaHoraActual
                    )

                    rutaDto.puntos.forEach { puntoDto ->
                        queries.insertPunto(
                            id = puntoDto.id,
                            razonSocial = puntoDto.razonSocial,
                            direccion = puntoDto.direccion,
                            zona = puntoDto.zona,
                            materialHabitual = puntoDto.materialHabitual,
                            secuencia = puntoDto.secuencia,
                            atendido = if (puntoDto.atendido) 1L else 0L
                        )
                    }
                }

                val rutaDomain = rutaDto.toDomain().copy(ultimaSincronizacion = fechaHoraActual)
                Result.success(rutaDomain)
            } else {
                Result.failure(clienteHttp.parseErrorResponse(response))
            }
        } catch (e: Exception) {
            Result.failure(if (e is ErrorApi) e else ErrorApi.NetworkError(e.message ?: "Error al sincronizar la ruta"))
        }
    }

    override suspend fun cerrarRuta(rutaId: Long): Result<Unit> {
        return try {
            val response = clienteHttp.httpClient.post("${ClienteHttp.BASE_URL}/rutas/$rutaId/cerrar")
            if (response.status.isSuccess()) {
                queries.updateEstadoRuta(estado = "cerrada", id = rutaId)
                Result.success(Unit)
            } else {
                Result.failure(clienteHttp.parseErrorResponse(response))
            }
        } catch (e: Exception) {
            Result.failure(if (e is ErrorApi) e else ErrorApi.NetworkError(e.message ?: "Error al cerrar la ruta"))
        }
    }
}
