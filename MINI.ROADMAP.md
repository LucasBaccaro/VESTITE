Entendido. Corrijo el rumbo inmediatamente.

El requerimiento es claro: **Replicar la lógica exacta del código JavaScript proporcionado en Kotlin Multiplatform**.

Esto significa que para el **Try-On (Gemini 3 Pro)**, no enviaremos solo descripciones. Enviaremos un **Payload Multimodal Complejo** que incluye:
1.  El **Prompt de texto** (con las instrucciones estrictas de preservación de identidad).
2.  La **Imagen del Usuario** (Base64).
3.  Las **Imágenes de las Prendas** seleccionadas (Base64).

Aquí tienes el Roadmap Técnico corregido y detallado para KMP.

---

# Roadmap KMP: Virtual Try-On (High Fidelity - Gemini 3 Pro)

## Arquitectura de Modelos AI
1.  **Análisis de Prenda (Upload):** `gemini-2.5-flash-image`. (Rápido, solo genera metadatos de texto).
2.  **Generación de Try-On:** `gemini-3-pro-image-preview`. (Lento, alta calidad, recibe múltiples imágenes + texto).

---

## FASE 1: Database Schema (Supabase)

Igual que antes, pero asegurando que guardamos la URL de la imagen para poder descargarla y convertirla a Base64 cuando hagamos el Try-On.

```sql
-- 1. Perfiles (Foto Base)
CREATE TABLE profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    full_body_image_url TEXT NOT NULL, 
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. Categorías (Estáticas)
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    slug TEXT NOT NULL UNIQUE, -- 'upper', 'lower', 'footwear'
    display_name TEXT NOT NULL
);

-- 3. Prendas (Con metadatos de IA)
CREATE TABLE garments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id),
    category_id UUID NOT NULL REFERENCES categories(id),
    
    image_url TEXT NOT NULL,   -- Necesaria para descargar y enviar a Gemini Pro
    ai_description TEXT,       -- Generado por Flash al subir
    ai_fit TEXT DEFAULT 'regular', -- Generado por Flash
    
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 4. Outfits (Resultados)
CREATE TABLE outfits (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id),
    generated_image_url TEXT NOT NULL,
    occasion TEXT, 
    created_at TIMESTAMPTZ DEFAULT NOW()
);
```

---

## FASE 2: Carga de Prendas (Gemini Flash)

Aquí el flujo es automático. El usuario sube foto -> Gemini Flash describe -> Se guarda.

### 2.1 UseCase: AnalyzeGarment (Kotlin)

```kotlin
// features/wardrobe/domain/usecase/AnalyzeGarmentUseCase.kt
// Usamos Flash porque es rápido y barato para describir texto.

suspend fun analyzeGarment(imageBytes: ByteArray): GarmentMetadata {
    // Prompt simple para Flash
    val prompt = """
        Analiza esta prenda. Retorna un JSON con:
        - description: descripción visual detallada (color, material, estilo).
        - fit: tipo de ajuste (tight, regular, loose, oversized).
        Responde SOLO el JSON.
    """.trimIndent()

    // Llamada a gemini-2.5-flash
    return geminiRepository.generateMetadata(imageBytes, prompt)
}
```

---

## FASE 3: Virtual Try-On (Porting del Código JS a Kotlin)

Esta es la parte crítica. Vamos a traducir tu función `generateTryOnImage` y `buildTryOnPrompt` a Kotlin puro usando Ktor.

### 3.1 DTOs para Gemini API (Serialization)

Necesitamos replicar la estructura JSON exacta que espera Gemini 3 Pro.

```kotlin
// features/tryon/data/remote/dto/GeminiRequest.kt
@Serializable
data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null,
    @SerialName("inline_data")
    val inlineData: InlineData? = null
)

@Serializable
data class InlineData(
    @SerialName("mime_type")
    val mimeType: String = "image/jpeg",
    val data: String // Base64
)

@Serializable
data class GenerationConfig(
    val responseModalities: List<String> = listOf("TEXT", "IMAGE"),
    val imageConfig: ImageConfig
)

@Serializable
data class ImageConfig(
    val aspectRatio: String = "3:4",
    val imageSize: String = "2K"
)
```

