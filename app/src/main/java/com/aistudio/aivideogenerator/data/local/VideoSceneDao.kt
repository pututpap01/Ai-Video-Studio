package com.aistudio.aivideogenerator.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aistudio.aivideogenerator.data.model.VideoScene

@Dao
interface VideoSceneDao {

    @Query("SELECT * FROM video_scenes WHERE projectId = :projectId ORDER BY sceneNumber ASC")
    suspend fun getScenesForProject(projectId: Long): List<VideoScene>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenes(scenes: List<VideoScene>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScene(scene: VideoScene): Long

    @Update
    suspend fun updateScene(scene: VideoScene)

    @Delete
    suspend fun deleteScene(scene: VideoScene)

    @Query("DELETE FROM video_scenes WHERE projectId = :projectId")
    suspend fun deleteScenesForProject(projectId: Long)
}
