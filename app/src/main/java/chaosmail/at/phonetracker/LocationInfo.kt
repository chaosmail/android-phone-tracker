package chaosmail.at.phonetracker

import android.Manifest
import android.app.Activity
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.util.Log

data class NormalizedLocationInfo(val longitude: Double, val latitude: Double)

class LocationInfo (private val activity: Activity, private val locationManager: LocationManager): LocationListener {

    // The minimum distance to change Updates in meters
    private val MIN_UPDATE_DISTANCE: Float = 1.0f

    // The minimum time between updates in milliseconds
    private val MIN_UPDATE_TIME = 1000 * 1L

    private var location: Location? = null

    init {
        startTracking()
    }

    fun startTracking() {
        // Getting GPS status
        val isGPSEnabled = locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER)

        // Getting network status
        val isNetworkEnabled = locationManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        try {
            if (isNetworkEnabled) {
                // register location change handler
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        MIN_UPDATE_TIME,
                        MIN_UPDATE_DISTANCE, this)

                // get the last known location to start
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }
            // If GPS enabled, get latitude/longitude using GPS Services
            else if (isGPSEnabled) {
                // register location change handler
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        MIN_UPDATE_TIME,
                        MIN_UPDATE_DISTANCE, this)
                // get the last known location to start
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            }
        } catch (e: SecurityException) {
            Log.e("PhoneTracke", "Permission denied to access location data")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(activity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        SecurityUtil.PERM_ACCESS_FINE_LOCATION)
            }
        }
    }

    @Suppress("unused")
    fun stopTracking() {
        locationManager.removeUpdates(this)
    }

    fun getLocation(): NormalizedLocationInfo? {
        if (location != null) {
            return NormalizedLocationInfo(latitude = location!!.latitude, longitude = location!!.longitude)
        }
        return null
    }

    override fun onLocationChanged(newLocation: android.location.Location?) {
        location = newLocation // update location
    }
    override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}
    override fun onProviderEnabled(p0: String?) {}
    override fun onProviderDisabled(p0: String?) {}
}
