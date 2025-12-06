package baccaro.vestite.app.features.weather.domain.model

/**
 * Modelo de dominio para información del clima
 *
 * @property temperature Temperatura actual en grados Celsius
 * @property feelsLike Sensación térmica en grados Celsius
 * @property condition Descripción de la condición climática (ej: "Soleado", "Nublado")
 * @property humidity Humedad relativa en porcentaje (0-100)
 * @property windSpeed Velocidad del viento en km/h
 * @property locationName Nombre de la ubicación (null hasta que geocoding complete)
 */
data class Weather(
    val temperature: Double,
    val feelsLike: Double,
    val condition: String,
    val humidity: Int,
    val windSpeed: Double,
    val locationName: String? = null
)
