package baccaro.vestite.app.features.authentication.domain.repository

import baccaro.vestite.app.features.authentication.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Interface del repositorio de autenticación
 * Define las operaciones de autenticación sin detalles de implementación
 */
interface AuthRepository {

    /**
     * Flow que emite el usuario actual autenticado
     */
    val currentUser: Flow<User?>

    /**
     * Flow que indica si hay un usuario autenticado
     */
    val isAuthenticated: Flow<Boolean>

    /**
     * Registrar un nuevo usuario con email y contraseña
     */
    suspend fun signUp(
        email: String,
        password: String,
        username: String
    ): Result<User>

    /**
     * Iniciar sesión con email y contraseña
     */
    suspend fun signIn(
        email: String,
        password: String
    ): Result<User>

    /**
     * Iniciar sesión con Google usando ID token (flujo nativo con KMPAuth)
     */
    suspend fun signInWithGoogleIdToken(idToken: String): Result<User>

    /**
     * Cerrar sesión
     */
    suspend fun signOut(): Result<Unit>

    /**
     * Obtener el usuario actual
     */
    suspend fun getCurrentUser(): User?

    /**
     * Actualizar perfil del usuario
     */
    suspend fun updateProfile(
        username: String? = null,
        avatarUrl: String? = null
    ): Result<User>
}
