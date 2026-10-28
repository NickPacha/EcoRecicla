package pe.upeu.ecorecicla.backend.repositorio

import pe.upeu.ecorecicla.backend.dominio.PesajeBackend
import pe.upeu.ecorecicla.backend.dominio.PuntoBackend
import pe.upeu.ecorecicla.backend.dominio.RutaBackend
import pe.upeu.ecorecicla.backend.seguridad.Rol
import pe.upeu.ecorecicla.backend.seguridad.UsuarioBackend
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

object BackendDataStore {

    val usuarios = mutableListOf<UsuarioBackend>()
    val puntos = mutableListOf<PuntoBackend>()
    val rutas = mutableListOf<RutaBackend>()
    val pesajes = mutableListOf<PesajeBackend>()

    private val idPesajeCounter = AtomicLong(100)

    fun resetData() {
        usuarios.clear()
        puntos.clear()
        rutas.clear()
        pesajes.clear()
        inicializarDatosSemilla()
    }

    fun generarSiguientePesajeId(): Long = idPesajeCounter.incrementAndGet()

    init {
        inicializarDatosSemilla()
    }

    private fun inicializarDatosSemilla() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

        val hoy = dateFormat.format(Date())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val ayer = dateFormat.format(cal.time)

        // 1. Roles y Privilegios
        val rolOperario = Rol("OPERARIO", setOf("PUNTO_LEER", "PESAJE_LEER", "PESAJE_CREAR", "PESAJE_EDITAR"))
        val rolSupervisor = Rol("SUPERVISOR", setOf("PUNTO_LEER", "PESAJE_LEER", "PESAJE_CREAR", "PESAJE_EDITAR", "PESAJE_ANULAR", "RUTA_CERRAR"))
        val rolAdmin = Rol("ADMINISTRADOR", setOf("PUNTO_LEER", "PESAJE_LEER", "PUNTO_GESTIONAR"))

        usuarios.add(UsuarioBackend(1, "operario1", "Eco2026*", "Juan Pérez - Operario", rolOperario))
        usuarios.add(UsuarioBackend(2, "supervisor1", "Eco2026*", "María López - Supervisora", rolSupervisor))
        usuarios.add(UsuarioBackend(3, "admin", "Eco2026*", "Carlos Ramos - Administrador", rolAdmin))
        usuarios.add(UsuarioBackend(4, "operario2", "Eco2026*", "Pedro Gómez - Operario Aux", rolOperario))

        // 2. Mínimo 12 Puntos de Acopio en 3 Zonas
        val p1 = PuntoBackend(101, "Reciclaje Los Olivos S.A.C.", "Av. Universitaria 1234", "Zona Norte", "Plástico PET", 1, true)
        val p2 = PuntoBackend(102, "EkoPlast Independencia", "Calle Los Almendros 456", "Zona Norte", "Cartón / Papel", 2, true)
        val p3 = PuntoBackend(103, "BioVerde Comas", "Av. San Felipe 789", "Zona Norte", "Vidrio", 3, false)
        val p4 = PuntoBackend(104, "EcoResiduos Carabayllo", "Av. Tupac Amaru 321", "Zona Norte", "Metal / Aluminio", 4, false)

        val p5 = PuntoBackend(201, "Recicladora Centro Cercado", "Jr. de la Unión 852", "Zona Centro", "Cartón OND", 1, true)
        val p6 = PuntoBackend(202, "Plásticos Breña S.R.L.", "Av. Brasil 147", "Zona Centro", "Plástico HDPE", 2, true)
        val p7 = PuntoBackend(203, "Acopio La Victoria", "Av. Grau 963", "Zona Centro", "Chatarra Ferrosa", 3, false)
        val p8 = PuntoBackend(204, "EcoLince Recicla", "Av. Arequipa 555", "Zona Centro", "Plástico PET", 4, false)

