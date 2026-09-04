package com.example.releaf.ui.map

import android.Manifest
import android.content.Context.LOCATION_SERVICE
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.releaf.data.remote.dto.PoiDto
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun OsmMap(
    pois: List<PoiDto>,
    onPoiClick: (PoiDto) -> Unit,
    centerOnLocation: Boolean = false,
    onCenterConsumed: () -> Unit = {},
    focusPoint: GeoPoint? = null,
    isDarkMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasLocationPermission by remember { mutableStateOf(false) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var userLocation by remember { mutableStateOf(GeoPoint(3.1390, 101.6869)) }

    val locationManager = context.getSystemService(LOCATION_SERVICE) as? LocationManager
    val registeredListeners = remember { mutableListOf<LocationListener>() }

    // Never leave location listeners behind when the map leaves composition.
    DisposableEffect(Unit) {
        onDispose {
            registeredListeners.forEach {
                try {
                    locationManager?.removeUpdates(it)
                } catch (_: Exception) {
                }
            }
            registeredListeners.clear()
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    var shouldAskNotification by remember { mutableStateOf(false) }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        shouldAskNotification = true
    }

    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fine || coarse) {
            hasLocationPermission = true
            shouldAskNotification = true
        } else {
            locationLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(shouldAskNotification) {
        if (shouldAskNotification && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotif = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasNotif) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(hasLocationPermission, mapViewRef) {
        if (hasLocationPermission) {
            val map = mapViewRef
            if (map != null) {
                val mainLooper = Looper.getMainLooper()
                val now = System.currentTimeMillis()
                val freshThreshold = 2 * 60 * 1000L

                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        userLocation = GeoPoint(location.latitude, location.longitude)
                        mapViewRef?.let { centerPlain(it, userLocation) }
                        try {
                            locationManager?.removeUpdates(this)
                        } catch (_: Exception) {
                        }
                        registeredListeners.remove(this)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(
                        provider: String?,
                        status: Int,
                        extras: Bundle?
                    ) {
                    }

                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }

                try {
                    val gps = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    val network =
                        locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                    val fresh = listOfNotNull(gps, network)
                        .filter { now - it.time < freshThreshold }
                        .maxByOrNull { it.time }

                    if (fresh != null) {
                        userLocation = GeoPoint(fresh.latitude, fresh.longitude)
                        centerPlain(map, userLocation)
                    } else {
                        try {
                            locationManager?.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                0L,
                                0f,
                                listener,
                                mainLooper
                            )
                            locationManager?.requestLocationUpdates(
                                LocationManager.NETWORK_PROVIDER,
                                0L,
                                0f,
                                listener,
                                mainLooper
                            )
                            registeredListeners.add(listener)
                        } catch (_: SecurityException) {
                        }
                    }
                } catch (_: SecurityException) {
                }
                try {
                    val overlay = MyLocationNewOverlay(GpsMyLocationProvider(context), map)
                    overlay.enableMyLocation()
                    map.overlays.add(overlay)
                } catch (_: Exception) {
                }
            }
        }
    }

    // One-shot centering: center on the user position ONCE when requested, then
    // consume the flag so the map never keeps snapping back to the user.
    LaunchedEffect(centerOnLocation, hasLocationPermission, mapViewRef) {
        if (!centerOnLocation || !hasLocationPermission || mapViewRef == null) {
            return@LaunchedEffect
        }
        val map = mapViewRef ?: return@LaunchedEffect
        val mainLooper = Looper.getMainLooper()

        try {
            val now = System.currentTimeMillis()
            val freshThreshold = 2 * 60 * 1000L

            val gps = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val network = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            val fresh = listOfNotNull(gps, network)
                .filter { now - it.time < freshThreshold }
                .maxByOrNull { it.time }

            if (fresh != null) {
                userLocation = GeoPoint(fresh.latitude, fresh.longitude)
                centerPlain(map, userLocation)
                try {
                    onCenterConsumed()
                } catch (_: Exception) {
                }
                return@LaunchedEffect
            }

            var consumed = false
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (consumed) return
                    consumed = true
                    userLocation = GeoPoint(location.latitude, location.longitude)
                    mapViewRef?.let { centerPlain(it, userLocation) }
                    try {
                        locationManager?.removeUpdates(this)
                    } catch (_: Exception) {
                    }
                    registeredListeners.remove(this)
                    try {
                        onCenterConsumed()
                    } catch (_: Exception) {
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(
                    provider: String?,
                    status: Int,
                    extras: Bundle?
                ) {
                }

                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0L,
                0f,
                listener,
                mainLooper
            )
            locationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                0L,
                0f,
                listener,
                mainLooper
            )
            registeredListeners.add(listener)
        } catch (_: SecurityException) {
            try {
                onCenterConsumed()
            } catch (_: Exception) {
            }
        }
    }

    // Rebuild the marker overlay only when the POI data actually changes, not on
    // every recomposition (search keystrokes, filter toggles, snackbars, polls).
    var lastPoisKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(focusPoint) {
        if (focusPoint != null) {
            mapViewRef?.controller?.setCenter(focusPoint)
            mapViewRef?.controller?.setZoom(19.0)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            try {
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    setHorizontalMapRepetitionEnabled(false)
                    setVerticalMapRepetitionEnabled(false)
                    minZoomLevel = 3.0
                    maxZoomLevel = 20.0
                    zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                    controller.setZoom(19.0)
                    controller.setCenter(userLocation)
                    mapViewRef = this
                }
            } catch (e: Exception) {
                TextView(ctx).apply {
                    text = "Map: ${e.message}"
                    textSize = 14f
                    setTextColor(Color.GRAY)
                    setPadding(32, 32, 32, 32)
                }
            }
        },
        update = { view ->
            if (view is MapView) {
                try {
                    val poisKey =
                        pois.joinToString(",") { "${it.id}:${it.is_verified}:${it.cleanliness}:${it.rating}" }
                    if (poisKey != lastPoisKey) {
                        lastPoisKey = poisKey
                        view.overlays.removeIf { it is PoiMarkersOverlay }
                        view.overlays.add(PoiMarkersOverlay(pois, onPoiClick))
                        view.invalidate()
                    }

                    val filter = if (isDarkMode) {
                        // Google Maps style Dark Mode Matrix
                        val matrix = floatArrayOf(
                            -0.7f, 0f, 0f, 0f, 210f, // Red: Navy/Grey base
                            0f, -0.7f, 0f, 0f, 210f, // Green: Navy/Grey base
                            0f, 0f, -0.6f, 0f, 230f, // Blue: Slightly bluer water/bg
                            0f, 0f, 0f, 1.0f, 0f
                        )
                        ColorMatrixColorFilter(matrix)
                    } else null
                    view.overlayManager.tilesOverlay.setColorFilter(filter)
                } catch (_: Exception) {
                }
            }
        }
    )
}

private fun centerPlain(mapView: MapView, point: GeoPoint) {
    mapView.post {
        try {
            mapView.controller.setCenter(point)
            mapView.controller.setZoom(19.0)
        } catch (_: Exception) {
        }
    }
}
