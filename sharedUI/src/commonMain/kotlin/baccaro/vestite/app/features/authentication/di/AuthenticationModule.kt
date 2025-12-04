package baccaro.vestite.app.features.authentication.di

import baccaro.vestite.app.features.authentication.data.repository.AuthRepositoryImpl
import baccaro.vestite.app.features.authentication.domain.repository.AuthRepository
import baccaro.vestite.app.features.authentication.domain.usecase.*
import baccaro.vestite.app.features.authentication.presentation.login.LoginViewModel
import baccaro.vestite.app.features.authentication.presentation.register.RegisterViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Módulo de Koin para el feature de autenticación
 */
val authenticationModule = module {

    // Repository
    single<AuthRepository> {
        AuthRepositoryImpl(get())
    }

    // Use Cases
    factory { SignInUseCase(get()) }
    factory { SignUpUseCase(get()) }
    factory { SignInWithGoogleNativeUseCase(get()) }
    factory { SignOutUseCase(get()) }
    factory { GetCurrentUserUseCase(get()) }

    // ViewModels
    viewModel { LoginViewModel(get(), get()) }
    viewModel { RegisterViewModel(get()) }
}
