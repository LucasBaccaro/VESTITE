package baccaro.vestite.app.features.authentication.domain.usecase

import baccaro.vestite.app.core.util.Constants
import baccaro.vestite.app.features.authentication.domain.model.User
import baccaro.vestite.app.features.authentication.domain.repository.AuthRepository

/**
 * Caso de uso para iniciar sesión con email y contraseña
 */
class SignInUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String
    ): Result<User> {
        // Validaciones
        if (email.isBlank()) {
            return Result.failure(IllegalArgumentException("El email no puede estar vacío"))
        }

        if (!email.contains("@")) {
            return Result.failure(IllegalArgumentException("Email inválido"))
        }

        if (password.length < Constants.MIN_PASSWORD_LENGTH) {
            return Result.failure(
                IllegalArgumentException("La contraseña debe tener al menos ${Constants.MIN_PASSWORD_LENGTH} caracteres")
            )
        }

        // Llamar al repositorio
        return authRepository.signIn(email.trim(), password)
    }
}
