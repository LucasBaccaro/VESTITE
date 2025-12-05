package baccaro.vestite.app.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject
import platform.posix.memcpy
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.UIKit.UIApplication
import platform.UIKit.UIImagePickerControllerOriginalImage
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Implementación de iOS para seleccionar imágenes
 * Soporta galería (Photo Library) y cámara
 */
@Composable
actual fun rememberImagePicker(
    onImageSelected: (imageBytes: ByteArray?, fileName: String?) -> Unit
): ImagePickerLauncher {

    return remember {
        object : ImagePickerLauncher {
            override fun launchGallery() {
                presentImagePicker(
                    sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary,
                    onImageSelected = onImageSelected
                )
            }

            override fun launchCamera() {
                presentImagePicker(
                    sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera,
                    onImageSelected = onImageSelected
                )
            }
        }
    }
}

/**
 * Presenta el UIImagePickerController
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalTime::class)
private fun presentImagePicker(
    sourceType: UIImagePickerControllerSourceType,
    onImageSelected: (imageBytes: ByteArray?, fileName: String?) -> Unit
) {
    val pickerController = UIImagePickerController()
    pickerController.sourceType = sourceType

    val delegate = object : NSObject(),
        UIImagePickerControllerDelegateProtocol,
        UINavigationControllerDelegateProtocol {

        override fun imagePickerController(
            picker: UIImagePickerController,
            didFinishPickingMediaWithInfo: Map<Any?, *>
        ) {
            val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage

            if (image != null) {
                try {
                    val imageBytes = uiImageToByteArray(image)
                    val fileName = if (sourceType == UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera) {
                        "camera_${Clock.System.now().toEpochMilliseconds()}.jpg"
                    } else {
                        "gallery_image_${Clock.System.now().toEpochMilliseconds()}.jpg"
                    }
                    onImageSelected(imageBytes, fileName)
                } catch (e: Exception) {
                    println("Error converting image: ${e.message}")
                    onImageSelected(null, null)
                }
            } else {
                onImageSelected(null, null)
            }

            picker.dismissViewControllerAnimated(true, null)
        }

        override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
            onImageSelected(null, null)
            picker.dismissViewControllerAnimated(true, null)
        }
    }

    pickerController.delegate = delegate

    val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
    rootViewController?.presentViewController(pickerController, animated = true, completion = null)
}

/**
 * Convierte un UIImage a ByteArray
 */
@OptIn(ExperimentalForeignApi::class)
private fun uiImageToByteArray(image: UIImage): ByteArray {
    // Comprimir imagen a JPEG con calidad 0.8
    val imageData = UIImageJPEGRepresentation(image, 0.8)
        ?: throw Exception("Cannot convert image to JPEG")

    // Convertir NSData a ByteArray
    return imageData.toByteArray()
}

/**
 * Extensión para convertir NSData a ByteArray
 */
@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = this.length.toInt()
    val bytes = ByteArray(size)

    if (size > 0) {
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this.bytes, this.length)
        }
    }

    return bytes
}