### 3.2 Lógica de Prompt (Igual al JS)

```kotlin
// features/tryon/domain/logic/PromptBuilder.kt

data class GarmentSpec(
    val description: String, 
    val fit: String, 
    val imageBytes: ByteArray
)

fun buildTryOnPrompt(
    upper: GarmentSpec?,
    lower: GarmentSpec?,
    footwear: GarmentSpec?
): String {
    val garmentInstructions = buildString {
        append("PRENDAS A COLOCAR:\n")
        var imageIndex = 2 // Imagen 1 es el usuario

        upper?.let {
            append("- PRENDA SUPERIOR (de la Imagen $imageIndex): ${it.description}\n")
            append("  - Ajuste: ${it.fit}\n")
            append("  - Ubicación: Torso y brazos\n\n")
            imageIndex++
        }

        lower?.let {
            append("- PRENDA INFERIOR (de la Imagen $imageIndex): ${it.description}\n")
            append("  - Ajuste: ${it.fit}\n")
            append("  - Ubicación: Piernas\n\n")
            imageIndex++
        }

        footwear?.let {
            append("- CALZADO (de la Imagen $imageIndex): ${it.description}\n")
            append("  - Ajuste: ${it.fit}\n")
            append("  - Ubicación: Pies\n")
        }
    }

    return """
        Actúa como un fotógrafo de moda profesional y editor experto especializado en virtual try-on.

        TAREA: Crea una imagen fotorrealista de la persona en la [Imagen 1] vistiendo las prendas especificadas.

        $garmentInstructions

        REQUISITOS CRÍTICOS - ORDEN DE PRIORIDAD:

        1. PRESERVACIÓN DE IDENTIDAD (MÁXIMA PRIORIDAD):
           - NO ALTERES la cara, rasgos faciales, expresión, ni identidad de la persona
           - Mantén EXACTAMENTE el mismo rostro, ojos, nariz, boca, cejas de la [Imagen 1]
           - Conserva el mismo tono de piel, textura y características faciales
           - NO modifiques el peinado, color de cabello ni accesorios faciales
           - La persona debe ser 100% reconocible como la misma de la foto original

        2. PRESERVACIÓN DEL CUERPO:
           - Mantén la misma pose, postura y posición del cuerpo
           - Conserva el mismo tipo de cuerpo y proporciones
           - NO alteres la altura, complexión ni estructura corporal

        3. APLICACIÓN DE PRENDAS:
           - Coloca las prendas especificadas sobre el cuerpo de la persona
           - Las prendas deben adaptarse naturalmente al cuerpo
           - Respeta la física de la tela: pliegues, caídas, arrugas naturales
           
        4. INTEGRACIÓN VISUAL:
           - Mantén la misma iluminación y sombras del entorno original
           - Las sombras de las prendas deben coincidir con la luz de la [Imagen 1]
           - Conserva el mismo fondo y contexto de la foto original
           - El resultado debe parecer una foto real, no un montaje

        5. COHERENCIA DEL OUTFIT:
           - Las prendas deben verse coordinadas y naturales juntas
           - Reemplaza SOLO las prendas especificadas
           - Mantén cualquier otra ropa o accesorios no especificados

        RECORDATORIO FINAL: La cara y la identidad de la persona NO deben cambiar en absoluto. Es fundamental que la persona sea completamente reconocible.

        Genera solo la imagen final sin texto adicional.
    """.trimIndent()
}
```

### 3.3 Implementación del Repository (La llamada a la API)

Aquí replicamos el orden estricto: **Texto -> Imagen Usuario -> Imágenes Prendas**.

