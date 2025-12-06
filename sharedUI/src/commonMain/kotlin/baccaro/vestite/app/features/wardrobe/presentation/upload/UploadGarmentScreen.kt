package baccaro.vestite.app.features.wardrobe.presentation.upload

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import baccaro.vestite.app.core.util.rememberImagePicker
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.memory.MemoryCache
import coil3.request.ImageRequest
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoilApi::class)
@Composable
fun UploadGarmentScreen(
    onNavigateBack: () -> Unit,
    viewModel: UploadGarmentViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Image picker launcher
    val imagePicker = rememberImagePicker { imageBytes, fileName ->
        if (imageBytes != null && fileName != null) {
            viewModel.analyzeGarment(imageBytes, fileName)
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    LaunchedEffect(state.success) {
        if (state.success) {
            snackbarHostState.showSnackbar("Prenda guardada exitosamente")
            viewModel.clearSuccess()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.showPreview) "Confirmar Prenda" else "Agregar Prenda") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // Loading: Analyzing with AI
                state.isAnalyzing -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Analizando prenda con IA...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                // Loading: Uploading to server
                state.isUploading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Guardando prenda...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                // Show preview after analysis
                state.showPreview -> {
                    PreviewScreen(
                        state = state,
                        onCategorySelected = viewModel::selectCategory,
                        onSave = viewModel::saveGarment
                    )
                }

                // Initial screen: Select image
                else -> {
                    InitialScreen(
                        onGalleryClick = { imagePicker.launchGallery() },
                        onCameraClick = { imagePicker.launchCamera() }
                    )
                }
            }
        }
    }
}

/**
 * Initial screen: Image selection
 */
@Composable
private fun InitialScreen(
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Sube una foto de tu prenda",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "La IA la analizará y podrás categorizarla",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Gallery button
        OutlinedButton(
            onClick = onGalleryClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("📁 Seleccionar desde Galería")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Camera button
        OutlinedButton(
            onClick = onCameraClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("📷 Tomar Foto")
        }
    }
}

/**
 * Preview screen: Show analyzed image with AI results and category selector
 */
@OptIn(ExperimentalCoilApi::class)
@Composable
private fun PreviewScreen(
    state: UploadGarmentState,
    onCategorySelected: (String) -> Unit,
    onSave: () -> Unit
) {
    val context = LocalPlatformContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .build()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Image preview
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            state.analyzedImageBytes?.let { bytes ->
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(bytes)
                        .build(),
                    contentDescription = "Prenda analizada",
                    imageLoader = imageLoader,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // AI Analysis results
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Análisis IA",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = state.aiDescription ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Category selection
        Text(
            text = "Selecciona la categoría",
            style = MaterialTheme.typography.titleMedium
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 0.dp)
        ) {
            items(state.categories) { category ->
                FilterChip(
                    selected = state.selectedCategoryId == category.id,
                    onClick = { onCategorySelected(category.id) },
                    label = { Text(category.displayName) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Save button
        Button(
            onClick = onSave,
            enabled = state.selectedCategoryId != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar Prenda")
        }
    }
}
