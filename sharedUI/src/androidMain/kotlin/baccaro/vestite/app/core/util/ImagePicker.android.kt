package baccaro.vestite.app.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Implementación de Android para seleccionar imágenes
 * Soporta galería (Photo Picker) y cámara
 */
@Composable
actual fun rememberImagePicker(
    onImageSelected: (imageBytes: ByteArray?, fileName: String?) -> Unit
): ImagePickerLauncher {
    val context = LocalContext.current

    // Launcher para galería (Photo Picker - NO requiere permisos)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val imageBytes = uriToByteArray(context, uri)
                val fileName = getFileNameFromUri(context, uri) ?: "gallery_image.jpg"
                onImageSelected(imageBytes, fileName)
            } catch (e: Exception) {
                println("Error reading gallery image: ${e.message}")
                onImageSelected(null, null)
            }
        } else {
            onImageSelected(null, null)
        }
    }

    // Crear URI temporal para la foto de cámara
    val tempImageUri = remember {
        val tempFile = File.createTempFile(
            "camera_image_${System.currentTimeMillis()}",
            ".jpg",
            context.cacheDir
        ).apply {
            deleteOnExit()
        }
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    }

    // Launcher para cámara (NO requiere permisos - usa caché)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            try {
                val imageBytes = uriToByteArray(context, tempImageUri)
                val fileName = "camera_${System.currentTimeMillis()}.jpg"
                onImageSelected(imageBytes, fileName)
            } catch (e: Exception) {
                println("Error reading camera image: ${e.message}")
                onImageSelected(null, null)
            }
        } else {
            onImageSelected(null, null)
        }
    }

    return remember {
        object : ImagePickerLauncher {
            override fun launchGallery() {
                galleryLauncher.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            }

            override fun launchCamera() {
                cameraLauncher.launch(tempImageUri)
            }
        }
    }
}

/**
 * Convierte un URI a ByteArray
 */
private fun uriToByteArray(context: Context, uri: Uri): ByteArray {
    val inputStream = context.contentResolver.openInputStream(uri)
        ?: throw Exception("Cannot open input stream")

    return inputStream.use { stream ->
        val buffer = ByteArrayOutputStream()
        val data = ByteArray(1024)
        var nRead: Int
        while (stream.read(data, 0, data.size).also { nRead = it } != -1) {
            buffer.write(data, 0, nRead)
        }
        buffer.toByteArray()
    }
}

/**
 * Obtiene el nombre del archivo desde el URI
 */
private fun getFileNameFromUri(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                cursor.getString(nameIndex)
            } else {
                null
            }
        }
    } catch (e: Exception) {
        println("Error getting file name: ${e.message}")
        null
    }
}
