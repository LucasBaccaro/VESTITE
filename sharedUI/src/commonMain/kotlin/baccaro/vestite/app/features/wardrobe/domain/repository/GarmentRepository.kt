package baccaro.vestite.app.features.wardrobe.domain.repository

import baccaro.vestite.app.features.wardrobe.domain.model.Category
import baccaro.vestite.app.features.wardrobe.domain.model.Garment
import baccaro.vestite.app.features.wardrobe.domain.model.GarmentMetadata

interface GarmentRepository {
    /**
     * Analyzes a garment image using Gemini Flash to extract metadata
     */
    suspend fun analyzeGarmentImage(imageBytes: ByteArray): Result<GarmentMetadata>

    /**
     * Uploads a garment image to Supabase Storage
     * @return The public URL of the uploaded image
     */
    suspend fun uploadGarmentImage(imageBytes: ByteArray, fileName: String): Result<String>

    /**
     * Saves a garment to the database
     */
    suspend fun saveGarment(
        categoryId: String,
        imageUrl: String,
        metadata: GarmentMetadata
    ): Result<Garment>

    /**
     * Gets all garments for the current user
     */
    suspend fun getGarments(): Result<List<Garment>>

    /**
     * Gets garments filtered by category
     */
    suspend fun getGarmentsByCategory(categoryId: String): Result<List<Garment>>

    /**
     * Gets all categories
     */
    suspend fun getCategories(): Result<List<Category>>

    /**
     * Deletes a garment
     */
    suspend fun deleteGarment(garmentId: String): Result<Unit>
}
