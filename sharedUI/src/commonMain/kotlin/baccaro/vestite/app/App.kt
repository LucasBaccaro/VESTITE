package baccaro.vestite.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import baccaro.vestite.app.core.di.coreModule
import baccaro.vestite.app.core.presentation.navigation.AppNavigation
import baccaro.vestite.app.features.authentication.di.authenticationModule
import baccaro.vestite.app.features.wardrobe.di.wardrobeModule
import baccaro.vestite.app.features.weather.di.weatherModule
import baccaro.vestite.app.theme.AppTheme
import com.mmk.kmpauth.google.GoogleAuthCredentials
import com.mmk.kmpauth.google.GoogleAuthProvider
import org.koin.compose.KoinApplication

/**
 * Main application entry point for VESTITE.
 *
 * Initializes Koin dependency injection with all required modules and sets up
 * the application theme and navigation. Also initializes KMPAuth for native Google Sign-In.
 *
 * @param onThemeChanged Callback to update platform-specific theme settings
 */
@Composable
fun App(
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {}
) {
    // Initialize Google Auth Provider before using GoogleButtonUiContainer
    var authReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        GoogleAuthProvider.create(
            credentials = GoogleAuthCredentials(
                serverId = BuildConfig.GOOGLE_WEB_CLIENT_ID
            )
        )
        authReady = true
    }

    // Initialize Koin dependency injection with all feature modules
    KoinApplication(
        application = {
            modules(
                coreModule,
                authenticationModule,
                wardrobeModule,
                weatherModule
            )
        }
    ) {
        AppTheme(onThemeChanged) {
            // Only show app content when Google Auth is initialized
            if (authReady) {
                AppNavigation()
            }
        }
    }
}
