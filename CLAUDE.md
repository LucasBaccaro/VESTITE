# CLAUDE.md

Guía de contexto para Claude Code cuando trabaja con este repositorio.

## Project Overview

VESTITE es un proyecto Kotlin Multiplatform Mobile (KMM) para Android e iOS. Usa Jetpack Compose Multiplatform para UI y sigue **Clean Architecture** con módulos basados en features. El código compartido está en el módulo `sharedUI` con capas separadas para data, domain y presentation.

## Architecture

### Clean Architecture con Feature Modules

```
sharedUI/src/commonMain/kotlin/baccaro/vestite/app/
├── core/
│   ├── data/remote/         # Supabase client
│   ├── di/                  # Koin DI
│   ├── util/                # Utilities
│   └── presentation/
│       ├── navigation/      # App navigation
│       └── components/      # Reusable UI
└── features/
    └── authentication/
        ├── data/
        │   ├── remote/dto/
        │   ├── repository/
        │   └── mapper/
        ├── domain/
        │   ├── model/
        │   ├── repository/
        │   └── usecase/
        ├── presentation/
        │   ├── login/
        │   └── register/
        └── di/
```

**Current Features:**
- `authentication/` - Email/password y Google Sign-In nativo con KMPAuth

**Future Features:**
- `wardrobe/` - Guardarropas
- `assistant/` - AI stylist

## Tech Stack

### Core
- **Kotlin Multiplatform** (Android, iOS)
- **Compose Multiplatform** 1.10.0-rc01
- **Material3** 1.10.0-alpha05
- **Navigation Compose** 2.9.1

### Backend & Auth
- **Supabase KT** 3.2.2 - Auth, Database, Storage, Realtime
- **KMPAuth** 2.1.0 - Google Sign-In nativo
- **Ktor** 3.3.3 - HTTP client

### Other
- **Koin** 4.0.0 - Dependency Injection
- **Room** 2.8.4 - Local database
- **Coil** 3.3.0 - Image loading

## Configuration

### local.properties (Git-ignored)

```properties
supabase.url=https://your-project.supabase.co
supabase.anon.key=your-anon-key
google.web.client.id=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com
```

Acceso en código:
```kotlin
BuildConfig.SUPABASE_URL
BuildConfig.SUPABASE_ANON_KEY
BuildConfig.GOOGLE_WEB_CLIENT_ID
```

## Authentication Flow

### Email/Password
1. Usuario ingresa credenciales en `LoginScreen`/`RegisterScreen`
2. `ViewModel` → `UseCase` → `Repository` → Supabase Auth
3. Sesión guardada automáticamente
4. Navegación reacciona al estado de autenticación

### Google Sign-In (Nativo con KMPAuth)

**Flujo completo:**
```
Usuario toca botón
    ↓
KMPAuth (GoogleButtonUiContainer)
    ↓
UI Nativa de Google:
  - Android: Bottom sheet (One Tap)
  - iOS: GoogleSignIn SDK
    ↓
Usuario selecciona cuenta
    ↓
KMPAuth retorna idToken
    ↓
LoginViewModel.onGoogleSignInResult(idToken)
    ↓
SignInWithGoogleNativeUseCase(idToken)
    ↓
AuthRepository.signInWithGoogleIdToken(idToken)
    ↓
Supabase: auth.signInWith(IDToken)
    ↓
Supabase valida token con Google
    ↓
Sesión creada y guardada
    ↓
Navegación → Home
```

**Implementación:**
```kotlin
// LoginScreen.kt
GoogleButtonUiContainer(
    onGoogleSignInResult = { googleUser ->
        viewModel.onGoogleSignInResult(googleUser?.idToken)
    }
) {
    OutlinedButton(onClick = { this.onClick() }) {
        Text("Continuar con Google")
    }
}

// AuthRepository.kt
suspend fun signInWithGoogleIdToken(idToken: String): Result<User> {
    supabase.auth.signInWith(IDToken) {
        provider = Google
        this.idToken = idToken
    }
    return Result.success(user)
}
```

**Setup requerido:**
- Web Client ID en Google Cloud Console (para Supabase)
- Android Client ID con SHA-1 (para identificar app)
- Ambos IDs configurados en Supabase Dashboard
- Ver `KMPAUTH_SETUP.md` para detalles

## Key Components

### App.kt
- Entry point de la aplicación
- Inicializa KMPAuth con `GoogleAuthProvider.create()`
- Usa `remember { mutableStateOf(false) }` para esperar inicialización
- Muestra UI solo cuando `authReady = true`

