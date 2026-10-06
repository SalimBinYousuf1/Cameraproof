package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.exifinterface.media.ExifInterface
import com.example.data.model.LocationData
import com.example.data.model.StampPosition
import com.example.data.model.StampSettings
import com.example.data.model.StampTextSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

data class StamperResult(
    val filePath: String,
    val thumbnailPath: String,
    val width: Int,
    val height: Int
)

object ImageStamper {

    suspend fun processAndStampImage(
        context: Context,
        jpegBytes: ByteArray,
        rotationDegrees: Int,
        projectName: String,
        note: String,
        locationData: LocationData,
        stampSettings: StampSettings,
        timestamp: Long
    ): StamperResult = withContext(Dispatchers.Default) {

        // 1. Determine orientation from Exif or CameraX
        var exifRotation = 0
        try {
            val exif = ExifInterface(ByteArrayInputStream(jpegBytes))
            exifRotation = when (exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: Exception) {
            // Ignore
        }

        val totalRotation = if (exifRotation != 0) exifRotation else rotationDegrees

        // 2. Decode raw bitmap
        val options = BitmapFactory.Options().apply {
            inMutable = true
        }
        val rawBitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size, options)
            ?: throw IllegalStateException("Failed to decode captured image bytes")

        // 3. Rotate bitmap if needed so stamp is never sideways or cut off
        val uprightBitmap = if (totalRotation != 0) {
            val matrix = Matrix().apply { postRotate(totalRotation.toFloat()) }
            val rotated = Bitmap.createBitmap(
                rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true
            )
            if (rotated != rawBitmap) {
                rawBitmap.recycle()
            }
            rotated
        } else {
            rawBitmap
        }

        // 4. Burn the stamp onto upright bitmap
        burnStamp(
            bitmap = uprightBitmap,
            projectName = projectName,
            note = note,
            locationData = locationData,
            stampSettings = stampSettings,
            timestamp = timestamp
        )

        // 5. Save stamped JPEG to app private storage
        val photosDir = File(context.filesDir, "photos").apply { mkdirs() }
        val photoFile = File(photosDir, "salim_${timestamp}.jpg")

        FileOutputStream(photoFile).use { out ->
            uprightBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }

        // 6. Write EXIF GPS tags to the saved file
        try {
            val exif = ExifInterface(photoFile.absolutePath)
            if (locationData.latitude != null && locationData.longitude != null) {
                exif.setLatLong(locationData.latitude, locationData.longitude)
            }
            if (locationData.altitude != null) {
                exif.setAltitude(locationData.altitude)
            }
            exif.setAttribute(
                ExifInterface.TAG_DATETIME,
                DateFormatter.formatDateTime(timestamp, true)
            )
            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, note)
            exif.saveAttributes()
        } catch (e: Exception) {
            // Ignore EXIF write failure
        }

        // 7. Generate downscaled thumbnail for instant smooth gallery loading
        val thumbsDir = File(context.filesDir, "thumbnails").apply { mkdirs() }
        val thumbFile = File(thumbsDir, "thumb_${timestamp}.jpg")

        val thumbMaxDim = 480
        val scale = minOf(
            1f,
            thumbMaxDim.toFloat() / maxOf(uprightBitmap.width, uprightBitmap.height)
        )
        val thumbW = (uprightBitmap.width * scale).toInt().coerceAtLeast(1)
        val thumbH = (uprightBitmap.height * scale).toInt().coerceAtLeast(1)

        val thumbBitmap = Bitmap.createScaledBitmap(uprightBitmap, thumbW, thumbH, true)
        FileOutputStream(thumbFile).use { out ->
            thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        if (thumbBitmap != uprightBitmap) {
            thumbBitmap.recycle()
        }

        val finalWidth = uprightBitmap.width
        val finalHeight = uprightBitmap.height
        uprightBitmap.recycle()

        StamperResult(
            filePath = photoFile.absolutePath,
            thumbnailPath = thumbFile.absolutePath,
            width = finalWidth,
            height = finalHeight
        )
    }

