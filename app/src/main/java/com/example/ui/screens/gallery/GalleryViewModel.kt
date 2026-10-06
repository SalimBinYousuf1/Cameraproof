package com.example.ui.screens.gallery

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.SalimApplication
import com.example.data.billing.BillingManager
import com.example.data.db.PhotoEntity
import com.example.data.db.ProjectEntity
import com.example.data.model.ReportSettings
import com.example.data.preferences.SettingsDataStore
import com.example.data.repository.PhotoRepository
import com.example.data.repository.ProjectRepository
import com.example.util.PdfReportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream

class GalleryViewModel(
    private val projectRepository: ProjectRepository,
    private val photoRepository: PhotoRepository,
    private val settingsDataStore: SettingsDataStore,
    private val billingManager: BillingManager,
    val projectId: Long
) : ViewModel() {

    val project: StateFlow<ProjectEntity?> = projectRepository.getProject(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val photos: StateFlow<List<PhotoEntity>> = photoRepository.getPhotosForProject(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reportSettings: StateFlow<ReportSettings> = settingsDataStore.reportSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportSettings())

    val isPro: StateFlow<Boolean> = billingManager.isPro

    private val _photoToDelete = MutableStateFlow<PhotoEntity?>(null)
    val photoToDelete: StateFlow<PhotoEntity?> = _photoToDelete.asStateFlow()

    private val _photoToEditNote = MutableStateFlow<PhotoEntity?>(null)
    val photoToEditNote: StateFlow<PhotoEntity?> = _photoToEditNote.asStateFlow()

    private val _showPdfDialog = MutableStateFlow(false)
    val showPdfDialog: StateFlow<Boolean> = _showPdfDialog.asStateFlow()

    private val _isGeneratingPdf = MutableStateFlow(false)
    val isGeneratingPdf: StateFlow<Boolean> = _isGeneratingPdf.asStateFlow()

    private val _pdfGeneratedFile = MutableStateFlow<File?>(null)
    val pdfGeneratedFile: StateFlow<File?> = _pdfGeneratedFile.asStateFlow()

    private val _galleryMessage = MutableStateFlow<String?>(null)
    val galleryMessage: StateFlow<String?> = _galleryMessage.asStateFlow()

    fun onDeletePhotoClicked(photo: PhotoEntity) {
        _photoToDelete.value = photo
    }

    fun dismissDeleteDialog() {
        _photoToDelete.value = null
    }

    fun confirmDeletePhoto() {
        val photo = _photoToDelete.value ?: return
        viewModelScope.launch {
            photoRepository.deletePhoto(photo)
            _photoToDelete.value = null
        }
    }

    fun onEditNoteClicked(photo: PhotoEntity) {
        _photoToEditNote.value = photo
    }

    fun dismissEditNoteDialog() {
        _photoToEditNote.value = null
    }

    fun savePhotoNote(photoId: Long, note: String) {
        viewModelScope.launch {
            photoRepository.updatePhotoNote(photoId, note)
            _photoToEditNote.value = null
        }
    }

    fun openPdfExportDialog() {
        _showPdfDialog.value = true
    }

    fun dismissPdfExportDialog() {
        _showPdfDialog.value = false
    }

    fun shareSinglePhoto(context: Context, photo: PhotoEntity) {
        try {
            val file = File(photo.filePath)
            if (!file.exists()) return

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                if (photo.note.isNotBlank()) {
                    putExtra(Intent.EXTRA_TEXT, photo.note)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share Proof Photo"))
        } catch (e: Exception) {
            _galleryMessage.value = "Failed to share photo: ${e.message}"
        }
    }

    fun generatePdfForSharing(context: Context, onReady: (File) -> Unit) {
        val proj = project.value ?: return
        val photoList = photos.value
        if (photoList.isEmpty()) {
            _galleryMessage.value = "Project has no photos to export."
            return
        }

        viewModelScope.launch {
            _isGeneratingPdf.value = true
            try {
                val file = PdfReportGenerator.generatePdfToCache(
                    context = context,
                    project = proj,
                    photos = photoList,
                    reportSettings = reportSettings.value,
                    isPro = isPro.value
                )
                _pdfGeneratedFile.value = file
                onReady(file)
            } catch (e: Exception) {
                _galleryMessage.value = "Failed to generate PDF: ${e.message}"
            } finally {
                _isGeneratingPdf.value = false
            }
        }
    }

    fun exportPdfToUri(context: Context, destinationUri: Uri) {
        val proj = project.value ?: return
        val photoList = photos.value
        if (photoList.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _isGeneratingPdf.value = true
            try {
                context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                    PdfReportGenerator.generatePdf(
                        context = context,
                        project = proj,
                        photos = photoList,
                        reportSettings = reportSettings.value,
                        isPro = isPro.value,
                        outputStream = out
                    )
                }
                _galleryMessage.value = "PDF report saved successfully."
            } catch (e: Exception) {
                _galleryMessage.value = "Failed to save PDF: ${e.message}"
            } finally {
                _isGeneratingPdf.value = false
                _showPdfDialog.value = false
            }
        }
    }

    fun sharePdfFile(context: Context, pdfFile: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Salim Proof Report - ${project.value?.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share PDF Report"))
        } catch (e: Exception) {
            _galleryMessage.value = "Failed to share PDF: ${e.message}"
        }
    }

    fun clearMessage() {
        _galleryMessage.value = null
    }

    class Factory(
        private val app: SalimApplication,
        private val projectId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GalleryViewModel(
                app.projectRepository,
                app.photoRepository,
                app.settingsDataStore,
                app.billingManager,
                projectId
            ) as T
        }
    }
}
