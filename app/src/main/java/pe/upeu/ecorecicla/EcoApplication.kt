package pe.upeu.ecorecicla

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import pe.upeu.ecorecicla.backend.web.EcoApiServer
import pe.upeu.ecorecicla.di.appModule

class EcoApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Iniciar servicio backend REST EcoAPI en puerto 8080
        try {
            EcoApiServer.start(port = 8080)
        } catch (_: Exception) {
            // Servidor ya iniciado o puerto ocupado
        }

        startKoin {
            androidLogger()
            androidContext(this@EcoApplication)
            modules(appModule)
        }
    }
}
