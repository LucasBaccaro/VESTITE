package baccaro.vestite.app.features.wardrobe.data.mapper

import baccaro.vestite.app.features.wardrobe.data.remote.dto.CategoryDto
import baccaro.vestite.app.features.wardrobe.domain.model.Category
import baccaro.vestite.app.features.wardrobe.domain.model.CategorySlug

fun CategoryDto.toDomain(): Category {
    return Category(
        id = id,
        slug = CategorySlug.fromValue(slug),
        displayName = displayName
    )
}

fun List<CategoryDto>.toDomain(): List<Category> {
    return map { it.toDomain() }
}
