package baccaro.vestite.app.features.weather.domain.usecase

import baccaro.vestite.app.features.weather.domain.model.Location
import baccaro.vestite.app.features.weather.domain.repository.WeatherRepository

/**
 * Caso de uso para obtener la ubicación actual del dispositivo
 *
 * @property repository Repositorio de clima que provee acceso a la ubicación
 */
class GetLocationUseCase(
    private val repository: WeatherRepository
) {
    /**
     * Ejecuta el caso de uso para obtener la ubicación
     *
     * @return Result con Location si es exitoso, o error si falla
     */
    suspend operator fun invoke(): Result<Location> {
        return repository.getLocation()
    }
}
