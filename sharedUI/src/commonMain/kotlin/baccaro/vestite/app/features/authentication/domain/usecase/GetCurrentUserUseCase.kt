package baccaro.vestite.app.features.authentication.domain.usecase

import baccaro.vestite.app.features.authentication.domain.model.User
import baccaro.vestite.app.features.authentication.domain.repository.AuthRepository

/**
 * Caso de uso para obtener el usuario actual
 */
class GetCurrentUserUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): User? {
        return authRepository.getCurrentUser()
    }
}
