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
- `profile/` - Perfil de usuario con logout
- `looks/` - Outfits guardados (stub)
- `aiGeneration/` - Generación AI de outfits (stub)
- `chat/` - Asistente IA de estilo (stub)
- `weather/` - Clima basado en ubicación (Open-Meteo API)

**Future Features:**
- `tryon/` - Virtual Try-On con Gemini 3 Pro
- `assistant/` - AI stylist con chat completo

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
- **Gemini AI** - Análisis automático de prendas (Gemini 2.5 Flash)
- **Gemini AI Image** - Edición y remoción de fondo (Gemini 2.5 Flash Image)
- **Coil** 3.3.0 - Image loading con AsyncImage
- **ExifInterface** 1.3.7 - Manejo de orientación de imágenes (Android)
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

Feature completa para gestión de guardarropa personal con análisis automático de prendas usando Gemini AI. Permite al usuario subir fotos de prendas (desde galería o cámara), que son automáticamente:
1. **Analizadas** por Gemini 2.5 Flash para extraer descripción concisa
2. **Editadas** por Gemini 2.5 Flash Image para remover fondo y mejorar calidad
3. **Almacenadas** en Supabase Storage con imagen profesional lista para marketplace

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
Usuario toca "Galería" o "Cámara"
    ↓
ImagePicker (expect/actual):
  - Android: PickVisualMedia / TakePicture (sin permisos)
  - iOS: UIImagePickerController
  - EXIF orientation handling automático
  - Compresión automática (<5 MB)
    ↓
Imagen seleccionada → ByteArray
    ↓
