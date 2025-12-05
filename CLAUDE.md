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
- `wardrobe/` - Guardarropa con análisis AI y gestión de prendas

**Future Features:**
- `tryon/` - Virtual Try-On con Gemini 3 Pro
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

### AI & Image Processing
- **Gemini AI** - Análisis automático de prendas (Gemini 2.0 Flash)
- **Coil** 3.3.0 - Image loading con AsyncImage
- **FileProvider** - Compartir imágenes para cámara (Android)

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
gemini.api.key=YOUR_GEMINI_API_KEY
```

Acceso en código:
```kotlin
BuildConfig.SUPABASE_URL
BuildConfig.SUPABASE_ANON_KEY
BuildConfig.GOOGLE_WEB_CLIENT_ID
BuildConfig.GEMINI_API_KEY
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

## Wardrobe Feature (Guardarropa)

### Overview

Feature completa para gestión de guardarropa personal con análisis automático de prendas usando Gemini AI. Permite al usuario subir fotos de prendas (desde galería o cámara), que son automáticamente analizadas por IA para extraer metadatos (descripción, tipo de ajuste), y almacenadas en Supabase.

### Arquitectura

```
features/wardrobe/
├── data/
│   ├── remote/
│   │   ├── dto/          # DTOs de Gemini y Supabase
│   │   │   ├── CategoryDto.kt
│   │   │   ├── GarmentDto.kt
│   │   │   └── GeminiDto.kt
│   │   └── repository/
│   │       ├── GeminiRepository.kt       # Cliente Gemini Flash API
│   │       └── GarmentRepositoryImpl.kt  # Implementación con Supabase
│   └── mapper/           # Mappers DTO → Domain
│       ├── CategoryMapper.kt
│       ├── GarmentMapper.kt
│       └── GeminiMapper.kt
├── domain/
│   ├── model/
│   │   ├── Category.kt          # Categorías (upper, lower, footwear)
│   │   ├── Garment.kt           # Prenda con metadatos AI
│   │   └── GarmentMetadata.kt   # Resultado análisis Gemini
│   ├── repository/
│   │   └── GarmentRepository.kt # Interface del repositorio
│   └── usecase/
│       ├── UploadGarmentUseCase.kt
│       ├── GetGarmentsUseCase.kt
│       ├── GetGarmentsByCategoryUseCase.kt
│       ├── GetCategoriesUseCase.kt
│       └── DeleteGarmentUseCase.kt
├── presentation/
│   ├── list/
│   │   ├── WardrobeListScreen.kt    # Grid de prendas con filtros
│   │   ├── WardrobeListViewModel.kt
│   │   └── WardrobeListState.kt
│   └── upload/
│       ├── UploadGarmentScreen.kt   # Upload con galería/cámara
│       ├── UploadGarmentViewModel.kt
│       └── UploadGarmentState.kt
└── di/
    └── WardrobeModule.kt  # Koin DI
```

### Flujo Completo de Upload

```
Usuario selecciona categoría (Superior/Inferior/Calzado)
    ↓
Usuario toca "Galería" o "Cámara"
    ↓
ImagePicker (expect/actual):
  - Android: PickVisualMedia / TakePicture (sin permisos)
  - iOS: TODO (stub preparado)
    ↓
Imagen seleccionada → ByteArray
    ↓
UploadGarmentUseCase:
  1. Gemini Flash analiza imagen
     → { description: "...", fit: "regular" }
  2. Upload a Supabase Storage (bucket: garments)
     → URL pública
  3. Insert en DB (tabla: garments)
     → Prenda guardada con metadatos
    ↓
Success: Vuelve a lista de prendas
```

### Database Schema

**Tablas:**

```sql
-- Perfiles (para Virtual Try-On futuro)
profiles (
    id UUID PK → auth.users(id),
    full_body_image_url TEXT,
    created_at, updated_at
)

-- Categorías (predefinidas)
categories (
    id UUID PK,
    slug TEXT UNIQUE ('upper', 'lower', 'footwear'),
    display_name TEXT
)

-- Prendas con metadatos AI
garments (
    id UUID PK,
    user_id UUID → auth.users(id),
    category_id UUID → categories(id),
    image_url TEXT,           -- URL en Storage
    ai_description TEXT,      -- Generado por Gemini
    ai_fit TEXT,              -- tight/regular/loose/oversized
    created_at, updated_at
)

-- Outfits generados (futuro)
outfits (
    id UUID PK,
    user_id UUID → auth.users(id),
    generated_image_url TEXT,
    occasion TEXT,
    upper_garment_id, lower_garment_id, footwear_garment_id,
    created_at
)
```

**RLS (Row Level Security):**
- Todos los datos son privados por usuario
- Políticas: users solo ven/editan sus propios datos
- Trigger automático: crea perfil al registrarse un usuario

