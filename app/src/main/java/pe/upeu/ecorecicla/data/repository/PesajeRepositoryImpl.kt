package pe.upeu.ecorecicla.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.upeu.ecorecicla.data.local.AlmacenSeguroAndroid
import pe.upeu.ecorecicla.data.local.EcoDatabase
import pe.upeu.ecorecicla.data.remote.ClienteHttp
import pe.upeu.ecorecicla.data.remote.dto.CrearPesajeDto
import pe.upeu.ecorecicla.data.remote.dto.PesajeDto
import pe.upeu.ecorecicla.domain.model.ErrorApi
import pe.upeu.ecorecicla.domain.model.EstadoPesaje
import pe.upeu.ecorecicla.domain.model.Pesaje
import pe.upeu.ecorecicla.domain.repository.PesajeRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PesajeRepositoryImpl(
    private val clienteHttp: ClienteHttp,
    private val almacenSeguro: AlmacenSeguroAndroid,
    private val database: EcoDatabase
) : PesajeRepository {

    private val queries = database.ecoDatabaseQueries

    override fun getPesajesByPuntoFlow(puntoId: Long): Flow<List<Pesaje>> {
        return queries.getPesajesByPunto(puntoId).asFlow().mapToList(Dispatchers.IO).map { entities ->
            entities.map { entity ->
                val estado = if (entity.estadoTipo.equals("Anulado", ignoreCase = true)) {
                    EstadoPesaje.Anulado(entity.motivoAnulacion ?: "")
                } else {
                    EstadoPesaje.Registrado
                }
                Pesaje(
                    id = entity.id,
                    puntoId = entity.puntoId,
                    fechaHora = entity.fechaHora,
                    material = entity.material,
                    pesoBruto = entity.pesoBruto,
                    tara = entity.tara,
                    pesoNeto = entity.pesoNeto,
                    operario = entity.operario,
                    estado = estado,
                    pendiente = entity.pendiente == 1L,
                    observacion = entity.observacion
                )
            }
        }
    }

    override suspend fun registrarPesaje(
        puntoId: Long,
        material: String,
        pesoBruto: Double,
        tara: Double,
        observacion: String?
    ): Result<Pesaje> {
        val pesoNetoCalculado = pesoBruto - tara
        if (pesoNetoCalculado <= 0) {
            return Result.failure(
                ErrorApi.BadRequest("El peso neto debe ser mayor que cero (Peso Bruto: $pesoBruto, Tara: $tara).")
            )
        }

        val usuarioActual = almacenSeguro.obtenerUsuario() ?: "operario1"
        val fechaHoraIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())

        return try {
            val response = clienteHttp.httpClient.post("${ClienteHttp.BASE_URL}/pesajes") {
                setBody(CrearPesajeDto(puntoId, material, pesoBruto, tara, observacion))
            }

            if (response.status.isSuccess()) {
                val bodyText = response.bodyAsText()
                val pesajeDto = clienteHttp.json.decodeFromString<PesajeDto>(bodyText)
                val pesajeDomain = pesajeDto.toDomain(pendiente = false)

                database.transaction {
                    queries.insertPesaje(
                        id = pesajeDomain.id,
                        puntoId = pesajeDomain.puntoId,
                        fechaHora = pesajeDomain.fechaHora,
                        material = pesajeDomain.material,
                        pesoBruto = pesajeDomain.pesoBruto,
                        tara = pesajeDomain.tara,
                        pesoNeto = pesajeDomain.pesoNeto,
                        operario = pesajeDomain.operario,
                        estadoTipo = "Registrado",
                        motivoAnulacion = null,
                        pendiente = 0L,
                        observacion = pesajeDomain.observacion
                    )
                    queries.updatePuntoAtendido(atendido = 1L, id = puntoId)
                }

                Result.success(pesajeDomain)
            } else {
                Result.failure(clienteHttp.parseErrorResponse(response))
            }
        } catch (e: Exception) {
            val minId = queries.getMinId().executeAsOne() ?: -1L
            val pesajeLocal = Pesaje(
                id = minId,
                puntoId = puntoId,
                fechaHora = fechaHoraIso,
                material = material,
                pesoBruto = pesoBruto,
                tara = tara,
                pesoNeto = pesoNetoCalculado,
                operario = usuarioActual,
                estado = EstadoPesaje.Registrado,
                pendiente = true,
                observacion = observacion
            )

            database.transaction {
                queries.insertPesaje(
                    id = pesajeLocal.id,
                    puntoId = pesajeLocal.puntoId,
                    fechaHora = pesajeLocal.fechaHora,
                    material = pesajeLocal.material,
                    pesoBruto = pesajeLocal.pesoBruto,
                    tara = pesajeLocal.tara,
                    pesoNeto = pesajeLocal.pesoNeto,
                    operario = pesajeLocal.operario,
                    estadoTipo = "Registrado",
                    motivoAnulacion = null,
                    pendiente = 1L,
                    observacion = pesajeLocal.observacion
                )
                queries.updatePuntoAtendido(atendido = 1L, id = puntoId)
            }

            Result.success(pesajeLocal)
        }
    }

    override suspend fun corregirPesaje(
        id: Long,
        material: String,
        pesoBruto: Double,
        tara: Double,
        observacion: String?
    ): Result<Pesaje> {
        val pesoNetoCalculado = pesoBruto - tara
        if (pesoNetoCalculado <= 0) {
            return Result.failure(
                ErrorApi.BadRequest("El peso neto debe ser mayor que cero.")
            )
        }

        return try {
            val response = clienteHttp.httpClient.put("${ClienteHttp.BASE_URL}/pesajes/$id") {
                setBody(CrearPesajeDto(0L, material, pesoBruto, tara, observacion))
            }

            if (response.status.isSuccess()) {
                val bodyText = response.bodyAsText()
                val pesajeDto = clienteHttp.json.decodeFromString<PesajeDto>(bodyText)
                val pesajeDomain = pesajeDto.toDomain(pendiente = false)

                queries.insertPesaje(
                    id = pesajeDomain.id,
                    puntoId = pesajeDomain.puntoId,
                    fechaHora = pesajeDomain.fechaHora,
                    material = pesajeDomain.material,
                    pesoBruto = pesajeDomain.pesoBruto,
                    tara = pesajeDomain.tara,
                    pesoNeto = pesajeDomain.pesoNeto,
                    operario = pesajeDomain.operario,
                    estadoTipo = "Registrado",
                    motivoAnulacion = null,
                    pendiente = 0L,
                    observacion = pesajeDomain.observacion
                )

                Result.success(pesajeDomain)
            } else {
                Result.failure(clienteHttp.parseErrorResponse(response))
            }
        } catch (e: Exception) {
            Result.failure(if (e is ErrorApi) e else ErrorApi.NetworkError(e.message ?: "Error de red al corregir pesaje"))
        }
    }

    override suspend fun anularPesaje(id: Long, motivo: String): Result<Unit> {
        return try {
            val response = clienteHttp.httpClient.delete("${ClienteHttp.BASE_URL}/pesajes/$id?motivo=$motivo")
            if (response.status.isSuccess()) {
                queries.anularPesajeLocal(motivo = motivo, pendiente = 0L, id = id)
                Result.success(Unit)
            } else {
                Result.failure(clienteHttp.parseErrorResponse(response))
            }
        } catch (e: Exception) {
            queries.anularPesajeLocal(motivo = motivo, pendiente = 1L, id = id)
            Result.success(Unit)
        }
    }

    override suspend fun getPesajesPendientes(): List<Pesaje> {
        return queries.getPesajesPendientes().executeAsList().map { entity ->
            val estado = if (entity.estadoTipo.equals("Anulado", ignoreCase = true)) {
                EstadoPesaje.Anulado(entity.motivoAnulacion ?: "")
            } else {
                EstadoPesaje.Registrado
            }
            Pesaje(
                id = entity.id,
                puntoId = entity.puntoId,
                fechaHora = entity.fechaHora,
                material = entity.material,
                pesoBruto = entity.pesoBruto,
                tara = entity.tara,
                pesoNeto = entity.pesoNeto,
                operario = entity.operario,
                estado = estado,
                pendiente = true,
                observacion = entity.observacion
            )
        }
    }

    override suspend fun syncPesajes(): Result<Unit> {
        return try {
            val pendientes = getPesajesPendientes()
            for (pesaje in pendientes) {
                if (pesaje.id < 0) {
                    val response = clienteHttp.httpClient.post("${ClienteHttp.BASE_URL}/pesajes") {
                        setBody(
                            CrearPesajeDto(
                                puntoId = pesaje.puntoId,
                                material = pesaje.material,
                                pesoBruto = pesaje.pesoBruto,
                                tara = pesaje.tara,
                                observacion = pesaje.observacion
                            )
                        )
                    }

                    if (response.status.isSuccess()) {
                        val bodyText = response.bodyAsText()
                        val serverPesaje = clienteHttp.json.decodeFromString<PesajeDto>(bodyText)
                        queries.updatePesajeIdAfterSync(
                            nuevoId = serverPesaje.id,
                            nuevoPesoNeto = serverPesaje.pesoNeto,
                            oldId = pesaje.id
                        )
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(if (e is ErrorApi) e else ErrorApi.NetworkError(e.message ?: "Error al sincronizar pesajes"))
        }
    }
}
