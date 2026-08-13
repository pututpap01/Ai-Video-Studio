package com.aistudio.aivideogenerator.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class VideoProjectWithScenes(
    @Embedded val project: VideoProject,
    @Relation(
        parentColumn = "id",
        entityColumn = "projectId"
    )
    val scenes: List<VideoScene>
)
