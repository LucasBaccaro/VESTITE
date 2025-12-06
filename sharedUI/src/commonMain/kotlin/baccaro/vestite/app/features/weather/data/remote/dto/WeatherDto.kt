package baccaro.vestite.app.features.weather.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO para la respuesta de la API de Open-Meteo
 *
 * @property current Datos del clima actual
 * @property currentUnits Unidades de medida utilizadas
 */
@Serializable
data class WeatherResponseDto(
    val current: CurrentWeatherDto,
    @SerialName("current_units")
    val currentUnits: CurrentUnitsDto? = null
)

/**
 * DTO para los datos actuales del clima
 *
 * @property temperature2m Temperatura a 2 metros de altura
 * @property relativeHumidity2m Humedad relativa a 2 metros
 * @property apparentTemperature Sensación térmica
 * @property weatherCode Código de condición climática WMO
 * @property windSpeed10m Velocidad del viento a 10 metros
 */
@Serializable
data class CurrentWeatherDto(
    @SerialName("temperature_2m")
    val temperature2m: Double,
    @SerialName("relative_humidity_2m")
    val relativeHumidity2m: Int,
    @SerialName("apparent_temperature")
    val apparentTemperature: Double,
    @SerialName("weather_code")
    val weatherCode: Int,
    @SerialName("wind_speed_10m")
    val windSpeed10m: Double
)

/**
 * DTO para las unidades de medida
 */
@Serializable
data class CurrentUnitsDto(
    @SerialName("temperature_2m")
    val temperature2m: String? = null,
    @SerialName("wind_speed_10m")
    val windSpeed10m: String? = null
)
