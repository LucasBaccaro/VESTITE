package baccaro.vestite.app.features.weather.data.repository

import baccaro.vestite.app.features.weather.data.location.LocationService
import baccaro.vestite.app.features.weather.data.mapper.toDomain
import baccaro.vestite.app.features.weather.data.remote.dto.WeatherResponseDto
import baccaro.vestite.app.features.weather.domain.model.Location
import baccaro.vestite.app.features.weather.domain.model.Weather
import baccaro.vestite.app.features.weather.domain.repository.WeatherRepository
import dev.jordond.compass.Coordinates
import dev.jordond.compass.geocoder.MobileGeocoder
import dev.jordond.compass.geocoder.placeOrNull
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * Implementación del repositorio de clima
 *
 * Usa Open-Meteo API para obtener datos del clima, LocationService
 * para obtener la ubicación del dispositivo, y MobileGeocoder para
 * convertir coordenadas en nombres de lugares.
 *
 * @property httpClient Cliente HTTP para hacer requests a la API
 * @property locationService Servicio de ubicación multiplataforma
 */
class WeatherRepositoryImpl(
    private val httpClient: HttpClient,
    private val locationService: LocationService
) : WeatherRepository {

    companion object {
        private const val BASE_URL = "https://api.open-meteo.com/v1/forecast"
    }

    /**
     * Obtiene el clima actual para una ubicación geográfica
     *
     * Usa Open-Meteo API que es gratuita y no requiere API key.
     * Solicita: temperatura, humedad, sensación térmica, condición climática y viento.
     * Además usa geocoding para obtener el nombre del lugar.
     *
     * @param latitude Latitud en grados decimales
     * @param longitude Longitud en grados decimales
     * @return Result con Weather si es exitoso, o error si falla
     */
    override suspend fun getCurrentWeather(
        latitude: Double,
        longitude: Double
    ): Result<Weather> {
        return try {
            // 1. Obtener datos del clima de Open-Meteo
            val response = httpClient.get(BASE_URL) {
                parameter("latitude", latitude)
                parameter("longitude", longitude)
                parameter(
                    "current",
                    "temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m"
                )
                parameter("timezone", "auto")
            }

            val weatherDto = response.body<WeatherResponseDto>()

            // 2. Obtener nombre del lugar usando reverse geocoding (simple pattern)
            val coordinates = Coordinates(latitude, longitude)
            val locationName = try {
                // Usar MobileGeocoder para obtener la localidad (ciudad)
                val locality = MobileGeocoder().placeOrNull(coordinates)?.locality
                locality ?: "Mi ubicación"
            } catch (e: Exception) {
                // Si falla el geocoding, usar fallback pero continuar con el clima
                "Mi ubicación"
            }

            // 3. Convertir a dominio con nombre de lugar
            val weather = weatherDto.toDomain(locationName)

            Result.success(weather)
        } catch (e: Exception) {
            Result.failure(
                Exception("Error al obtener clima: ${e.message}", e)
            )
        }
    }

    /**
     * Obtiene la ubicación actual del dispositivo
     *
     * Delega al LocationService que usa Compass Geolocation.
     *
     * @return Result con Location si es exitoso, o error si falla
     */
    override suspend fun getLocation(): Result<Location> {
        return locationService.getCurrentLocation()
    }
}
