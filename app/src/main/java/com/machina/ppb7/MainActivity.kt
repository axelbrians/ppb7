package com.machina.ppb7

import android.Manifest
import android.annotation.SuppressLint
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.machina.ppb7.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback {


    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient


    private var defaultLocation = Location("")
    private var googleMap: GoogleMap? = null
    private var lastKnownLocation: Location? = null
    private var locationPermissionGranted = false

    // 1
    private lateinit var locationCallback: LocationCallback
    // 2
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = SupportMapFragment.newInstance()
        supportFragmentManager
            .beginTransaction()
            .add(R.id.activity_main_fragment_container, mapFragment)
            .commit()

        getLocationPermission()

        mapFragment.getMapAsync(this)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(location: LocationResult) {
                super.onLocationResult(location)
                Log.d(GMAP_TAG, "updated from callback")
                lastKnownLocation = location.lastLocation
                if (lastKnownLocation != null) {
                    googleMap?.apply {
                        clear()
                        moveCamera(CameraUpdateFactory.newLatLngZoom(
                            LatLng(lastKnownLocation!!.latitude,
                                lastKnownLocation!!.longitude), 5000.toFloat()))

                    }

                    updateLngLatUI()
                }
            }
        }

        createLocationRequest()

        binding.activityMainStartBtn.setOnClickListener {
            if (it.isEnabled) {
                it.isEnabled = false
                binding.activityMainStopBtn.isEnabled = true
                if (locationUpdateState) {
                    startLocationUpdates()
                }
            }
        }

        binding.activityMainStopBtn.setOnClickListener {
            if (it.isEnabled) {
                it.isEnabled = false
                binding.activityMainStartBtn.isEnabled = true
                fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
//        googleMap.addMarker(
//            MarkerOptions()
//                .position(LatLng(0.0, 0.0))
//                .title("Marker")
//        )

        setupMap()
        updateLocationUI()
    }

    @SuppressLint("MissingPermission")
    private fun setupMap() {
        if (locationPermissionGranted) {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener(this) { location ->
                location?.let {
                    Log.d(GMAP_TAG, "updated from on success")
                    lastKnownLocation = location
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 500f))
                    updateLngLatUI()
                }
            }

        }
    }

    private fun createLocationRequest() {

        // 1
        locationRequest = LocationRequest()
        // 2
        locationRequest.interval = 10000
        // 3
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        // 4
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        // 5
        task.addOnSuccessListener {
            locationUpdateState = true
//            startLocationUpdates()
        }
        task.addOnFailureListener { e ->
            // 6
            if (e is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    e.startResolutionForResult(this,
                        REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    private fun startLocationUpdates() {
        //1
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
            return
        }
        //2
        Log.d(GMAP_TAG, "start listening")
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper()/* Looper */)
    }

    private fun updateLngLatUI() {
        val long = lastKnownLocation?.longitude.toString()
        val lat = lastKnownLocation?.latitude.toString()
        if (long.isNotBlank())
            binding.activityMainLong.text = long
        if (lat.isNotBlank())
            binding.activityMainLat.text = lat

        if (lastKnownLocation?.longitude != null && lastKnownLocation?.latitude != null) {
            val langLot = LatLng(lastKnownLocation!!.latitude, lastKnownLocation!!.longitude)
            googleMap?.addMarker(MarkerOptions().position(langLot).title("Your Location"))
        }
    }


    private fun updateLocationUI() {
        if (googleMap == null) {
            return
        }

        try {
            if (locationPermissionGranted) {
//                googleMap?.isMyLocationEnabled = true
//                googleMap?.uiSettings?.isMyLocationButtonEnabled = true
            } else {
//                googleMap?.isMyLocationEnabled = false
//                googleMap?.uiSettings?.isMyLocationButtonEnabled = false
                lastKnownLocation = null
                getLocationPermission()
            }
        } catch (e: SecurityException) {
            Log.e("Exception", e.message, e)
        }
    }

//    private fun getDeviceLocation() {
//        /*
//         * Get the best and most recent location of the device, which may be null in rare
//         * cases when a location is not available.
//         */
//        try {
//            if (locationPermissionGranted) {
//                val locationResult = fusedLocationProviderClient.lastLocation
//                locationResult.addOnCompleteListener(this) { task ->
//                    if (task.isSuccessful) {
//                        // Set the map's camera position to the current location of the device.
//                        lastKnownLocation = task.result
//                        val long = lastKnownLocation?.longitude.toString()
//                        val lat = lastKnownLocation?.latitude.toString()
//                        if (long.isNotBlank())
//                            binding.activityMainLong.text = long
//                        if (lat.isNotBlank())
//                            binding.activityMainLat.text = lat
//
//
//                        if (lastKnownLocation != null) {
//                            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(
//                                LatLng(lastKnownLocation!!.latitude,
//                                    lastKnownLocation!!.longitude), 2000.toFloat()))
//                        }
//                    } else {
//                        Log.d(GMAP_TAG, "Current location is null. Using defaults.")
//                        Log.e(GMAP_TAG, "Exception: %s", task.exception)
////                        googleMap?.moveCamera(CameraUpdateFactory
////                            .newLatLngZoom(defaultLocation, 100.toFloat()))
//                        googleMap?.uiSettings?.isMyLocationButtonEnabled = false
//                    }
//                }
//            }
//        } catch (e: SecurityException) {
//            Log.e("Exception: %s", e.message, e)
//        }
//    }

    private fun getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        }
    }

    // 2
    override fun onPause() {
        super.onPause()
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationPermissionGranted = false
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true
                }
            }
        }
        updateLocationUI()
    }

    companion object {
        const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 11
        const val REQUEST_CHECK_SETTINGS = 12
        const val GMAP_TAG = "google maps"
    }
}