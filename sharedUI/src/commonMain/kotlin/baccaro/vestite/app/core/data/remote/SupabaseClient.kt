package baccaro.vestite.app.core.data.remote

import baccaro.vestite.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.logging.LogLevel
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.PropertyConversionMethod
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

/**
 * Factory para crear el cliente de Supabase
 * Las credenciales se leen desde BuildConfig (configurado en build.gradle.kts)
 */
object SupabaseClientFactory {

    fun create(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            defaultLogLevel = if (BuildConfig.DEBUG) {
                LogLevel.DEBUG
            } else {
                LogLevel.INFO
            }

            install(Auth) {
                flowType = FlowType.PKCE
                alwaysAutoRefresh = true
                autoLoadFromStorage = true
                scheme = "vestite"
                host = "login-callback"
            }

            install(Postgrest) {
                defaultSchema = "public"
                propertyConversionMethod = PropertyConversionMethod.CAMEL_CASE_TO_SNAKE_CASE
            }

            install(Storage)
            install(Realtime)
            install(Functions)
        }
    }
}
