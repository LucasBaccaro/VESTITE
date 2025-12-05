package baccaro.vestite.app.core.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import baccaro.vestite.app.core.presentation.main.MainScreen
import baccaro.vestite.app.features.authentication.domain.repository.AuthRepository
import baccaro.vestite.app.features.authentication.presentation.login.LoginScreen
import baccaro.vestite.app.features.authentication.presentation.register.RegisterScreen
import baccaro.vestite.app.features.wardrobe.presentation.upload.UploadGarmentScreen
import org.koin.compose.koinInject

private const val TAG = "AppNavigation"

/**
 * Application navigation routes.
 *
 * Defines all navigation destinations in the app using a sealed class hierarchy.
 * Each route has a unique string identifier used by Jetpack Navigation.
 *
 * Architecture:
 * - Auth screens: Login, Register
 * - Main screen: Container with BottomBar (Home, Wardrobe, Looks, AI Generation)
 * - Secondary screens: Screens without BottomBar (Profile, Upload, Chat, Detail, etc.)
 */
sealed class Screen(val route: String) {
    // Auth screens
    data object Login : Screen("login")
    data object Register : Screen("register")

    // Main screen with BottomBar
    data object Main : Screen("main")

    // BottomBar destinations (inside Main)
    sealed class BottomBar(route: String) : Screen(route) {
        data object Home : BottomBar("home")
        data object Wardrobe : BottomBar("wardrobe")
        data object Looks : BottomBar("looks")
        data object AIGeneration : BottomBar("ai_generation")
    }

    // Secondary screens (without BottomBar)
    data object Profile : Screen("profile")
    data object UploadGarment : Screen("upload_garment")
    data object ChatAssistant : Screen("chat_assistant")
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

    // Determine initial route based on INITIAL authentication state
    // CRITICAL: Must be stable (not change after first composition) to prevent NavHost recreation
    val startDestination = remember {
        if (isAuthenticated) {
            Screen.Main.route
        } else {
            Screen.Login.route
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ========== AUTH SCREENS ==========

        // Login screen
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // Register screen
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // ========== MAIN SCREEN (with BottomBar) ==========

        // Main screen container with BottomBar
        // Contains: Home, Wardrobe, Looks, AI Generation
        composable(Screen.Main.route) {
            MainScreen(
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToUpload = {
                    navController.navigate(Screen.UploadGarment.route)
                },
                onNavigateToChatAssistant = {
                    navController.navigate(Screen.ChatAssistant.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            )
        }

        // ========== SECONDARY SCREENS (without BottomBar) ==========

        // Profile screen
        composable(Screen.Profile.route) {
            baccaro.vestite.app.features.profile.presentation.ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            )
        }

        // Upload garment screen
        composable(Screen.UploadGarment.route) {
            UploadGarmentScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Chat assistant screen
        composable(Screen.ChatAssistant.route) {
            baccaro.vestite.app.features.chat.presentation.ChatAssistantScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
