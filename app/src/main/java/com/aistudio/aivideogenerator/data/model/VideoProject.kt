package com.aistudio.aivideogenerator.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_projects")
data class VideoProject(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val logline: String,
    val genreStyle: String,
    val aspectRatio: String, // "16:9", "9:16", "1:1"
    val voiceStyle: String,
    val createdAt: Long = System.currentTimeMillis(),
    val sceneCount: Int = 0,
    val totalDurationSeconds: Int = 0,
    val status: String = "Draft", // "Draft", "Generated", "Completed"
    val thumbnailUrl: String? = null
)
