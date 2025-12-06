package baccaro.vestite.app.features.weather.data.location

import baccaro.vestite.app.features.weather.domain.model.Location
import dev.jordond.compass.geolocation.Geolocator
import dev.jordond.compass.geolocation.mobile

/**
 * Implementación de LocationService usando Compass library
 *
 * Compass proporciona una API unificada para geolocalización en Android e iOS,
 * manejando automáticamente los permisos y las diferencias de plataforma.
 */
class LocationServiceImpl(
    private val geolocator: Geolocator = Geolocator.mobile()
) : LocationService {

    /**
     * Obtiene la ubicación actual del dispositivo usando Compass
     *
     * Compass maneja automáticamente:
     * - Solicitud de permisos en runtime
     * - Diferencias entre Android e iOS
     * - Timeouts y errores de ubicación
     *
     * @return Result con Location si es exitoso, o error si falla
     */
    override suspend fun getCurrentLocation(): Result<Location> {
        return try {
            // Obtener ubicación actual con Compass (retorna GeolocatorResult)
            val geolocatorResult = geolocator.current()
            
            // Extraer Location del GeolocatorResult
            val compassLocation = geolocatorResult.getOrNull()
                ?: return Result.failure(Exception("No se pudo obtener la ubicación"))
            
            // Convertir a nuestro modelo de dominio
            // compassLocation es dev.jordond.compass.Location
            val location = Location(
                latitude = compassLocation.coordinates.latitude,
                longitude = compassLocation.coordinates.longitude
            )
            
            Result.success(location)
        } catch (e: Exception) {
            // Manejar errores de permisos, GPS deshabilitado, etc.
            val errorMessage = when {
                e.message?.contains("permission", ignoreCase = true) == true -> 
                    "Permisos de ubicación no otorgados"
                e.message?.contains("location", ignoreCase = true) == true -> 
                    e.message ?: "Error al obtener ubicación"
                else -> "No se pudo obtener la ubicación: ${e.message}"
            }
            Result.failure(Exception(errorMessage))
        }
    }
}
