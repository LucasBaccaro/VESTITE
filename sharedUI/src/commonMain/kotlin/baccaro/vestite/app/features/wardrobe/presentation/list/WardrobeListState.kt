package baccaro.vestite.app.features.wardrobe.presentation.list

import baccaro.vestite.app.features.wardrobe.domain.model.Category
import baccaro.vestite.app.features.wardrobe.domain.model.Garment

data class WardrobeListState(
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val garments: List<Garment> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
