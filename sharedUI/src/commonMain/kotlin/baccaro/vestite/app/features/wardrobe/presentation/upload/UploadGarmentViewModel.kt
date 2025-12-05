package baccaro.vestite.app.features.wardrobe.presentation.upload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import baccaro.vestite.app.features.wardrobe.domain.repository.GarmentRepository
import baccaro.vestite.app.features.wardrobe.domain.usecase.GetCategoriesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UploadGarmentViewModel(
    private val garmentRepository: GarmentRepository,
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

    /**
     * Step 1: Analyze garment with AI and show preview
     */
    fun analyzeGarment(imageBytes: ByteArray, fileName: String) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isAnalyzing = true,
                    error = null,
                    success = false
                )
            }

            garmentRepository.analyzeGarmentImage(imageBytes).fold(
                onSuccess = { metadata ->
                    println("Garment analyzed successfully: ${metadata.description}")
                    _state.update {
                        it.copy(
                            isAnalyzing = false,
                            showPreview = true,
                            analyzedImageBytes = imageBytes,
                            analyzedFileName = fileName,
                            aiDescription = metadata.description,
                            aiFit = metadata.fit  // Already a GarmentFit from repository
                        )
                    }
                },
                onFailure = { error ->
                    println("Error analyzing garment: ${error.message}")
                    _state.update {
                        it.copy(
                            isAnalyzing = false,
                            error = error.message ?: "Error al analizar la prenda"
                        )
                    }
                }
            )
        }
    }

    /**
     * Step 2: Save garment to Storage + DB with selected category
     */
    fun saveGarment() {
        val categoryId = _state.value.selectedCategoryId
        if (categoryId == null) {
            _state.update { it.copy(error = "Selecciona una categoría") }
            return
        }

        val imageBytes = _state.value.analyzedImageBytes
        val fileName = _state.value.analyzedFileName
        val aiDescription = _state.value.aiDescription
        val aiFit = _state.value.aiFit

        if (imageBytes == null || fileName == null || aiDescription == null || aiFit == null) {
            _state.update { it.copy(error = "Datos incompletos") }
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isUploading = true,
                    error = null
                )
            }

            // Upload image to Storage
            garmentRepository.uploadGarmentImage(imageBytes, fileName).fold(
                onSuccess = { imageUrl ->
                    // Save garment to DB
                    garmentRepository.saveGarment(
                        categoryId = categoryId,
                        imageUrl = imageUrl,
                        metadata = baccaro.vestite.app.features.wardrobe.domain.model.GarmentMetadata(
                            description = aiDescription,
                            fit = aiFit  // Already a GarmentFit
                        )
                    ).fold(
                        onSuccess = { garment ->
                            println("Garment saved successfully: ${garment.id}")
                            _state.update {
                                it.copy(
                                    isUploading = false,
                                    success = true
                                )
                            }
                        },
                        onFailure = { error ->
                            println("Error saving garment: ${error.message}")
                            _state.update {
                                it.copy(
                                    isUploading = false,
                                    error = error.message ?: "Error al guardar prenda"
                                )
                            }
                        }
                    )
                },
                onFailure = { error ->
                    println("Error uploading image: ${error.message}")
                    _state.update {
                        it.copy(
                            isUploading = false,
                            error = error.message ?: "Error al subir imagen"
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
