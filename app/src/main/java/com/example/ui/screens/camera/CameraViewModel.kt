package com.example.ui.screens.camera

import android.content.Context
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.MeteringPointFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.SalimApplication
import com.example.data.db.PhotoEntity
import com.example.data.db.ProjectEntity
import com.example.data.db.ProjectWithPhotoCount
import com.example.data.model.LocationData
import com.example.data.model.StampSettings
import com.example.data.preferences.SettingsDataStore
import com.example.data.repository.PhotoRepository
import com.example.data.repository.ProjectRepository
import com.example.util.ImageStamper
import com.example.util.LocationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

enum class FlashMode {
    OFF,
    AUTO,
    ON
}

class CameraViewModel(
    private val projectRepository: ProjectRepository,
    private val photoRepository: PhotoRepository,
    private val settingsDataStore: SettingsDataStore,
    private val locationHelper: LocationHelper,
    initialProjectId: Long?
) : ViewModel() {

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    val projects: StateFlow<List<ProjectWithPhotoCount>> = projectRepository.projectsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stampSettings: StateFlow<StampSettings> = settingsDataStore.stampSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StampSettings())

    val locationData: StateFlow<LocationData> = locationHelper.currentLocation

    private val _selectedProjectId = MutableStateFlow<Long?>(initialProjectId)
    val selectedProjectId: StateFlow<Long?> = _selectedProjectId.asStateFlow()

    private val _currentProject = MutableStateFlow<ProjectEntity?>(null)
    val currentProject: StateFlow<ProjectEntity?> = _currentProject.asStateFlow()

    private val _flashMode = MutableStateFlow(FlashMode.AUTO)
    val flashMode: StateFlow<FlashMode> = _flashMode.asStateFlow()

    private val _lensFacing = MutableStateFlow(CameraSelector.LENS_FACING_BACK)
    val lensFacing: StateFlow<Int> = _lensFacing.asStateFlow()

    private val _showGrid = MutableStateFlow(false)
    val showGrid: StateFlow<Boolean> = _showGrid.asStateFlow()

    private val _quickNote = MutableStateFlow("")
    val quickNote: StateFlow<String> = _quickNote.asStateFlow()

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _lastCapturedPhoto = MutableStateFlow<PhotoEntity?>(null)
    val lastCapturedPhoto: StateFlow<PhotoEntity?> = _lastCapturedPhoto.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var activeCameraControl: CameraControl? = null
    private var activeCameraInfo: CameraInfo? = null

    init {
        viewModelScope.launch {
            if (initialProjectId == null) {
                val storedId = settingsDataStore.activeProjectIdFlow.first()
                if (storedId != null) {
                    _selectedProjectId.value = storedId
                }
            }

            // Sync current project entity
            selectedProjectId.collect { id ->
                if (id != null) {
                    _currentProject.value = projectRepository.getProjectDirect(id)
                } else {
                    val first = projects.value.firstOrNull()?.project
                    _currentProject.value = first
                    _selectedProjectId.value = first?.id
                }
            }
        }

        // Start location updates when camera is opened
        locationHelper.startLocationUpdates()
    }

    fun selectProject(projectId: Long) {
        _selectedProjectId.value = projectId
        viewModelScope.launch {
            settingsDataStore.setActiveProjectId(projectId)
            _currentProject.value = projectRepository.getProjectDirect(projectId)
        }
    }

    fun setQuickNote(note: String) {
        _quickNote.value = note
    }

    fun toggleFlash() {
        _flashMode.value = when (_flashMode.value) {
            FlashMode.OFF -> FlashMode.AUTO
            FlashMode.AUTO -> FlashMode.ON
            FlashMode.ON -> FlashMode.OFF
        }
    }

    fun toggleLensFacing() {
        _lensFacing.value = if (_lensFacing.value == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
    }

    fun toggleGrid() {
        _showGrid.value = !_showGrid.value
    }

    fun bindCamera(control: CameraControl, info: CameraInfo) {
        activeCameraControl = control
        activeCameraInfo = info
    }

    fun onPinchZoom(zoomRatioDelta: Float) {
        val info = activeCameraInfo ?: return
        val currentZoom = info.zoomState.value?.zoomRatio ?: 1f
        val newZoom = (currentZoom * zoomRatioDelta).coerceIn(
            info.zoomState.value?.minZoomRatio ?: 1f,
            info.zoomState.value?.maxZoomRatio ?: 5f
        )
        activeCameraControl?.setZoomRatio(newZoom)
    }

    fun onTapToFocus(pointFactory: MeteringPointFactory, x: Float, y: Float) {
        val point = pointFactory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point).build()
        activeCameraControl?.startFocusAndMetering(action)
    }

    fun capturePhoto(context: Context, imageCapture: ImageCapture) {
        if (_isCapturing.value) return

        var projectId = _selectedProjectId.value
        val projectName = _currentProject.value?.name ?: "Inspection Proof"

        viewModelScope.launch {
            // Auto create a default project if none exists yet
            if (projectId == null) {
                val newId = projectRepository.createProject("Inspection Proof")
                _selectedProjectId.value = newId
                _currentProject.value = projectRepository.getProjectDirect(newId)
                projectId = newId
            }

            _isCapturing.value = true
            _errorMessage.value = null

            // Configure flash
            imageCapture.flashMode = when (_flashMode.value) {
                FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
                FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
                FlashMode.ON -> ImageCapture.FLASH_MODE_ON
            }

            imageCapture.takePicture(
                cameraExecutor,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        viewModelScope.launch {
                            try {
                                processCapturedImageProxy(context, image, projectId!!, projectName)
                            } catch (e: Exception) {
                                _errorMessage.value = "Failed to stamp photo: ${e.message}"
                            } finally {
                                _isCapturing.value = false
                            }
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        _isCapturing.value = false
                        _errorMessage.value = "Capture failed: ${exception.message}"
                    }
                }
            )
        }
    }

    private suspend fun processCapturedImageProxy(
        context: Context,
        image: ImageProxy,
        projectId: Long,
        projectName: String
    ) = withContext(Dispatchers.IO) {
        val rotationDegrees = image.imageInfo.rotationDegrees
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        image.close()

        val timestamp = System.currentTimeMillis()
        val loc = locationData.value
        val note = quickNote.value
        val settings = stampSettings.value

        // Burn stamp onto image and write to disk
        val result = ImageStamper.processAndStampImage(
            context = context,
            jpegBytes = bytes,
            rotationDegrees = rotationDegrees,
            projectName = projectName,
            note = note,
            locationData = loc,
            stampSettings = settings,
            timestamp = timestamp
        )

        // Save into Room database
        val photo = PhotoEntity(
            projectId = projectId,
            filePath = result.filePath,
            thumbnailPath = result.thumbnailPath,
            timestamp = timestamp,
            latitude = loc.latitude,
            longitude = loc.longitude,
            altitude = loc.altitude,
            accuracyMeters = loc.accuracyMeters,
            addressLine = loc.addressLine,
            note = note,
            width = result.width,
            height = result.height,
            rotationDegrees = rotationDegrees
        )

        val photoId = photoRepository.savePhoto(photo)
        _lastCapturedPhoto.value = photo.copy(id = photoId)
    }

    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        locationHelper.stopLocationUpdates()
        cameraExecutor.shutdown()
    }

    class Factory(
        private val app: SalimApplication,
        private val projectId: Long?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CameraViewModel(
                app.projectRepository,
                app.photoRepository,
                app.settingsDataStore,
                app.locationHelper,
                projectId
            ) as T
        }
    }
}
