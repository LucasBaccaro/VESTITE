package baccaro.vestite.app.features.wardrobe.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import baccaro.vestite.app.features.wardrobe.domain.usecase.DeleteGarmentUseCase
import baccaro.vestite.app.features.wardrobe.domain.usecase.GetCategoriesUseCase
import baccaro.vestite.app.features.wardrobe.domain.usecase.GetGarmentsByCategoryUseCase
import baccaro.vestite.app.features.wardrobe.domain.usecase.GetGarmentsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WardrobeListViewModel(
    private val getGarmentsUseCase: GetGarmentsUseCase,
    private val getGarmentsByCategoryUseCase: GetGarmentsByCategoryUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val deleteGarmentUseCase: DeleteGarmentUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(WardrobeListState())
    val state: StateFlow<WardrobeListState> = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        loadCategories()
        loadGarments()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            getCategoriesUseCase().fold(
                onSuccess = { categories ->
                    _state.update { it.copy(categories = categories) }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(error = error.message ?: "Error al cargar categorías")
                    }
                }
            )
        }
    }

    fun loadGarments() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val result = if (_state.value.selectedCategoryId != null) {
                getGarmentsByCategoryUseCase(_state.value.selectedCategoryId!!)
            } else {
                getGarmentsUseCase()
            }

            result.fold(
                onSuccess = { garments ->
                    _state.update {
                        it.copy(
                            garments = garments,
                            isLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Error al cargar prendas"
                        )
                    }
                }
            )
        }
    }

    fun selectCategory(categoryId: String?) {
        _state.update { it.copy(selectedCategoryId = categoryId) }
        loadGarments()
    }

    fun deleteGarment(garmentId: String) {
        viewModelScope.launch {
            deleteGarmentUseCase(garmentId).fold(
                onSuccess = {
                    // Recargar la lista después de eliminar
                    loadGarments()
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(error = error.message ?: "Error al eliminar prenda")
                    }
                }
            )
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
