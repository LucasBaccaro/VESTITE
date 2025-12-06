package baccaro.vestite.app.features.weather.data.mapper

import baccaro.vestite.app.features.weather.data.remote.dto.WeatherResponseDto
import baccaro.vestite.app.features.weather.domain.model.Weather

/**
 * Convierte un DTO de respuesta de Open-Meteo a modelo de dominio
 *
 * @param locationName Nombre opcional del lugar (se puede agregar después con geocoding)
 * @return Objeto Weather del dominio
 */
fun WeatherResponseDto.toDomain(locationName: String? = null): Weather {
    return Weather(
        temperature = current.temperature2m,
        feelsLike = current.apparentTemperature,
        condition = mapWeatherCode(current.weatherCode),
        humidity = current.relativeHumidity2m,
        windSpeed = current.windSpeed10m,
        locationName = locationName
    )
}

/**
 * Mapea el código WMO de clima a descripción legible
 *
 * Basado en WMO Weather interpretation codes (WW)
 * https://open-meteo.com/en/docs
 *
 * @param code Código WMO del clima
 * @return Descripción del clima en español
 */
fun mapWeatherCode(code: Int): String {
    return when (code) {
        0 -> "Despejado"
        1 -> "Mayormente despejado"
        2 -> "Parcialmente nublado"
        3 -> "Nublado"
        45, 48 -> "Niebla"
        51, 53, 55 -> "Llovizna"
        56, 57 -> "Llovizna helada"
        61, 63, 65 -> "Lluvia"
        66, 67 -> "Lluvia helada"
        71, 73, 75 -> "Nieve"
        77 -> "Granizo"
        80, 81, 82 -> "Chubascos"
        85, 86 -> "Chubascos de nieve"
        95 -> "Tormenta"
        96, 99 -> "Tormenta con granizo"
        else -> "Desconocido"
    }
}
