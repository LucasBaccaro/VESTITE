# Dependencias Instaladas - VESTITE

Este documento lista todas las dependencias necesarias para el proyecto.

## ✅ Dependencias Actualizadas

### Supabase KT (v3.0.4)

Según la documentación oficial de Supabase-KT, necesitas:

1. **Cliente Base** (requerido):
   ```kotlin
   implementation("io.github.jan-tennert.supabase:supabase-kt:3.0.4")
   ```

2. **Plugins** (los que necesites):
   ```kotlin
   implementation("io.github.jan-tennert.supabase:auth-kt:3.0.4")
   implementation("io.github.jan-tennert.supabase:postgrest-kt:3.0.4")
   implementation("io.github.jan-tennert.supabase:storage-kt:3.0.4")
   implementation("io.github.jan-tennert.supabase:realtime-kt:3.0.4")
   implementation("io.github.jan-tennert.supabase:functions-kt:3.0.4")
   ```

**Ya configurado en:** `gradle/libs.versions.toml` y `sharedUI/build.gradle.kts`

### Koin (v4.0.0)

Para Kotlin Multiplatform con Compose:

```kotlin
implementation("io.insert-koin:koin-core:4.0.0")
implementation("io.insert-koin:koin-compose:4.0.0")
implementation("io.insert-koin:koin-compose-viewmodel:4.0.0")
```

**Ya configurado en:** `gradle/libs.versions.toml` y `sharedUI/build.gradle.kts`

**Documentación:** https://insert-koin.io/docs/reference/koin-compose/multiplatform

### Navigation Compose (v2.9.1)

```kotlin
implementation("org.jetbrains.androidx.navigation:navigation-compose:2.9.1")
```

**Ya configurado en:** `gradle/libs.versions.toml` y `sharedUI/build.gradle.kts`

## 📦 Archivo `gradle/libs.versions.toml`

Las versiones están centralizadas:

```toml
[versions]
supabase = "3.0.4"
koin = "4.0.0"
koin-compose = "4.0.0"
navigation-compose = "2.9.1"

[libraries]
# Supabase
supabase-client = { module = "io.github.jan-tennert.supabase:supabase-kt", version.ref = "supabase" }
supabase-postgrest = { module = "io.github.jan-tennert.supabase:postgrest-kt", version.ref = "supabase" }
supabase-auth = { module = "io.github.jan-tennert.supabase:auth-kt", version.ref = "supabase" }
supabase-storage = { module = "io.github.jan-tennert.supabase:storage-kt", version.ref = "supabase" }
supabase-realtime = { module = "io.github.jan-tennert.supabase:realtime-kt", version.ref = "supabase" }
supabase-functions = { module = "io.github.jan-tennert.supabase:functions-kt", version.ref = "supabase" }

# Koin
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin-compose" }
koin-compose-viewmodel = { module = "io.insert-koin:koin-compose-viewmodel", version.ref = "koin-compose" }

# Navigation
navigation-compose = { module = "org.jetbrains.androidx.navigation:navigation-compose", version.ref = "navigation-compose" }
```

## 📝 Archivo `sharedUI/build.gradle.kts`

En `commonMain.dependencies`:

```kotlin
// Supabase (cliente base + plugins)
implementation(libs.supabase.client)       // ✅ Cliente base
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
```

## ✅ Para Verificar

Ejecuta en terminal:

```bash
./gradlew :sharedUI:dependencies --configuration commonMainImplementation | grep -E "(supabase|koin|navigation)"
```

Deberías ver:

```
+--- io.github.jan-tennert.supabase:supabase-kt:3.0.4
+--- io.github.jan-tennert.supabase:postgrest-kt:3.0.4
+--- io.github.jan-tennert.supabase:auth-kt:3.0.4
+--- io.github.jan-tennert.supabase:storage-kt:3.0.4
+--- io.github.jan-tennert.supabase:realtime-kt:3.0.4
+--- io.github.jan-tennert.supabase:functions-kt:3.0.4
+--- io.insert-koin:koin-core:4.0.0
+--- io.insert-koin:koin-compose:4.0.0
+--- io.insert-koin:koin-compose-viewmodel:4.0.0
+--- org.jetbrains.androidx.navigation:navigation-compose:2.9.1
```

## 🔄 Sincronizar Gradle

Después de verificar los archivos:

1. En Android Studio: **File** → **Sync Project with Gradle Files**
2. O en terminal:
   ```bash
   ./gradlew clean build
   ```

## 📚 Referencias

- **Supabase-KT:** https://github.com/supabase-community/supabase-kt
- **Koin Multiplatform:** https://insert-koin.io/docs/reference/koin-compose/multiplatform
- **Navigation Compose:** https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-navigation-routing.html
