package baccaro.vestite.app.features.wardrobe.presentation.upload

import baccaro.vestite.app.features.wardrobe.domain.model.Category

data class UploadGarmentState(
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val isLoading: Boolean = false,
    val isAnalyzing: Boolean = false,
    val isUploading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)
