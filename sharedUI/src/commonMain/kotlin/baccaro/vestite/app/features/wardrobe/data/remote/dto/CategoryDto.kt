package baccaro.vestite.app.features.wardrobe.data.remote.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime

@Serializable
data class CategoryDto @OptIn(ExperimentalTime::class) constructor(
    @SerialName("id")
    val id: String,

    @SerialName("slug")
    val slug: String,

    @SerialName("display_name")
    val displayName: String,

    @SerialName("created_at")
    val createdAt: kotlin.time.Instant? = null
)
