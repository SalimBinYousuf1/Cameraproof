package com.example.ui.screens.camera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    onNavigateToGallery: (Long) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var cameraPermissionDeniedPermanently by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            val activity = context as? Activity
            if (activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)) {
                cameraPermissionDeniedPermanently = true
            }
        }
    }

    // Optional Location Permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Location helper automatically listens */ }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
        // Also request location for GPS stamping
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    if (!hasCameraPermission) {
        CameraPermissionRationaleScreen(
            permanentlyDenied = cameraPermissionDeniedPermanently,
            onRequestPermission = {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onOpenSettings = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        )
        return
    }

    // Camera is authorized
    val currentProject by viewModel.currentProject.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val selectedProjectId by viewModel.selectedProjectId.collectAsState()
    val stampSettings by viewModel.stampSettings.collectAsState()
    val locationData by viewModel.locationData.collectAsState()
    val flashMode by viewModel.flashMode.collectAsState()
    val lensFacing by viewModel.lensFacing.collectAsState()
    val showGrid by viewModel.showGrid.collectAsState()
    val quickNote by viewModel.quickNote.collectAsState()
    val isCapturing by viewModel.isCapturing.collectAsState()
    val lastCapturedPhoto by viewModel.lastCapturedPhoto.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showNoteSheet by remember { mutableStateOf(false) }
    var showProjectSheet by remember { mutableStateOf(false) }
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var shutterFlashActive by remember { mutableStateOf(false) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // CameraX Preview View
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    previewViewRef = this
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        viewModel.onPinchZoom(zoom)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        focusPoint = offset
                        val factory = previewViewRef?.meteringPointFactory
                        if (factory != null) {
                            viewModel.onTapToFocus(factory, offset.x, offset.y)
                        }
                    }
                }
        )

        // Bind CameraX on lifecycle and lens change
        LaunchedEffect(lensFacing, previewViewRef) {
            val previewView = previewViewRef ?: return@LaunchedEffect
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    viewModel.bindCamera(camera.cameraControl, camera.cameraInfo)
                } catch (e: Exception) {
                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.camera_error_in_use))
                    }
                }
            }, ContextCompat.getMainExecutor(context))
        }

        // Rule of thirds grid
        if (showGrid) {
            CameraGridOverlay()
        }

        // Tap to focus indicator
        focusPoint?.let { pt ->
            FocusIndicator(point = pt)
        }

        // Shutter flash animation
        AnimatedVisibility(
            visible = shutterFlashActive,
            enter = fadeIn(tween(50)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            )
        }

        // Top Controls: Flash, Grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            Spacer(modifier = Modifier.height(28.dp))
            CameraTopBar(
                flashMode = flashMode,
                showGrid = showGrid,
                onFlashClick = { viewModel.toggleFlash() },
                onGridClick = { viewModel.toggleGrid() },
                onCloseClick = onNavigateBack
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Live Stamp overlay preview bar
            LiveStampBar(
                projectName = currentProject?.name ?: stringResource(R.string.camera_select_project),
                locationData = locationData,
                quickNote = quickNote,
                stampSettings = stampSettings,
                onNoteClick = { showNoteSheet = true },
                onProjectClick = { showProjectSheet = true }
            )
        }

        // Bottom Controls: Gallery thumb, Shutter, Switch camera
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            CameraBottomBar(
                isCapturing = isCapturing,
                lastPhoto = lastCapturedPhoto,
                onShutterClick = {
                    shutterFlashActive = true
                    scope.launch {
                        delay(80)
                        shutterFlashActive = false
                    }
                    viewModel.capturePhoto(context, imageCapture)
                },
                onGalleryClick = {
                    selectedProjectId?.let { pid ->
                        onNavigateToGallery(pid)
                    } ?: run {
                        val firstId = projects.firstOrNull()?.project?.id
                        if (firstId != null) onNavigateToGallery(firstId) else onNavigateBack()
                    }
                },
                onSwitchCameraClick = { viewModel.toggleLensFacing() }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.Center)
        )
    }

    if (showNoteSheet) {
        QuickNoteSheet(
            initialNote = quickNote,
            onDismiss = { showNoteSheet = false },
            onSave = { note ->
                viewModel.setQuickNote(note)
                showNoteSheet = false
            }
        )
    }

    if (showProjectSheet) {
        ProjectSelectSheet(
            projects = projects,
            currentProjectId = selectedProjectId,
            onSelectProject = { pid ->
                viewModel.selectProject(pid)
                showProjectSheet = false
            },
            onDismiss = { showProjectSheet = false }
        )
    }
}

@Composable
private fun CameraPermissionRationaleScreen(
    permanentlyDenied: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.perm_camera_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (permanentlyDenied) {
                    stringResource(R.string.perm_denied_permanent)
                } else {
                    stringResource(R.string.perm_camera_desc)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            if (permanentlyDenied) {
                Button(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_settings_permission_btn")
                ) {
                    Text(stringResource(R.string.perm_settings_button))
                }
            } else {
                Button(
                    onClick = onRequestPermission,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("grant_camera_permission_btn")
                ) {
                    Text(stringResource(R.string.perm_camera_button))
                }
            }
        }
    }
}
