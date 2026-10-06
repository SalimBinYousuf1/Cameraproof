package com.example.data.repository

import com.example.data.db.ProjectDao
import com.example.data.db.ProjectEntity
import com.example.data.db.ProjectWithPhotoCount
import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val projectDao: ProjectDao) {

    val projectsFlow: Flow<List<ProjectWithPhotoCount>> = projectDao.getProjectsWithPhotoCount()

    fun getProject(id: Long): Flow<ProjectEntity?> = projectDao.getProjectById(id)

    suspend fun getProjectDirect(id: Long): ProjectEntity? = projectDao.getProjectByIdDirect(id)

    suspend fun getProjectCount(): Int = projectDao.getProjectCount()

    suspend fun canCreateProject(isPro: Boolean): Boolean {
        if (isPro) return true
        val count = projectDao.getProjectCount()
        return count < FREE_PROJECT_LIMIT
    }

    suspend fun createProject(name: String, description: String = ""): Long {
        val now = System.currentTimeMillis()
        val project = ProjectEntity(
            name = name.trim(),
            description = description.trim(),
            createdAt = now,
            updatedAt = now
        )
        return projectDao.insertProject(project)
    }

    suspend fun updateProject(project: ProjectEntity) {
        val updated = project.copy(updatedAt = System.currentTimeMillis())
        projectDao.updateProject(updated)
    }

    suspend fun deleteProject(project: ProjectEntity) {
        projectDao.deleteProject(project)
    }

    companion object {
        const val FREE_PROJECT_LIMIT = 3
    }
}
