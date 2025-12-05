package baccaro.vestite.app.features.wardrobe.presentation.upload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import baccaro.vestite.app.features.wardrobe.domain.usecase.GetCategoriesUseCase
import baccaro.vestite.app.features.wardrobe.domain.usecase.UploadGarmentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UploadGarmentViewModel(
    private val uploadGarmentUseCase: UploadGarmentUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(UploadGarmentState())
    val state: StateFlow<UploadGarmentState> = _state.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            getCategoriesUseCase().fold(
                onSuccess = { categories ->
                    _state.update {
                        it.copy(
                            categories = categories,
                            isLoading = false,
                            selectedCategoryId = categories.firstOrNull()?.id
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Error al cargar categorías"
                        )
                    }
                }
            )
        }
    }

    fun selectCategory(categoryId: String) {
        _state.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun uploadGarment(imageBytes: ByteArray, fileName: String) {
        val categoryId = _state.value.selectedCategoryId
        if (categoryId == null) {
            _state.update { it.copy(error = "Selecciona una categoría") }
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isUploading = true,
                    isAnalyzing = true,
                    error = null,
                    success = false
                )
            }

            uploadGarmentUseCase(imageBytes, categoryId, fileName).fold(
                onSuccess = { garment ->
                    println("Garment uploaded successfully: ${garment.id}")
                    _state.update {
                        it.copy(
                            isUploading = false,
                            isAnalyzing = false,
                            success = true
                        )
                    }
                },
                onFailure = { error ->
                    println("Error uploading garment: ${error.message}")
                    _state.update {
                        it.copy(
                            isUploading = false,
                            isAnalyzing = false,
                            error = error.message ?: "Error al subir prenda"
                        )
                    }
                }
            )
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun clearSuccess() {
        _state.update { it.copy(success = false) }
    }
}
