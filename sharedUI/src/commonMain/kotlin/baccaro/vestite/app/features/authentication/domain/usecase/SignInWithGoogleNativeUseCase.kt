package baccaro.vestite.app.features.authentication.domain.usecase

import baccaro.vestite.app.features.authentication.domain.model.User
import baccaro.vestite.app.features.authentication.domain.repository.AuthRepository

/**
 * Caso de uso para iniciar sesión con Google usando autenticación nativa
 *
 * Recibe el ID token obtenido por KMPAuth y lo envía a Supabase
 * para crear la sesión del usuario.
 *
 * Ventajas:
 * - UX nativa: Bottom sheet de Google (One Tap)
 * - Más rápido: No abre navegador ni WebView
 * - Más seguro: El token se valida directamente
 */
class SignInWithGoogleNativeUseCase(
    private val authRepository: AuthRepository
) {
    /**
     * Autentica al usuario con Supabase usando el ID token de Google
     *
     * @param idToken Token de ID obtenido de Google Sign-In
     * @return Result con el usuario autenticado o un error
     */
    suspend operator fun invoke(idToken: String): Result<User> {
        return authRepository.signInWithGoogleIdToken(idToken)
    }
}
