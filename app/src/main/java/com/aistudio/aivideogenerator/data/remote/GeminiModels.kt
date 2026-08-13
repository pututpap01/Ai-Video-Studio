package com.aistudio.aivideogenerator.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent? = null
)

// Output data structures parsed from Gemini JSON output
@JsonClass(generateAdapter = true)
data class GeneratedStoryboardJson(
    val title: String,
    val logline: String,
    val genreStyle: String,
    val scenes: List<GeneratedSceneJson>
)

@JsonClass(generateAdapter = true)
data class GeneratedSceneJson(
    val sceneNumber: Int,
    val visualPrompt: String,
    val narrationScript: String,
    val durationSeconds: Int,
    val cameraMotion: String,
    val shotType: String
)
