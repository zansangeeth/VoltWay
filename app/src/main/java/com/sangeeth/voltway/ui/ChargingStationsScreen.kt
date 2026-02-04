package com.sangeeth.voltway.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.sangeeth.voltway.model.Feature

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargingStationsScreen(
    stations: List<Feature>,
    selectedLocation: com.mapbox.geojson.Point?,
    isNavigating: Boolean = false,
    routePoints: List<Point> = emptyList(),
    onNavigateTo: (Double, Double) -> Unit,
    onCancelNavigation: (() -> Unit)? = null
) {
    val scaffoldState = rememberBottomSheetScaffoldState()
    var polylineAnnotationManager by remember { mutableStateOf<PolylineAnnotationManager?>(null) }
    
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = if (isNavigating) 0.dp else 160.dp, // Hide list during navigation
        sheetContainerColor = Color(0xFF1C1C1E),
        sheetContentColor = Color.White,
        containerColor = Color.Transparent,
        sheetContent = {
            if (!isNavigating) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    item {
                        Text(
                            text = "Nearby Stations (${stations.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    items(stations) { feature ->
                        StationCard(feature = feature, onNavigate = { lat, lng ->
                            onNavigateTo(lat, lng)
                        })
                    }
                }
            } else {
                // Empty sheet content or navigation info could go here
                Box(modifier = Modifier.height(1.dp)) 
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    MapView(context).apply {
                        getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) {
                            location.updateSettings {
                                enabled = true
                                locationPuck = location.createDefault2DPuck(context, withBearing = true)
                            }
                        }
                        
                        getMapboxMap().setCamera(
                            CameraOptions.Builder()
                                .center(selectedLocation ?: com.mapbox.geojson.Point.fromLngLat(-122.0, 37.0))
                                .zoom(if (isNavigating) 15.0 else 11.0)
                                .pitch(if (isNavigating) 45.0 else 0.0)
                                .build()
                        )
                        
                        // Initialize manager once
                        polylineAnnotationManager = annotations.createPolylineAnnotationManager()
                    }
                },
                update = { mapView ->
                    val mapboxMap = mapView.getMapboxMap()
                    selectedLocation?.let { point ->
                        mapboxMap.setCamera(
                            CameraOptions.Builder()
                                .center(point)
                                .zoom(if (isNavigating) 16.0 else 14.0)
                                .pitch(if (isNavigating) 60.0 else 0.0)
                                .build()
                        )
                    }

                    // Handle route line drawing using the persistent manager
                    polylineAnnotationManager?.let { manager ->
                        manager.deleteAll()
                        if (routePoints.isNotEmpty()) {
                            val polylineAnnotationOptions = PolylineAnnotationOptions()
                                .withPoints(routePoints)
                                .withLineColor("#007AFF")
                                .withLineWidth(6.0)
                            manager.create(polylineAnnotationOptions)
                        }
                    }
                }
            )

            if (isNavigating) {
                // Navigation Overlay
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1C1C1E).copy(alpha = 0.9f),
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Navigating to Station",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Follow the route on the map",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray
                            )
                        }
                        Button(
                            onClick = { onCancelNavigation?.invoke() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red
                            )
                        ) {
                            Text("Exit")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StationCard(
    feature: Feature,
    onNavigate: (Double, Double) -> Unit
) {
    val props = feature.properties
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = props.name ?: "Charging Station",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFFFFF)
                    )
                    Text(
                        text = props.fullAddress ?: "Address unavailable",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8E8E93)
                    )
                }
                // Availability Indicator (Mocking for now as EV API is restricted)
                Surface(
                    color = Color(0xFF34C759).copy(alpha = 0.2f),
                    shape = CircleShape
                ) {
                    Text(
                        text = "AVAILABLE",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF34C759),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (props.metadata?.phone != null || props.metadata?.website != null) {
                Text(
                    text = props.metadata?.phone ?: props.metadata?.website ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF0A84FF)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                val distance = props.distance ?: 0.0
                Text(
                    text = "Distance: ${"%.2f".format(distance / 1000.0)} km",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { 
                        val lat = props.coordinates?.latitude ?: feature.geometry.coordinates[1]
                        val lng = props.coordinates?.longitude ?: feature.geometry.coordinates[0]
                        onNavigate(lat, lng)
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Navigate", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
