package baccaro.vestite.app.features.authentication.domain.usecase

import baccaro.vestite.app.features.authentication.domain.repository.AuthRepository

/**
 * Caso de uso para cerrar sesión
 */
class SignOutUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return authRepository.signOut()
    }
}
