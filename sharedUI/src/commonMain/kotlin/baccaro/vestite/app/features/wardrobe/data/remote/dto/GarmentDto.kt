package baccaro.vestite.app.features.wardrobe.data.remote.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime

@Serializable
data class GarmentDto @OptIn(ExperimentalTime::class) constructor(
    @SerialName("id")
    val id: String,

    @SerialName("user_id")
    val userId: String,

    @SerialName("category_id")
    val categoryId: String,

    @SerialName("image_url")
    val imageUrl: String,

    @SerialName("ai_description")
    val aiDescription: String? = null,

    @SerialName("ai_fit")
    val aiFit: String = "regular",

    @SerialName("created_at")
    val createdAt: kotlin.time.Instant,

    @SerialName("updated_at")
    val updatedAt: kotlin.time.Instant
)

@Serializable
data class InsertGarmentDto(
    @SerialName("user_id")
    val userId: String,

    @SerialName("category_id")
    val categoryId: String,

    @SerialName("image_url")
    val imageUrl: String,

    @SerialName("ai_description")
    val aiDescription: String?,

    @SerialName("ai_fit")
    val aiFit: String
)
