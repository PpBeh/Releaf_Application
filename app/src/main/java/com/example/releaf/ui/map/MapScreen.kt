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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

import com.example.releaf.ui.theme.AppStrings
import com.example.releaf.ui.theme.string
import com.example.releaf.ui.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    notificationsViewModel: NotificationsViewModel,
    onDirectionClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    currentUserId: String,
    isDarkMode: Boolean = false,
    themeViewModel: ThemeViewModel
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
    val context = androidx.compose.ui.platform.LocalContext.current
    var showAddPoiDialog by remember { mutableStateOf(false) }
    var isFetchingLocation by remember { mutableStateOf(false) }
    var currentLat by remember { mutableStateOf(3.1390) }
    var currentLng by remember { mutableStateOf(101.6869) }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showSubscriptionDialog by remember { mutableStateOf(false) }
    var isSubscribed by remember { mutableStateOf(context.getSharedPreferences("billing_prefs", android.content.Context.MODE_PRIVATE).getBoolean("isPro", false)) }
    var dailyPointsClaimed by remember { mutableStateOf(context.getSharedPreferences("billing_prefs", android.content.Context.MODE_PRIVATE).getBoolean("daily_claimed", false)) }
    var showPhotoSourcePickerForPoi by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isSubscribed = context.getSharedPreferences("billing_prefs", android.content.Context.MODE_PRIVATE).getBoolean("isPro", false)
        dailyPointsClaimed = context.getSharedPreferences("billing_prefs", android.content.Context.MODE_PRIVATE).getBoolean("daily_claimed", false)
    }

    val sheetState = rememberModalBottomSheetState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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

    val cameraPhotoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val uri = saveBitmapToUri(context, bitmap)
            viewModel.selectedPoi.value?.let { poi ->
                viewModel.uploadPhoto(poi.id, uri, context)
            }
        }
    }

    val filteredSearchPois = pois.filter {
        searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true)
    }

    LaunchedEffect(isFetchingLocation) {
        if (isFetchingLocation) {
            snackbarHostState.showSnackbar("Getting your location...")
        }
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

    // Handle POI deep links arriving via onNewIntent while MapScreen is active
    val pendingPoiId by com.example.releaf.data.remote.DeepLinkHolder.pendingPoiIdFlow.collectAsState()
    LaunchedEffect(pendingPoiId) {
        pendingPoiId?.let { poiId ->
            kotlinx.coroutines.delay(300)
            viewModel.openPoiFromDeepLink(poiId)
            // focus map on the POI after it loads
            // delay a bit to allow poi fetch, then focus
            kotlinx.coroutines.delay(400)
            viewModel.selectedPoi.value?.let { poi ->
                if (poi.id == poiId) {
                    focusPoint = org.osmdroid.util.GeoPoint(poi.latitude, poi.longitude)
                }
            }
            com.example.releaf.data.remote.DeepLinkHolder.clearPoiId()
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
                    "${AppStrings.get("found_nearest", themeViewModel.language.value)}: ${nearest.name}"
                )
            } else {
                snackbarHostState.showSnackbar(AppStrings.get("none_found", themeViewModel.language.value))
            }
        }
        if (msg is PoiActionResult.Message) {
            snackbarHostState.showSnackbar(
                when (msg.message) {
                    "created" -> AppStrings.get("poi_created", themeViewModel.language.value)
                    "create_failed" -> AppStrings.get("create_failed", themeViewModel.language.value)
                    "too_close" -> AppStrings.get("too_close", themeViewModel.language.value)
                    "photo_uploaded" -> AppStrings.get("photo_uploaded", themeViewModel.language.value)
                    "photo_failed" -> AppStrings.get("photo_failed", themeViewModel.language.value)
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
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .graphicsLayer { alpha = mapAlphaAnim }
            ) {
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
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                    .padding(top = 4.dp)
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            viewModel.onSearchQueryChanged(it)
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(string("search_placeholder", themeViewModel)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Diamond Subscription Button
                    androidx.compose.material3.Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showSubscriptionDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSubscribed) Color(0xFFFFD54F) else MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("💎", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (isSubscribed) "Pro" else "Get Pro",
                                fontWeight = FontWeight.Bold,
                                color = if (isSubscribed) Color(0xFF5D4037) else MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                if (searchResults.isNotEmpty() && searchQuery.isNotBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
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

                Box(modifier = Modifier.padding(horizontal = 12.dp)) {
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
                    .padding(start = 16.dp, bottom = padding.calculateBottomPadding() + 16.dp)
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
                    .padding(end = 16.dp, bottom = padding.calculateBottomPadding() + 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        isFetchingLocation = true
                        fetchFreshLocation(context) { lat, lng ->
                            currentLat = lat
                            currentLng = lng
                            isFetchingLocation = false
                            showAddPoiDialog = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add pin")
                }
                SmallFloatingActionButton(
                    onClick = {
                        fetchFreshLocation(context) { lat, lng ->
                            viewModel.findNearestPois(lat, lng, "TOILET")
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Wc, contentDescription = string("nearest_toilet", themeViewModel))
                }
                SmallFloatingActionButton(
                    onClick = {
                        fetchFreshLocation(context) { lat, lng ->
                            viewModel.findNearestPois(lat, lng, "TRASH_CAN")
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Delete, contentDescription = string("nearest_trash_can", themeViewModel))
                }
                SmallFloatingActionButton(
                    onClick = { centerOnLocation = !centerOnLocation },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = string("my_location", themeViewModel))
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
                onAddPhotoClick = { showPhotoSourcePickerForPoi = true },
                actionResult = actionResult
            )
        }
    }

    if (showPhotoSourcePickerForPoi) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showPhotoSourcePickerForPoi = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Upload Facility Photo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPhotoSourcePickerForPoi = false
                                detailPhotoPicker.launch("image/*")
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Choose from Gallery", fontWeight = FontWeight.Bold)
                            Text("Select an existing photo from device", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPhotoSourcePickerForPoi = false
                                cameraPhotoPicker.launch(null)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Take Photo with Camera", fontWeight = FontWeight.Bold)
                            Text("Capture a new photo right now", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = { showPhotoSourcePickerForPoi = false }) {
                        Text("Cancel")
                    }
                }
            }
        }
    }

    if (showSubscriptionDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showSubscriptionDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(Color(0xFF6A1B9A), Color(0xFFAB47BC), Color(0xFFFFD54F))
                                )
                            )
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💎", fontSize = 42.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Releaf Diamond Pro",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                "Unlock Exclusive Member Perks",
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Subscription Benefits",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SubscriptionBenefitCard(
                        icon = "🎁",
                        title = "Daily Free Points & Gems",
                        description = "Claim 100 bonus points and 5 gems every single day to level up faster."
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SubscriptionBenefitCard(
                        icon = "🎨",
                        title = "Customizable Profile Banner",
                        description = "Express your unique style with forest themes, gradients, and custom cover images."
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SubscriptionBenefitCard(
                        icon = "👑",
                        title = "Exclusive Profile Borders",
                        description = "Stand out in reviews and comments with golden avatar frames and glowing badges."
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SubscriptionBenefitCard(
                        icon = "💧",
                        title = "Unlimited Garden Care",
                        description = "Extra daily watering and fertilizing uses for all your garden plants."
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (!isSubscribed) {
                        Button(
                            onClick = {
                                isSubscribed = true
                                context.getSharedPreferences("billing_prefs", android.content.Context.MODE_PRIVATE).edit().putBoolean("isPro", true).apply()
                                scope.launch {
                                    snackbarHostState.showSnackbar("🎉 Welcome to Releaf Diamond Pro Membership!")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A))
                        ) {
                            Text("Subscribe Now • $2.99 / mo", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFE8F5E9)
                            ) {
                                Text(
                                    "Active Pro Subscription ✓",
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    if (!dailyPointsClaimed) {
                                        dailyPointsClaimed = true
                                        context.getSharedPreferences("billing_prefs", android.content.Context.MODE_PRIVATE).edit().putBoolean("daily_claimed", true).apply()
                                        scope.launch {
                                            try {
                                                val gardenRepo = com.example.releaf.data.repository.GardenRepository()
                                                val authRepo = com.example.releaf.data.repository.AuthRepository()
                                                val garden = gardenRepo.getGarden(currentUserId)
                                                if (garden != null) {
                                                    gardenRepo.updateGarden(currentUserId, com.example.releaf.data.remote.dto.GardenUpdateDto(
                                                        current_exp = garden.current_exp + 100,
                                                        exp_target = garden.exp_target,
                                                        grow_uses_left = garden.grow_uses_left,
                                                        fertilize_uses_left = garden.fertilize_uses_left,
                                                        current_points = garden.current_points + 100,
                                                        current_gems = garden.current_gems + 5
                                                    ))
                                                }
                                                val profile = authRepo.getProfile(currentUserId)
                                                if (profile != null) {
                                                    authRepo.updateTotalPoints(currentUserId, (profile.total_points ?: 0) + 100)
                                                }
                                                com.example.releaf.data.remote.SupabaseModule.triggerRefresh()
                                            } catch (_: Exception) {}
                                            snackbarHostState.showSnackbar("🎁 Claimed today's 100 Points & 5 Gems!")
                                        }
                                    }
                                },
                                enabled = !dailyPointsClaimed,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107))
                            ) {
                                Text(
                                    if (dailyPointsClaimed) "Today's Reward Claimed ✓" else "🎁 Claim Daily 100 Points & 5 Gems",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = { showSubscriptionDialog = false }) {
                        Text("Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showAddPoiDialog) {
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
private fun SubscriptionBenefitCard(
    icon: String,
    title: String,
    description: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(2.dp))
                Text(description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

private fun fetchFreshLocation(
    context: android.content.Context,
    onResult: (lat: Double, lng: Double) -> Unit
) {
    val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? android.location.LocationManager
    val mainLooper = android.os.Looper.getMainLooper()
    val now = System.currentTimeMillis()
    val freshThreshold = 2 * 60 * 1000L

    val gps = try {
        lm?.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
    } catch (_: SecurityException) { null }
    val network = try {
        lm?.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
    } catch (_: SecurityException) { null }

    val fresh = listOfNotNull(gps, network)
        .filter { now - it.time < freshThreshold }
        .maxByOrNull { it.time }

    if (fresh != null) {
        onResult(fresh.latitude, fresh.longitude)
        return
    }

    val bestKnown = listOfNotNull(gps, network).maxByOrNull { it.time }

    val listener = object : android.location.LocationListener {
        override fun onLocationChanged(location: android.location.Location) {
            onResult(location.latitude, location.longitude)
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
        lm?.requestLocationUpdates(android.location.LocationManager.GPS_PROVIDER, 0L, 0f, listener, mainLooper)
        lm?.requestLocationUpdates(android.location.LocationManager.NETWORK_PROVIDER, 0L, 0f, listener, mainLooper)
    } catch (_: SecurityException) { }

    // Fallback: if no fix arrives in 5 seconds (e.g., indoors), use best last-known location
    val handler = android.os.Handler(mainLooper)
    handler.postDelayed({
        try {
            lm?.removeUpdates(listener)
        } catch (_: Exception) { }
        if (bestKnown != null) {
            onResult(bestKnown.latitude, bestKnown.longitude)
        }
    }, 5000L)
}
