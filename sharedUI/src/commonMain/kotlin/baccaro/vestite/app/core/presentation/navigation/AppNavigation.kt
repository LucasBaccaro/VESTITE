package baccaro.vestite.app.core.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import baccaro.vestite.app.features.authentication.domain.repository.AuthRepository
import baccaro.vestite.app.features.authentication.presentation.login.LoginScreen
import baccaro.vestite.app.features.authentication.presentation.register.RegisterScreen
import baccaro.vestite.app.features.wardrobe.presentation.list.WardrobeListScreen
import baccaro.vestite.app.features.wardrobe.presentation.upload.UploadGarmentScreen
import baccaro.vestite.app.core.presentation.home.HomeScreen
import org.koin.compose.koinInject

private const val TAG = "AppNavigation"

/**
 * Application navigation routes.
 *
 * Defines all navigation destinations in the app using a sealed class hierarchy.
 * Each route has a unique string identifier used by Jetpack Navigation.
 */
sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Home : Screen("home")
    data object WardrobeList : Screen("wardrobe_list")
    data object UploadGarment : Screen("upload_garment")
}

/**
 * Main navigation component for the VESTITE application.
 *
 * Manages app-wide navigation and automatically reacts to authentication state changes.
 * Users are automatically redirected between Login and Home screens based on their
 * authentication status, providing a seamless experience for OAuth login flows.
 *
 * Navigation Behavior:
 * - **Not Authenticated**: Shows Login screen, allows navigation to Register
 * - **Authenticated**: Shows Home screen, prevents back navigation to Login
 * - **State Changes**: Automatically navigates when authentication state changes
 *   (e.g., after successful OAuth login or logout)
 *
 * This reactive approach ensures that when a user completes OAuth authentication
 * in a browser and returns to the app, they are automatically navigated to the
 * Home screen without manual intervention.
 *
 * @param navController The navigation controller managing the navigation stack
 * @param authRepository Repository providing authentication state (injected via Koin)
 *
 * @see AuthRepository.isAuthenticated
 */
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    authRepository: AuthRepository = koinInject()
) {
    // Observe authentication state as a Compose state
    val isAuthenticated by authRepository.isAuthenticated.collectAsState(initial = false)

    // Determine initial route based on current authentication state
    val startDestination = if (isAuthenticated) {
        Screen.Home.route
    } else {
        Screen.Login.route
    }
    
    /**
     * Reactive navigation based on authentication state changes.
     *
     * This effect runs whenever [isAuthenticated] changes, ensuring the user is always
     * on the appropriate screen for their authentication status.
     *
     * Use Cases:
     * - User completes OAuth login → Navigates to Home
     * - User logs out → Navigates to Login
     * - Session expires → Navigates to Login
     *
     * The navigation clears the entire back stack to prevent users from navigating
     * back to screens they shouldn't access (e.g., Login when authenticated, or
     * Home when not authenticated).
     */
    LaunchedEffect(isAuthenticated) {

        if (isAuthenticated) {
            // User is authenticated, ensure they're on the Home screen
            if (navController.currentDestination?.route != Screen.Home.route) {
                navController.navigate(Screen.Home.route) {
                    // Clear entire back stack to prevent back navigation to Login
                    popUpTo(0) { inclusive = true }
                }
            }
        } else {
            // User is not authenticated, ensure they're on the Login screen
            if (navController.currentDestination?.route != Screen.Login.route) {
                navController.navigate(Screen.Login.route) {
                    // Clear entire back stack
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Pantalla de Login
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // Pantalla de Registro
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // Pantalla Home
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToWardrobe = {
                    navController.navigate(Screen.WardrobeList.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        // Pantalla de lista de guardarropa
        composable(Screen.WardrobeList.route) {
            WardrobeListScreen(
                onNavigateToUpload = {
                    navController.navigate(Screen.UploadGarment.route)
                }
            )
        }

        // Pantalla de subir prenda
        composable(Screen.UploadGarment.route) {
            UploadGarmentScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
