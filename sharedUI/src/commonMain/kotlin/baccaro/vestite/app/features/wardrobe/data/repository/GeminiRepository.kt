package baccaro.vestite.app.features.wardrobe.data.repository

import baccaro.vestite.app.BuildConfig
import baccaro.vestite.app.features.wardrobe.data.mapper.toDomain
import baccaro.vestite.app.features.wardrobe.data.remote.dto.Content
import baccaro.vestite.app.features.wardrobe.data.remote.dto.GarmentAnalysisResponse
import baccaro.vestite.app.features.wardrobe.data.remote.dto.GenerationConfig
import baccaro.vestite.app.features.wardrobe.data.remote.dto.GeminiRequest
import baccaro.vestite.app.features.wardrobe.data.remote.dto.GeminiResponse
import baccaro.vestite.app.features.wardrobe.data.remote.dto.InlineData
import baccaro.vestite.app.features.wardrobe.data.remote.dto.Part
import baccaro.vestite.app.features.wardrobe.domain.model.GarmentMetadata
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.util.encodeBase64
import kotlinx.serialization.json.Json

/**
 * Repository para interactuar con Gemini Flash API
 * Usado para analizar imágenes de prendas y extraer metadatos
 */
class GeminiRepository(
    private val httpClient: HttpClient
) {
    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val GEMINI_FLASH_MODEL = "gemini-2.5-flash"
        private const val GEMINI_FLASH_IMAGE_MODEL = "gemini-2.5-flash-image"
        private const val GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }

    /**
     * Analiza una imagen de prenda usando Gemini Flash
     * Retorna metadatos estructurados (description, fit)
     */
    suspend fun analyzeGarmentImage(imageBytes: ByteArray): Result<GarmentMetadata> {
        return try {
            // Construir prompt para análisis
            val prompt = buildAnalysisPrompt()

            // Convertir imagen a base64
            val base64Image = imageBytes.encodeBase64()

            // Check image size (Gemini limit is ~4-5 MB for original image)
            val imageSizeMB = imageBytes.size / (1024.0 * 1024.0)


            if (imageSizeMB > 5.0) {
                throw Exception("Imagen muy grande : Gemini acepta hasta 5 MB. Por favor usa una imagen más pequeña o comprime la foto.")
            }

            // Construir request - EXACT format from working RN code
            val request = GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            // IMAGE FIRST (inline_data)
                            Part(inlineData = InlineData(
                                mimeType = "image/jpeg",
                                data = base64Image
                            )),
                            // TEXT SECOND
                            Part(text = prompt)
                        )
                    )
                ),
                generationConfig = GenerationConfig(
                    temperature = 0.1,
                    maxOutputTokens = 4096,
                    responseModalities = listOf("TEXT")  // CRITICAL: Disable thinking mode
                )
            )

            // LOG REQUEST DETAILS
            println("=== GEMINI API REQUEST ===")
            println("Model: $GEMINI_FLASH_MODEL")
            println("URL: $GEMINI_API_BASE_URL/$GEMINI_FLASH_MODEL:generateContent")
            println("Image size (base64): ${base64Image.length} chars")
            println("Prompt length: ${prompt.length} chars")
            println("Prompt: $prompt")
            println("GenerationConfig:")
            println("  - temperature: ${request.generationConfig?.temperature}")
            println("  - maxOutputTokens: ${request.generationConfig?.maxOutputTokens}")
            println("  - responseModalities: ${request.generationConfig?.responseModalities}")
            println("=== END REQUEST ===")

            // Ejecutar llamada
            val response = httpClient.post("$GEMINI_API_BASE_URL/$GEMINI_FLASH_MODEL:generateContent") {
                header("x-goog-api-key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            // Check HTTP status and LOG ERROR DETAILS
            if (response.status.value !in 200..299) {
                println("=== GEMINI API ERROR ===")
                println("HTTP Status: ${response.status.value} ${response.status.description}")
                try {
                    val errorBody = response.body<String>()
                    println("Error Response Body:")
                    println(errorBody)
                } catch (e: Exception) {
                    println("Could not read error body: ${e.message}")
                }
                println("=== END ERROR ===")
                throw Exception("API request failed: HTTP ${response.status.value} ${response.status.description}")
            }

            // Parsear respuesta
            val geminiResponse = response.body<GeminiResponse>()

            // EXTENSIVE LOGGING - Critical for debugging
            println("=== GEMINI API RESPONSE ===")
            println("Candidates: ${geminiResponse.candidates?.size ?: 0}")
            println("PromptFeedback: ${geminiResponse.promptFeedback}")
            geminiResponse.candidates?.forEachIndexed { index, candidate ->
                println("Candidate $index:")
                println("  - finishReason: ${candidate.finishReason}")
                println("  - content.parts.size: ${candidate.content?.parts?.size}")
                candidate.content?.parts?.forEachIndexed { partIndex, part ->
                    println("    Part $partIndex: text=${part.text?.take(100)}")
                }
            }
            println("=== END GEMINI RESPONSE ===")

            // Check if blocked by safety filters
            if (geminiResponse.promptFeedback?.safetyRatings?.isNotEmpty() == true) {
                throw Exception("Contenido bloqueado por filtros de seguridad: ${geminiResponse.promptFeedback.safetyRatings}")
            }

            // Check for empty candidates
            val candidates = geminiResponse.candidates
            if (candidates.isNullOrEmpty()) {
                throw Exception("Gemini no retornó candidatos. Verifica que el modelo '$GEMINI_FLASH_MODEL' esté disponible.")
            }

            // Check finish reason
            val candidate = candidates.first()
            when (candidate.finishReason) {
                "SAFETY" -> throw Exception("Contenido bloqueado por seguridad. Intenta con otra imagen.")
                "RECITATION" -> throw Exception("Contenido bloqueado por copyright.")
                "MAX_TOKENS" -> throw Exception("Respuesta truncada por límite de tokens.")
                null -> println("Warning: finishReason is null")
            }

            // Extract text
            val textResponse = candidate.content?.parts?.firstOrNull()?.text
            if (textResponse.isNullOrBlank()) {
                throw Exception("Sin respuesta de texto. FinishReason: ${candidate.finishReason}, Parts: ${candidate.content?.parts?.size}")
            }

            // Extraer JSON del texto (puede venir envuelto en markdown)
            val jsonText = extractJson(textResponse)

            // Parsear JSON a objeto
            val analysisResponse = json.decodeFromString<GarmentAnalysisResponse>(jsonText)

            Result.success(analysisResponse.toDomain())
        } catch (e: Exception) {
            println("GeminiRepository - Error analyzing garment: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Error al analizar la prenda: ${e.message}"))
        }
    }

    /**
     * Remueve el fondo de una imagen de prenda usando Gemini Flash Image
     * Retorna la imagen editada con fondo blanco en base64
     */
    suspend fun removeBackground(imageBytes: ByteArray): Result<ByteArray> {
        return try {
            // Construir prompt para edición
            val prompt = buildBackgroundRemovalPrompt()

            // Convertir imagen a base64
            val base64Image = imageBytes.encodeBase64()

            println("=== GEMINI IMAGE EDIT REQUEST ===")
            println("Model: $GEMINI_FLASH_IMAGE_MODEL")
            println("Image size (base64): ${base64Image.length} chars")
            println("Prompt: $prompt")
            println("=== END REQUEST ===")

            // Construir request - ORDEN: prompt PRIMERO, imagen DESPUÉS
            val request = GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = prompt),
                            Part(inlineData = InlineData(
                                mimeType = "image/jpeg",
                                data = base64Image
                            ))
                        )
                    )
                )
            )

            // Ejecutar llamada
            val response = httpClient.post("$GEMINI_API_BASE_URL/$GEMINI_FLASH_IMAGE_MODEL:generateContent") {
                header("x-goog-api-key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            // Check HTTP status
            if (response.status.value !in 200..299) {
                println("=== GEMINI IMAGE EDIT ERROR ===")
                println("HTTP Status: ${response.status.value} ${response.status.description}")
                try {
                    val errorBody = response.body<String>()
                    println("Error Response Body:")
                    println(errorBody)
                } catch (e: Exception) {
                    println("Could not read error body: ${e.message}")
                }
                println("=== END ERROR ===")
                throw Exception("API request failed: HTTP ${response.status.value}")
            }

            // Parsear respuesta
            val geminiResponse = response.body<GeminiResponse>()

            println("=== GEMINI IMAGE EDIT RESPONSE ===")
            println("Candidates: ${geminiResponse.candidates?.size ?: 0}")
            geminiResponse.candidates?.forEachIndexed { index, candidate ->
                println("Candidate $index:")
                println("  - finishReason: ${candidate.finishReason}")
                println("  - content.parts.size: ${candidate.content?.parts?.size}")
                candidate.content?.parts?.forEachIndexed { partIndex, part ->
                    println("    Part $partIndex: text=${part.text?.take(50)}, hasInlineData=${part.inlineData != null}")
                }
            }
            println("=== END RESPONSE ===")

            // Check for empty candidates
            val candidates = geminiResponse.candidates
            if (candidates.isNullOrEmpty()) {
                throw Exception("Gemini no retornó candidatos de imagen")
            }

            // Check finish reason
            val candidate = candidates.first()
            when (candidate.finishReason) {
                "SAFETY" -> throw Exception("Contenido bloqueado por seguridad")
                "RECITATION" -> throw Exception("Contenido bloqueado por copyright")
                "MAX_TOKENS" -> throw Exception("Respuesta truncada")
            }

            // Extract image from response
            val imagePart = candidate.content?.parts?.firstOrNull { it.inlineData != null }
            val resultBase64 = imagePart?.inlineData?.data

            if (resultBase64.isNullOrBlank()) {
                throw Exception("No se pudo extraer la imagen procesada")
            }

            println("✅ Fondo removido exitosamente, tamaño resultado: ${resultBase64.length} chars")

            // Decodificar base64 a ByteArray
            val resultBytes = kotlin.io.encoding.Base64.decode(resultBase64)

            Result.success(resultBytes)
        } catch (e: Exception) {
            println("GeminiRepository - Error removing background: ${e.message}")
            e.printStackTrace()

            val errorMessage = when {
                e.message?.contains("API key", ignoreCase = true) == true -> "API Key inválida"
                e.message?.contains("quota", ignoreCase = true) == true -> "Límite de API alcanzado"
                e.message?.contains("safety", ignoreCase = true) == true -> "Imagen bloqueada por filtros de seguridad"
                else -> "Error al procesar la imagen: ${e.message}"
            }

            Result.failure(Exception(errorMessage))
        }
    }

    private fun buildAnalysisPrompt(): String {
        return """
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
        """.trimIndent()
    }

    private fun buildBackgroundRemovalPrompt(): String {
        return """
            Eres un editor de imágenes profesional especializado en fotografía de producto.

            TAREA: Edita esta imagen para aislar la prenda/objeto y colocar un fondo blanco puro.

            INSTRUCCIONES CRÍTICAS:

            1. PRESERVACIÓN DEL OBJETO:
               - Mantén la prenda/objeto EXACTAMENTE como está
               - NO modifiques colores, texturas, sombras del objeto
               - NO alteres la forma, tamaño o detalles de la prenda
               - Conserva todos los pliegues, arrugas y características naturales
               - Mantén la iluminación y sombras propias del objeto

            2. REMOCIÓN DEL FONDO:
               - Elimina COMPLETAMENTE el fondo original
               - Reemplaza con blanco puro (#FFFFFF)
               - Asegúrate de que no queden restos del fondo anterior
               - Corta limpiamente los bordes del objeto
               - Si hay sombras proyectadas en el fondo, elimínalas

            3. BORDES Y RECORTE:
               - Los bordes del objeto deben quedar limpios y precisos
               - Mantén detalles finos como costuras, botones, cordones
               - Si hay partes transparentes o semi-transparentes, manténlas naturales

            4. CALIDAD FINAL:
               - La prenda debe verse natural sobre el fondo blanco
               - Sin halos, bordes extraños o artefactos
               - Alta definición y claridad
               - Como si fuera una foto profesional de catálogo

            RESULTADO ESPERADO: Una imagen de producto profesional con fondo blanco puro, lista para e-commerce.

            Genera solo la imagen editada, sin texto adicional.
        """.trimIndent()
    }

    /**
     * Extrae JSON de una respuesta que puede venir envuelta en markdown
     */
    private fun extractJson(text: String): String {
        // Si viene con markdown code block ```json ... ```
        val jsonBlockRegex = Regex("```json\\s*(.+?)\\s*```", RegexOption.DOT_MATCHES_ALL)
        val matchResult = jsonBlockRegex.find(text)
        if (matchResult != null) {
            return matchResult.groupValues[1].trim()
        }

        // Si viene con markdown code block ``` ... ```
        val codeBlockRegex = Regex("```\\s*(.+?)\\s*```", RegexOption.DOT_MATCHES_ALL)
        val codeMatchResult = codeBlockRegex.find(text)
        if (codeMatchResult != null) {
            return codeMatchResult.groupValues[1].trim()
        }

        // Si no tiene wrapping, retornar el texto limpio
        return text.trim()
    }
}
