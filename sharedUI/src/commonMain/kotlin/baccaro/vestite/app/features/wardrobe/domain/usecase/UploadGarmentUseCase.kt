package baccaro.vestite.app.features.wardrobe.domain.usecase

import baccaro.vestite.app.features.wardrobe.domain.model.Garment
import baccaro.vestite.app.features.wardrobe.domain.repository.GarmentRepository

/**
 * Use case para subir y guardar una prenda
 * Flujo completo: Analiza imagen → Sube a Storage → Guarda en DB
 */
class UploadGarmentUseCase(
    private val repository: GarmentRepository
) {
    suspend operator fun invoke(
        imageBytes: ByteArray,
        categoryId: String,
        fileName: String
    ): Result<Garment> {
        return try {
            // 1. Analizar imagen con Gemini Flash
            val metadataResult = repository.analyzeGarmentImage(imageBytes)
            if (metadataResult.isFailure) {
                return Result.failure(metadataResult.exceptionOrNull()!!)
            }
            val metadata = metadataResult.getOrThrow()

            // 2. Subir imagen a Supabase Storage
            val uploadResult = repository.uploadGarmentImage(imageBytes, fileName)
            if (uploadResult.isFailure) {
                return Result.failure(uploadResult.exceptionOrNull()!!)
            }
            val imageUrl = uploadResult.getOrThrow()

            // 3. Guardar prenda en la base de datos
            repository.saveGarment(
                categoryId = categoryId,
                imageUrl = imageUrl,
                metadata = metadata
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
