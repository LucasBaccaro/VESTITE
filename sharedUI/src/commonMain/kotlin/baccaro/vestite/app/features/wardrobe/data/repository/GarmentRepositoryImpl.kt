package baccaro.vestite.app.features.wardrobe.data.repository

import baccaro.vestite.app.features.wardrobe.data.mapper.toDomain
import baccaro.vestite.app.features.wardrobe.data.remote.dto.CategoryDto
import baccaro.vestite.app.features.wardrobe.data.remote.dto.GarmentDto
import baccaro.vestite.app.features.wardrobe.data.remote.dto.InsertGarmentDto
import baccaro.vestite.app.features.wardrobe.domain.model.Category
import baccaro.vestite.app.features.wardrobe.domain.model.Garment
import baccaro.vestite.app.features.wardrobe.domain.model.GarmentMetadata
import baccaro.vestite.app.features.wardrobe.domain.repository.GarmentRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Implementación del repositorio de prendas usando Supabase y Gemini
 */
class GarmentRepositoryImpl(
    private val supabase: SupabaseClient,
    private val geminiRepository: GeminiRepository
) : GarmentRepository {

    companion object {
        private const val BUCKET_GARMENTS = "garments"
        private const val TABLE_GARMENTS = "garments"
        private const val TABLE_CATEGORIES = "categories"
    }

    override suspend fun analyzeGarmentImage(imageBytes: ByteArray): Result<GarmentMetadata> {
        return geminiRepository.analyzeGarmentImage(imageBytes)
    }

    override suspend fun removeBackgroundFromImage(imageBytes: ByteArray): Result<ByteArray> {
        return geminiRepository.removeBackground(imageBytes)
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun uploadGarmentImage(
        imageBytes: ByteArray,
        fileName: String
    ): Result<String> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Usuario no autenticado"))

            // Generar path único: userId/timestamp-fileName
            val timestamp = Clock.System.now().toEpochMilliseconds()
            val path = "$userId/$timestamp-$fileName"

            // Subir a Storage
            val bucket = supabase.storage[BUCKET_GARMENTS]
            bucket.upload(path, imageBytes) {
                upsert = false
            }

            // Obtener URL pública
            val publicUrl = bucket.publicUrl(path)

            Result.success(publicUrl)
        } catch (e: Exception) {
            println("GarmentRepository - Error uploading image: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Error al subir imagen: ${e.message}"))
        }
    }

    override suspend fun saveGarment(
        categoryId: String,
        imageUrl: String,
        metadata: GarmentMetadata
    ): Result<Garment> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Usuario no autenticado"))

            val insertDto = InsertGarmentDto(
                userId = userId,
                categoryId = categoryId,
                imageUrl = imageUrl,
                aiDescription = metadata.description
            )

            val savedGarment = supabase.from(TABLE_GARMENTS)
                .insert(insertDto) {
                    select()
                }
                .decodeSingle<GarmentDto>()

            Result.success(savedGarment.toDomain())
        } catch (e: Exception) {
            println("GarmentRepository - Error saving garment: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Error al guardar prenda: ${e.message}"))
        }
    }

    override suspend fun getGarments(): Result<List<Garment>> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Usuario no autenticado"))

            val garments = supabase.from(TABLE_GARMENTS)
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                    order(column = "created_at", order = Order.DESCENDING)
                }
                .decodeList<GarmentDto>()

            Result.success(garments.toDomain())
        } catch (e: Exception) {
            println("GarmentRepository - Error getting garments: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Error al obtener prendas: ${e.message}"))
        }
    }

    override suspend fun getGarmentsByCategory(categoryId: String): Result<List<Garment>> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Usuario no autenticado"))

            val garments = supabase.from(TABLE_GARMENTS)
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("category_id", categoryId)
                    }
                    order(column = "created_at", order = Order.DESCENDING)
                }
                .decodeList<GarmentDto>()

            Result.success(garments.toDomain())
        } catch (e: Exception) {
            println("GarmentRepository - Error getting garments by category: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Error al obtener prendas: ${e.message}"))
        }
    }

    override suspend fun getCategories(): Result<List<Category>> {
        return try {
            val categories = supabase.from(TABLE_CATEGORIES)
                .select()
                .decodeList<CategoryDto>()

            Result.success(categories.toDomain())
        } catch (e: Exception) {
            println("GarmentRepository - Error getting categories: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Error al obtener categorías: ${e.message}"))
        }
    }

    override suspend fun deleteGarment(garmentId: String): Result<Unit> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Usuario no autenticado"))

            // Obtener la prenda para eliminar la imagen también
            val garment = supabase.from(TABLE_GARMENTS)
                .select(columns = Columns.list("id", "image_url", "user_id")) {
                    filter {
                        eq("id", garmentId)
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<GarmentDto>()
                ?: return Result.failure(Exception("Prenda no encontrada"))

            // Eliminar imagen de Storage (opcional, puede fallar sin romper el flujo)
            try {
                val path = garment.imageUrl.substringAfter("$BUCKET_GARMENTS/")
                val bucket = supabase.storage[BUCKET_GARMENTS]
                bucket.delete(path)
            } catch (e: Exception) {
                println("Warning: Could not delete image from storage: ${e.message}")
            }

            // Eliminar de la base de datos
            supabase.from(TABLE_GARMENTS)
                .delete {
                    filter {
                        eq("id", garmentId)
                        eq("user_id", userId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            println("GarmentRepository - Error deleting garment: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Error al eliminar prenda: ${e.message}"))
        }
    }
}
