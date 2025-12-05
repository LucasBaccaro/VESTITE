package baccaro.vestite.app.core.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import baccaro.vestite.app.features.authentication.domain.repository.AuthRepository
import org.koin.compose.koinInject

/**
 * Home screen - Main landing page after login
 * Displays user information and quick actions
 *
 * Features:
 * - Profile icon in TopAppBar (top-right) navigates to Profile screen
 * - FAB (+) for quick garment upload
 * - Chat assistant button for AI styling assistance
 *
 * Note: This screen is now inside MainScreen's Scaffold, so it doesn't need its own Scaffold.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToUpload: () -> Unit,
    onNavigateToChatAssistant: () -> Unit,
    authRepository: AuthRepository = koinInject()
) {
    val currentUser by authRepository.currentUser.collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VESTITE") },
                actions = {
                    // Profile icon button
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Perfil",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToUpload) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar prenda"
                )
            }
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

                Spacer(modifier = Modifier.height(32.dp))

                // Button to open chat assistant
                OutlinedButton(
                    onClick = onNavigateToChatAssistant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Chat con Asistente IA")
                }
            } ?: run {
                CircularProgressIndicator()
            }
        }
    }
}