### AppActivity.kt (Android)
- Activity principal simplificada
- Solo `onCreate()` con `setContent`
- No maneja deep links ni intents especiales

### AppNavigation.kt
- Navegación centralizada con Compose Navigation
- Observa `isAuthenticated` flow de Supabase
- Redirige automáticamente según estado de auth
- Rutas: Login, Register, Home

### LoginScreen.kt
- Usa `GoogleButtonUiContainer` de KMPAuth
- Callback directo con `idToken`
- Sin deep links ni OAuth web

### AuthRepository
Un solo método para Google:
```kotlin
suspend fun signInWithGoogleIdToken(idToken: String): Result<User>
```

### Use Cases
- `SignInUseCase` - Email/password login
- `SignUpUseCase` - Email/password register
- `SignInWithGoogleNativeUseCase` - Google nativo (único método)
- `SignOutUseCase` - Logout
- `GetCurrentUserUseCase` - Get current user

## Module Structure

### Koin DI
```kotlin
val authenticationModule = module {
    single<AuthRepository> { AuthRepositoryImpl(get()) }

    factory { SignInUseCase(get()) }
    factory { SignUpUseCase(get()) }
    factory { SignInWithGoogleNativeUseCase(get()) }
    factory { SignOutUseCase(get()) }
    factory { GetCurrentUserUseCase(get()) }

    viewModel { LoginViewModel(get(), get()) }
    viewModel { RegisterViewModel(get()) }
}
```

### ViewModel Dependencies
```kotlin
// LoginViewModel recibe solo 2 use cases
LoginViewModel(
    signInUseCase: SignInUseCase,
    signInWithGoogleNativeUseCase: SignInWithGoogleNativeUseCase
)
```

## Build Commands

```bash
# Android
./gradlew :androidApp:assembleDebug

# Clean
./gradlew clean

# Run
# Android Studio: Run 'androidApp'
```

APK location: `androidApp/build/outputs/apk/debug/`

## Development Notes

### General
- **Package**: `baccaro.vestite.app`
- **Android**: minSdk 24 (KMPAuth), targetSdk 36, Java 17
- **iOS**: Targets iosX64, iosArm64, iosSimulatorArm64

### Patterns
- Use `Result<T>` para operaciones que pueden fallar
- ViewModels exponen `StateFlow<State>` para UI
- Use Cases pequeños y enfocados (Single Responsibility)
- Mappers: DTO → Domain Model entre capas
- Navegación reacciona a flows de Supabase

### Authentication
- Supabase maneja sesiones automáticamente
- Tokens se refrescan automáticamente
- Estado persiste entre reinicios de app
- KMPAuth inicializado en App.kt con LaunchedEffect

## Adding New Features

1. Crear estructura en `features/nombre/`:
   ```
   ├── data/
   │   ├── remote/dto/
   │   ├── repository/
   │   └── mapper/
   ├── domain/
   │   ├── model/
   │   ├── repository/
   │   └── usecase/
   ├── presentation/
   │   └── screens/
   └── di/
       └── NombreModule.kt
   ```

2. Crear módulo Koin:
   ```kotlin
   val nombreModule = module {
       single<Repository> { RepositoryImpl(get()) }
       factory { UseCase(get()) }
       viewModel { ViewModel(get()) }
   }
   ```

3. Registrar en `App.kt`:
   ```kotlin
   modules(coreModule, authenticationModule, nombreModule)
   ```

4. Agregar rutas en `AppNavigation.kt`

## Important Files

- `gradle/libs.versions.toml` - Version catalog
- `sharedUI/build.gradle.kts` - KMP config, BuildConfig
- `local.properties` - API keys (git-ignored)
- `KMPAUTH_SETUP.md` - Google Sign-In setup completo
- `App.kt` - Inicialización de KMPAuth
- `AppNavigation.kt` - Navigation y auth state

## Testing

- **Unit tests**: `commonTest/`
- **Repository tests**: Mock Supabase client
- **Use case tests**: Mock repositories
- **ViewModel tests**: Mock use cases
- Room schemas: `sharedUI/schemas/`


## Notas Importantes

- **No hay deep links** para autenticación (todo nativo)
- **No hay OAuth web** (solo KMPAuth nativo)
- **Un solo método** de Google Sign-In: `signInWithGoogleIdToken`
- KMPAuth se inicializa **antes** de mostrar UI
- Supabase maneja **todo el estado** de sesión automáticamente
