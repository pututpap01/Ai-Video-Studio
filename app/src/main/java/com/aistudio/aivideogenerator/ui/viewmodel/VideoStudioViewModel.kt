package com.aistudio.aivideogenerator.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.aivideogenerator.data.local.AppDatabase
import com.aistudio.aivideogenerator.data.model.VideoProject
import com.aistudio.aivideogenerator.data.model.VideoProjectWithScenes
import com.aistudio.aivideogenerator.data.model.VideoScene
import com.aistudio.aivideogenerator.data.remote.GeminiRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class GenerationState {
    object Idle : GenerationState()
    data class Generating(val step: String, val progress: Float) : GenerationState()
    data class Success(val projectId: Long) : GenerationState()
    data class Error(val message: String) : GenerationState()
}

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentSceneIndex: Int = 0,
    val currentSceneProgress: Float = 0f,
    val totalElapsedTimeSeconds: Float = 0f,
    val isMuted: Boolean = false
)

class VideoStudioViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val projectDao = db.videoProjectDao()
    private val sceneDao = db.videoSceneDao()
    private val geminiRepo = GeminiRepository()

    val allProjects = projectDao.getAllProjectsFlow()

    private val _currentProjectWithScenes = MutableStateFlow<VideoProjectWithScenes?>(null)
    val currentProjectWithScenes: StateFlow<VideoProjectWithScenes?> = _currentProjectWithScenes.asStateFlow()

    private val _generationState = MutableStateFlow<GenerationState>(GenerationState.Idle)
    val generationState: StateFlow<GenerationState> = _generationState.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var playbackJob: Job? = null

    init {
        // Pre-populate with sample projects if database is empty
        viewModelScope.launch {
            projectDao.getAllProjectsFlow().collect { projects ->
                if (projects.isEmpty()) {
                    seedSampleProjects()
                }
            }
        }
    }

    fun loadProject(projectId: Long) {
        viewModelScope.launch {
            projectDao.getProjectWithScenesFlow(projectId).collect { data ->
                _currentProjectWithScenes.value = data
                stopPlayback()
            }
        }
    }

    fun generateNewVideoProject(
        prompt: String,
        style: String,
        aspectRatio: String,
        voiceStyle: String,
        targetDurationSeconds: Int
    ) {
        viewModelScope.launch {
            _generationState.value = GenerationState.Generating("Writing AI Video Script...", 0.15f)
            delay(600)
            _generationState.value = GenerationState.Generating("Directing Storyboard & Camera Angles...", 0.45f)
            delay(600)
            _generationState.value = GenerationState.Generating("Synthesizing Voiceovers & Prompts...", 0.75f)

            try {
                val (project, scenes) = geminiRepo.generateVideoStoryboard(
                    userPrompt = prompt,
                    style = style,
                    aspectRatio = aspectRatio,
                    voiceStyle = voiceStyle,
                    durationSeconds = targetDurationSeconds
                )

                _generationState.value = GenerationState.Generating("Saving to Video Studio Library...", 0.95f)

                val savedProjectId = projectDao.insertProject(project)
                val scenesWithProjectId = scenes.map { it.copy(projectId = savedProjectId) }
                sceneDao.insertScenes(scenesWithProjectId)

                _generationState.value = GenerationState.Success(savedProjectId)
                loadProject(savedProjectId)
            } catch (e: Exception) {
                _generationState.value = GenerationState.Error(e.message ?: "Failed to generate video storyboard")
            }
        }
    }

    fun resetGenerationState() {
        _generationState.value = GenerationState.Idle
    }

    fun updateScene(scene: VideoScene) {
        viewModelScope.launch {
            sceneDao.updateScene(scene)
            val currentProj = _currentProjectWithScenes.value
            if (currentProj != null) {
                loadProject(currentProj.project.id)
            }
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            stopPlayback()
            projectDao.deleteProjectById(projectId)
            if (_currentProjectWithScenes.value?.project?.id == projectId) {
                _currentProjectWithScenes.value = null
            }
        }
    }

    // --- Interactive Video Player Timeline Logic ---
    fun togglePlayback() {
        if (_playbackState.value.isPlaying) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    fun pausePlayback() {
        playbackJob?.cancel()
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        _playbackState.value = PlaybackState(
            isPlaying = false,
            currentSceneIndex = 0,
            currentSceneProgress = 0f,
            totalElapsedTimeSeconds = 0f
        )
    }

    fun selectScene(index: Int) {
        val scenes = _currentProjectWithScenes.value?.scenes ?: return
        if (index in scenes.indices) {
            val elapsed = scenes.take(index).sumOf { it.durationSeconds }.toFloat()
            _playbackState.value = _playbackState.value.copy(
                currentSceneIndex = index,
                currentSceneProgress = 0f,
                totalElapsedTimeSeconds = elapsed
            )
        }
    }

    fun toggleMute() {
        _playbackState.value = _playbackState.value.copy(isMuted = !_playbackState.value.isMuted)
    }

    private fun startPlayback() {
        val data = _currentProjectWithScenes.value ?: return
        val scenes = data.scenes
        if (scenes.isEmpty()) return

        playbackJob?.cancel()
        _playbackState.value = _playbackState.value.copy(isPlaying = true)

        playbackJob = viewModelScope.launch {
            var sceneIdx = _playbackState.value.currentSceneIndex
            var sceneProg = _playbackState.value.currentSceneProgress

            while (sceneIdx < scenes.size && _playbackState.value.isPlaying) {
                val currentScene = scenes[sceneIdx]
                val durationMs = currentScene.durationSeconds * 1000L
                val intervalMs = 50L
                val stepProgress = intervalMs.toFloat() / durationMs.toFloat()

                while (sceneProg < 1.0f && _playbackState.value.isPlaying) {
                    delay(intervalMs)
                    sceneProg += stepProgress

                    val totalElapsed = scenes.take(sceneIdx).sumOf { it.durationSeconds } + (sceneProg * currentScene.durationSeconds)

                    _playbackState.value = _playbackState.value.copy(
                        currentSceneIndex = sceneIdx,
                        currentSceneProgress = sceneProg.coerceAtMost(1.0f),
                        totalElapsedTimeSeconds = totalElapsed
                    )
                }

                if (_playbackState.value.isPlaying) {
                    sceneIdx++
                    sceneProg = 0f
                    if (sceneIdx >= scenes.size) {
                        // Reached end of video
                        stopPlayback()
                        break
                    }
                }
            }
        }
    }

    private suspend fun seedSampleProjects() {
        val sampleProj1 = VideoProject(
            title = "Cyberpunk Neo Tokyo 2099",
            logline = "A high-octane neon chase through the subterranean levels of Neo Tokyo.",
            genreStyle = "Cyberpunk 3D",
            aspectRatio = "16:9",
            voiceStyle = "Cinematic Male Trailer",
            sceneCount = 4,
            totalDurationSeconds = 22,
            status = "Completed"
        )
        val p1Id = projectDao.insertProject(sampleProj1)

        sceneDao.insertScenes(
            listOf(
                VideoScene(
                    projectId = p1Id,
                    sceneNumber = 1,
                    visualPrompt = "Neon illuminated skyscraper city at midnight, rain slicked streets reflecting holographic advertisements, flying vehicle zooming past camera, ultra detailed, cinematic volumetric lighting.",
                    narrationScript = "In the year 2099, Neo Tokyo never sleeps. Below the clouds, the neon pulse carries forgotten secrets.",
                    durationSeconds = 6,
                    cameraMotion = "Fast Flythrough",
                    shotType = "Extreme Wide Shot"
                ),
                VideoScene(
                    projectId = p1Id,
                    sceneNumber = 2,
                    visualPrompt = "Close-up of a cybernetically enhanced courier wearing glowing LED visor, reflection of neon sign in eyes, rain drops on chrome jacket, hyperrealistic 8k render.",
                    narrationScript = "Ren is a courier carrying the ultimate payload: memory chips containing the city's dark history.",
                    durationSeconds = 5,
                    cameraMotion = "Slow Zoom In",
                    shotType = "Close Up"
                ),
                VideoScene(
                    projectId = p1Id,
                    sceneNumber = 3,
                    visualPrompt = "A sleek matte black hoverbike drifting around a corner at high speeds, purple plasma exhaust trail, sparks flying from metal barrier, motion blur.",
                    narrationScript = "When the syndicate closes in, speed is the only armor that matters.",
                    durationSeconds = 5,
                    cameraMotion = "Tracking Side Shot",
                    shotType = "Action Tracking"
                ),
                VideoScene(
                    projectId = p1Id,
                    sceneNumber = 4,
                    visualPrompt = "Hoverbike leaping across a massive gap between two towering megastructures, massive glowing holographic billboard in background, dramatic lens flare.",
                    narrationScript = "One leap could mean total freedom—or falling into the abyss forever.",
                    durationSeconds = 6,
                    cameraMotion = "Low Angle Crane Up",
                    shotType = "Cinematic Climax"
                )
            )
        )

        val sampleProj2 = VideoProject(
            title = "Ancient Egypt: Secrets of Giza",
            logline = "An immersive documentary journey into the construction of the Great Pyramids.",
            genreStyle = "Documentary",
            aspectRatio = "9:16",
            voiceStyle = "Deep British Narrator",
            sceneCount = 3,
            totalDurationSeconds = 16,
            status = "Completed"
        )
        val p2Id = projectDao.insertProject(sampleProj2)

        sceneDao.insertScenes(
            listOf(
                VideoScene(
                    projectId = p2Id,
                    sceneNumber = 1,
                    visualPrompt = "Golden hour sunrise illuminating the Giza plateau 4,500 years ago, thousands of limestone workers moving massive blocks, Nile river flowing in background.",
                    narrationScript = "Four thousand five hundred years ago, the sands of Giza witnessed the greatest engineering marvel in human history.",
                    durationSeconds = 5,
                    cameraMotion = "High Aerial Sweep",
                    shotType = "Drone Wide Shot"
                ),
                VideoScene(
                    projectId = p2Id,
                    sceneNumber = 2,
                    visualPrompt = "Close up of ancient Egyptian architect studying star alignments with golden plumb line against night sky, desert wind softly blowing dust.",
                    narrationScript = "Aligned perfectly with the cardinal compass and the stars of Orion, every stone was placed with astronomical precision.",
                    durationSeconds = 6,
                    cameraMotion = "Slow Orbit",
                    shotType = "Medium Close Up"
                ),
                VideoScene(
                    projectId = p2Id,
                    sceneNumber = 3,
                    visualPrompt = "The completed Great Pyramid gleaming with polished white casing stone capping in gold leaf, reflecting bright desert sunlight.",
                    narrationScript = "A monumental testament that defied time itself.",
                    durationSeconds = 5,
                    cameraMotion = "Pan Up To Capstone",
                    shotType = "Low Angle Monumental"
                )
            )
        )
    }
}
