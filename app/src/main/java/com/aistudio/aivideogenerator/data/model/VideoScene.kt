package com.aistudio.aivideogenerator.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "video_scenes",
    foreignKeys = [
        ForeignKey(
            entity = VideoProject::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"])]
)
data class VideoScene(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val sceneNumber: Int,
    val visualPrompt: String,
    val narrationScript: String,
    val durationSeconds: Int = 5,
    val cameraMotion: String = "Zoom In", // "Zoom In", "Pan Right", "Drone Sweep", "Static", "Tilt Up"
    val shotType: String = "Medium Shot", // "Wide Shot", "Close Up", "Medium Shot", "Aerial Drone"
    val keyframeImageUrl: String? = null,
    val audioGenerated: Boolean = false
)
