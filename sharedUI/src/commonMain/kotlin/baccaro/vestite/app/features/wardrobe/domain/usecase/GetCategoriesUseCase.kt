package baccaro.vestite.app.features.wardrobe.domain.usecase

import baccaro.vestite.app.features.wardrobe.domain.model.Category
import baccaro.vestite.app.features.wardrobe.domain.repository.GarmentRepository

/**
 * Use case para obtener todas las categorías disponibles
 */
class GetCategoriesUseCase(
    private val repository: GarmentRepository
) {
    suspend operator fun invoke(): Result<List<Category>> {
        return repository.getCategories()
    }
}
