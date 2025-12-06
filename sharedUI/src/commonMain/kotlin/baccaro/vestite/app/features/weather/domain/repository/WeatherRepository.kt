package baccaro.vestite.app.features.weather.domain.repository

import baccaro.vestite.app.features.weather.domain.model.Location
import baccaro.vestite.app.features.weather.domain.model.Weather

/**
 * Interface del repositorio de clima
 *
 * Define las operaciones de acceso a datos para obtener información del clima
 * y ubicación del usuario.
 */
interface WeatherRepository {
    /**
     * Obtiene el clima actual para una ubicación geográfica
     *
     * @param latitude Latitud en grados decimales
     * @param longitude Longitud en grados decimales
     * @return Result con Weather si es exitoso, o error si falla
     */
    suspend fun getCurrentWeather(latitude: Double, longitude: Double): Result<Weather>

    /**
     * Obtiene la ubicación actual del dispositivo
     *
     * @return Result con Location si es exitoso, o error si falla
     */
    suspend fun getLocation(): Result<Location>
}
