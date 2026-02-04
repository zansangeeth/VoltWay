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
import kotlinx.coroutines.*
import com.sangeeth.voltway.location.LocationProvider
import com.sangeeth.voltway.network.MapboxService
import com.sangeeth.voltway.ui.ChargingStationsScreen
import com.sangeeth.voltway.ui.theme.VoltWayTheme
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.geojson.Point
import com.mapbox.turf.TurfTransformation

class MainActivity : AppCompatActivity() {
    private lateinit var mapboxNavigation: MapboxNavigation
    private val accessToken by lazy { getString(R.string.mapbox_access_token) }
    
    // Observer for arrival events
    private val arrivalObserver = object : ArrivalObserver {
        override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
            Toast.makeText(this@MainActivity, "You have arrived at the charging station!", Toast.LENGTH_LONG).show()
        }
        override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {}
        override fun onWaypointArrival(routeProgress: RouteProgress) {}
    }
    
    // Observer for route updates
    private val routesObserver = RoutesObserver { result ->
        if (result.navigationRoutes.isNotEmpty()) {
            // Handle route drawing - we might need to pass this to the screen
        }
    }

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
        
        // Initialize Mapbox Navigation v2 style
        // Check if provider is already registered using the retrieve attempt or similar for v2
        try {
            MapboxNavigationProvider.retrieve()
        } catch (e: Exception) {
            MapboxNavigationProvider.create(
                NavigationOptions.Builder(this)
                    .accessToken(accessToken)
                    .build()
            )
        }
        mapboxNavigation = MapboxNavigationProvider.retrieve()
        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxNavigation.registerArrivalObserver(arrivalObserver)

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

    override fun onDestroy() {
        super.onDestroy()
        if (::mapboxNavigation.isInitialized) {
            mapboxNavigation.unregisterRoutesObserver(routesObserver)
            mapboxNavigation.unregisterArrivalObserver(arrivalObserver)
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
                            var isNavigating by remember { mutableStateOf(false) }
                            var routePoints by remember { mutableStateOf<List<Point>>(emptyList()) }
                            
                            val requestRouteCallback = remember {
                                object : NavigationRouterCallback {
                                    override fun onRoutesReady(routes: List<NavigationRoute>, routerOrigin: RouterOrigin) {
                                        if (routes.isNotEmpty()) {
                                            mapboxNavigation.setNavigationRoutes(routes)
                                            val geometry = routes[0].directionsRoute.geometry()
                                            if (geometry != null) {
                                                val pts = com.mapbox.geojson.LineString.fromPolyline(geometry, 6).coordinates()
                                                routePoints = TurfTransformation.simplify(pts, 0.0001)
                                            }
                                        }
                                    }
                                    override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {}
                                    override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {}
                                }
                            }

                            // Use user location if no station is selected, otherwise station location
                            val targetLocation = selectedLatLng?.let { com.mapbox.geojson.Point.fromLngLat(it.second, it.first) } 
                                ?: com.mapbox.geojson.Point.fromLngLat(lng, lat)
                            
                            ChargingStationsScreen(
                                stations = stations,
                                selectedLocation = targetLocation,
                                isNavigating = isNavigating,
                                routePoints = routePoints,
                                onNavigateTo = { latDest, lngDest ->
                                    selectedLatLng = latDest to lngDest
                                    isNavigating = true
                                    
                                    val origin = Point.fromLngLat(lng, lat)
                                    val destination = Point.fromLngLat(lngDest, latDest)
                                    val routeOptions = RouteOptions.builder()
                                        .coordinatesList(listOf(origin, destination))
                                        .profile(DirectionsCriteria.PROFILE_DRIVING)
                                        .steps(true)
                                        .build()
                                    
                                    mapboxNavigation.requestRoutes(routeOptions, requestRouteCallback)
                                    mapboxNavigation.startTripSession()
                                },
                                onCancelNavigation = {
                                    isNavigating = false
                                    routePoints = emptyList()
                                    mapboxNavigation.stopTripSession()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

