package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.model.LocationData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class LocationHelper(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _currentLocation = MutableStateFlow(LocationData())
    val currentLocation: StateFlow<LocationData> = _currentLocation.asStateFlow()

    private var isTracking = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            processLocation(location)
        }
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (!hasLocationPermission()) {
            _currentLocation.value = LocationData(
                isGpsAvailable = false,
                isLocating = false
            )
            return
        }

        if (isTracking) return
        isTracking = true

        _currentLocation.value = _currentLocation.value.copy(isLocating = true)

        try {
            // Get last known location first for immediate response
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    processLocation(loc)
                }
            }

            // Start continuous high accuracy location request
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000)
                .setMinUpdateIntervalMillis(2000)
                .setMinUpdateDistanceMeters(1.0f)
                .build()

            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        } catch (e: Exception) {
            Log.w("LocationHelper", "Fused location failed, attempting LocationManager fallback: ${e.message}")
            useLocationManagerFallback()
        }
    }

    @SuppressLint("MissingPermission")
    private fun useLocationManagerFallback() {
        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val gpsLoc = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val netLoc = lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val best = gpsLoc ?: netLoc
            if (best != null) {
                processLocation(best)
            } else {
                _currentLocation.value = LocationData(
                    isGpsAvailable = false,
                    isLocating = false
                )
            }
        } catch (e: Exception) {
            _currentLocation.value = LocationData(
                isGpsAvailable = false,
                isLocating = false
            )
        }
    }

    fun stopLocationUpdates() {
        if (!isTracking) return
        isTracking = false
        try {
            fusedClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun processLocation(location: Location) {
        val lat = location.latitude
        val lng = location.longitude
        val alt = if (location.hasAltitude()) location.altitude else null
        val acc = if (location.hasAccuracy()) location.accuracy else null

        _currentLocation.value = LocationData(
            latitude = lat,
            longitude = lng,
            altitude = alt,
            accuracyMeters = acc,
            addressLine = _currentLocation.value.addressLine, // Preserve previous until geocoded
            isGpsAvailable = true,
            isLocating = false
        )

        // Reverse geocode asynchronously
        scope.launch {
            val address = reverseGeocode(lat, lng)
            _currentLocation.value = _currentLocation.value.copy(
                addressLine = address
            )
        }
    }

    suspend fun reverseGeocode(lat: Double, lng: Double): String = withContext(Dispatchers.IO) {
        try {
            if (!Geocoder.isPresent()) {
                return@withContext "Address unavailable (offline)"
            }
            val geocoder = Geocoder(context, Locale.getDefault())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Synchronous bridge with coroutine for API 33+
                var resultAddress: String? = null
                val lock = Object()
                geocoder.getFromLocation(lat, lng, 1) { addresses ->
                    synchronized(lock) {
                        resultAddress = formatAddress(addresses.firstOrNull())
                        (lock as java.lang.Object).notifyAll()
                    }
                }
                synchronized(lock) {
                    if (resultAddress == null) {
                        try {
                            (lock as java.lang.Object).wait(2500)
                        } catch (e: InterruptedException) {
                            // Timeout
                        }
                    }
                }
                resultAddress ?: "Address unavailable (offline)"
            } else {
                @Suppress("DEPRECATION")
                val list = geocoder.getFromLocation(lat, lng, 1)
                formatAddress(list?.firstOrNull()) ?: "Address unavailable (offline)"
            }
        } catch (e: Exception) {
            Log.d("LocationHelper", "Geocoding failed: ${e.message}")
            "Address unavailable (offline)"
        }
    }

    private fun formatAddress(address: Address?): String? {
        if (address == null) return null
        val thoroughfare = address.thoroughfare
        val subThoroughfare = address.subThoroughfare
        val locality = address.locality ?: address.subAdminArea ?: address.adminArea
        val postalCode = address.postalCode

        val line1 = when {
            subThoroughfare != null && thoroughfare != null -> "$subThoroughfare $thoroughfare"
            thoroughfare != null -> thoroughfare
            else -> address.getAddressLine(0)
        }

        val line2 = when {
            locality != null && postalCode != null -> "$locality, $postalCode"
            locality != null -> locality
            else -> null
        }

        return when {
            line1 != null && line2 != null && !line1.contains(line2) -> "$line1, $line2"
            line1 != null -> line1
            else -> null
        }
    }
}
