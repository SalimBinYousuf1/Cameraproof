package com.example

import com.example.util.DateFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class StampFormattingTest {

    @Test
    fun testCoordinatesFormatting_ValidPositive() {
        val formatted = DateFormatter.formatCoordinates(37.77492, -122.41941, 5.0f)
        assertTrue("Should contain North indicator", formatted.contains("37.77492° N"))
        assertTrue("Should contain West indicator", formatted.contains("122.41941° W"))
        assertTrue("Should contain accuracy", formatted.contains("±5m"))
    }

    @Test
    fun testCoordinatesFormatting_ValidNegative() {
        val formatted = DateFormatter.formatCoordinates(-33.8688, 151.2093, 10.0f)
        assertTrue("Should contain South indicator", formatted.contains("33.86880° S"))
        assertTrue("Should contain East indicator", formatted.contains("151.20930° E"))
        assertTrue("Should contain accuracy", formatted.contains("±10m"))
    }

    @Test
    fun testCoordinatesFormatting_NullValues() {
        val formattedNullLat = DateFormatter.formatCoordinates(null, -122.41941, 5f)
        assertEquals("GPS unavailable", formattedNullLat)

        val formattedNullLng = DateFormatter.formatCoordinates(37.77492, null, 5f)
        assertEquals("GPS unavailable", formattedNullLng)

        val formattedBothNull = DateFormatter.formatCoordinates(null, null, null)
        assertEquals("GPS unavailable", formattedBothNull)
    }

    @Test
    fun testDateTimeFormatting_24HourVs12Hour() {
        val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
            set(2026, Calendar.OCTOBER, 6, 14, 30, 45)
        }
        val timestamp = cal.timeInMillis

        val formatted24 = DateFormatter.formatDateTime(timestamp, use24Hour = true)
        assertTrue("24-hour should contain 14:30:45", formatted24.contains("14:30:45"))

        val formatted12 = DateFormatter.formatDateTime(timestamp, use24Hour = false)
        assertTrue("12-hour should contain 02:30:45 or 2:30:45", formatted12.contains("02:30:45") || formatted12.contains("2:30:45"))
        assertTrue("12-hour should contain PM", formatted12.uppercase().contains("PM"))
    }
}
