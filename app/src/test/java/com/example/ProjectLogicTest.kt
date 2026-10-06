package com.example

import com.example.data.db.ProjectDao
import com.example.data.db.ProjectEntity
import com.example.data.db.ProjectWithPhotoCount
import com.example.data.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectLogicTest {

    private class FakeProjectDao(private var count: Int = 0) : ProjectDao {
        override fun getProjectsWithPhotoCount(): Flow<List<ProjectWithPhotoCount>> = flowOf(emptyList())
        override fun getProjectById(id: Long): Flow<ProjectEntity?> = flowOf(null)
        override suspend fun getProjectByIdDirect(id: Long): ProjectEntity? = null
        override suspend fun getProjectCount(): Int = count
        override suspend fun insertProject(project: ProjectEntity): Long {
            count++
            return count.toLong()
        }
        override suspend fun updateProject(project: ProjectEntity) {}
        override suspend fun deleteProject(project: ProjectEntity) {
            count = maxOf(0, count - 1)
        }
    }

    @Test
    fun testFreeTier_CanCreateUnderLimit() = runBlocking {
        val fakeDao = FakeProjectDao(count = 2)
        val repository = ProjectRepository(fakeDao)

        val canCreate = repository.canCreateProject(isPro = false)
        assertTrue("Free tier with 2 projects should be allowed to create 3rd", canCreate)
    }

    @Test
    fun testFreeTier_CannotCreateAtLimit() = runBlocking {
        val fakeDao = FakeProjectDao(count = 3)
        val repository = ProjectRepository(fakeDao)

        val canCreate = repository.canCreateProject(isPro = false)
        assertFalse("Free tier with 3 projects should be blocked", canCreate)
    }

    @Test
    fun testProTier_CanCreateUnlimited() = runBlocking {
        val fakeDao = FakeProjectDao(count = 100)
        val repository = ProjectRepository(fakeDao)

        val canCreate = repository.canCreateProject(isPro = true)
        assertTrue("Pro tier with 100 projects should be allowed to create more", canCreate)
    }
}
