package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {

    @Query("SELECT * FROM photos WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getPhotosForProject(projectId: Long): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE projectId = :projectId ORDER BY timestamp ASC")
    suspend fun getPhotosForProjectListAsc(projectId: Long): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE id = :id")
    fun getPhotoById(id: Long): Flow<PhotoEntity?>

    @Query("SELECT * FROM photos WHERE id = :id")
    suspend fun getPhotoByIdDirect(id: Long): PhotoEntity?

    @Query("SELECT * FROM photos WHERE projectId = :projectId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestPhotoForProject(projectId: Long): PhotoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEntity): Long

    @Query("UPDATE photos SET note = :note WHERE id = :id")
    suspend fun updatePhotoNote(id: Long, note: String)

    @Delete
    suspend fun deletePhoto(photo: PhotoEntity)
}