        val p9 = PuntoBackend(301, "VerdeSur Miraflores", "Av. Larco 111", "Zona Sur", "Papel Blanco", 1, true)
        val p10 = PuntoBackend(302, "Surco Recicla Mas", "Av. Caminos del Inca 222", "Zona Sur", "Vidrio Verde", 2, true)
        val p11 = PuntoBackend(303, "EkoSanJuan S.J.M.", "Av. Los Héroes 333", "Zona Sur", "Plástico Film", 3, false)
        val p12 = PuntoBackend(304, "BioChorrillos", "Av. Huaylas 444", "Zona Sur", "RAEE / Electrónico", 4, false)

        puntos.addAll(listOf(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12))

        // 3. Rutas (1 Abierta para hoy con 6 puntos planificados; 1 Cerrada para ayer)
        val rutaHoy = RutaBackend(
            id = 1,
            fecha = hoy,
            estado = "abierta",
            usuarioAsignado = "operario1",
            puntos = mutableListOf(p1, p2, p3, p4, p5, p6)
        )

        val rutaAyer = RutaBackend(
            id = 2,
            fecha = ayer,
            estado = "cerrada",
            usuarioAsignado = "operario1",
            puntos = mutableListOf(p9, p10)
        )

        rutas.addAll(listOf(rutaHoy, rutaAyer))

        // 4. Mínimo 15 Pesajes Precargados
        pesajes.add(PesajeBackend(1, 101, isoFormat.format(Date()), "Plástico PET", 120.0, 20.0, 100.0, "operario1", "Registrado"))
        pesajes.add(PesajeBackend(2, 101, isoFormat.format(Date()), "Cartón", 85.0, 10.0, 75.0, "operario1", "Registrado"))
        pesajes.add(PesajeBackend(3, 102, isoFormat.format(Date()), "Cartón / Papel", 200.0, 30.0, 170.0, "operario1", "Registrado"))
        pesajes.add(PesajeBackend(4, 102, isoFormat.format(Date()), "Plástico PET", 50.0, 5.0, 45.0, "operario1", "Anulado", "Bolsa contaminada"))
        pesajes.add(PesajeBackend(5, 201, isoFormat.format(Date()), "Cartón OND", 310.0, 40.0, 270.0, "operario1", "Registrado"))

        // Pesaje registrado por OTRO operario (operario2) para validación de RN-02
        pesajes.add(PesajeBackend(6, 202, isoFormat.format(Date()), "Plástico HDPE", 140.0, 15.0, 125.0, "operario2", "Registrado"))

        pesajes.add(PesajeBackend(7, 301, isoFormat.format(cal.time), "Papel Blanco", 90.0, 10.0, 80.0, "operario1", "Registrado"))
        pesajes.add(PesajeBackend(8, 301, isoFormat.format(cal.time), "Vidrio", 180.0, 25.0, 155.0, "operario1", "Registrado"))
        pesajes.add(PesajeBackend(9, 302, isoFormat.format(cal.time), "Vidrio Verde", 220.0, 30.0, 190.0, "operario1", "Registrado"))
        pesajes.add(PesajeBackend(10, 103, isoFormat.format(Date()), "Vidrio", 300.0, 40.0, 260.0, "supervisor1", "Registrado"))
        pesajes.add(PesajeBackend(11, 104, isoFormat.format(Date()), "Aluminio", 70.0, 8.0, 62.0, "operario1", "Registrado"))
        pesajes.add(PesajeBackend(12, 203, isoFormat.format(Date()), "Chatarra", 500.0, 60.0, 440.0, "operario1", "Registrado"))
        pesajes.add(PesajeBackend(13, 204, isoFormat.format(Date()), "Plástico PET", 95.0, 12.0, 83.0, "operario1", "Registrado"))
        pesajes.add(PesajeBackend(14, 303, isoFormat.format(Date()), "Plástico Film", 110.0, 15.0, 95.0, "operario1", "Registrado"))
        pesajes.add(PesajeBackend(15, 304, isoFormat.format(Date()), "RAEE", 160.0, 20.0, 140.0, "operario1", "Registrado"))
    }
}
