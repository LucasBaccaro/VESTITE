package baccaro.vestite.app.core.util

import androidx.compose.runtime.Composable

/**
 * Implementación de iOS para seleccionar imágenes
 * TODO: Implementar usando UIImagePickerController o PHPickerViewController
 */
@Composable
actual fun rememberImagePicker(
    onImageSelected: (imageBytes: ByteArray?, fileName: String?) -> Unit
): ImagePickerLauncher {
    return object : ImagePickerLauncher {
        override fun launchGallery() {
            println("iOS Gallery Picker not implemented yet")
            onImageSelected(null, null)
        }

        override fun launchCamera() {
            println("iOS Camera not implemented yet")
            onImageSelected(null, null)
        }
    }
}
