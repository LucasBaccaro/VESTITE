package baccaro.vestite.app.features.weather.domain.usecase

import baccaro.vestite.app.features.weather.domain.model.Weather
import baccaro.vestite.app.features.weather.domain.repository.WeatherRepository

/**
 * Caso de uso para obtener el clima actual basado en coordenadas geográficas
 *
 * @property repository Repositorio de clima que provee acceso a la API del clima
 */
class GetCurrentWeatherUseCase(
    private val repository: WeatherRepository
) {
    /**
     * Ejecuta el caso de uso para obtener el clima
     *
     * @param latitude Latitud en grados decimales
     * @param longitude Longitud en grados decimales
     * @return Result con Weather si es exitoso, o error si falla
     */
    suspend operator fun invoke(latitude: Double, longitude: Double): Result<Weather> {
        return repository.getCurrentWeather(latitude, longitude)
    }
}
