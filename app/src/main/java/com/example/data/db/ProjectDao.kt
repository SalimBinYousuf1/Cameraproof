package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    @Query("""
        SELECT p.*, 
               (SELECT COUNT(*) FROM photos WHERE photos.projectId = p.id) AS photoCount,
               (SELECT filePath FROM photos WHERE photos.projectId = p.id ORDER BY timestamp DESC LIMIT 1) AS latestPhotoPath
        FROM projects p
        ORDER BY p.updatedAt DESC
    """)
    fun getProjectsWithPhotoCount(): Flow<List<ProjectWithPhotoCount>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectById(id: Long): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectByIdDirect(id: Long): ProjectEntity?

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun getProjectCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)
}
