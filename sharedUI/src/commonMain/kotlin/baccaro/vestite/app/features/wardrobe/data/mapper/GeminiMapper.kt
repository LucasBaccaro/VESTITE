package baccaro.vestite.app.features.wardrobe.data.mapper

import baccaro.vestite.app.features.wardrobe.data.remote.dto.GarmentAnalysisResponse
import baccaro.vestite.app.features.wardrobe.domain.model.GarmentFit
import baccaro.vestite.app.features.wardrobe.domain.model.GarmentMetadata

fun GarmentAnalysisResponse.toDomain(): GarmentMetadata {
    return GarmentMetadata(
        description = description,
        fit = GarmentFit.fromValue(fit)
    )
}
