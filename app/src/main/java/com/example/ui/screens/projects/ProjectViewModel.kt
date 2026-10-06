package com.example.ui.screens.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.SalimApplication
import com.example.data.billing.BillingManager
import com.example.data.db.ProjectEntity
import com.example.data.db.ProjectWithPhotoCount
import com.example.data.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProjectViewModel(
    private val projectRepository: ProjectRepository,
    private val billingManager: BillingManager
) : ViewModel() {

    val projects: StateFlow<List<ProjectWithPhotoCount>> = projectRepository.projectsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isPro: StateFlow<Boolean> = billingManager.isPro

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    private val _projectToRename = MutableStateFlow<ProjectEntity?>(null)
    val projectToRename: StateFlow<ProjectEntity?> = _projectToRename.asStateFlow()

    private val _projectToDelete = MutableStateFlow<ProjectWithPhotoCount?>(null)
    val projectToDelete: StateFlow<ProjectWithPhotoCount?> = _projectToDelete.asStateFlow()

    private val _showPaywall = MutableStateFlow(false)
    val showPaywall: StateFlow<Boolean> = _showPaywall.asStateFlow()

    fun onAddProjectClicked() {
        viewModelScope.launch {
            val canCreate = projectRepository.canCreateProject(isPro.value)
            if (canCreate) {
                _showCreateDialog.value = true
            } else {
                _showPaywall.value = true
            }
        }
    }

    fun dismissCreateDialog() {
        _showCreateDialog.value = false
    }

    fun createProject(name: String, description: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            projectRepository.createProject(name, description)
            _showCreateDialog.value = false
        }
    }

    fun onRenameClicked(project: ProjectEntity) {
        _projectToRename.value = project
    }

    fun dismissRenameDialog() {
        _projectToRename.value = null
    }

    fun renameProject(project: ProjectEntity, newName: String, newDesc: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            projectRepository.updateProject(project.copy(name = newName.trim(), description = newDesc.trim()))
            _projectToRename.value = null
        }
    }

    fun onDeleteClicked(project: ProjectWithPhotoCount) {
        _projectToDelete.value = project
    }

    fun dismissDeleteDialog() {
        _projectToDelete.value = null
    }

    fun confirmDeleteProject() {
        val item = _projectToDelete.value ?: return
        viewModelScope.launch {
            projectRepository.deleteProject(item.project)
            _projectToDelete.value = null
        }
    }

    fun dismissPaywall() {
        _showPaywall.value = false
    }

    class Factory(private val app: SalimApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProjectViewModel(app.projectRepository, app.billingManager) as T
        }
    }
}
