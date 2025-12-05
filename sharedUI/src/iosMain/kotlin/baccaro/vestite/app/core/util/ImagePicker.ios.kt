package baccaro.vestite.app.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject
import platform.posix.memcpy
import platform.CoreGraphics.*
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import kotlin.math.min

/**
 * Implementación de iOS para seleccionar imágenes
 * Soporta galería (Photo Library) y cámara con compresión automática
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
@OptIn(ExperimentalForeignApi::class)
private fun presentImagePicker(
    sourceType: UIImagePickerControllerSourceType,
    onImageSelected: (imageBytes: ByteArray?, fileName: String?) -> Unit
) {
    // Verificar si la fuente está disponible
    if (!UIImagePickerController.isSourceTypeAvailable(sourceType)) {
        println("Source type not available (Camera not available on simulator)")
        onImageSelected(null, null)
        return
    }

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
                    val imageBytes = compressImage(image)
                    val timestamp = (NSDate().timeIntervalSince1970 * 1000).toLong()
                    val fileName = if (sourceType == UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera) {
                        "camera_${timestamp}.jpg"
                    } else {
                        "gallery_image_${timestamp}.jpg"
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

    val rootViewController = platform.UIKit.UIApplication.sharedApplication.keyWindow?.rootViewController
    rootViewController?.presentViewController(pickerController, animated = true, completion = null)
}

/**
 * Comprime un UIImage con redimensionamiento y ajuste de calidad
 * Mantiene la imagen bajo 5 MB (límite de Gemini)
 */
@OptIn(ExperimentalForeignApi::class)
private fun compressImage(image: UIImage): ByteArray {
    // Redimensionar si es necesario (max 2048x2048)
    val maxDimension = 2048.0
    val originalWidth = image.size.useContents { width }
    val originalHeight = image.size.useContents { height }

    val scale = minOf(
        maxDimension / originalWidth,
        maxDimension / originalHeight,
        1.0 // No aumentar si ya es pequeña
    )

    val resizedImage = if (scale < 1.0) {
        val newWidth = originalWidth * scale
        val newHeight = originalHeight * scale
        println("Resizing image from ${originalWidth}x${originalHeight} to ${newWidth}x${newHeight}")
        resizeImage(image, newWidth, newHeight)
    } else {
        image
    }

    // Comprimir con diferentes calidades hasta que esté bajo 5 MB
    var quality = 0.9
    var compressedData: NSData

    do {
        compressedData = UIImageJPEGRepresentation(resizedImage, quality)
            ?: throw Exception("Cannot compress image")

        val sizeMB = compressedData.length.toDouble() / (1024.0 * 1024.0)
        val qualityPercent = (quality * 100).toInt()
        println("Compressed at quality $qualityPercent: ${sizeMB.toString().take(4)} MB")

        if (sizeMB <= 5.0) {
            break
        }

        quality -= 0.1
    } while (quality >= 0.5) // No bajar de calidad 50%

    val finalSizeMB = compressedData.length.toDouble() / (1024.0 * 1024.0)
    println("Final image size: ${(finalSizeMB * 100).toInt() / 100.0} MB")

    if (finalSizeMB > 5.0) {
        throw Exception("No se pudo comprimir la imagen a menos de 5 MB. Intenta con una foto más pequeña.")
    }

    return compressedData.toByteArray()
}

/**
 * Redimensiona un UIImage a las dimensiones especificadas
 */
@OptIn(ExperimentalForeignApi::class)
private fun resizeImage(image: UIImage, width: Double, height: Double): UIImage {
    val size = CGSizeMake(width, height)

    UIGraphicsBeginImageContextWithOptions(size, false, 1.0)
    image.drawInRect(CGRectMake(0.0, 0.0, width, height))
    val resizedImage = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()

    return resizedImage ?: image
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