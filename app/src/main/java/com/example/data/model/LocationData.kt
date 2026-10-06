package com.example.data.model

data class LocationData(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val accuracyMeters: Float? = null,
    val addressLine: String? = null,
    val isGpsAvailable: Boolean = false,
    val isLocating: Boolean = false
) {
    fun hasValidCoordinates(): Boolean = latitude != null && longitude != null
}
