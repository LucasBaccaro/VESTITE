package baccaro.vestite.app.features.weather.domain.model

/**
 * Modelo de dominio para ubicación geográfica
 *
 * @property latitude Latitud en grados decimales
 * @property longitude Longitud en grados decimales
 */
data class Location(
    val latitude: Double,
    val longitude: Double
)
