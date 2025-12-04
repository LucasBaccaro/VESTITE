package baccaro.vestite.app.features.authentication.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO para el usuario de Supabase
 * Representa la estructura de datos que viene de la API
 */
@Serializable
data class UserDto(
    val id: String,
    val email: String,
    @SerialName("user_metadata")
    val userMetadata: UserMetadata? = null,
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class UserMetadata(
    val username: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null
)
