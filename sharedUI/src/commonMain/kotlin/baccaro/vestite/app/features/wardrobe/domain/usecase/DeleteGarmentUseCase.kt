package baccaro.vestite.app.features.wardrobe.domain.usecase

import baccaro.vestite.app.features.wardrobe.domain.repository.GarmentRepository

/**
 * Use case para eliminar una prenda
 */
class DeleteGarmentUseCase(
    private val repository: GarmentRepository
) {
    suspend operator fun invoke(garmentId: String): Result<Unit> {
        return repository.deleteGarment(garmentId)
    }
}
