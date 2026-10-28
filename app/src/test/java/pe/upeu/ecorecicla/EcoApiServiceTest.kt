package pe.upeu.ecorecicla

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pe.upeu.ecorecicla.backend.repositorio.BackendDataStore
import pe.upeu.ecorecicla.backend.servicio.EcoApiService
import pe.upeu.ecorecicla.data.remote.dto.CrearPesajeDto
import pe.upeu.ecorecicla.domain.model.ErrorApi

class EcoApiServiceTest {

    private lateinit var ecoApiService: EcoApiService

    @Before
    fun setUp() {
        BackendDataStore.resetData()
        ecoApiService = EcoApiService()
    }

    @Test
    fun testLoginCorrectoRetornaJwtYPrivilegios() {
        val response = ecoApiService.login("operario1", "Eco2026*")

        assertNotNull(response.token)
        assertEquals("operario1", response.usuario)
        assertEquals("OPERARIO", response.rol)
        assertTrue(response.privilegios.contains("PESAJE_CREAR"))
    }

    @Test(expected = ErrorApi.Unauthorized::class)
    fun testLoginIncorrectoLanzaUnauthorized() {
        ecoApiService.login("operario1", "PasswordIncorrecta")
    }

    @Test(expected = ErrorApi.BadRequest::class)
    fun testRN03CalculoPesoNetoInvalidoLanzaBadRequest() {
        val dtoInvalido = CrearPesajeDto(
            puntoId = 101,
            material = "Plástico PET",
            pesoBruto = 10.0,
            tara = 15.0, // Peso neto = -5.0 <= 0
            observacion = "Prueba RN-03"
        )
        ecoApiService.registrarPesaje(dtoInvalido, "operario1", setOf("PESAJE_CREAR"))
    }

    @Test
    fun testRN03CalculoPesoNetoValidoCalculadoPorServidor() {
        val dtoValido = CrearPesajeDto(
            puntoId = 101,
            material = "Plástico PET",
            pesoBruto = 120.0,
            tara = 20.0, // Peso neto = 100.0
            observacion = "Prueba RN-03 OK"
        )
        val pesaje = ecoApiService.registrarPesaje(dtoValido, "operario1", setOf("PESAJE_CREAR"))

        assertEquals(100.0, pesaje.pesoNeto, 0.001)
        assertEquals("Registrado", pesaje.estadoTipo)
    }

    @Test(expected = ErrorApi.Conflict::class)
    fun testRN01RutaCerradaInmutableLanzaConflict() {
        // Punto 301 pertenece a la ruta 2 que está CERRADA
        val dto = CrearPesajeDto(
            puntoId = 301,
            material = "Vidrio",
            pesoBruto = 50.0,
            tara = 10.0,
            observacion = "Intento en ruta cerrada"
        )
        ecoApiService.registrarPesaje(dto, "operario1", setOf("PESAJE_CREAR"))
    }

    @Test(expected = ErrorApi.Forbidden::class)
    fun testRN02OperarioNoPuedeEditarPesajeDeOtroOperario() {
        // Pesaje ID 6 pertenece al operario2
        val dtoEdit = CrearPesajeDto(
            puntoId = 202,
            material = "Plástico HDPE",
            pesoBruto = 150.0,
            tara = 15.0,
            observacion = "Edición no autorizada"
        )
        ecoApiService.corregirPesaje(
            id = 6,
            dto = dtoEdit,
            usuarioActual = "operario1", // operario1 intenta editar pesaje de operario2
            rolActual = "OPERARIO",
            privilegios = setOf("PESAJE_EDITAR")
        )
    }

    @Test(expected = ErrorApi.Conflict::class)
    fun testRN04CerrarRutaConPuntosPendientesLanzaConflict() {
        // Ruta 1 tiene puntos pendientes sin atender
        ecoApiService.cerrarRuta(
            rutaId = 1,
            usuarioActual = "supervisor1",
            privilegios = setOf("RUTA_CERRAR")
        )
    }
}
