package pe.upeu.ecorecicla.backend.servicio

import pe.upeu.ecorecicla.backend.config.JwtService
import pe.upeu.ecorecicla.backend.dominio.PesajeBackend
import pe.upeu.ecorecicla.backend.repositorio.BackendDataStore
import pe.upeu.ecorecicla.data.remote.dto.CrearPesajeDto
import pe.upeu.ecorecicla.data.remote.dto.LoginResponseDto
import pe.upeu.ecorecicla.data.remote.dto.PaginaDto
import pe.upeu.ecorecicla.data.remote.dto.PesajeDto
import pe.upeu.ecorecicla.data.remote.dto.PuntoAcopioDto
import pe.upeu.ecorecicla.data.remote.dto.RutaDiaDto
import pe.upeu.ecorecicla.domain.model.ErrorApi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EcoApiService(
    private val jwtService: JwtService = JwtService()
) {

    fun login(usuario: String, contrasena: String): LoginResponseDto {
        val user = BackendDataStore.usuarios.find { it.usuario == usuario && it.contrasena == contrasena }
            ?: throw ErrorApi.Unauthorized("Credenciales inválidas. Verifique usuario y contraseña.")

        val privilegios = user.rol.privilegios
        val token = jwtService.generarToken(user.usuario, user.rol.nombre, privilegios)

        return LoginResponseDto(
            token = token,
            usuario = user.usuario,
            rol = user.rol.nombre,
            privilegios = privilegios.toList()
        )
    }

    fun getMe(usuario: String): LoginResponseDto {
        val user = BackendDataStore.usuarios.find { it.usuario == usuario }
            ?: throw ErrorApi.NotFound("Usuario $usuario no encontrado.")

        val token = jwtService.generarToken(user.usuario, user.rol.nombre, user.rol.privilegios)

        return LoginResponseDto(
            token = token,
            usuario = user.usuario,
            rol = user.rol.nombre,
            privilegios = user.rol.privilegios.toList()
        )
    }

    fun getRutaHoy(usuario: String, privilegios: Set<String>): RutaDiaDto {
        verificarPrivilegio(privilegios, "PUNTO_LEER")

        val rutaHoy = BackendDataStore.rutas.find { it.estado == "abierta" }
            ?: BackendDataStore.rutas.firstOrNull()
            ?: throw ErrorApi.NotFound("No existe ninguna ruta asignada para el día de hoy.")

        return RutaDiaDto(
            id = rutaHoy.id,
            fecha = rutaHoy.fecha,
            estado = rutaHoy.estado,
            puntos = rutaHoy.puntos.map { p ->
                val tienePesajes = BackendDataStore.pesajes.any { it.puntoId == p.id && it.estadoTipo != "Anulado" }
                PuntoAcopioDto(
                    id = p.id,
                    razonSocial = p.razonSocial,
                    direccion = p.direccion,
                    zona = p.zona,
                    materialHabitual = p.materialHabitual,
                    secuencia = p.secuencia,
                    atendido = p.atendido || tienePesajes
                )
            }
        )
    }

    fun getPuntosPaginado(zona: String?, pagina: Int, tamanio: Int, privilegios: Set<String>): PaginaDto<PuntoAcopioDto> {
        verificarPrivilegio(privilegios, "PUNTO_LEER")

        val filtrados = if (!zona.isNullOrBlank()) {
            BackendDataStore.puntos.filter { it.zona.equals(zona, ignoreCase = true) }
        } else {
            BackendDataStore.puntos
        }

        val totalElementos = filtrados.size.toLong()
        val totalPaginas = if (totalElementos == 0L) 1 else kotlin.math.ceil(totalElementos.toDouble() / tamanio).toInt()
        val inicio = (pagina * tamanio).coerceAtMost(filtrados.size)
        val fin = ((pagina + 1) * tamanio).coerceAtMost(filtrados.size)

        val subList = if (inicio < filtrados.size) filtrados.subList(inicio, fin) else emptyList()

        val dtos = subList.map { p ->
            PuntoAcopioDto(
                id = p.id,
                razonSocial = p.razonSocial,
                direccion = p.direccion,
                zona = p.zona,
                materialHabitual = p.materialHabitual,
                secuencia = p.secuencia,
                atendido = p.atendido
            )
        }

        return PaginaDto(
            contenido = dtos,
            pagina = pagina,
            tamanio = tamanio,
            totalElementos = totalElementos,
            totalPaginas = totalPaginas,
            ultima = pagina >= totalPaginas - 1
        )
    }

    fun getPesajesByPuntoPaginado(puntoId: Long, pagina: Int, tamanio: Int, privilegios: Set<String>): PaginaDto<PesajeDto> {
        verificarPrivilegio(privilegios, "PESAJE_LEER")

        val filtrados = BackendDataStore.pesajes.filter { it.puntoId == puntoId }
        val totalElementos = filtrados.size.toLong()
        val totalPaginas = if (totalElementos == 0L) 1 else kotlin.math.ceil(totalElementos.toDouble() / tamanio).toInt()
        val inicio = (pagina * tamanio).coerceAtMost(filtrados.size)
        val fin = ((pagina + 1) * tamanio).coerceAtMost(filtrados.size)

        val subList = if (inicio < filtrados.size) filtrados.subList(inicio, fin) else emptyList()

        val dtos = subList.map { p ->
            PesajeDto(
                id = p.id,
                puntoId = p.puntoId,
                fechaHora = p.fechaHora,
                material = p.material,
                pesoBruto = p.pesoBruto,
                tara = p.tara,
                pesoNeto = p.pesoNeto,
                operario = p.operario,
                estadoTipo = p.estadoTipo,
                motivoAnulacion = p.motivoAnulacion,
                observacion = p.observacion
            )
        }

        return PaginaDto(
            contenido = dtos,
            pagina = pagina,
            tamanio = tamanio,
            totalElementos = totalElementos,
            totalPaginas = totalPaginas,
            ultima = pagina >= totalPaginas - 1
        )
    }

    fun registrarPesaje(dto: CrearPesajeDto, usuarioActual: String, privilegios: Set<String>): PesajeDto {
        verificarPrivilegio(privilegios, "PESAJE_CREAR")

        // Validar RN-01 (Ruta Cerrada Inmutable)
        val rutaPadre = BackendDataStore.rutas.find { r -> r.puntos.any { it.id == dto.puntoId } }
        if (rutaPadre != null && rutaPadre.estado == "cerrada") {
            throw ErrorApi.Conflict("RN-01: La ruta del día se encuentra CERRADA y no permite registrar nuevos pesajes.")
        }

        // Validar RN-03 (Cálculo Obligatorio de Peso Neto > 0)
        val pesoNetoCalculado = dto.pesoBruto - dto.tara
        if (pesoNetoCalculado <= 0) {
            throw ErrorApi.BadRequest(
                message = "RN-03: El peso neto debe ser estrictamente mayor que cero (Bruto: ${dto.pesoBruto}, Tara: ${dto.tara}).",
                validationErrors = mapOf("pesoBruto" to "El peso bruto debe ser mayor que la tara")
            )
        }

        val nuevoId = BackendDataStore.generarSiguientePesajeId()
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val fechaHoraIso = isoFormat.format(Date())

        val nuevoPesaje = PesajeBackend(
            id = nuevoId,
            puntoId = dto.puntoId,
            fechaHora = fechaHoraIso,
            material = dto.material,
            pesoBruto = dto.pesoBruto,
            tara = dto.tara,
            pesoNeto = pesoNetoCalculado,
            operario = usuarioActual,
            estadoTipo = "Registrado",
            motivoAnulacion = null,
            observacion = dto.observacion
        )

        BackendDataStore.pesajes.add(0, nuevoPesaje)

        // Marcar punto como atendido
        BackendDataStore.puntos.find { it.id == dto.puntoId }?.atendido = true

        return PesajeDto(
            id = nuevoPesaje.id,
            puntoId = nuevoPesaje.puntoId,
            fechaHora = nuevoPesaje.fechaHora,
            material = nuevoPesaje.material,
            pesoBruto = nuevoPesaje.pesoBruto,
            tara = nuevoPesaje.tara,
            pesoNeto = nuevoPesaje.pesoNeto,
            operario = nuevoPesaje.operario,
            estadoTipo = nuevoPesaje.estadoTipo,
            motivoAnulacion = nuevoPesaje.motivoAnulacion,
            observacion = nuevoPesaje.observacion
        )
    }

    fun corregirPesaje(id: Long, dto: CrearPesajeDto, usuarioActual: String, rolActual: String, privilegios: Set<String>): PesajeDto {
        verificarPrivilegio(privilegios, "PESAJE_EDITAR")

        val pesaje = BackendDataStore.pesajes.find { it.id == id }
            ?: throw ErrorApi.NotFound("Pesaje con ID $id no encontrado.")

        // Validar RN-01 (Ruta Cerrada Inmutable)
        val rutaPadre = BackendDataStore.rutas.find { r -> r.puntos.any { it.id == pesaje.puntoId } }
        if (rutaPadre != null && rutaPadre.estado == "cerrada") {
            throw ErrorApi.Conflict("RN-01: La ruta asignada se encuentra CERRADA y no permite modificaciones.")
        }

        // Validar RN-02 (Límite de Modificación para Operarios)
        if (rolActual.equals("OPERARIO", ignoreCase = true) && !pesaje.operario.equals(usuarioActual, ignoreCase = true)) {
            throw ErrorApi.Forbidden("RN-02: Un operario únicamente puede modificar o corregir los pesajes registrados por su propio usuario.")
        }

        // Validar RN-03
        val pesoNetoCalculado = dto.pesoBruto - dto.tara
        if (pesoNetoCalculado <= 0) {
            throw ErrorApi.BadRequest("RN-03: El peso neto debe ser estrictamente mayor que cero.")
        }

        val idx = BackendDataStore.pesajes.indexOf(pesaje)
        val actualizado = pesaje.copy(
            material = dto.material,
            pesoBruto = dto.pesoBruto,
            tara = dto.tara,
            pesoNeto = pesoNetoCalculado,
            observacion = dto.observacion
        )

        BackendDataStore.pesajes[idx] = actualizado

        return PesajeDto(
            id = actualizado.id,
            puntoId = actualizado.puntoId,
            fechaHora = actualizado.fechaHora,
            material = actualizado.material,
            pesoBruto = actualizado.pesoBruto,
            tara = actualizado.tara,
            pesoNeto = actualizado.pesoNeto,
            operario = actualizado.operario,
            estadoTipo = actualizado.estadoTipo,
            motivoAnulacion = actualizado.motivoAnulacion,
            observacion = actualizado.observacion
        )
    }

    fun anularPesaje(id: Long, motivo: String?, usuarioActual: String, privilegios: Set<String>): PesajeDto {
        verificarPrivilegio(privilegios, "PESAJE_ANULAR")

        val pesaje = BackendDataStore.pesajes.find { it.id == id }
            ?: throw ErrorApi.NotFound("Pesaje con ID $id no encontrado.")

        // Validar RN-01
        val rutaPadre = BackendDataStore.rutas.find { r -> r.puntos.any { it.id == pesaje.puntoId } }
        if (rutaPadre != null && rutaPadre.estado == "cerrada") {
            throw ErrorApi.Conflict("RN-01: La ruta asignada se encuentra CERRADA y no se pueden anular pesajes.")
        }

        pesaje.estadoTipo = "Anulado"
        pesaje.motivoAnulacion = motivo ?: "Anulado por el usuario $usuarioActual"

        return PesajeDto(
            id = pesaje.id,
            puntoId = pesaje.puntoId,
            fechaHora = pesaje.fechaHora,
            material = pesaje.material,
            pesoBruto = pesaje.pesoBruto,
            tara = pesaje.tara,
            pesoNeto = pesaje.pesoNeto,
            operario = pesaje.operario,
            estadoTipo = pesaje.estadoTipo,
            motivoAnulacion = pesaje.motivoAnulacion,
            observacion = pesaje.observacion
        )
    }

    fun cerrarRuta(rutaId: Long, usuarioActual: String, privilegios: Set<String>): RutaDiaDto {
        verificarPrivilegio(privilegios, "RUTA_CERRAR")

        val ruta = BackendDataStore.rutas.find { it.id == rutaId }
            ?: throw ErrorApi.NotFound("Ruta con ID $rutaId no encontrada.")

        // Validar RN-04 (Restricción de Cierre de Ruta)
        val puntosPendientes = ruta.puntos.filter { punto ->
            BackendDataStore.pesajes.none { it.puntoId == punto.id && it.estadoTipo != "Anulado" }
        }

        if (puntosPendientes.isNotEmpty()) {
            throw ErrorApi.Conflict(
                "RN-04: No se puede cerrar la ruta ID $rutaId. Existen ${puntosPendientes.size} puntos de acopio pendientes de pesaje."
            )
        }

        ruta.estado = "cerrada"

        return RutaDiaDto(
            id = ruta.id,
            fecha = ruta.fecha,
            estado = ruta.estado,
            puntos = ruta.puntos.map { p ->
                PuntoAcopioDto(
                    id = p.id,
                    razonSocial = p.razonSocial,
                    direccion = p.direccion,
                    zona = p.zona,
                    materialHabitual = p.materialHabitual,
                    secuencia = p.secuencia,
                    atendido = true
                )
            }
        )
    }

    private fun verificarPrivilegio(privilegios: Set<String>, privilegioExigido: String) {
        if (!privilegios.contains(privilegioExigido)) {
            throw ErrorApi.Forbidden("Acceso Denegado: No posee el privilegio requerido [$privilegioExigido].")
        }
    }
}