**Storage Buckets:**
- `garments` (public) - Imágenes de prendas
- `avatars` (public) - Fotos de perfil
- `outfits` (private) - Outfits generados

**Storage Policies (CRÍTICO - Configurar en Supabase Dashboard):**
```sql
-- Permitir a usuarios autenticados subir sus propias imágenes
CREATE POLICY "Users can upload their own garments"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (
  bucket_id = 'garments'
  AND (storage.foldername(name))[1] = auth.uid()::text
);

-- Permitir a usuarios actualizar sus propias imágenes
CREATE POLICY "Users can update their own garments"
ON storage.objects FOR UPDATE
TO authenticated
USING (
  bucket_id = 'garments'
  AND (storage.foldername(name))[1] = auth.uid()::text
);

-- Permitir a usuarios eliminar sus propias imágenes
CREATE POLICY "Users can delete their own garments"
ON storage.objects FOR DELETE
TO authenticated
USING (
  bucket_id = 'garments'
  AND (storage.foldername(name))[1] = auth.uid()::text
);

-- Permitir lectura pública de imágenes
CREATE POLICY "Anyone can view garments"
ON storage.objects FOR SELECT
TO public
USING (bucket_id = 'garments');
```

**Nota:** Sin estas políticas, obtendrás error "new row violates row-level security policy" al intentar subir imágenes.

### Gemini AI Integration

**Modelo usado:** `gemini-2.5-flash`
- Rápido (~1-2 segundos)
- Económico y estable
- Análisis de imagen → JSON estructurado
- Modelo actualizado y más confiable que la versión experimental

**Configuración Crítica:**
```kotlin
// WardrobeModule.kt - HttpClient para Gemini
single<HttpClient>(qualifier = named("gemini")) {
    HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = true
                encodeDefaults = true  // CRÍTICO: Serializa valores por defecto (mime_type)
            })
        }
    }
}
```

**Prompt:**
```
Analiza esta prenda de vestir. Retorna un JSON con exactamente estos campos:
- description: descripción visual detallada (color, material, tipo, estilo)
- fit: tipo de ajuste (debe ser exactamente uno de estos: "tight", "regular", "loose", "oversized")

Responde SOLO con el JSON, sin markdown ni texto adicional.

Ejemplo:
{"description": "Campera de cuero negra tipo biker con cierre metálico", "fit": "regular"}
```

**Implementación Correcta (basada en código React Native funcional):**
```kotlin
// GeminiRepository.kt
suspend fun analyzeGarmentImage(imageBytes: ByteArray): Result<GarmentMetadata> {
    val prompt = buildAnalysisPrompt()
    val base64Image = imageBytes.encodeBase64()

    // Validar tamaño de imagen (límite Gemini: 5 MB)
    val imageSizeMB = imageBytes.size / (1024.0 * 1024.0)
    if (imageSizeMB > 5.0) {
        throw Exception("Imagen muy grande (${imageSizeMB} MB). Gemini acepta hasta 5 MB.")
    }

    // Request con formato EXACTO del código React Native que funciona
    val request = GeminiRequest(
        contents = listOf(
            Content(
                parts = listOf(
                    // CRÍTICO: Imagen PRIMERO, texto DESPUÉS
                    Part(inlineData = InlineData(
                        mimeType = "image/jpeg",
                        data = base64Image
                    )),
                    Part(text = prompt)
                )
            )
        ),
        generationConfig = GenerationConfig(
            temperature = 0.1,                    // Respuestas consistentes
            maxOutputTokens = 4096,               // Suficiente para JSON
            responseModalities = listOf("TEXT")   // Deshabilita thinking mode
        )
    )

    val response = httpClient.post(
        "$GEMINI_API_BASE_URL/gemini-2.5-flash:generateContent"
    ) {
        header("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
        contentType(ContentType.Application.Json)
        setBody(request)
    }

    // Validar HTTP status
    if (response.status.value !in 200..299) {
        throw Exception("API request failed: HTTP ${response.status.value}")
    }

    // Parse y validación robusta de respuesta
    val geminiResponse = response.body<GeminiResponse>()

    // Validar candidatos y finish reason
    val candidates = geminiResponse.candidates
    if (candidates.isNullOrEmpty()) {
        throw Exception("Gemini no retornó candidatos")
    }

    val candidate = candidates.first()
    when (candidate.finishReason) {
        "SAFETY" -> throw Exception("Contenido bloqueado por seguridad")
        "RECITATION" -> throw Exception("Contenido bloqueado por copyright")
        "MAX_TOKENS" -> throw Exception("Respuesta truncada")
    }

    val textResponse = candidate.content?.parts?.firstOrNull()?.text
    if (textResponse.isNullOrBlank()) {
        throw Exception("Sin respuesta de texto")
    }

    // Extraer y parsear JSON
    val jsonText = extractJson(textResponse)
    val analysisResponse = json.decodeFromString<GarmentAnalysisResponse>(jsonText)

    return Result.success(analysisResponse.toDomain())
}
```

