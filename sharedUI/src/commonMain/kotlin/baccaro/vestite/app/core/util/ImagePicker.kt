package baccaro.vestite.app.core.util

import androidx.compose.runtime.Composable

/**
 * Interface para lanzar el selector de imágenes
 */
interface ImagePickerLauncher {
    /**
     * Lanza el selector de galería (Photo Picker)
     */
    fun launchGallery()

    /**
     * Lanza la cámara para tomar una foto
     */
    fun launchCamera()
}

/**
 * Composable multiplataforma para seleccionar imágenes de la galería o cámara
 *
 * @param onImageSelected Callback que recibe los bytes de la imagen y el nombre del archivo
 * @return ImagePickerLauncher para lanzar galería o cámara
 */
@Composable
expect fun rememberImagePicker(
    onImageSelected: (imageBytes: ByteArray?, fileName: String?) -> Unit
): ImagePickerLauncher
