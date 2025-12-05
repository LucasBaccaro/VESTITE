package baccaro.vestite.app.features.wardrobe.data.repository

import baccaro.vestite.app.BuildConfig
import baccaro.vestite.app.features.wardrobe.data.mapper.toDomain
import baccaro.vestite.app.features.wardrobe.data.remote.dto.Content
import baccaro.vestite.app.features.wardrobe.data.remote.dto.GarmentAnalysisResponse
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
        private const val GEMINI_FLASH_MODEL = "gemini-2.0-flash-exp"
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

            // Construir request
            val request = GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = prompt),
                            Part(inlineData = InlineData(data = base64Image))
                        )
                    )
                )
            )

            // Ejecutar llamada
            val response = httpClient.post("$GEMINI_API_BASE_URL/$GEMINI_FLASH_MODEL:generateContent") {
                header("x-goog-api-key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            // Parsear respuesta
            val geminiResponse = response.body<GeminiResponse>()
            val textResponse = geminiResponse.candidates?.firstOrNull()
                ?.content?.parts?.firstOrNull()
                ?.text
                ?: throw Exception("No text response from Gemini")

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
