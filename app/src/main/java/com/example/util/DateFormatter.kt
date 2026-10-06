package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatter {

    fun formatDateTime(timestamp: Long, use24Hour: Boolean): String {
        val pattern = if (use24Hour) {
            "yyyy-MM-dd HH:mm:ss"
        } else {
            "yyyy-MM-dd hh:mm:ss a"
        }
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDateOnly(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatCoordinates(latitude: Double?, longitude: Double?, accuracy: Float?): String {
        if (latitude == null || longitude == null) return "GPS unavailable"

        val latDirection = if (latitude >= 0) "N" else "S"
        val lonDirection = if (longitude >= 0) "E" else "W"

        val latStr = String.format(Locale.US, "%.5f° %s", kotlin.math.abs(latitude), latDirection)
        val lonStr = String.format(Locale.US, "%.5f° %s", kotlin.math.abs(longitude), lonDirection)

        val accStr = if (accuracy != null && accuracy > 0) {
            String.format(Locale.US, "  ±%.0fm", accuracy)
        } else ""

        return "$latStr, $lonStr$accStr"
    }
}
