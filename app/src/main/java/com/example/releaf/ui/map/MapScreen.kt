package com.example.releaf.ui.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.releaf.model.CleanlinessStatus
import com.example.releaf.model.PoiCategory
import com.example.releaf.ui.components.MapFilterBar
import com.example.releaf.ui.viewmodel.MapViewModel
import com.example.releaf.ui.viewmodel.NotificationsViewModel
import com.example.releaf.ui.viewmodel.PoiActionResult
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.example.releaf.data.remote.dto.PoiDto
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    notificationsViewModel: NotificationsViewModel,
    onDirectionClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    currentUserId: String,
    isDarkMode: Boolean = false
) {
    val pois by viewModel.filteredPois.collectAsState()
    val selectedPoi by viewModel.selectedPoi.collectAsState()
    val photos by viewModel.photos.collectAsState()
    val actionResult by viewModel.actionResult.collectAsState()
    val reviewCount by viewModel.reviewCount.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val analyzedStatus by viewModel.analyzedStatus.collectAsState()
    val analyzedStatusTime by viewModel.analyzedStatusTime.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val enabledCategories by viewModel.enabledCategories.collectAsState()
    val enabledCleanliness by viewModel.enabledCleanliness.collectAsState()
    val excludedPaid by viewModel.excludedPaid.collectAsState()
    val showUnverified by viewModel.showUnverified.collectAsState()
    var showAddPoiDialog by remember { mutableStateOf(false) }
    var currentLat by remember { mutableStateOf(3.1390) }
    var currentLng by remember { mutableStateOf(101.6869) }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> selectedPhotoUri = uri }

    val detailPhotoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.selectedPoi.value?.let { poi ->
                viewModel.uploadPhoto(poi.id, uri, context)
            }
        }
    }

    // val cameraLauncher = rememberLauncherForActivityResult(
    //    ActivityResultContracts.TakePicturePreview()
    // ) { bitmap -> }

    val filteredSearchPois = pois.filter {
        searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true)
    }

    var focusPoint by remember { mutableStateOf<org.osmdroid.util.GeoPoint?>(null) }

    LaunchedEffect(Unit) {
        viewModel.setCurrentUserId(currentUserId)
        viewModel.loadPois()
        com.example.releaf.data.remote.DeepLinkHolder.pendingPoiId?.let { poiId ->
            kotlinx.coroutines.delay(800)
            viewModel.openPoiFromDeepLink(poiId)
            com.example.releaf.data.remote.DeepLinkHolder.clearPoiId()
        }
        while (true) {
            kotlinx.coroutines.delay(5000)
            viewModel.loadPois()
        }
    }

    LaunchedEffect(actionResult) {
        val msg = actionResult
        if (msg is PoiActionResult.NearestFound) {
            val nearest = if (msg.targetCategory == "TRASH_CAN") msg.trashCan ?: msg.toilet
                          else msg.toilet ?: msg.trashCan
            if (nearest != null) {
                viewModel.selectPoi(nearest)
                focusPoint = org.osmdroid.util.GeoPoint(nearest.latitude, nearest.longitude)
                snackbarHostState.showSnackbar(
                    "Found nearest: ${nearest.name} (${nearest.category.lowercase().replace('_', ' ')})"
                )
            } else {
                snackbarHostState.showSnackbar("No toilets or trash cans found.")
            }
        }
        if (msg is PoiActionResult.Message) {
            snackbarHostState.showSnackbar(
                when (msg.message) {
                    "created" -> "POI created!"
                    "create_failed" -> "Failed to create POI"
                    "too_close" -> "A POI already exists within 5m of this location."
                    "photo_uploaded" -> "Photo uploaded!"
                    "photo_failed" -> "Photo upload failed"
                    else -> msg.message.take(100)
                }
            )
        }
    }

    LaunchedEffect(selectedPoi?.id) {
        val poiId = selectedPoi?.id ?: return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(4000)
            viewModel.refreshPoiDetails(poiId)
        }
    }

    // var showMap by remember { mutableStateOf(true) }
    var centerOnLocation by remember { mutableStateOf(false) }
    val searchResults by viewModel.searchResults.collectAsState()

    val notifications by notificationsViewModel.notifications.collectAsState()
    val unreadCount by notificationsViewModel.unreadCount.collectAsState()
    var showNotifications by remember { mutableStateOf(false) }

    LaunchedEffect(currentUserId) {
        notificationsViewModel.loadNotifications(currentUserId)
        while (true) {
            kotlinx.coroutines.delay(10000)
            notificationsViewModel.loadNotifications(currentUserId)
        }
    }

    var mapAlpha by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        mapAlpha = 1f
    }
    val mapAlphaAnim by androidx.compose.animation.core.animateFloatAsState(
        targetValue = mapAlpha,
        animationSpec = androidx.compose.animation.core.tween(1200),
        label = "mapFadeIn"
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = mapAlphaAnim }) {
            OsmMap(
                pois = filteredSearchPois,
                onPoiClick = { viewModel.selectPoi(it) },
                centerOnLocation = centerOnLocation,
                focusPoint = focusPoint,
                isDarkMode = isDarkMode,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.onSearchQueryChanged(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search toilets and trash cans...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )
            }

            if (searchResults.isNotEmpty() && searchQuery.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column {
                        searchResults.take(5).forEach { result ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.clearSearch()
                                        searchQuery = ""
                                        viewModel.selectPoi(result)
                                        focusPoint = org.osmdroid.util.GeoPoint(result.latitude, result.longitude)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (result.category == "TOILET") Icons.Default.Wc else Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(result.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${result.cleanliness} | ${if (result.is_paid) "Paid" else "Free"} | ${if (result.is_verified) "Verified" else "Unverified"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            MapFilterBar(
                enabledCategories = enabledCategories,
                onToggleCategory = { viewModel.toggleCategory(it) },
                onResetCategories = { viewModel.resetCategories() },
                enabledCleanliness = enabledCleanliness,
                onToggleCleanliness = { viewModel.toggleCleanliness(it) },
                onResetCleanliness = { viewModel.resetCleanliness() },
                excludedPaid = excludedPaid,
                onTogglePaid = { viewModel.togglePaid(it) },
                showUnverified = showUnverified,
                onToggleUnverified = { viewModel.toggleUnverified() }
            )
        }

        BadgedBox(
            badge = {
                val unread = unreadCount
                if (unread > 0) {
                    Badge { Text("$unread") }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = { showNotifications = true },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Icon(Icons.Default.Notifications, contentDescription = "Notifications")
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FloatingActionButton(onClick = { showAddPoiDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add pin")
            }
            SmallFloatingActionButton(
                onClick = {
                    val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? android.location.LocationManager
                    try {
                        val loc = lm?.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                            ?: lm?.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                        if (loc != null) {
                            viewModel.findNearestPois(loc.latitude, loc.longitude, "TOILET")
                        } else {
                            viewModel.findNearestPois(currentLat, currentLng, "TOILET")
                        }
                    } catch (_: SecurityException) { }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.Wc, contentDescription = "Nearest Toilet")
            }
            SmallFloatingActionButton(
                onClick = {
                    val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? android.location.LocationManager
                    try {
                        val loc = lm?.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                            ?: lm?.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                        if (loc != null) {
                            viewModel.findNearestPois(loc.latitude, loc.longitude, "TRASH_CAN")
                        } else {
                            viewModel.findNearestPois(currentLat, currentLng, "TRASH_CAN")
                        }
                    } catch (_: SecurityException) { }
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Nearest Trash Can")
            }
            SmallFloatingActionButton(
                onClick = { centerOnLocation = !centerOnLocation },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "My location")
            }
        }
    }

    selectedPoi?.let { poi ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.clearSelection() },
            sheetState = sheetState
        ) {
            PoiDetailSheet(
                poi = poi,
                photos = photos,
                reviewCount = reviewCount,
                isFavorite = isFavorite,
                isProcessing = isProcessing,
                analyzedStatus = analyzedStatus,
                analyzedStatusTime = analyzedStatusTime,
                onCloseClick = { viewModel.clearSelection() },
                onDirectionClick = {
                    val uri = Uri.parse("google.navigation:q=${poi.latitude},${poi.longitude}&mode=w")
                    val navIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage("com.google.android.apps.maps")
                    }
                    try {
                        context.startActivity(navIntent)
                    } catch (_: Exception) {
                        val fallback = Uri.parse("geo:${poi.latitude},${poi.longitude}?q=${poi.latitude},${poi.longitude}(${poi.name})")
                        context.startActivity(Intent(Intent.ACTION_VIEW, fallback))
                    }
                },
                onCommentClick = { onCommentClick(poi.id) },
                onVerifyClick = {
                    val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? LocationManager
                    val loc = try {
                        lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                            ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    } catch (_: SecurityException) { null }
                    viewModel.verifyPoi(poi.id, currentUserId, loc?.latitude ?: 0.0, loc?.longitude ?: 0.0)
                },
                onReportNotExist = {
                    val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? LocationManager
                    val loc = try {
                        lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                            ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    } catch (_: SecurityException) { null }
                    viewModel.reportNotExist(poi.id, currentUserId, loc?.latitude ?: 0.0, loc?.longitude ?: 0.0)
                },
                onFavoriteClick = { viewModel.toggleFavorite(poi.id) },
                onShareClick = {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Check out ${poi.name} on Releaf! \uD83D\uDEBB Open: releaf://poi/${poi.id}"
                        )
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share POI"))
                },
                onAddPhotoClick = { detailPhotoPicker.launch("image/*") },
                actionResult = actionResult
            )
        }
    }

    if (showAddPoiDialog) {
        val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? LocationManager
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val loc = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (loc != null) {
                currentLat = loc.latitude
                currentLng = loc.longitude
            }
        }
        AddPoiDialog(
            lat = currentLat,
            lng = currentLng,
            photoUri = selectedPhotoUri,
            onDismiss = {
                showAddPoiDialog = false
                selectedPhotoUri = null
            },
            onPickPhoto = { photoPicker.launch("image/*") },
            onSubmit = { name, category, desc, isPaid ->
                viewModel.createPoi(name, category, currentLat, currentLng, isPaid, currentUserId, desc)
                showAddPoiDialog = false
                selectedPhotoUri = null
                viewModel.loadPois()
            }
        )
    }

    if (showNotifications) {
        ModalBottomSheet(
            onDismissRequest = { showNotifications = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Notifications",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { notificationsViewModel.markAllAsRead(currentUserId) }
                    ) {
                        Text("Mark all read")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (notifications.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No notifications yet", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    notifications.forEach { notification ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                                .background(
                                    if (!notification.is_read && notification.user_id != null)
                                        Color(0xFFE3F2FD) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                                .clickable {
                                    notificationsViewModel.markAsRead(notification, currentUserId)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                when (notification.type) {
                                    "ANNOUNCEMENT" -> Icons.Default.Notifications
                                    "LIKE" -> Icons.Default.ThumbUp
                                    else -> Icons.Default.Info
                                },
                                contentDescription = null,
                                tint = if (notification.type == "ANNOUNCEMENT") Color(0xFF1E88E5) else Color(0xFF43A047)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    notification.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    notification.body,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    com.example.releaf.data.remote.TimeFormatter.formatCommentTime(notification.created_at),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
}

@Composable
private fun PoiCard(poi: PoiDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(poi.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    if (!poi.is_verified) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "(Unverified)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFF9800)
                        )
                    }
                }
                Text(
                    "${poi.category} | ${poi.cleanliness} | ${if (poi.is_paid) "Paid" else "Free"}",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.padding(end = 2.dp))
                    Text(poi.rating.toString(), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPoiDialog(
    lat: Double,
    lng: Double,
    photoUri: Uri?,
    onDismiss: () -> Unit,
    onPickPhoto: () -> Unit,
    onSubmit: (name: String, category: String, description: String, isPaid: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("TOILET") }
    var description by remember { mutableStateOf("") }
    var isPaid by remember { mutableStateOf(false) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == "TOILET") "Add New Toilet" else "Add New Trash Can") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { category = "TOILET" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (category == "TOILET") Color(0xFFE53935) else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text("Toilet")
                    }
                    Button(
                        onClick = { category = "TRASH_CAN" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (category == "TRASH_CAN") Color(0xFF43A047) else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text("Trash Can")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Location (e.g. Level 1)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Paid: ", modifier = Modifier.weight(1f))
                    Button(onClick = { isPaid = !isPaid }) {
                        Text(if (isPaid) "Yes" else "Free")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "GPS: ${"%.4f".format(lat)}, ${"%.4f".format(lng)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = onPickPhoto) {
                        Text(if (photoUri != null) "Change Photo" else "Add Photo")
                    }
                    if (photoUri != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Image(
                            rememberAsyncImagePainter(model = photoUri),
                            contentDescription = "Preview",
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(name, category, description, isPaid) },
                enabled = name.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
