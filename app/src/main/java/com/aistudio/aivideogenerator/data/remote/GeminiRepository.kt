package com.aistudio.aivideogenerator.data.remote

import android.util.Log
import com.example.BuildConfig
import com.aistudio.aivideogenerator.data.model.VideoProject
import com.aistudio.aivideogenerator.data.model.VideoScene
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiRepository(
    private val apiService: GeminiApiService = GeminiApiService.create()
) {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    suspend fun generateVideoStoryboard(
        userPrompt: String,
        style: String,
        aspectRatio: String,
        voiceStyle: String,
        durationSeconds: Int
    ): Pair<VideoProject, List<VideoScene>> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w("GeminiRepo", "Gemini API key is not configured. Falling back to local AI director engine.")
            return@withContext generateFallbackStoryboard(userPrompt, style, aspectRatio, voiceStyle, durationSeconds)
        }

        val sceneCount = when {
            durationSeconds <= 15 -> 3
            durationSeconds <= 30 -> 4
            else -> 6
        }

        val systemInstructionText = """
            You are a Hollywood Director and AI Video Producer. 
            Generate a complete, production-ready video storyboard script for an AI Video Generator (like Veo / Sora / Runway).
            
            Return strictly a JSON object with this exact schema:
            {
              "title": "Short Catchy Title",
              "logline": "1-sentence video summary",
              "genreStyle": "$style",
              "scenes": [
                {
                  "sceneNumber": 1,
                  "visualPrompt": "Detailed photorealistic camera visual prompt describing subject, lighting, movement, environment, color grading for AI video generation model.",
                  "narrationScript": "Voiceover sentence spoken in this scene.",
                  "durationSeconds": 5,
                  "cameraMotion": "Zoom In",
                  "shotType": "Wide Shot"
                }
              ]
            }
            Do not wrap in markdown or extra text. Output plain valid JSON only.
        """.trimIndent()

        val promptText = """
            Create an AI video storyboard based on this idea:
            Prompt: "$userPrompt"
            Visual Style: $style
            Aspect Ratio: $aspectRatio
            Target Duration: $durationSeconds seconds
            Scene Count: $sceneCount scenes
            Voice Style: $voiceStyle
        """.trimIndent()

        try {
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(parts = listOf(GeminiPart(text = promptText)))
                ),
                generationConfig = GenerationConfig(
                    responseMimeType = "application/json",
                    temperature = 0.7f
                ),
                systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemInstructionText)))
            )

            val response = apiService.generateContent(apiKey, request)
            val rawJson = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""

            val cleanedJson = rawJson.replace("```json", "").replace("```", "").trim()
            val adapter = moshi.adapter(GeneratedStoryboardJson::class.java)
            val parsed = adapter.fromJson(cleanedJson)

            if (parsed != null && parsed.scenes.isNotEmpty()) {
                val project = VideoProject(
                    title = parsed.title,
                    logline = parsed.logline,
                    genreStyle = parsed.genreStyle.ifBlank { style },
                    aspectRatio = aspectRatio,
                    voiceStyle = voiceStyle,
                    sceneCount = parsed.scenes.size,
                    totalDurationSeconds = parsed.scenes.sumOf { it.durationSeconds },
                    status = "Generated"
                )

                val scenes = parsed.scenes.mapIndexed { index, sceneJson ->
                    VideoScene(
                        projectId = 0,
                        sceneNumber = index + 1,
                        visualPrompt = sceneJson.visualPrompt,
                        narrationScript = sceneJson.narrationScript,
                        durationSeconds = sceneJson.durationSeconds,
                        cameraMotion = sceneJson.cameraMotion.ifBlank { "Pan Right" },
                        shotType = sceneJson.shotType.ifBlank { "Medium Shot" },
                        keyframeImageUrl = null
                    )
                }

                return@withContext Pair(project, scenes)
            } else {
                return@withContext generateFallbackStoryboard(userPrompt, style, aspectRatio, voiceStyle, durationSeconds)
            }
        } catch (e: Exception) {
            Log.e("GeminiRepo", "Error calling Gemini API: ${e.message}", e)
            return@withContext generateFallbackStoryboard(userPrompt, style, aspectRatio, voiceStyle, durationSeconds)
        }
    }

    private fun generateFallbackStoryboard(
        userPrompt: String,
        style: String,
        aspectRatio: String,
        voiceStyle: String,
        durationSeconds: Int
    ): Pair<VideoProject, List<VideoScene>> {
        val title = when {
            userPrompt.contains("space", ignoreCase = true) || userPrompt.contains("mars", ignoreCase = true) -> "Cosmic Horizon 2050"
            userPrompt.contains("coffee", ignoreCase = true) -> "Aroma & Morning Dreams"
            userPrompt.contains("egypt", ignoreCase = true) || userPrompt.contains("history", ignoreCase = true) -> "Echoes of the Pharoahs"
            userPrompt.contains("cyber", ignoreCase = true) || userPrompt.contains("future", ignoreCase = true) -> "Neo City Nights"
            else -> userPrompt.take(24).ifBlank { "AI Video Masterpiece" } + "..."
        }

        val logline = "A captivating $style visual journey exploring $userPrompt with cinematic narration and stunning AI imagery."

        val defaultScenes = listOf(
            VideoScene(
                projectId = 0,
                sceneNumber = 1,
                visualPrompt = "Cinematic opening shot of $userPrompt, $style style, glowing atmospheric lighting, dramatic volumetric fog, 8k resolution, photorealistic.",
                narrationScript = "In a world shaped by endless possibilities, a new vision emerges from the horizon.",
                durationSeconds = 5,
                cameraMotion = "Slow Zoom In",
                shotType = "Wide Atmospheric Shot"
            ),
            VideoScene(
                projectId = 0,
                sceneNumber = 2,
                visualPrompt = "Detailed close-up focal point of $userPrompt, intricate $style textures, vivid color grading, dynamic lens flare.",
                narrationScript = "Every detail carries purpose, seamlessly blending technology with artistic brilliance.",
                durationSeconds = 6,
                cameraMotion = "Smooth Pan Right",
                shotType = "Medium Close Up"
            ),
            VideoScene(
                projectId = 0,
                sceneNumber = 3,
                visualPrompt = "Action-packed dynamic angle of $userPrompt, high energy, particles floating in air, $style aesthetic, hyper-detailed render.",
                narrationScript = "Witness the powerful momentum as innovation transforms the present into the future.",
                durationSeconds = 5,
                cameraMotion = "Drone Arc Sweep",
                shotType = "Action Tracking Shot"
            ),
            VideoScene(
                projectId = 0,
                sceneNumber = 4,
                visualPrompt = "Grand finale climax sequence of $userPrompt, dramatic sunset skyline, cinematic flare, breathtaking composite scene.",
                narrationScript = "The journey has just begun. Welcome to the future of AI video creation.",
                durationSeconds = 6,
                cameraMotion = "Pull Back & Tilt Up",
                shotType = "Epic Wide Shot"
            )
        )

        val project = VideoProject(
            title = title,
            logline = logline,
            genreStyle = style,
            aspectRatio = aspectRatio,
            voiceStyle = voiceStyle,
            sceneCount = defaultScenes.size,
            totalDurationSeconds = defaultScenes.sumOf { it.durationSeconds },
            status = "Generated"
        )

        return Pair(project, defaultScenes)
    }
}
