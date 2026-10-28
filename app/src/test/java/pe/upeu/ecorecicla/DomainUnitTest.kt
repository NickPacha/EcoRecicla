package pe.upeu.ecorecicla

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.upeu.ecorecicla.domain.model.ErrorApi
import pe.upeu.ecorecicla.domain.model.EstadoPesaje

class DomainUnitTest {

    @Test
    fun testCalculoPesoNetoValido() {
        val pesoBruto = 150.0
        val tara = 20.0
        val pesoNeto = pesoBruto - tara

        assertEquals(130.0, pesoNeto, 0.001)
        assertTrue(pesoNeto > 0)
    }

    @Test
    fun testOfflinePesajeIdNegativo() {
        val minIdActual = -3L
        val nuevoIdProvisional = minIdActual - 1

        assertEquals(-4L, nuevoIdProvisional)
        assertTrue(nuevoIdProvisional < 0)
    }

    @Test
    fun testEstadoPesajeAnulado() {
        val estado: EstadoPesaje = EstadoPesaje.Anulado("Bolsa rota")
        assertTrue(estado is EstadoPesaje.Anulado)
        assertEquals("Bolsa rota", (estado as EstadoPesaje.Anulado).motivo)
    }

    @Test
    fun testErrorApiBadRequest() {
        val error = ErrorApi.BadRequest("Datos inválidos", mapOf("pesoBruto" to "Debe ser positivo"))
        assertEquals("Datos inválidos", error.message)
        assertEquals("Debe ser positivo", error.validationErrors["pesoBruto"])
    }
}
