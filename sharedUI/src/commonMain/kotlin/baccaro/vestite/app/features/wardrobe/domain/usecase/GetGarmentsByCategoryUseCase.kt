package baccaro.vestite.app.features.wardrobe.domain.usecase

import baccaro.vestite.app.features.wardrobe.domain.model.Garment
import baccaro.vestite.app.features.wardrobe.domain.repository.GarmentRepository

/**
 * Use case para obtener prendas filtradas por categoría
 */
class GetGarmentsByCategoryUseCase(
    private val repository: GarmentRepository
) {
    suspend operator fun invoke(categoryId: String): Result<List<Garment>> {
        return repository.getGarmentsByCategory(categoryId)
    }
}
