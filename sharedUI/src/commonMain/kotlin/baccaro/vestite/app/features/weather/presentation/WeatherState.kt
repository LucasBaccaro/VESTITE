package baccaro.vestite.app.features.weather.presentation

import baccaro.vestite.app.features.weather.domain.model.Weather

/**
 * Estado del weather card
 *
 * @property weather Datos del clima actual (null si no se ha cargado)
 * @property isLoading Indica si se está cargando el clima
 * @property error Mensaje de error si falló la carga
 * @property hasLoaded Indica si ya se intentó cargar el clima al menos una vez (para evitar recargas innecesarias)
 */
data class WeatherState(
    val weather: Weather? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasLoaded: Boolean = false
)
