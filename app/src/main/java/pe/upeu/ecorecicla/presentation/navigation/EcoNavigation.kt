package pe.upeu.ecorecicla.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.koin.androidx.compose.koinViewModel
import pe.upeu.ecorecicla.presentation.detalle.DetallePuntoScreen
import pe.upeu.ecorecicla.presentation.detalle.DetallePuntoViewModel
import pe.upeu.ecorecicla.presentation.listado.RutaDiaScreen
import pe.upeu.ecorecicla.presentation.listado.RutaDiaViewModel
import pe.upeu.ecorecicla.presentation.perfil.PerfilScreen
import pe.upeu.ecorecicla.presentation.perfil.PerfilViewModel
import pe.upeu.ecorecicla.presentation.sesion.LoginScreen
import pe.upeu.ecorecicla.presentation.sesion.LoginViewModel

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object RutaDia : Screen("ruta_dia")
    data object DetallePunto : Screen("detalle_punto/{puntoId}") {
        fun createRoute(puntoId: Long) = "detalle_punto/$puntoId"
    }
    data object Perfil : Screen("perfil")
}

@Composable
fun EcoNavigation(
    startDestination: String = Screen.Login.route
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            val viewModel: LoginViewModel = koinViewModel()
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.RutaDia.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.RutaDia.route) {
            val viewModel: RutaDiaViewModel = koinViewModel()
            RutaDiaScreen(
                viewModel = viewModel,
                onPuntoSelected = { puntoId ->
                    navController.navigate(Screen.DetallePunto.createRoute(puntoId))
                },
                onPerfilClick = {
                    navController.navigate(Screen.Perfil.route)
                }
            )
        }

        composable(
            route = Screen.DetallePunto.route,
            arguments = listOf(navArgument("puntoId") { type = NavType.LongType })
        ) { backStackEntry ->
            val puntoId = backStackEntry.arguments?.getLong("puntoId") ?: 0L
            val viewModel: DetallePuntoViewModel = koinViewModel()
            DetallePuntoScreen(
                puntoId = puntoId,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Perfil.route) {
            val viewModel: PerfilViewModel = koinViewModel()
            PerfilScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onLogoutSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
