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
 * Convierte un URI a ByteArray con compresión automática
 * Comprime la imagen para mantenerla bajo 5 MB (límite de Gemini)
 */
private fun uriToByteArray(context: Context, uri: Uri): ByteArray {
    val inputStream = context.contentResolver.openInputStream(uri)
        ?: throw Exception("Cannot open input stream")

    // Cargar imagen como Bitmap
    val originalBitmap = BitmapFactory.decodeStream(inputStream)
        ?: throw Exception("Cannot decode image")

    inputStream.close()

    // Calcular tamaño redimensionado (max 2048x2048 para mantener calidad razonable)
    val maxDimension = 2048
    val scale = minOf(
        maxDimension.toFloat() / originalBitmap.width,
        maxDimension.toFloat() / originalBitmap.height,
        1.0f // No aumentar si ya es pequeña
    )

    val resizedBitmap = if (scale < 1.0f) {
        val newWidth = (originalBitmap.width * scale).toInt()
        val newHeight = (originalBitmap.height * scale).toInt()
        println("Resizing image from ${originalBitmap.width}x${originalBitmap.height} to ${newWidth}x${newHeight}")
        Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true).also {
            originalBitmap.recycle() // Liberar memoria
        }
    } else {
        originalBitmap
    }

    // Comprimir con diferentes calidades hasta que esté bajo 5 MB
    var quality = 90
    var compressedBytes: ByteArray

    do {
        val outputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        compressedBytes = outputStream.toByteArray()

        val sizeMB = compressedBytes.size / (1024.0 * 1024.0)
        println("Compressed at quality $quality: ${String.format("%.2f", sizeMB)} MB")

        if (sizeMB <= 5.0) {
            break
        }

        quality -= 10
    } while (quality >= 50) // No bajar de calidad 50

    resizedBitmap.recycle() // Liberar memoria

    val finalSizeMB = compressedBytes.size / (1024.0 * 1024.0)
    println("Final image size: ${String.format("%.2f", finalSizeMB)} MB")

    if (finalSizeMB > 5.0) {
        throw Exception("No se pudo comprimir la imagen a menos de 5 MB. Intenta con una foto más pequeña.")
    }

    return compressedBytes
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
