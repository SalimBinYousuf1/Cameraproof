package com.example.ui.screens.camera

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.db.PhotoEntity
import com.example.data.db.ProjectWithPhotoCount
import com.example.data.model.LocationData
import com.example.data.model.StampSettings
import com.example.util.DateFormatter
import java.io.File

@Composable
fun CameraGridOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeW = 1.dp.toPx()
        val gridColor = Color(0x60FFFFFF)

        // Vertical lines
        val oneThirdW = size.width / 3f
        val twoThirdsW = oneThirdW * 2f
        drawLine(gridColor, Offset(oneThirdW, 0f), Offset(oneThirdW, size.height), strokeW)
        drawLine(gridColor, Offset(twoThirdsW, 0f), Offset(twoThirdsW, size.height), strokeW)

        // Horizontal lines
        val oneThirdH = size.height / 3f
        val twoThirdsH = oneThirdH * 2f
        drawLine(gridColor, Offset(0f, oneThirdH), Offset(size.width, oneThirdH), strokeW)
        drawLine(gridColor, Offset(0f, twoThirdsH), Offset(size.width, twoThirdsH), strokeW)
    }
}

@Composable
fun FocusIndicator(point: Offset) {
    val scale = remember { Animatable(1.4f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(point) {
        scale.animateTo(1.0f, tween(250))
        alpha.animateTo(0f, tween(600, delayMillis = 400))
    }

    Box(
        modifier = Modifier
            .offset { IntOffset((point.x - 35).toInt(), (point.y - 35).toInt()) }
            .size((70 * scale.value).dp)
            .border(
                1.5.dp,
                Color.White.copy(alpha = alpha.value),
                RoundedCornerShape(4.dp)
            )
    )
}

@Composable
fun LiveStampBar(
    projectName: String,
    locationData: LocationData,
    quickNote: String,
    stampSettings: StampSettings,
    onNoteClick: () -> Unit,
    onProjectClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xCC0F172A), // Dark slate 80% opacity
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Project row & Note badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onProjectClick),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = projectName,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Note chip
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (quickNote.isNotBlank()) Color(0xFF0369A1) else Color(0x40FFFFFF),
                    modifier = Modifier.clickable(onClick = onNoteClick)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (quickNote.isNotBlank()) quickNote else stringResource(R.string.camera_quick_note),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Location & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (locationData.hasValidCoordinates()) Color(0xFF10B981) else Color(0xFFF59E0B),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))

                val locText = when {
                    locationData.hasValidCoordinates() -> {
                        locationData.addressLine ?: DateFormatter.formatCoordinates(
                            locationData.latitude,
                            locationData.longitude,
                            locationData.accuracyMeters
                        )
                    }
                    locationData.isLocating -> stringResource(R.string.gps_acquiring)
                    else -> stringResource(R.string.gps_disabled)
                }

                Text(
                    text = locText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE2E8F0),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = DateFormatter.formatDateTime(System.currentTimeMillis(), stampSettings.use24Hour),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
fun CameraBottomBar(
    isCapturing: Boolean,
    lastPhoto: PhotoEntity?,
    onShutterClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onSwitchCameraClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Last photo thumbnail leading to Gallery
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x50000000))
                .border(1.5.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                .clickable(enabled = !isCapturing, onClick = onGalleryClick)
                .testTag("gallery_shortcut_btn"),
            contentAlignment = Alignment.Center
        ) {
            val thumbPath = lastPhoto?.thumbnailPath ?: lastPhoto?.filePath
            if (thumbPath != null && File(thumbPath).exists()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(File(thumbPath))
                        .crossfade(true)
                        .build(),
                    contentDescription = stringResource(R.string.nav_gallery),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = stringResource(R.string.nav_gallery),
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Shutter Button
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.25f))
                .padding(5.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(
                        enabled = !isCapturing,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onShutterClick
                    )
                    .testTag("camera_shutter_button"),
                contentAlignment = Alignment.Center
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(34.dp),
                        color = Color(0xFF0F172A),
                        strokeWidth = 3.dp
                    )
                }
            }
        }

        // Switch Lens Facing
        IconButton(
            onClick = onSwitchCameraClick,
            enabled = !isCapturing,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(0x50000000))
                .testTag("camera_switch_lens_button")
        ) {
            Icon(
                imageVector = Icons.Default.FlipCameraAndroid,
                contentDescription = stringResource(R.string.camera_switch),
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
fun CameraTopBar(
    flashMode: FlashMode,
    showGrid: Boolean,
    onFlashClick: () -> Unit,
    onGridClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Flash toggle
        IconButton(
            onClick = onFlashClick,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0x50000000))
                .testTag("camera_flash_toggle")
        ) {
            val icon = when (flashMode) {
                FlashMode.OFF -> Icons.Default.FlashOff
                FlashMode.AUTO -> Icons.Default.FlashAuto
                FlashMode.ON -> Icons.Default.FlashOn
            }
            Icon(
                imageVector = icon,
                contentDescription = stringResource(R.string.camera_flash),
                tint = if (flashMode == FlashMode.OFF) Color.White.copy(alpha = 0.6f) else Color(0xFFF59E0B),
                modifier = Modifier.size(22.dp)
            )
        }

        // Grid toggle
        IconButton(
            onClick = onGridClick,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0x50000000))
                .testTag("camera_grid_toggle")
        ) {
            Icon(
                imageVector = Icons.Default.GridOn,
                contentDescription = stringResource(R.string.camera_grid),
                tint = if (showGrid) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickNoteSheet(
    initialNote: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var note by remember { mutableStateOf(initialNote) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.camera_quick_note),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { newNote -> note = newNote },
                placeholder = { Text(stringResource(R.string.camera_quick_note_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_note_input"),
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.projects_cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.material3.Button(
                    onClick = { onSave(note) },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.projects_save))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectSelectSheet(
    projects: List<ProjectWithPhotoCount>,
    currentProjectId: Long?,
    onSelectProject: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.camera_select_project),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(14.dp))
            projects.forEach { item ->
                val isSelected = item.project.id == currentProjectId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectProject(item.project.id) }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = item.project.name,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