**Puntos Clave:**
- ✅ Orden correcto: imagen PRIMERO, texto DESPUÉS
- ✅ `encodeDefaults = true` para serializar `mime_type`
- ✅ `responseModalities: ["TEXT"]` deshabilita thinking mode
- ✅ Validación de tamaño de imagen antes de enviar
- ✅ Error handling robusto para todos los casos edge

### Image Picker (Expect/Actual)

**Multiplatforma con expect/actual pattern:**

```kotlin
// commonMain/ImagePicker.kt
interface ImagePickerLauncher {
    fun launchGallery()
    fun launchCamera()
}

@Composable
expect fun rememberImagePicker(
    onImageSelected: (imageBytes: ByteArray?, fileName: String?) -> Unit
): ImagePickerLauncher
```

**Android (actual) - CON COMPRESIÓN AUTOMÁTICA:**
- `PickVisualMedia` - Photo Picker (sin permisos desde API 33+)
- `TakePicture` - Cámara nativa (guarda en caché, sin permisos)
- `FileProvider` configurado para compartir URIs
- **Compresión automática de imágenes:**
  - Redimensiona a máximo 2048x2048 (mantiene buena calidad)
  - Comprime JPEG con calidad adaptiva (90-50)
  - Asegura que la imagen final sea menor a 5 MB (límite Gemini)
  - Libera memoria automáticamente (Bitmap.recycle())

**Implementación de Compresión (Android):**
```kotlin
private fun uriToByteArray(context: Context, uri: Uri): ByteArray {
    val inputStream = context.contentResolver.openInputStream(uri)
    val originalBitmap = BitmapFactory.decodeStream(inputStream)

    // Redimensionar si es necesario (max 2048x2048)
    val maxDimension = 2048
    val scale = minOf(
        maxDimension.toFloat() / originalBitmap.width,
        maxDimension.toFloat() / originalBitmap.height,
        1.0f
    )

    val resizedBitmap = if (scale < 1.0f) {
        Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
    } else {
        originalBitmap
    }

    // Comprimir con calidad adaptiva hasta estar bajo 5 MB
    var quality = 90
    do {
        val outputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        compressedBytes = outputStream.toByteArray()

        if (sizeMB <= 5.0) break
        quality -= 10
    } while (quality >= 50)

    return compressedBytes
}
```

**iOS (actual) - IMPLEMENTADO:**
- UIImagePickerController para galería y cámara
- Compresión similar a Android para mantener consistencia
- Sin permisos requeridos (usa Photo Library)

**Ventajas:**
- ✅ NO requiere permisos en Android (Photo Picker + caché privado)
- ✅ APIs modernas (ActivityResultContracts)
- ✅ Mejor privacidad (usuario controla qué compartir)
- ✅ Compresión automática transparente al usuario
- ✅ Optimizado para límites de Gemini API (5 MB)
- ✅ Gestión eficiente de memoria

### Koin DI Module

```kotlin
val wardrobeModule = module {
    // HttpClient dedicado para Gemini API
    single<HttpClient>(qualifier = named("gemini")) {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = true
                    encodeDefaults = true  // CRÍTICO: Serializa valores por defecto
                })
            }

            install(Logging) {
                logger = Logger.SIMPLE
                level = LogLevel.INFO
            }
        }
    }

    // Repositories
    single { GeminiRepository(get(qualifier = named("gemini"))) }
    single<GarmentRepository> { GarmentRepositoryImpl(get(), get()) }

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
```

### Navigation Routes

```kotlin
sealed class Screen(val route: String) {
    // ... auth routes
    data object WardrobeList : Screen("wardrobe_list")
    data object UploadGarment : Screen("upload_garment")
}

// Home → Wardrobe
HomeScreen(
    onNavigateToWardrobe = { navController.navigate(Screen.WardrobeList.route) }
)

// Wardrobe → Upload
WardrobeListScreen(
    onNavigateToUpload = { navController.navigate(Screen.UploadGarment.route) }
)

// Upload → selecciona imagen, analiza, guarda
UploadGarmentScreen(
    onNavigateBack = { navController.popBackStack() }
)
```

### UI Components

**WardrobeListScreen:**
- Grid 2 columnas con `LazyVerticalGrid`
- Filtros por categoría (chips)
- `SubcomposeAsyncImage` de Coil para cargar imágenes
- FloatingActionButton para agregar prenda
- Empty state cuando no hay prendas

