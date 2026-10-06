package com.example.data.repository

import com.example.data.db.PhotoDao
import com.example.data.db.PhotoEntity
import com.example.data.db.ProjectDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class PhotoRepository(
    private val photoDao: PhotoDao,
    private val projectDao: ProjectDao
) {

    fun getPhotosForProject(projectId: Long): Flow<List<PhotoEntity>> {
        return photoDao.getPhotosForProject(projectId)
    }

    suspend fun getPhotosListForProject(projectId: Long): List<PhotoEntity> {
        return photoDao.getPhotosForProjectListAsc(projectId)
    }

    fun getPhoto(id: Long): Flow<PhotoEntity?> {
        return photoDao.getPhotoById(id)
    }

    suspend fun getPhotoDirect(id: Long): PhotoEntity? {
        return photoDao.getPhotoByIdDirect(id)
    }

    suspend fun savePhoto(photo: PhotoEntity): Long {
        val id = photoDao.insertPhoto(photo)
        // Update project updatedAt
        val project = projectDao.getProjectByIdDirect(photo.projectId)
        if (project != null) {
            projectDao.updateProject(project.copy(updatedAt = System.currentTimeMillis()))
        }
        return id
    }

    suspend fun updatePhotoNote(id: Long, note: String) {
        photoDao.updatePhotoNote(id, note.trim())
    }

    suspend fun deletePhoto(photo: PhotoEntity) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(photo.filePath)
                if (file.exists()) {
                    file.delete()
                }
                photo.thumbnailPath?.let { thumbPath ->
                    val thumbFile = File(thumbPath)
                    if (thumbFile.exists()) {
                        thumbFile.delete()
                    }
                }
            } catch (e: Exception) {
                // Ignore file deletion error, proceed to remove DB record
            }
        }
        photoDao.deletePhoto(photo)
        val project = projectDao.getProjectByIdDirect(photo.projectId)
        if (project != null) {
            projectDao.updateProject(project.copy(updatedAt = System.currentTimeMillis()))
        }
    }
}