UploadGarmentViewModel.analyzeGarment():

  Paso 1: Análisis con Gemini 2.5 Flash (~3-5 seg)
    - Prompt conciso enfocado en prenda principal
    - Retorna: { description: "Zapatillas deportivas blancas..." }

  Paso 2: Edición con Gemini 2.5 Flash Image (~20-30 seg)
    - Remueve fondo completamente
    - Reemplaza con blanco puro (#FFFFFF)
    - Preserva todos los detalles de la prenda
    - Bordes limpios sin halos ni artefactos
    - Retorna: imagen editada en base64
    ↓
Preview Screen:
  - Muestra imagen editada con fondo blanco
  - Muestra descripción IA
  - Usuario selecciona categoría (Superior/Inferior/Calzado)
    ↓
Usuario presiona "Guardar Prenda"
    ↓
  1. Upload imagen editada a Supabase Storage (bucket: garments)
     → URL pública
  2. Insert en DB (tabla: garments)
     → Prenda guardada con metadatos
    ↓
Success: Vuelve a lista de prendas con imagen profesional
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
    image_url TEXT,           -- URL en Storage (imagen con fondo blanco)
    ai_description TEXT,      -- Descripción concisa generada por Gemini
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

**Modelos usados:**

1. **`gemini-2.5-flash`** - Análisis de imagen
   - Rápido (~3-5 segundos)
   - Económico (~$0.01 por análisis)
   - Análisis de imagen → JSON estructurado
   - Descripción concisa enfocada en prenda principal

2. **`gemini-2.5-flash-image`** - Edición de imagen
   - Tiempo: ~20-30 segundos
   - Costo: ~$0.039 por imagen editada
   - Background removal + mejora de calidad
   - Output: Imagen profesional con fondo blanco puro

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
```

**IMPORTANTE:** Los timeouts largos son necesarios porque Gemini Image Edit puede tardar 20-30 segundos en procesar.

**Prompts:**

1. **Análisis (Gemini 2.5 Flash):**
```
Analiza la imagen y describe ÚNICAMENTE la prenda de vestir principal de forma CONCISA.

Reglas:
- Si hay múltiples prendas, enfócate en la MÁS PROMINENTE (la que ocupa más espacio)
- Descripción breve: tipo de prenda, color principal, material (si es visible)
- Máximo 10-12 palabras
- NO describas accesorios secundarios, fondo, ni personas

Retorna SOLO un JSON con este campo:
- description: descripción concisa de la prenda principal

Ejemplos:
{"description": "Remera de algodón blanca con estampado central"}
{"description": "Pantalón jean azul oscuro de corte recto"}
{"description": "Zapatillas deportivas blancas con detalles rojos"}
```

2. **Edición de Imagen (Gemini 2.5 Flash Image):**
```
Eres un editor de imágenes profesional especializado en fotografía de producto.

TAREA: Edita esta imagen para aislar la prenda/objeto y colocar un fondo blanco puro.

INSTRUCCIONES CRÍTICAS:

1. PRESERVACIÓN DEL OBJETO:
   - Mantén la prenda/objeto EXACTAMENTE como está
   - NO modifiques colores, texturas, sombras del objeto
   - Conserva todos los pliegues, arrugas y características naturales

2. REMOCIÓN DEL FONDO:
   - Elimina COMPLETAMENTE el fondo original
   - Reemplaza con blanco puro (#FFFFFF)
   - Corta limpiamente los bordes del objeto

3. CALIDAD FINAL:
   - Sin halos, bordes extraños o artefactos
   - Alta definición y claridad
   - Como si fuera una foto profesional de catálogo

RESULTADO ESPERADO: Una imagen de producto profesional con fondo blanco puro, lista para e-commerce.
```

**Implementación:**

```kotlin
// GeminiRepository.kt

// Método 1: Análisis de imagen
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

// Método 2: Edición de imagen (background removal)
suspend fun removeBackground(imageBytes: ByteArray): Result<ByteArray> {
    val prompt = buildBackgroundRemovalPrompt()
    val base64Image = imageBytes.encodeBase64()

    val request = GeminiRequest(
        contents = listOf(
            Content(
                parts = listOf(
                    Part(text = prompt),  // PROMPT PRIMERO
                    Part(inlineData = InlineData(
                        mimeType = "image/jpeg",
                        data = base64Image
                    ))
                )
            )
        )
    )

    val response = httpClient.post(
        "$GEMINI_API_BASE_URL/gemini-2.5-flash-image:generateContent"
    ) {
        header("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
        contentType(ContentType.Application.Json)
        setBody(request)
    }

    val geminiResponse = response.body<GeminiResponse>()

    // Extraer imagen del response
    val imagePart = geminiResponse.candidates?.first()
        ?.content?.parts?.firstOrNull { it.inlineData != null }
    val resultBase64 = imagePart?.inlineData?.data
        ?: throw Exception("No se pudo extraer la imagen procesada")

    // Decodificar base64 a ByteArray
    val resultBytes = kotlin.io.encoding.Base64.decode(resultBase64)

    return Result.success(resultBytes)
}
```

**Puntos Clave:**
- ✅ **Análisis:** Orden imagen PRIMERO, texto DESPUÉS (para gemini-2.5-flash)
- ✅ **Edición:** Orden INVERTIDO - texto PRIMERO, imagen DESPUÉS (para gemini-2.5-flash-image)
- ✅ `encodeDefaults = true` para serializar `mime_type`
- ✅ `responseModalities: ["TEXT"]` deshabilita thinking mode en análisis
- ✅ Validación de tamaño de imagen antes de enviar (<5 MB)
- ✅ Timeout de 60 segundos para Image Edit (puede tardar 20-30 seg)
- ✅ Response de Image Edit contiene `inlineData` con imagen en base64
- ✅ Usar `kotlin.io.encoding.Base64.decode()` para decodificar imagen resultante
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

**Android (actual) - CON COMPRESIÓN Y CORRECCIÓN DE ORIENTACIÓN:**
- `PickVisualMedia` - Photo Picker (sin permisos desde API 33+)
- `TakePicture` - Cámara nativa (guarda en caché, sin permisos)
- `FileProvider` configurado para compartir URIs
- **Corrección automática de orientación EXIF:**
  - Lee metadatos EXIF de la imagen usando `androidx.exifinterface`
  - Aplica rotación correcta (90°, 180°, 270°) según orientación
  - Maneja flip horizontal/vertical si es necesario
  - Garantiza que la imagen se muestre en orientación correcta
- **Compresión automática de imágenes:**
  - Redimensiona a máximo 2048x2048 (mantiene buena calidad)
  - Comprime JPEG con calidad adaptiva (90-50)
  - Asegura que la imagen final sea menor a 5 MB (límite Gemini)
  - Libera memoria automáticamente (Bitmap.recycle())

**Implementación de Compresión y EXIF (Android):**
```kotlin
private fun uriToByteArray(context: Context, uri: Uri): ByteArray {
    val inputStream = context.contentResolver.openInputStream(uri)
    val originalBitmap = BitmapFactory.decodeStream(inputStream)
    inputStream?.close()

    // Leer orientación EXIF y aplicar rotación si es necesario
    val rotatedBitmap = try {
        context.contentResolver.openInputStream(uri)?.use { exifStream ->
            val exif = ExifInterface(exifStream)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(originalBitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(originalBitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(originalBitmap, 270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> flipBitmap(originalBitmap, horizontal = true)
                else -> originalBitmap
            }
        } ?: originalBitmap
    } catch (e: Exception) {
        originalBitmap
    }

    // Si se aplicó rotación, liberar el bitmap original
    if (rotatedBitmap !== originalBitmap) {
        originalBitmap.recycle()
    }

    // Redimensionar si es necesario (max 2048x2048)
    val maxDimension = 2048
    val scale = minOf(
        maxDimension.toFloat() / rotatedBitmap.width,
        maxDimension.toFloat() / rotatedBitmap.height,
        1.0f
    )

    val resizedBitmap = if (scale < 1.0f) {
        val newWidth = (rotatedBitmap.width * scale).toInt()
        val newHeight = (rotatedBitmap.height * scale).toInt()
        Bitmap.createScaledBitmap(rotatedBitmap, newWidth, newHeight, true).also {
            rotatedBitmap.recycle()
        }
    } else {
        rotatedBitmap
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

// Funciones helper para rotación
private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun flipBitmap(bitmap: Bitmap, horizontal: Boolean): Bitmap {
    val matrix = Matrix().apply {
        postScale(if (horizontal) -1f else 1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
```

**Dependencia requerida:**
```kotlin
// sharedUI/build.gradle.kts - androidMain.dependencies
implementation("androidx.exifinterface:exifinterface:1.3.7")
```

**iOS (actual) - IMPLEMENTADO:**
- UIImagePickerController para galería y cámara
- Compresión similar a Android para mantener consistencia
- Sin permisos requeridos (usa Photo Library)

**Ventajas:**
- ✅ NO requiere permisos en Android (Photo Picker + caché privado)
- ✅ APIs modernas (ActivityResultContracts)
- ✅ Mejor privacidad (usuario controla qué compartir)
- ✅ Corrección automática de orientación EXIF (fotos siempre en orientación correcta)
- ✅ Compresión automática transparente al usuario
- ✅ Optimizado para límites de Gemini API (5 MB)
- ✅ Gestión eficiente de memoria

### Koin DI Module

```kotlin
val wardrobeModule = module {
    // HttpClient dedicado para Gemini API (análisis + edición)
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
    single { GeminiRepository(get(qualifier = named("gemini"))) }  // Análisis + Edición
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

### Navigation Architecture

**Arquitectura con BottomBar:**
```
Auth Screens (Login/Register)
    ↓
MainScreen (Scaffold con BottomBar)
    ├─ Home Tab (con FAB y TopAppBar)
    ├─ Wardrobe Tab (grid de prendas)
    ├─ Looks Tab (stub)
    └─ AI Generation Tab (stub)

Secondary Screens (sin BottomBar, con back button)
    ├─ Profile (desde Home TopAppBar)
    ├─ Upload Garment (desde Home FAB)
    ├─ Chat Assistant (desde Home button)
    └─ Garment Detail (futuro)
```

**Stack Management:**
- Navegación entre tabs del BottomBar: NO se acumulan en el stack
- Solo Home queda en el fondo del stack (presionar back sale de la app)
- Secondary screens SÍ se acumulan (puedes volver con back button)

### Navigation Routes

```kotlin
sealed class Screen(val route: String) {
    // Auth screens
    data object Login : Screen("login")
    data object Register : Screen("register")

    // Main screen with BottomBar
    data object Main : Screen("main")

    // BottomBar destinations (inside Main)
    sealed class BottomBar(route: String) : Screen(route) {
        data object Home : BottomBar("home")
        data object Wardrobe : BottomBar("wardrobe")
        data object Looks : BottomBar("looks")
        data object AIGeneration : BottomBar("ai_generation")
    }

    // Secondary screens (without BottomBar)
    data object Profile : Screen("profile")
    data object UploadGarment : Screen("upload_garment")
    data object ChatAssistant : Screen("chat_assistant")
}

// Login/Register → Main
LoginScreen(
    onLoginSuccess = { navController.navigate(Screen.Main.route) }
)

// MainScreen contiene el BottomBar y maneja navegación interna
MainScreen(
    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
    onNavigateToUpload = { navController.navigate(Screen.UploadGarment.route) },
    onNavigateToChatAssistant = { navController.navigate(Screen.ChatAssistant.route) }
)

// HomeScreen (dentro de MainScreen)
HomeScreen(
    onNavigateToProfile = onNavigateToProfile,  // TopAppBar icon
    onNavigateToUpload = onNavigateToUpload,    // FAB
    onNavigateToChatAssistant = onNavigateToChatAssistant  // Button
)
```

### UI Components

**MainScreen:**
- Scaffold con NavigationBar (BottomBar)
- 4 tabs: Home, Wardrobe, Looks, AI Generation
- NavHost interno para manejar tabs
- Stack management optimizado (popUpTo Home)

**HomeScreen:**
- TopAppBar con título "VESTITE" y icono de perfil (top-right)
- FAB (+) para agregar prenda rápidamente
- Botón "Chat con Asistente IA"
- Información del usuario autenticado

**WardrobeListScreen:**
- Grid 2 columnas con `LazyVerticalGrid`
- Filtros por categoría (chips: Todas, Superior, Inferior, Calzado)
- `AsyncImage` de Coil para cargar imágenes
- Empty state cuando no hay prendas
- **Sin FAB** (solo en HomeScreen)

**UploadGarmentScreen (Nuevo Flujo con Preview):**

**Pantalla 1 - Selección:**
- Botones centrados: "📁 Galería" y "📷 Cámara"
- No requiere seleccionar categoría primero

**Pantalla 2 - Analizando:**
- Loading indicator
- Texto: "Analizando prenda con IA..."

**Pantalla 3 - Preview y Confirmación:**
- **Scrolleable** para pantallas pequeñas (`.verticalScroll(rememberScrollState())`)
- Card con imagen editada:
  - Fondo blanco profesional
  - Respeta aspect ratio original (horizontal/vertical)
  - `ContentScale.Fit` con `.heightIn(max = 400.dp)`
- Card con "Análisis IA":
  - Descripción concisa y enfocada (ej: "Zapatillas deportivas blancas...")
- Selector de categoría con `LazyRow` (scroll horizontal)
- Botón "Guardar Prenda" (habilitado solo si hay categoría seleccionada)

**Pantalla 4 - Guardando:**
- Loading indicator
- Texto: "Guardando prenda..."

**ProfileScreen:**
- TopAppBar con back button
- Foto de perfil circular (placeholder)
- Información del usuario (nombre, email)
- Botón "Cerrar Sesión" (rojo)

**ChatAssistantScreen:**
- Placeholder para futuro chat IA
- TopAppBar con back button

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

**Flujo completo de navegación:**
1. **Login** → **MainScreen** (BottomBar visible)
2. **Home Tab** por defecto:
   - TopAppBar: "VESTITE" | [Icono Perfil]
   - FAB: (+)
   - Botón: "Chat con Asistente IA"
3. Navegación entre tabs (Home, Wardrobe, Looks, AI Gen) - BottomBar siempre visible
4. Presionar back desde cualquier tab → **Sale de la app**

**Flujo de upload de prenda (ACTUALIZADO CON BACKGROUND REMOVAL):**
1. Home → FAB (+) → **UploadGarmentScreen**
2. **Pantalla Inicial**: "📁 Galería" o "📷 Cámara"
3. Usuario selecciona imagen → **Analizando con IA...** (loading ~25-35 segundos total)
   - Análisis con Gemini 2.5 Flash (~3-5 seg)
   - Edición con Gemini 2.5 Flash Image (~20-30 seg)
4. **Preview Screen** muestra:
   - Imagen editada con fondo blanco profesional
   - Análisis IA: "Zapatillas deportivas blancas con detalles negros y naranjas"
   - Selector scrolleable: [Superior] [Inferior] [Calzado]
5. Usuario selecciona categoría → **"Guardar Prenda"**
6. **Guardando...** (loading)
7. Success → Vuelve a Wardrobe tab
8. Prenda aparece en grid con imagen profesional con fondo blanco + descripción AI

**Flujo de perfil:**
1. Home → Icono perfil (top-right) → **ProfileScreen**
2. Muestra: foto, nombre, email
3. Botón: "Cerrar Sesión" → Logout → Login screen

**Flujo de chat:**
1. Home → "Chat con Asistente IA" → **ChatAssistantScreen**
2. Placeholder: "Coming Soon"

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
- **Rutas principales:**
  - Auth: Login, Register
  - Main: Contiene BottomBar (Home, Wardrobe, Looks, AI Generation)
  - Secondary: Profile, UploadGarment, ChatAssistant
- **Stack management:** popUpTo para evitar acumulación de tabs

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
- **Image Picker** usa expect/actual pattern con EXIF handling y compresión automática
  - Android: PickVisualMedia + TakePicture (sin permisos)
  - iOS: IMPLEMENTADO con UIImagePickerController
  - **EXIF orientation handling** con `androidx.exifinterface:1.3.7`
  - Corrección automática de rotación (90°, 180°, 270°)
  - Compresión automática a <5 MB (redimensiona a 2048x2048, JPEG calidad adaptiva 90-50)
- **Gemini AI Dual Processing:**
  1. **Análisis** con `gemini-2.5-flash` (~3-5 seg)
     - Prompt conciso enfocado en prenda principal
     - Descripción máximo 10-12 palabras
     - Request format: imagen PRIMERO, texto DESPUÉS
     - `responseModalities: ["TEXT"]` deshabilita thinking mode
  2. **Edición** con `gemini-2.5-flash-image` (~20-30 seg)
     - Background removal automático
     - Fondo blanco puro (#FFFFFF)
     - Request format: texto PRIMERO, imagen DESPUÉS
     - Respuesta contiene imagen editada en base64
     - **CRÍTICO:** Timeout de 60 segundos (puede tardar 20-30 seg)
- **Configuración crítica:**
  - `encodeDefaults = true` en JSON serializer
  - `HttpTimeout` con 60 segundos para Image Edit
  - Validación de tamaño antes de enviar (<5 MB)
  - Usar `kotlin.io.encoding.Base64.decode()` para imagen resultante
- **Supabase Storage** guarda imágenes editadas con URLs públicas
  - Bucket `garments` con políticas RLS configuradas
  - CRÍTICO: Configurar políticas de INSERT/UPDATE/DELETE en Storage
  - Sin políticas: error "new row violates row-level security policy"
  - Imágenes guardadas con fondo blanco profesional (listas para marketplace)
- **RLS activado** - cada usuario solo ve/edita sus datos
- **Trigger automático** crea perfil al registrarse usuario
- **FileProvider** configurado para compartir imágenes de cámara (Android)
- **Costo por prenda:** ~$0.05 USD total (análisis + edición)

### Próximos Features
- Virtual Try-On con Gemini 3 Pro (ver `MINI.ROADMAP.md`)
- ProfileScreen para subir foto de cuerpo entero
- Recomendaciones AI de outfits

## Weather Feature (Clima)

### Overview

Feature completo para mostrar el clima actual basado en la ubicación del usuario. Se muestra automáticamente en la HomeScreen mediante un `WeatherCard` que se actualiza al montar el componente.

### Arquitectura

```
features/weather/
├── data/
│   ├── remote/
│   │   ├── dto/          # DTOs de Open-Meteo API
│   │   │   └── WeatherDto.kt
│   │   └── repository/
│   │       └── WeatherRepositoryImpl.kt
│   ├── location/         # Servicios de ubicación (expect/actual)
│   │   ├── LocationService.kt
│   │   ├── LocationService.android.kt
│   │   └── LocationService.ios.kt
│   └── mapper/
│       └── WeatherMapper.kt   # Mappers DTO → Domain + WMO codes
├── domain/
│   ├── model/
│   │   ├── Location.kt        # Coordenadas geográficas
│   │   └── Weather.kt         # Datos del clima
│   ├── repository/
│   │   └── WeatherRepository.kt
│   └── usecase/
│       ├── GetLocationUseCase.kt
│       └── GetCurrentWeatherUseCase.kt
├── presentation/
│   ├── WeatherCard.kt        # Card para HomeScreen
│   ├── WeatherViewModel.kt
│   └── WeatherState.kt
└── di/
    ├── WeatherModule.kt
    ├── LocationServiceModule.kt (expect/actual)
    ├── LocationServiceModule.android.kt
    └── LocationServiceModule.ios.kt
```

### Flujo Completo

```
HomeScreen monta → WeatherCard se renderiza
    ↓
LaunchedEffect(Unit) → viewModel.loadWeather()
    ↓
GetLocationUseCase → LocationService (expect/actual)
    ├─ Android: FusedLocationProviderClient
    └─ iOS: CLLocationManager
    ↓
Location { latitude, longitude }
    ↓
GetCurrentWeatherUseCase(lat, lon)
    ↓
WeatherRepositoryImpl → Open-Meteo API
    GET https://api.open-meteo.com/v1/forecast?
        latitude=X&longitude=Y&
        current=temperature_2m,relative_humidity_2m,
                apparent_temperature,weather_code,wind_speed_10m
    ↓
WeatherResponseDto → toDomain() → Weather
    ↓
WeatherState.weather actualizado
    ↓
UI muestra: temperatura, condición, humedad, viento
```

### Open-Meteo API

**API usada:** https://api.open-meteo.com/v1/forecast

**Ventajas:**
- ✅ Gratuita (sin API key necesaria)
- ✅ Sin límites para uso personal
- ✅ Datos actualizados en tiempo real
- ✅ Documentación completa

**Datos obtenidos:**
- `temperature_2m` - Temperatura a 2m de altura (°C)
- `apparent_temperature` - Sensación térmica (°C)
- `relative_humidity_2m` - Humedad relativa (%)
- `wind_speed_10m` - Velocidad del viento (km/h)
- `weather_code` - Código WMO de condición climática

**Mapeo de Códigos WMO:**
```kotlin
0 → "Despejado"
1 → "Mayormente despejado"
2 → "Parcialmente nublado"
3 → "Nublado"
45, 48 → "Niebla"
51-57 → "Llovizna" / "Llovizna helada"
61-67 → "Lluvia" / "Lluvia helada"
71-77 → "Nieve" / "Granizo"
80-86 → "Chubascos" / "Chubascos de nieve"
95-99 → "Tormenta" / "Tormenta con granizo"
```

### Location Service (Expect/Actual)

**Patrón expect/actual para servicios multiplataforma:**

```kotlin
// commonMain/LocationService.kt
expect class LocationService {
    suspend fun getCurrentLocation(): Result<Location>
}
```

**Android (FusedLocationProviderClient):**
- Requiere Google Play Services (`play-services-location:21.3.0`)
- Permisos: `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION`
- Estrategia: Intenta `lastLocation` primero (rápido), luego `getCurrentLocation`
- Error handling: Retorna `null` en vez de lanzar excepciones
- Inyección de dependencias: Recibe `Context` automáticamente por Koin Android

**iOS (CLLocationManager):**
- Usa CoreLocation framework nativo
- Permisos: `NSLocationWhenInUseUsageDescription` en Info.plist
- Implementación con delegate pattern usando coroutines
- No requiere dependencias externas

### WeatherCard Component

**Estados:**
1. **Loading:** CircularProgressIndicator + "Obteniendo clima..."
2. **Error:** Icono + mensaje + botón "Reintentar"
3. **Success:** 
   - Header: Ubicación + botón refresh
   - Temperatura principal (grande)
   - Sensación térmica
   - Detalles: Humedad (%) y Viento (km/h)

**Características:**
- Se carga automáticamente con `LaunchedEffect(Unit)` al montar
- Botón de refresh manual para actualizar datos
- Diseño responsivo con Material3
- Iconos descriptivos (ubicación, humedad, viento)

### Koin DI Module

```kotlin
val weatherModule = module {
    // HttpClient para Weather API
    single<HttpClient>(qualifier = named("weather")) {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
    }

    // Location Service (platform-specific)
    locationServiceModule()

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
```

**Platform-specific DI:**
```kotlin
// Android
actual fun locationServiceModule(): Module = module {
    single { LocationService(get()) } // Context inyectado automáticamente
}

// iOS
actual fun locationServiceModule(): Module = module {
    single { LocationService() } // Sin parámetros
}
```

### Setup Completo

**Android:**
1. Permisos en `AndroidManifest.xml`:
   ```xml
   <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
   <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
   ```

2. Dependencias en `build.gradle.kts`:
   ```kotlin
   implementation("com.google.android.gms:play-services-location:21.3.0")
   implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
   ```

**iOS:**
1. Permisos en `iosApp/Info.plist`:
   ```xml
   <key>NSLocationWhenInUseUsageDescription</key>
   <string>VESTITE necesita acceso a tu ubicación para mostrarte el clima actual</string>
   ```

**Registro:**
- Módulo agregado en `App.kt`: `weatherModule`
- `WeatherCard` integrado en `HomeScreen.kt`

### Notas Importantes

**Multiplataforma:**
- **Expect/Actual:** Usado para LocationService con diferente DI por plataforma
- **SecurityException:** No existe en iOS/Native. Usar verificación basada en strings:
  ```kotlin
  // ❌ NO funciona en multiplataforma
  error is SecurityException -> "Permisos denegados"
  
  // ✅ Funciona en todas las plataformas
  error.message?.contains("Permisos", ignoreCase = true) == true -> "Permisos denegados"
  ```

**Error Handling:**
- Android LocationService retorna `null` en caso de error en vez de lanzar excepciones
- ViewModel maneja todos los estados (loading, error, success)
- Mensajes de error descriptivos según tipo de fallo

**Permisos Runtime:**
- Android: Los permisos se declaran en manifest, pero runtime prompts manejados por el sistema
- iOS: Permisos solicitados automáticamente por CLLocationManager al llamar `requestWhenInUseAuthorization()`
- TODO futuro: Agregar UI para solicitar permisos explícitamente antes de usar LocationService

### Testing

```bash
./gradlew :androidApp:assembleDebug
```

**Flujo de prueba:**
1. Abrir app → Login → HomeScreen
2. `WeatherCard` se muestra en la parte superior
3. Loading state aparecer brevemente
4. Si hay error de permisos: Mensaje + botón reintentar
5. Si es exitoso: Card muestra temperatura, condición, humedad, viento
6. Tocar botón refresh actualiza datos

