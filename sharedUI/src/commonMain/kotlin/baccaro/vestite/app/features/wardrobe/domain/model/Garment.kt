package baccaro.vestite.app.features.wardrobe.domain.model

import kotlinx.datetime.Instant
import kotlin.time.ExperimentalTime

data class Garment @OptIn(ExperimentalTime::class) constructor(
    val id: String,
    val userId: String,
    val categoryId: String,
    val imageUrl: String,
    val aiDescription: String?,
    val createdAt: kotlin.time.Instant,
    val updatedAt: kotlin.time.Instant
)
