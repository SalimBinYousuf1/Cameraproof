package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.db.PhotoEntity
import com.example.data.db.ProjectEntity
import com.example.data.model.ReportSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object PdfReportGenerator {

    private const val PAGE_WIDTH = 595 // Standard A4 width in points
    private const val PAGE_HEIGHT = 842 // Standard A4 height in points
    private const val MARGIN = 36f

    suspend fun generatePdf(
        context: Context,
        project: ProjectEntity,
        photos: List<PhotoEntity>,
        reportSettings: ReportSettings,
        isPro: Boolean,
        outputStream: OutputStream
    ) = withContext(Dispatchers.Default) {

        val pdfDocument = PdfDocument()

        // 2 photos per page
        val photosPerPage = 2
        val totalPages = if (photos.isEmpty()) 1 else (photos.size + photosPerPage - 1) / photosPerPage

        var photoIndex = 0

        for (pageNumber in 1..totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Background
            canvas.drawColor(Color.WHITE)

            // Draw Header on each page
            drawPageHeader(
                canvas = canvas,
                project = project,
                reportSettings = reportSettings,
                isPro = isPro,
                isFirstPage = pageNumber == 1
            )

            // Draw Photos for this page
            val contentTop = if (pageNumber == 1) 130f else 80f
            val availableHeight = PAGE_HEIGHT - contentTop - 50f
            val slotHeight = availableHeight / photosPerPage

            for (slot in 0 until photosPerPage) {
                if (photoIndex < photos.size) {
                    val photo = photos[photoIndex]
                    val slotTop = contentTop + (slot * slotHeight)

                    drawPhotoSlot(
                        canvas = canvas,
                        photo = photo,
                        index = photoIndex + 1,
                        top = slotTop,
                        height = slotHeight - 14f
                    )
                    photoIndex++
                }
            }

            // Draw Footer
            drawPageFooter(
                canvas = canvas,
                project = project,
                pageNumber = pageNumber,
                totalPages = totalPages,
                isPro = isPro
            )

            pdfDocument.finishPage(page)
        }

        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
    }

    suspend fun generatePdfToCache(
        context: Context,
        project: ProjectEntity,
        photos: List<PhotoEntity>,
        reportSettings: ReportSettings,
        isPro: Boolean
    ): File = withContext(Dispatchers.IO) {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val sanitizedName = project.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val file = File(reportsDir, "${sanitizedName}_report.pdf")

        FileOutputStream(file).use { out ->
            generatePdf(context, project, photos, reportSettings, isPro, out)
        }
        file
    }

    private fun drawPageHeader(
        canvas: Canvas,
        project: ProjectEntity,
        reportSettings: ReportSettings,
        isPro: Boolean,
        isFirstPage: Boolean
    ) {
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42) // Slate 900
            textSize = if (isFirstPage) 18f else 12f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105) // Slate 600
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240) // Slate 200
            strokeWidth = 1f
        }

        if (isFirstPage) {
            var y = MARGIN + 12f

            // Pro custom company header
            if (isPro && reportSettings.companyName.isNotBlank()) {
                val companyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(2, 132, 199) // Sky 600
                    textSize = 10.5f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                }
                canvas.drawText(reportSettings.companyName.uppercase(), MARGIN, y, companyPaint)
                y += 16f
            }

            canvas.drawText(project.name, MARGIN, y, titlePaint)
            y += 14f

            val details = buildString {
                if (project.description.isNotBlank()) {
                    append(project.description)
                    append(" • ")
                }
                append("Created: ")
                append(DateFormatter.formatDateOnly(project.createdAt))
                if (reportSettings.inspectorName.isNotBlank()) {
                    append(" • Inspector: ")
                    append(reportSettings.inspectorName)
                }
                if (reportSettings.inspectorLicense.isNotBlank()) {
                    append(" (ID: ${reportSettings.inspectorLicense})")
                }
            }
            canvas.drawText(details, MARGIN, y, subPaint)

            // Header separator line
            y += 14f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        } else {
            // Running header on page 2+
            val y = MARGIN + 10f
            canvas.drawText("Project: ${project.name}", MARGIN, y, titlePaint)
            val pageDate = DateFormatter.formatDateOnly(project.createdAt)
            val dateW = subPaint.measureText(pageDate)
            canvas.drawText(pageDate, PAGE_WIDTH - MARGIN - dateW, y, subPaint)
            canvas.drawLine(MARGIN, y + 8f, PAGE_WIDTH - MARGIN, y + 8f, linePaint)
        }
    }

    private fun drawPhotoSlot(
        canvas: Canvas,
        photo: PhotoEntity,
        index: Int,
        top: Float,
        height: Float
    ) {
        val bgRect = RectF(MARGIN, top, PAGE_WIDTH - MARGIN, top + height)

        // Slot border & background
        val slotBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(248, 250, 252) // Slate 50
            style = Paint.Style.FILL
        }
        val slotBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240) // Slate 200
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(bgRect, 8f, 8f, slotBgPaint)
        canvas.drawRoundRect(bgRect, 8f, 8f, slotBorderPaint)

        // Decode downscaled bitmap safely to prevent OOM
        val photoW = (PAGE_WIDTH - (MARGIN * 2)) * 0.52f
        val photoH = height - 16f
        val photoLeft = MARGIN + 8f
        val photoTop = top + 8f

        val bitmap = decodeSampledBitmap(photo.filePath, 600, 450)
        if (bitmap != null) {
            val destRect = RectF(photoLeft, photoTop, photoLeft + photoW, photoTop + photoH)
            drawBitmapAspectFit(canvas, bitmap, destRect)
            bitmap.recycle() // Recycle immediately!
        } else {
            // Draw placeholder if file missing
            val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(203, 213, 225)
                style = Paint.Style.FILL
            }
            canvas.drawRect(RectF(photoLeft, photoTop, photoLeft + photoW, photoTop + photoH), placeholderPaint)
        }

        // Draw Metadata column on right
        val metaLeft = photoLeft + photoW + 14f
        val metaTop = photoTop + 8f
        val metaWidth = (PAGE_WIDTH - MARGIN - 8f) - metaLeft

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139) // Slate 500
            textSize = 7.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42) // Slate 900
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(2, 132, 199)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        var curY = metaTop + 6f
        canvas.drawText("PHOTO #$index", metaLeft, curY, badgePaint)
        curY += 15f

        // Date/Time
        canvas.drawText("TIMESTAMP", metaLeft, curY, labelPaint)
        curY += 10f
        canvas.drawText(DateFormatter.formatDateTime(photo.timestamp, true), metaLeft, curY, valPaint)
        curY += 14f

        // Coordinates
        canvas.drawText("GPS COORDINATES", metaLeft, curY, labelPaint)
        curY += 10f
        val coordsText = if (photo.latitude != null && photo.longitude != null) {
            DateFormatter.formatCoordinates(photo.latitude, photo.longitude, photo.accuracyMeters)
        } else {
            "GPS unavailable"
        }
        canvas.drawText(coordsText, metaLeft, curY, valPaint)
        curY += 14f

        // Address
        if (!photo.addressLine.isNullOrBlank()) {
            canvas.drawText("LOCATION", metaLeft, curY, labelPaint)
            curY += 10f
            // Wrap address text if needed
            val addressLines = wrapText(photo.addressLine, valPaint, metaWidth)
            for (line in addressLines.take(2)) {
                canvas.drawText(line, metaLeft, curY, valPaint)
                curY += 10f
            }
            curY += 4f
        }

        // Note
        if (photo.note.isNotBlank()) {
            canvas.drawText("INSPECTOR NOTE", metaLeft, curY, labelPaint)
            curY += 10f
            val noteLines = wrapText(photo.note, valPaint, metaWidth)
            for (line in noteLines.take(3)) {
                canvas.drawText(line, metaLeft, curY, valPaint)
                curY += 10f
            }
        }
    }

    private fun drawBitmapAspectFit(canvas: Canvas, bitmap: Bitmap, dest: RectF) {
        val srcW = bitmap.width.toFloat()
        val srcH = bitmap.height.toFloat()
        val scale = minOf(dest.width() / srcW, dest.height() / srcH)
        val fitW = srcW * scale
        val fitH = srcH * scale

        val fitLeft = dest.left + (dest.width() - fitW) / 2f
        val fitTop = dest.top + (dest.height() - fitH) / 2f
        val fitRect = RectF(fitLeft, fitTop, fitLeft + fitW, fitTop + fitH)

        val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
        canvas.drawBitmap(bitmap, srcRect, fitRect, Paint(Paint.FILTER_BITMAP_FLAG))

        // Subtle frame around image
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRect(fitRect, framePaint)
    }

    private fun decodeSampledBitmap(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, options)

            var sampleSize = 1
            if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while ((halfHeight / sampleSize) >= reqHeight && (halfWidth / sampleSize) >= reqWidth) {
                    sampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // Half the memory of ARGB_8888
            }
            return BitmapFactory.decodeFile(path, decodeOptions)
        } catch (e: Exception) {
            return null
        }
    }

    private fun drawPageFooter(
        canvas: Canvas,
        project: ProjectEntity,
        pageNumber: Int,
        totalPages: Int,
        isPro: Boolean
    ) {
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184) // Slate 400
            textSize = 8f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val y = PAGE_HEIGHT - MARGIN + 12f

        // Left: Project title
        canvas.drawText("Salim Proof Report • ${project.name}", MARGIN, y, footerPaint)

        // Center: Free watermark (removed for Pro)
        if (!isPro) {
            val freeText = "Made with Salim Photo Proof Camera"
            val textW = footerPaint.measureText(freeText)
            canvas.drawText(freeText, (PAGE_WIDTH - textW) / 2f, y, footerPaint)
        }

        // Right: Page number
        val pageText = "Page $pageNumber of $totalPages"
        val pageW = footerPaint.measureText(pageText)
        canvas.drawText(pageText, PAGE_WIDTH - MARGIN - pageW, y, footerPaint)
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        return lines
    }
}
