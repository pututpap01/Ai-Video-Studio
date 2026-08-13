package com.aistudio.aivideogenerator.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.aistudio.aivideogenerator.data.model.VideoProject
import com.aistudio.aivideogenerator.data.model.VideoProjectWithScenes
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoProjectDao {

    @Query("SELECT * FROM video_projects ORDER BY createdAt DESC")
    fun getAllProjectsFlow(): Flow<List<VideoProject>>

    @Transaction
    @Query("SELECT * FROM video_projects WHERE id = :id")
    fun getProjectWithScenesFlow(id: Long): Flow<VideoProjectWithScenes?>

    @Transaction
    @Query("SELECT * FROM video_projects WHERE id = :id")
    suspend fun getProjectWithScenes(id: Long): VideoProjectWithScenes?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: VideoProject): Long

    @Update
    suspend fun updateProject(project: VideoProject)

    @Delete
    suspend fun deleteProject(project: VideoProject)

    @Query("DELETE FROM video_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)
}
