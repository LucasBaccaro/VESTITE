package baccaro.vestite.app.features.wardrobe.domain.model

import kotlinx.datetime.Instant
import kotlin.time.ExperimentalTime

data class Garment @OptIn(ExperimentalTime::class) constructor(
    val id: String,
    val userId: String,
    val categoryId: String,
    val imageUrl: String,
    val aiDescription: String?,
    val aiFit: GarmentFit,
    val createdAt: kotlin.time.Instant,
    val updatedAt: kotlin.time.Instant
)

enum class GarmentFit(val value: String) {
    TIGHT("tight"),
    REGULAR("regular"),
    LOOSE("loose"),
    OVERSIZED("oversized");

    companion object {
        fun fromValue(value: String): GarmentFit {
            return entries.find { it.value == value }
                ?: REGULAR
        }
    }
}
