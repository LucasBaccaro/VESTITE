package baccaro.vestite.app.features.weather.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import baccaro.vestite.app.features.weather.domain.usecase.GetCurrentWeatherUseCase
import baccaro.vestite.app.features.weather.domain.usecase.GetLocationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para el weather card
 *
 * Coordina la obtención de ubicación y clima, exponiendo el estado
 * a través de un StateFlow para que la UI reaccione automáticamente.
 *
 * @property getLocationUseCase Caso de uso para obtener la ubicación
 * @property getCurrentWeatherUseCase Caso de uso para obtener el clima
 */
class WeatherViewModel(
    private val getLocationUseCase: GetLocationUseCase,
    private val getCurrentWeatherUseCase: GetCurrentWeatherUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(WeatherState())
    val state: StateFlow<WeatherState> = _state.asStateFlow()

    /**
     * Carga el clima solo si no se ha cargado previamente
     *
     * Útil para cargar automáticamente la primera vez sin recargar
     * cada vez que se navega a la pantalla.
     */
    fun loadWeatherIfNeeded() {
        // Solo cargar si no se ha cargado antes
        if (!_state.value.hasLoaded) {
            loadWeather()
        }
    }

    /**
     * Carga el clima actual (fuerza una nueva carga)
     *
     * Flujo:
     * 1. Obtiene la ubicación del dispositivo
     * 2. Con la ubicación, obtiene el clima de la API
     * 3. Actualiza el estado con los resultados o errores
     */
    fun loadWeather() {
        viewModelScope.launch {
            // Indicar que está cargando
            _state.value = WeatherState(isLoading = true)

            // Paso 1: Obtener ubicación
            getLocationUseCase()
                .onSuccess { location ->
                    // Paso 2: Obtener clima con la ubicación
                    getCurrentWeatherUseCase(location.latitude, location.longitude)
                        .onSuccess { weather ->
                            // Éxito: Actualizar con datos del clima
                            _state.value = WeatherState(
                                weather = weather,
                                hasLoaded = true
                            )
                        }
                        .onFailure { error ->
                            // Error al obtener clima
                            _state.value = WeatherState(
                                error = error.message ?: "Error al obtener clima",
                                hasLoaded = true
                            )
                        }
                }
                .onFailure { error ->
                    // Error al obtener ubicación
                    val errorMessage = when {
                        error.message?.contains("Permisos", ignoreCase = true) == true -> "Permisos de ubicación denegados"
                        error.message?.contains("ubicación") == true -> error.message
                        else -> "No se pudo obtener la ubicación"
                    }
                    _state.value = WeatherState(
                        error = errorMessage,
                        hasLoaded = true
                    )
                }
        }
    }
}
