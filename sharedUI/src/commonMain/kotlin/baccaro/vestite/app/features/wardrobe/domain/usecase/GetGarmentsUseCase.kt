package baccaro.vestite.app.features.wardrobe.domain.usecase

import baccaro.vestite.app.features.wardrobe.domain.model.Garment
import baccaro.vestite.app.features.wardrobe.domain.repository.GarmentRepository

/**
 * Use case para obtener todas las prendas del usuario
 */
class GetGarmentsUseCase(
    private val repository: GarmentRepository
) {
    suspend operator fun invoke(): Result<List<Garment>> {
        return repository.getGarments()
    }
}
