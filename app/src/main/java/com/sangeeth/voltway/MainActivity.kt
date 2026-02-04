package com.sangeeth.voltway

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.sangeeth.voltway.location.LocationProvider
import com.sangeeth.voltway.network.MapboxService
import com.sangeeth.voltway.ui.ChargingStationsScreen
import com.sangeeth.voltway.ui.theme.VoltWayTheme

class MainActivity : AppCompatActivity() {
    private val accessToken by lazy { getString(R.string.mapbox_access_token) }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                fetchAndShowStations()
            } else {
                Toast.makeText(this, "Location permission required", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Request location permission
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED -> {
                fetchAndShowStations()
            }
            else -> {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun fetchAndShowStations() {
        val locationProvider = LocationProvider(this)
        locationProvider.getCurrentLocation { lat, lng ->
            lifecycleScope.launch(Dispatchers.IO) {
                val service = MapboxService(accessToken)
                val stationsResponse = try {
                    service.fetchChargingStations(lat, lng)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
                val stations = stationsResponse?.features ?: emptyList()
                
                withContext(Dispatchers.Main) {
                    setContent {
                        VoltWayTheme {
                            var selectedLatLng by remember { mutableStateOf<Pair<Double, Double>?>(null) }
                            // Use user location if no station is selected, otherwise station location
                            val targetLocation = selectedLatLng?.let { com.mapbox.geojson.Point.fromLngLat(it.second, it.first) } 
                                ?: com.mapbox.geojson.Point.fromLngLat(lng, lat)
                            
                            ChargingStationsScreen(
                                stations = stations,
                                selectedLocation = targetLocation,
                                onNavigateTo = { latDest, lngDest ->
                                    selectedLatLng = latDest to lngDest
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