**UploadGarmentScreen:**
- Selector de categoría (chips)
- Dos botones: "Galería" y "Cámara"
- Progress indicator durante análisis AI
- Snackbar de éxito/error
- Validación: categoría + imagen requeridos

### Setup Completo

Ver documentación detallada:
- `WARDROBE_SETUP.md` - Configuración completa (DB, Storage, Gemini)
- `IMAGE_PICKER_PERMISSIONS.md` - Explicación sobre permisos
- `PROFILE_TRIGGER_SETUP.md` - Trigger para crear perfiles automáticamente
- `supabase/schema.sql` - Schema SQL ejecutable

**Pasos requeridos:**
1. Ejecutar `supabase/schema.sql` en Supabase SQL Editor
2. Crear buckets en Supabase Storage
3. Configurar Storage Policies (RLS)
4. Agregar `gemini.api.key` en `local.properties`
5. Para Android: FileProvider ya configurado en `AndroidManifest.xml`

### Testing

```bash
./gradlew :androidApp:assembleDebug
```

**Flujo completo:**
1. Login → Home → "Ver mi Guardarropa"
2. Lista vacía → "+" → Upload screen
3. Seleccionar categoría (Superior/Inferior/Calzado)
4. Toca "Galería" → Photo Picker → Selecciona foto
5. O toca "Cámara" → App cámara → Toma foto
6. Loading: "Analizando prenda con IA..."
7. Success → Vuelve a lista
8. Prenda aparece en grid con imagen + descripción AI

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

### Configuration
- `gradle/libs.versions.toml` - Version catalog
- `sharedUI/build.gradle.kts` - KMP config, BuildConfig
- `local.properties` - API keys (git-ignored)
- `androidApp/src/main/AndroidManifest.xml` - FileProvider configurado

### Documentation
- `KMPAUTH_SETUP.md` - Google Sign-In setup completo
- `WARDROBE_SETUP.md` - Setup completo de Wardrobe feature
- `IMAGE_PICKER_PERMISSIONS.md` - Explicación sobre permisos de imagen
- `PROFILE_TRIGGER_SETUP.md` - Trigger automático de perfiles
- `SUPABASE_KMP.md` - Documentación de Supabase-KT
- `MINI.ROADMAP.md` - Roadmap para Virtual Try-On

### Core Files
- `App.kt` - Inicialización de KMPAuth
- `AppNavigation.kt` - Navigation y auth state
- `supabase/schema.sql` - Database schema completo

### Platform-specific
- `ImagePicker.kt` (commonMain) - Expect definition
- `ImagePicker.android.kt` (androidMain) - Android implementation con compresión automática
- `ImagePicker.ios.kt` (iosMain) - iOS implementation con compresión automática

## Testing

- **Unit tests**: `commonTest/`
- **Repository tests**: Mock Supabase client
- **Use case tests**: Mock repositories
- **ViewModel tests**: Mock use cases
- Room schemas: `sharedUI/schemas/`


## Notas Importantes

### Authentication
- **No hay deep links** para autenticación (todo nativo)
- **No hay OAuth web** (solo KMPAuth nativo)
- **Un solo método** de Google Sign-In: `signInWithGoogleIdToken`
- KMPAuth se inicializa **antes** de mostrar UI
- Supabase maneja **todo el estado** de sesión automáticamente

### Wardrobe
- **Image Picker** usa expect/actual pattern con compresión automática
  - Android: PickVisualMedia + TakePicture (sin permisos)
  - iOS: IMPLEMENTADO con UIImagePickerController
  - Compresión automática a <5 MB (redimensiona a 2048x2048, JPEG calidad adaptiva 90-50)
- **Gemini AI** con modelo `gemini-2.5-flash` (estable)
  - Analiza automáticamente cada prenda subida
  - Configuración crítica: `encodeDefaults = true` en JSON serializer
  - Request format: imagen PRIMERO, texto DESPUÉS
  - `responseModalities: ["TEXT"]` deshabilita thinking mode
  - Validación de tamaño antes de enviar (<5 MB)
- **Supabase Storage** guarda imágenes con URLs públicas
  - Bucket `garments` con políticas RLS configuradas
  - CRÍTICO: Configurar políticas de INSERT/UPDATE/DELETE en Storage
  - Sin políticas: error "new row violates row-level security policy"
- **RLS activado** - cada usuario solo ve/edita sus datos
- **Trigger automático** crea perfil al registrarse usuario
- **FileProvider** configurado para compartir imágenes de cámara (Android)

### Próximos Features
- Virtual Try-On con Gemini 3 Pro (ver `MINI.ROADMAP.md`)
- ProfileScreen para subir foto de cuerpo entero
- Recomendaciones AI de outfits
