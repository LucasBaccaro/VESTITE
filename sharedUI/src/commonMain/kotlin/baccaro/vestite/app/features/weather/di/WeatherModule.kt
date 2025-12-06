package baccaro.vestite.app.features.weather.di

import baccaro.vestite.app.features.weather.data.location.LocationService
import baccaro.vestite.app.features.weather.data.location.LocationServiceImpl
import baccaro.vestite.app.features.weather.data.repository.WeatherRepositoryImpl
import baccaro.vestite.app.features.weather.domain.repository.WeatherRepository
import baccaro.vestite.app.features.weather.domain.usecase.GetCurrentWeatherUseCase
import baccaro.vestite.app.features.weather.domain.usecase.GetLocationUseCase
import baccaro.vestite.app.features.weather.presentation.WeatherViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Módulo de Koin para el feature de weather (clima)
 *
 * Configura todas las dependencias necesarias:
 * - HttpClient para la API de Open-Meteo
 * - LocationService usando Compass (sin platform-specific code necesario)
 * - Repository y Use Cases
 * - ViewModel
 */
val weatherModule = module {

    // HttpClient para Weather API (Open-Meteo)
    single<HttpClient>(qualifier = named("weather")) {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = true
                })
            }
        }
    }

    // Location Service usando Compass (no requiere platform-specific DI)
    single<LocationService> { LocationServiceImpl() }

    // Repository
    single<WeatherRepository> {
        WeatherRepositoryImpl(
            httpClient = get(qualifier = named("weather")),
            locationService = get()
        )
    }

    // Use Cases
    factory { GetLocationUseCase(get()) }
    factory { GetCurrentWeatherUseCase(get()) }

    // ViewModel
    viewModel { WeatherViewModel(get(), get()) }
}
