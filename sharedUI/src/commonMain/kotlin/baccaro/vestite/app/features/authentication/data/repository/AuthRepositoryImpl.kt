package baccaro.vestite.app.features.authentication.data.repository

import baccaro.vestite.app.core.util.Constants
import baccaro.vestite.app.features.authentication.data.mapper.toDomain
import baccaro.vestite.app.features.authentication.domain.model.User
import baccaro.vestite.app.features.authentication.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Implementación del repositorio de autenticación usando Supabase
 */
class AuthRepositoryImpl(
    private val supabase: SupabaseClient
) : AuthRepository {

    override val currentUser: Flow<User?> = supabase.auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Authenticated -> status.session.user?.toDomain()
            else -> null
        }
    }

    override val isAuthenticated: Flow<Boolean> = supabase.auth.sessionStatus.map { status ->
        val authenticated = status is SessionStatus.Authenticated
        println("AuthRepository - Session status changed: $status, isAuthenticated: $authenticated")
        authenticated
    }

    override suspend fun signUp(
        email: String,
        password: String,
        username: String
    ): Result<User> {
        return try {
            val result = supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = buildJsonObject {
                    put("username", username)
                }
            }

            result?.let { userInfo ->
                Result.success(userInfo.toDomain())
            } ?: Result.failure(Exception("Error al crear usuario"))
        } catch (e: RestException) {
            Result.failure(Exception("Error de red: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signIn(
        email: String,
        password: String
    ): Result<User> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            val user = supabase.auth.currentUserOrNull()
            user?.let {
                Result.success(it.toDomain())
            } ?: Result.failure(Exception("Usuario no encontrado después del login"))
        } catch (e: RestException) {
            Result.failure(Exception("Credenciales inválidas"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<User> {
        return try {
            println("AuthRepository - Starting native Google sign in with ID token")

            // Autenticar con el ID token obtenido nativamente
            supabase.auth.signInWith(IDToken) {
                provider = Google
                this.idToken = idToken
            }

            println("AuthRepository - Native Google sign in successful")

            // Obtener el usuario autenticado
            val user = supabase.auth.currentUserOrNull()
            user?.let {
                Result.success(it.toDomain())
            } ?: Result.failure(Exception("Usuario no encontrado después del login"))
        } catch (e: RestException) {
            println("AuthRepository - Native Google sign in failed: ${e.message}")
            Result.failure(Exception("Error en autenticación nativa con Google: ${e.message}"))
        } catch (e: Exception) {
            println("AuthRepository - Native Google sign in failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            supabase.auth.signOut()
            Result.success(Unit)
        } catch (e: RestException) {
            Result.failure(Exception("Error al cerrar sesión: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): User? {
        return supabase.auth.currentUserOrNull()?.toDomain()
    }

    override suspend fun updateProfile(
        username: String?,
        avatarUrl: String?
    ): Result<User> {
        return try {
            val updatedUser = supabase.auth.updateUser {
                data = buildJsonObject {
                    username?.let { put("username", it) }
                    avatarUrl?.let { put("avatar_url", it) }
                }
            }

            Result.success(updatedUser.toDomain())
        } catch (e: RestException) {
            Result.failure(Exception("Error al actualizar perfil: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
