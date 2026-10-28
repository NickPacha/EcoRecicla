package pe.upeu.ecorecicla

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import org.koin.android.ext.android.inject
import pe.upeu.ecorecicla.data.local.AlmacenSeguroAndroid
import pe.upeu.ecorecicla.presentation.navigation.EcoNavigation
import pe.upeu.ecorecicla.presentation.navigation.Screen
import pe.upeu.ecorecicla.presentation.theme.EcoReciclaTheme

class MainActivity : ComponentActivity() {

    private val almacenSeguro: AlmacenSeguroAndroid by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val token = almacenSeguro.obtenerToken()
        val startDestination = if (!token.isNullOrBlank()) {
            Screen.RutaDia.route
        } else {
            Screen.Login.route
        }

        setContent {
            EcoReciclaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EcoNavigation(startDestination = startDestination)
                }
            }
        }
    }
}
