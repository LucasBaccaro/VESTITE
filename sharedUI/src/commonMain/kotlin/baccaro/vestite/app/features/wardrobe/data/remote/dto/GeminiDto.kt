package baccaro.vestite.app.features.wardrobe.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Request DTOs
@Serializable
data class GeminiRequest(
    @SerialName("contents")
    val contents: List<Content>,

    @SerialName("generationConfig")
    val generationConfig: GenerationConfig? = null
)

@Serializable
data class Content(
    @SerialName("parts")
    val parts: List<Part>
)

@Serializable
data class Part(
    @SerialName("text")
    val text: String? = null,

    @SerialName("inline_data")
    val inlineData: InlineData? = null
)

@Serializable
data class InlineData(
    @SerialName("mime_type")
    val mimeType: String = "image/jpeg",

    @SerialName("data")
    val data: String // Base64
)

@Serializable
data class GenerationConfig(
    @SerialName("temperature")
    val temperature: Double? = null,

    @SerialName("topP")
    val topP: Double? = null,

    @SerialName("topK")
    val topK: Int? = null,

    @SerialName("maxOutputTokens")
    val maxOutputTokens: Int? = null,

    @SerialName("responseMimeType")
    val responseMimeType: String? = null
)

// Response DTOs
@Serializable
data class GeminiResponse(
    @SerialName("candidates")
    val candidates: List<Candidate>? = null,

    @SerialName("promptFeedback")
    val promptFeedback: PromptFeedback? = null
)

@Serializable
data class Candidate(
    @SerialName("content")
    val content: Content? = null,

    @SerialName("finishReason")
    val finishReason: String? = null,

    @SerialName("safetyRatings")
    val safetyRatings: List<SafetyRating>? = null
)

@Serializable
data class PromptFeedback(
    @SerialName("safetyRatings")
    val safetyRatings: List<SafetyRating>? = null
)

@Serializable
data class SafetyRating(
    @SerialName("category")
    val category: String,

    @SerialName("probability")
    val probability: String
)

// Garment Analysis Response (parsed from JSON text)
@Serializable
data class GarmentAnalysisResponse(
    @SerialName("description")
    val description: String,

    @SerialName("fit")
    val fit: String
)
