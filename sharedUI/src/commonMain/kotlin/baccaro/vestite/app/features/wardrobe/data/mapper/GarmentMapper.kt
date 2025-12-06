package baccaro.vestite.app.features.wardrobe.data.mapper

import baccaro.vestite.app.features.wardrobe.data.remote.dto.GarmentDto
import baccaro.vestite.app.features.wardrobe.domain.model.Garment
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun GarmentDto.toDomain(): Garment {
    return Garment(
        id = id,
        userId = userId,
        categoryId = categoryId,
        imageUrl = imageUrl,
        aiDescription = aiDescription,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun List<GarmentDto>.toDomain(): List<Garment> {
    return map { it.toDomain() }
}
