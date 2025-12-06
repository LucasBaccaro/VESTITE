import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
    alias(libs.plugins.buildConfig)
}

kotlin {
    android {
        namespace = "baccaro.vestite.app"
        compileSdk = 36
        minSdk = 24
        androidResources.enable = true
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
            implementation(libs.compose.ui)
            implementation(libs.compose.foundation)
            implementation(libs.compose.resources)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.compose.material3)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.serialization)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.coil)
            implementation(libs.coil.network.ktor)
            implementation(libs.multiplatformSettings)
            implementation(libs.kotlinx.datetime)
            implementation(libs.room.runtime)
            implementation(libs.materialKolor)

            // Supabase (cliente base + plugins)
            implementation(libs.supabase.client)
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.auth)
            implementation(libs.supabase.storage)
            implementation(libs.supabase.realtime)
            implementation(libs.supabase.functions)

            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Navigation
            implementation(libs.navigation.compose)

            // KMPAuth - Google Sign-In
            implementation(libs.kmpauth.google)
            implementation(libs.kmpauth.uihelper)
            
            // Compass - Geolocation & Geocoding
            implementation(libs.compass.geocoder)
            implementation(libs.compass.geocoder.mobile)
            implementation(libs.compass.geolocation)
            implementation(libs.compass.geolocation.mobile)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.compose.ui.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            implementation(libs.compose.ui.tooling)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)

            // Google Play Services for Location
            implementation("com.google.android.gms:play-services-location:21.3.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

            // ExifInterface for image rotation handling
            implementation("androidx.exifinterface:exifinterface:1.3.7")
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

    }

    targets
        .withType<KotlinNativeTarget>()
        .matching { it.konanTarget.family.isAppleFamily }
        .configureEach {
            binaries {
                framework {
                    baseName = "SharedUI"
                    isStatic = true
                }
            }
        }
}

buildConfig {
    packageName("baccaro.vestite.app")

    // Debug flag
    buildConfigField("Boolean", "DEBUG", "true")

    // Lee de local.properties o variables de entorno
    buildConfigField(
        "String",
        "SUPABASE_URL",
        provider {
            val props = project.rootProject.file("local.properties")
            val value = if (props.exists()) {
                val properties = Properties()
                properties.load(props.inputStream())
                properties.getProperty("supabase.url")
            } else {
                System.getenv("SUPABASE_URL")
            } ?: "https://placeholder.supabase.co"
            "\"$value\""
        }
    )

    buildConfigField(
        "String",
        "SUPABASE_ANON_KEY",
        provider {
            val props = project.rootProject.file("local.properties")
            val value = if (props.exists()) {
                val properties = Properties()
                properties.load(props.inputStream())
                properties.getProperty("supabase.anon.key")
            } else {
                System.getenv("SUPABASE_ANON_KEY")
            } ?: "placeholder-key"
            "\"$value\""
        }
    )

    buildConfigField(
        "String",
        "GOOGLE_WEB_CLIENT_ID",
        provider {
            val props = project.rootProject.file("local.properties")
            val value = if (props.exists()) {
                val properties = Properties()
                properties.load(props.inputStream())
                properties.getProperty("google.web.client.id")
            } else {
                System.getenv("GOOGLE_WEB_CLIENT_ID")
            } ?: "placeholder-client-id.apps.googleusercontent.com"
            "\"$value\""
        }
    )

    buildConfigField(
        "String",
        "GEMINI_API_KEY",
        provider {
            val props = project.rootProject.file("local.properties")
            val value = if (props.exists()) {
                val properties = Properties()
                properties.load(props.inputStream())
                properties.getProperty("gemini.api.key")
            } else {
                System.getenv("GEMINI_API_KEY")
            } ?: "placeholder-gemini-key"
            "\"$value\""
        }
    )
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    with(libs.room.compiler) {
        add("kspAndroid", this)
        add("kspIosX64", this)
        add("kspIosArm64", this)
        add("kspIosSimulatorArm64", this)
    }
}
