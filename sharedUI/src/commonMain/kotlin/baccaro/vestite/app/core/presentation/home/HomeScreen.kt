package baccaro.vestite.app.core.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import baccaro.vestite.app.features.authentication.domain.repository.AuthRepository
import baccaro.vestite.app.features.authentication.domain.usecase.SignOutUseCase
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Pantalla principal después del login
 * Aquí puedes agregar el contenido principal de tu app
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    authRepository: AuthRepository = koinInject(),
    signOutUseCase: SignOutUseCase = koinInject()
) {
    val currentUser by authRepository.currentUser.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    var isLoggingOut by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VESTITE") },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                isLoggingOut = true
                                signOutUseCase()
                                isLoggingOut = false
                                onLogout()
                            }
                        },
                        enabled = !isLoggingOut
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cerrar sesión"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            currentUser?.let { user ->
                Text(
                    text = "¡Bienvenido!",
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Usuario: ${user.username ?: "Sin nombre"}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Email: ${user.email}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Autenticación exitosa ✅",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            } ?: run {
                CircularProgressIndicator()
            }
        }
    }
}
