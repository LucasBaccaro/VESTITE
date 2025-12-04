package baccaro.vestite.app.core.di

import baccaro.vestite.app.core.data.remote.SupabaseClientFactory
import org.koin.dsl.module

/**
 * Módulo de Koin para dependencias del core
 */
val coreModule = module {

    // Supabase Client (Singleton)
    single {
        SupabaseClientFactory.create()
    }
}