```kotlin
// features/tryon/data/repository/GeminiTryOnRepository.kt

class GeminiTryOnRepository(
    private val client: HttpClient,
    private val apiKey: String
) {
    suspend fun generateTryOn(
        userImageBytes: ByteArray,
        upper: GarmentSpec?,
        lower: GarmentSpec?,
        footwear: GarmentSpec?
    ): Result<ByteArray> {
        return try {
            // 1. Construir Prompt de Texto
            val textPrompt = buildTryOnPrompt(upper, lower, footwear)
            
            // 2. Construir Partes (Orden estricto para Gemini 3 Pro)
            val parts = mutableListOf<Part>()
            
            // A. Texto primero
            parts.add(Part(text = textPrompt))
            
            // B. Imagen Usuario (Imagen 1)
            parts.add(Part(inlineData = InlineData(data = userImageBytes.toBase64())))
            
            // C. Imágenes de Prendas (Imagen 2, 3, 4...)
            upper?.let {
                parts.add(Part(inlineData = InlineData(data = it.imageBytes.toBase64())))
            }
            lower?.let {
                parts.add(Part(inlineData = InlineData(data = it.imageBytes.toBase64())))
            }
            footwear?.let {
                parts.add(Part(inlineData = InlineData(data = it.imageBytes.toBase64())))
            }

            // 3. Crear Request Body
            val requestBody = GeminiRequest(
                contents = listOf(Content(parts = parts)),
                generationConfig = GenerationConfig(
                    imageConfig = ImageConfig() // 3:4, 2K
                )
            )

            // 4. Ejecutar llamada
            val response = client.post("https://generativelanguage.googleapis.com/v1beta/models/gemini-3-pro-image-preview:generateContent") {
                header("x-goog-api-key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            // 5. Parsear respuesta (igual que en JS)
            val responseData = response.body<GeminiResponse>() // Necesitas crear este DTO de respuesta
            val base64Image = responseData.candidates.firstOrNull()
                ?.content?.parts?.firstOrNull()
                ?.inlineData?.data 
                ?: throw Exception("No image generated")

            Result.success(base64Image.decodeBase64Bytes())

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## FASE 4: Flujo de Usuario Detallado

### 1. Pre-condición: Foto de Perfil
*   El usuario va a "Perfil".
*   Sube foto cuerpo entero.
*   Se guarda en Supabase `profiles`.

### 2. Carga de Ropa (Automatizada)
*   Botón "+".
*   Foto a la prenda.
*   **Gemini Flash** analiza la foto.
*   Retorna: `{"description": "Campera de cuero negra tipo biker", "fit": "regular"}`.
*   App guarda en Supabase `garments`: Foto URL + Description + Fit.
*   Usuario no edita nada, solo ve "Guardado!".

### 3. Generación de Outfit (El núcleo)
*   Usuario selecciona: 1 Superior + 1 Inferior + 1 Calzado.
*   Botón "PROBAR (TRY ON)".
*   **Backend (KMP App):**
    1.  Descarga `user_profile_image` -> ByteArray.
    2.  Descarga `upper_garment_image` -> ByteArray.
    3.  Descarga `lower_garment_image` -> ByteArray.
    4.  Recupera los textos de `description` y `fit` de la DB.
    5.  Llama a `GeminiTryOnRepository.generateTryOn(...)`.
*   Gemini Pro recibe todo el contexto (Imágenes reales + instrucciones).
*   Retorna Imagen Final.

### 4. Guardado
*   Usuario ve el resultado.
*   Botón "Guardar".
*   Input: Categoría/Ocasión (ej: "Salida noche").
*   Insert en `outfits`.

---

## Dependencias KMP Necesarias

Para que esto funcione igual que tu JS, necesitas estas librerías en `build.gradle.kts`:

```kotlin
// Networking & Serialization
implementation("io.ktor:ktor-client-core:3.0.0")
implementation("io.ktor:ktor-client-content-negotiation:3.0.0")
implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.0")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

// Base64 encoding/decoding
implementation("io.ktor:ktor-utils:3.0.0") // Contiene extensiones Base64
```

Esta estructura cumple estrictamente con tu requerimiento: usar **Gemini 3 Pro**, enviar **imágenes reales mezcladas** en el prompt, y usar los **prompts específicos** para proteger la identidad del usuario.