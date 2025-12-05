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

    private fun buildAnalysisPrompt(): String {
        return """
            Analiza esta prenda de vestir. Retorna un JSON con exactamente estos campos:
            - description: descripción visual detallada (color, material, tipo, estilo)
            - fit: tipo de ajuste (debe ser exactamente uno de estos: "tight", "regular", "loose", "oversized")

            Responde SOLO con el JSON, sin markdown ni texto adicional.

            Ejemplo:
            {"description": "Campera de cuero negra tipo biker con cierre metálico", "fit": "regular"}
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
