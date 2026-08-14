package com.example.releaf.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.location.LocationManager
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
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
    focusPoint: GeoPoint? = null,
    isDarkMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var locationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var userLocation by remember { mutableStateOf(GeoPoint(3.1390, 101.6869)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions.values.all { it }
    }

    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            hasLocationPermission = true
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission && mapViewRef != null) {
            val map = mapViewRef ?: return@LaunchedEffect
            val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? LocationManager
            try {
                val loc = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (loc != null) {
                    userLocation = GeoPoint(loc.latitude, loc.longitude)
                    map.controller.animateTo(userLocation)
                }
            } catch (_: SecurityException) { }
            try {
                val overlay = MyLocationNewOverlay(GpsMyLocationProvider(context), map)
                overlay.enableMyLocation()
                map.overlays.add(overlay)
                locationOverlay = overlay
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(centerOnLocation) {
        if (centerOnLocation && hasLocationPermission) {
            val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? LocationManager
            val mainLooper = android.os.Looper.getMainLooper()

            val listener = object : android.location.LocationListener {
                override fun onLocationChanged(location: android.location.Location) {
                    userLocation = GeoPoint(location.latitude, location.longitude)
                    mapViewRef?.controller?.setCenter(userLocation)
                    mapViewRef?.controller?.setZoom(16.0)
                    try {
                        lm?.removeUpdates(this)
                    } catch (_: Exception) { }
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            try {
                val now = System.currentTimeMillis()
                val freshThreshold = 2 * 60 * 1000L

                val gps = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                val network = lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                val fresh = listOfNotNull(gps, network)
                    .filter { now - it.time < freshThreshold }
                    .maxByOrNull { it.time }

                if (fresh != null) {
                    userLocation = GeoPoint(fresh.latitude, fresh.longitude)
                    mapViewRef?.controller?.setCenter(userLocation)
                    mapViewRef?.controller?.setZoom(16.0)
                }

                lm?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, listener, mainLooper)
                lm?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, listener, mainLooper)
            } catch (_: SecurityException) { }
        }
    }

    LaunchedEffect(focusPoint) {
        if (focusPoint != null) {
            mapViewRef?.controller?.setCenter(focusPoint)
            mapViewRef?.controller?.setZoom(17.0)
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
                    controller.setZoom(16.0)
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
                    view.overlays.removeIf { it is Marker || it is PoiMarkersOverlay }
                    view.overlays.add(PoiMarkersOverlay(pois, onPoiClick))

                    val filter = if (isDarkMode) {
                        val inverseMatrix = floatArrayOf(
                            -1.0f, 0.0f, 0.0f, 0.0f, 255.0f,
                            0.0f, -1.0f, 0.0f, 0.0f, 255.0f,
                            0.0f, 0.0f, -1.0f, 0.0f, 255.0f,
                            0.0f, 0.0f, 0.0f, 1.0f, 0.0f
                        )
                        android.graphics.ColorMatrixColorFilter(inverseMatrix)
                    } else null
                    view.overlayManager.tilesOverlay.setColorFilter(filter)

                    view.invalidate()
                } catch (_: Exception) { }
            }
        }
    )
}

private fun createMarkerIcon(category: String, verified: Boolean): android.graphics.drawable.Drawable {
    val size = 96
    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val pinColor = if (category == "TOILET") Color.parseColor("#E53935") else Color.parseColor("#43A047")

    val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = pinColor; style = Paint.Style.FILL }
    canvas.drawCircle(size / 2f, size / 2f - 4f, size / 2.5f, pinPaint)

    val triangle = android.graphics.Path().apply {
        moveTo(size / 2f - 10f, size / 2f + 8f)
        lineTo(size / 2f + 10f, size / 2f + 8f)
        lineTo(size / 2f, size / 2f + 22f)
        close()
    }
    val triPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = pinColor; style = Paint.Style.FILL }
    canvas.drawPath(triangle, triPaint)

    if (category == "TOILET") {
        val figurePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 2.5f
        }
        canvas.drawCircle(size / 2f - 7f, size / 2f - 12f, 6f, figurePaint)
        canvas.drawCircle(size / 2f + 7f, size / 2f - 12f, 6f, figurePaint)
        canvas.drawLine(size / 2f - 7f, size / 2f - 6f, size / 2f + 7f, size / 2f - 6f, figurePaint)
        canvas.drawLine(size / 2f, size / 2f - 18f, size / 2f, size / 2f + 2f, figurePaint)
    } else {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE; textSize = 24f; textAlign = Paint.Align.CENTER; isFakeBoldText = true
        }
        canvas.drawText("TC", size / 2f, size / 2f + 2f, textPaint)
    }

    if (!verified) {
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.parseColor("#FF9800"); style = Paint.Style.FILL
        }
        canvas.drawCircle(size - 14f, 14f, 12f, badgePaint)
        val exPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE; textSize = 18f; textAlign = Paint.Align.CENTER; isFakeBoldText = true
        }
        canvas.drawText("!", size - 14f, 20f, exPaint)
    }

    return android.graphics.drawable.BitmapDrawable(null, bitmap)
}
