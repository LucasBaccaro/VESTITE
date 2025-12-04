package baccaro.vestite.app.features.authentication.data.mapper

import baccaro.vestite.app.features.authentication.domain.model.User
import io.github.jan.supabase.auth.user.UserInfo
import kotlin.time.ExperimentalTime

/**
 * Mapper para convertir entre UserInfo de Supabase y User del dominio
 */
@OptIn(ExperimentalTime::class)
fun UserInfo.toDomain(): User {
    return User(
        id = id,
        email = email ?: "",
        username = userMetadata?.get("username")?.toString()?.removeSurrounding("\""),
        avatarUrl = userMetadata?.get("avatar_url")?.toString()?.removeSurrounding("\""),
        createdAt = createdAt?.toEpochMilliseconds() ?: 0L
    )
}
