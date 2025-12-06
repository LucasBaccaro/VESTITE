package baccaro.vestite.app.features.weather.data.location

import baccaro.vestite.app.features.weather.domain.model.Location

/**
 * Servicio de ubicación usando Compass library
 *
 * Compass maneja automáticamente los permisos y proporciona una API
 * unificada para Android e iOS sin necesidad de expect/actual.
 */
interface LocationService {
    /**
     * Obtiene la ubicación actual del dispositivo
     *
     * @return Result con Location si es exitoso, o error si falla
     */
    suspend fun getCurrentLocation(): Result<Location>
}

