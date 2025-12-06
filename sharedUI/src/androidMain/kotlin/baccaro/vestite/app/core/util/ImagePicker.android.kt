package baccaro.vestite.app.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
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
 * Convierte un URI a ByteArray con compresión automática y corrección de rotación EXIF
 * Comprime la imagen para mantenerla bajo 5 MB (límite de Gemini)
 */
private fun uriToByteArray(context: Context, uri: Uri): ByteArray {
    val inputStream = context.contentResolver.openInputStream(uri)
        ?: throw Exception("Cannot open input stream")

    // Cargar imagen como Bitmap
    val originalBitmap = BitmapFactory.decodeStream(inputStream)
        ?: throw Exception("Cannot decode image")

    inputStream.close()

    // Leer orientación EXIF y aplicar rotación si es necesario
    val rotatedBitmap = try {
        context.contentResolver.openInputStream(uri)?.use { exifStream ->
            val exif = ExifInterface(exifStream)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            println("EXIF Orientation: $orientation")

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(originalBitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(originalBitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(originalBitmap, 270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> flipBitmap(originalBitmap, horizontal = true, vertical = false)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> flipBitmap(originalBitmap, horizontal = false, vertical = true)
                else -> originalBitmap
            }
        } ?: originalBitmap
    } catch (e: Exception) {
        println("Warning: Could not read EXIF data: ${e.message}")
        originalBitmap
    }

    // Si se aplicó rotación, liberar el bitmap original
    if (rotatedBitmap !== originalBitmap) {
        originalBitmap.recycle()
    }

    // Calcular tamaño redimensionado (max 2048x2048 para mantener calidad razonable)
    val maxDimension = 2048
    val scale = minOf(
        maxDimension.toFloat() / rotatedBitmap.width,
        maxDimension.toFloat() / rotatedBitmap.height,
        1.0f // No aumentar si ya es pequeña
    )

    val resizedBitmap = if (scale < 1.0f) {
        val newWidth = (rotatedBitmap.width * scale).toInt()
        val newHeight = (rotatedBitmap.height * scale).toInt()
        println("Resizing image from ${rotatedBitmap.width}x${rotatedBitmap.height} to ${newWidth}x${newHeight}")
        Bitmap.createScaledBitmap(rotatedBitmap, newWidth, newHeight, true).also {
            rotatedBitmap.recycle() // Liberar memoria
        }
    } else {
        rotatedBitmap
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

/**
 * Rota un bitmap por el ángulo especificado
 */
private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix().apply {
        postRotate(degrees)
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

/**
 * Voltea un bitmap horizontal o verticalmente
 */
private fun flipBitmap(bitmap: Bitmap, horizontal: Boolean, vertical: Boolean): Bitmap {
    val matrix = Matrix().apply {
        postScale(
            if (horizontal) -1f else 1f,
            if (vertical) -1f else 1f,
            bitmap.width / 2f,
            bitmap.height / 2f
        )
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
