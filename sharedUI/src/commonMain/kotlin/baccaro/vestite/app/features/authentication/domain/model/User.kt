package baccaro.vestite.app.features.authentication.domain.model

/**
 * Modelo de dominio para el usuario
 */
data class User(
    val id: String,
    val email: String,
    val username: String? = null,
    val avatarUrl: String? = null,
    val createdAt: Long
)