    private fun burnStamp(
        bitmap: Bitmap,
        projectName: String,
        note: String,
        locationData: LocationData,
        stampSettings: StampSettings,
        timestamp: Long
    ) {
        val canvas = Canvas(bitmap)
        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()

        // Reference dimension scale (normalized to 1440px width)
        val scale = (width / 1440f).coerceIn(0.5f, 3.5f)

        val textSizePx = when (stampSettings.textSize) {
            StampTextSize.SMALL -> 26f * scale
            StampTextSize.MEDIUM -> 34f * scale
            StampTextSize.LARGE -> 42f * scale
        }

        val padding = 28f * scale
        val margin = 36f * scale
        val lineSpacing = 10f * scale
        val pillRadius = 14f * scale

        // Build list of lines to draw
        val lines = mutableListOf<String>()

        if (stampSettings.showDateTime) {
            lines.add(DateFormatter.formatDateTime(timestamp, stampSettings.use24Hour))
        }

        if (stampSettings.showProjectName && projectName.isNotBlank()) {
            lines.add("PROJECT: $projectName")
        }

        if (stampSettings.showCoordinates) {
            val coords = if (locationData.hasValidCoordinates()) {
                DateFormatter.formatCoordinates(
                    locationData.latitude,
                    locationData.longitude,
                    locationData.accuracyMeters
                )
            } else if (locationData.isLocating) {
                "GPS: Acquiring signal..."
            } else {
                "GPS: Unavailable"
            }
            lines.add("GPS: $coords")
        }

        if (stampSettings.showAddress) {
            val addr = locationData.addressLine ?: if (locationData.isLocating) {
                "Acquiring address..."
            } else {
                "Address unavailable (offline)"
            }
            lines.add("LOC: $addr")
        }

        if (stampSettings.showNote && note.isNotBlank()) {
            lines.add("NOTE: $note")
        }

        if (lines.isEmpty()) return

        // Text Paint
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = textSizePx
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            setShadowLayer(3f * scale, 1f * scale, 1f * scale, Color.BLACK)
        }

        // Measure text bounds
        var maxLineWidth = 0f
        val lineHeight = textPaint.fontSpacing

        for (line in lines) {
            val w = textPaint.measureText(line)
            if (w > maxLineWidth) maxLineWidth = w
        }

        // Wrap lines if exceeding 85% of image width
        val maxAllowedWidth = width * 0.85f
        if (maxLineWidth > maxAllowedWidth) {
            // scale down text paint if needed to fit perfectly
            val adjustment = maxAllowedWidth / maxLineWidth
            textPaint.textSize = textSizePx * adjustment
            maxLineWidth = maxAllowedWidth
        }

        val boxWidth = maxLineWidth + (padding * 2)
        val boxHeight = (lines.size * textPaint.fontSpacing) + (padding * 2) - lineSpacing

        // Calculate box position based on StampPosition
        val (boxLeft, boxTop) = when (stampSettings.position) {
            StampPosition.BOTTOM_LEFT -> Pair(margin, height - margin - boxHeight)
            StampPosition.BOTTOM_RIGHT -> Pair(width - margin - boxWidth, height - margin - boxHeight)
            StampPosition.TOP_LEFT -> Pair(margin, margin)
            StampPosition.TOP_RIGHT -> Pair(width - margin - boxWidth, margin)
        }

        val boxRect = RectF(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight)

        // Draw dark translucent background card
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(185, 15, 23, 42) // Slate 900 at 72% opacity
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(boxRect, pillRadius, pillRadius, bgPaint)

        // Draw crisp border outline
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(80, 255, 255, 255) // Subtle white outline
            strokeWidth = 2f * scale
            style = Paint.Style.STROKE
        }
        canvas.drawRoundRect(boxRect, pillRadius, pillRadius, borderPaint)

        // Draw text lines
        var currentY = boxTop + padding + textPaint.textSize - (textPaint.fontMetrics.descent / 2)
        for (line in lines) {
            canvas.drawText(line, boxLeft + padding, currentY, textPaint)
            currentY += textPaint.fontSpacing
        }
    }
}
