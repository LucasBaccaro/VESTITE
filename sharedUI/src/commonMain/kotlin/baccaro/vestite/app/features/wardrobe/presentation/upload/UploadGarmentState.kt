package baccaro.vestite.app.features.wardrobe.presentation.upload

import baccaro.vestite.app.features.wardrobe.domain.model.Category

data class UploadGarmentState(
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val isLoading: Boolean = false,
    val isAnalyzing: Boolean = false,
    val isUploading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    // Preview state
    val showPreview: Boolean = false,
    val analyzedImageBytes: ByteArray? = null,
    val analyzedFileName: String? = null,
    val aiDescription: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as UploadGarmentState

        if (categories != other.categories) return false
        if (selectedCategoryId != other.selectedCategoryId) return false
        if (isLoading != other.isLoading) return false
        if (isAnalyzing != other.isAnalyzing) return false
        if (isUploading != other.isUploading) return false
        if (error != other.error) return false
        if (success != other.success) return false
        if (showPreview != other.showPreview) return false
        if (analyzedImageBytes != null) {
            if (other.analyzedImageBytes == null) return false
            if (!analyzedImageBytes.contentEquals(other.analyzedImageBytes)) return false
        } else if (other.analyzedImageBytes != null) return false
        if (analyzedFileName != other.analyzedFileName) return false
        if (aiDescription != other.aiDescription) return false

        return true
    }

    override fun hashCode(): Int {
        var result = categories.hashCode()
        result = 31 * result + (selectedCategoryId?.hashCode() ?: 0)
        result = 31 * result + isLoading.hashCode()
        result = 31 * result + isAnalyzing.hashCode()
        result = 31 * result + isUploading.hashCode()
        result = 31 * result + (error?.hashCode() ?: 0)
        result = 31 * result + success.hashCode()
        result = 31 * result + showPreview.hashCode()
        result = 31 * result + (analyzedImageBytes?.contentHashCode() ?: 0)
        result = 31 * result + (analyzedFileName?.hashCode() ?: 0)
        result = 31 * result + (aiDescription?.hashCode() ?: 0)
        return result
    }
}
