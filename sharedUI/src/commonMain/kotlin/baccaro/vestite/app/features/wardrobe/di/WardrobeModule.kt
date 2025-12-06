package baccaro.vestite.app.features.wardrobe.di

import baccaro.vestite.app.features.wardrobe.data.repository.GarmentRepositoryImpl
import baccaro.vestite.app.features.wardrobe.data.repository.GeminiRepository
import baccaro.vestite.app.features.wardrobe.domain.repository.GarmentRepository
import baccaro.vestite.app.features.wardrobe.domain.usecase.DeleteGarmentUseCase
import baccaro.vestite.app.features.wardrobe.domain.usecase.GetCategoriesUseCase
import baccaro.vestite.app.features.wardrobe.domain.usecase.GetGarmentsByCategoryUseCase
import baccaro.vestite.app.features.wardrobe.domain.usecase.GetGarmentsUseCase
import baccaro.vestite.app.features.wardrobe.domain.usecase.UploadGarmentUseCase
import baccaro.vestite.app.features.wardrobe.presentation.list.WardrobeListViewModel
import baccaro.vestite.app.features.wardrobe.presentation.upload.UploadGarmentViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Módulo de Koin para el feature de wardrobe (guardarropa)
 */
val wardrobeModule = module {

    // HttpClient para Gemini API
    single<HttpClient>(qualifier = org.koin.core.qualifier.named("gemini")) {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = true
                    encodeDefaults = true  // CRITICAL: Include default values in serialization
                })
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 60_000  // 60 segundos para Image Edit
                connectTimeoutMillis = 15_000  // 15 segundos para conectar
                socketTimeoutMillis = 60_000   // 60 segundos para socket
            }

            install(Logging) {
                logger = Logger.SIMPLE
                level = LogLevel.INFO
            }
        }
    }

    // Repositories
    single {
        GeminiRepository(get(qualifier = org.koin.core.qualifier.named("gemini")))
    }

    single<GarmentRepository> {
        GarmentRepositoryImpl(get(), get())
    }

    // Use Cases
    factory { UploadGarmentUseCase(get()) }
    factory { GetGarmentsUseCase(get()) }
    factory { GetGarmentsByCategoryUseCase(get()) }
    factory { GetCategoriesUseCase(get()) }
    factory { DeleteGarmentUseCase(get()) }

    // ViewModels
    viewModel { UploadGarmentViewModel(get(), get()) }
    viewModel { WardrobeListViewModel(get(), get(), get(), get()) }
}
