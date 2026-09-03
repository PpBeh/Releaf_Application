package com.example.releaf.ui.map

import android.content.Intent
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.rememberAsyncImagePainter
import com.example.releaf.ui.components.MapFilterBar
import com.example.releaf.ui.theme.AppStrings
import com.example.releaf.ui.theme.string
import com.example.releaf.ui.viewmodel.MapViewModel
import com.example.releaf.ui.viewmodel.NotificationsViewModel
import com.example.releaf.ui.viewmodel.PoiActionResult
import com.example.releaf.ui.viewmodel.ThemeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import androidx.core.net.toUri
import androidx.core.content.edit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    notificationsViewModel: NotificationsViewModel,
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
    val lang by themeViewModel.language.collectAsState()
    fun t(key: String) = AppStrings.get(key, lang)
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showSubscriptionDialog by remember { mutableStateOf(false) }
    val billingPrefs =
        context.getSharedPreferences("billing_prefs", android.content.Context.MODE_PRIVATE)
    var isSubscribed by remember { mutableStateOf(false) }
    var isSubscribing by remember { mutableStateOf(false) }
    var dailyPointsClaimed by remember {
        mutableStateOf(
            isDailyRewardClaimedToday(
                billingPrefs,
                currentUserId
            )
        )
    }
    var isClaimingDaily by remember { mutableStateOf(false) }
    var showPhotoSourcePickerForPoi by remember { mutableStateOf(false) }
    var showAddPhotoSourcePicker by remember { mutableStateOf(false) }
    var showEnableLocationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentUserId) {
        dailyPointsClaimed = isDailyRewardClaimedToday(billingPrefs, currentUserId)
        // Pro status lives on the account (server), not on this device.
        if (currentUserId.isNotBlank()) {
            isSubscribed = try {
                com.example.releaf.data.repository.AuthRepository()
                    .getProfile(currentUserId)?.is_pro == true
            } catch (_: Exception) {
                false
            }
        } else {
            isSubscribed = false
        }
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

    var tempCameraUriForPoi by remember { mutableStateOf<Uri?>(null) }
    val cameraPhotoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUriForPoi != null) {
            val uri = tempCameraUriForPoi!!
            viewModel.selectedPoi.value?.let { poi ->
                viewModel.uploadPhoto(poi.id, uri, context)
            }
        }
    }

    var tempCameraUriForAddPoi by remember { mutableStateOf<Uri?>(null) }
    val addPoiCameraPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUriForAddPoi != null) {
            selectedPhotoUri = tempCameraUriForAddPoi
        }
    }

    val filteredSearchPois = pois.filter {
        searchQuery.isBlank() || it.name.contains(
            searchQuery,
            ignoreCase = true
        ) || it.description.contains(searchQuery, ignoreCase = true)
    }

    LaunchedEffect(isFetchingLocation) {
        if (isFetchingLocation) {
            snackbarHostState.showSnackbar("Getting your location...")
        }
    }

    var focusPoint by remember { mutableStateOf<org.osmdroid.util.GeoPoint?>(null) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(Unit, lifecycleOwner) {
        viewModel.setCurrentUserId(currentUserId)
        viewModel.loadPois()
        lifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            while (true) {
                delay(5000.milliseconds)
                viewModel.loadPois()
            }
        }
    }

    // Handle POI deep links arriving via onNewIntent while MapScreen is active
    val pendingPoiId by com.example.releaf.data.remote.DeepLinkHolder.pendingPoiIdFlow.collectAsState()
    LaunchedEffect(pendingPoiId) {
        val poiId = pendingPoiId ?: return@LaunchedEffect
        delay(300.milliseconds)
        viewModel.openPoiFromDeepLink(poiId)
        // focus map on the POI after it loads
        // delay a bit to allow poi fetch, then focus
        delay(400.milliseconds)
        viewModel.selectedPoi.value?.let { poi ->
            if (poi.id == poiId) {
                focusPoint = org.osmdroid.util.GeoPoint(poi.latitude, poi.longitude)
            }
        }
        com.example.releaf.data.remote.DeepLinkHolder.clearPoiId()
    }

    LaunchedEffect(actionResult) {
        val msg = actionResult ?: return@LaunchedEffect
        if (msg is PoiActionResult.NearestFound) {
            val nearest = if (msg.targetCategory == "TRASH_CAN") msg.trashCan ?: msg.toilet
            else msg.toilet ?: msg.trashCan
            if (nearest != null) {
                viewModel.selectPoi(nearest)
                focusPoint = org.osmdroid.util.GeoPoint(nearest.latitude, nearest.longitude)
                snackbarHostState.showSnackbar(
                    "${
                        AppStrings.get(
                            "found_nearest",
                            themeViewModel.language.value
                        )
                    }: ${nearest.name}"
                )
            } else {
                snackbarHostState.showSnackbar(
                    AppStrings.get(
                        "none_found",
                        themeViewModel.language.value
                    )
                )
            }
            viewModel.clearActionResult()
        }
        if (msg is PoiActionResult.Message) {
            val sheetInlineKeys = setOf(
                "already_verified", "now_verified", "verification_counted", "verify_failed",
                "already_reported", "now_unverified", "report_counted", "report_failed"
            )
            val handledBySheet =
                msg.message.startsWith("too_far_") || msg.message in sheetInlineKeys
            val snackbarText = when {
                msg.message == "created" -> AppStrings.get(
                    "poi_created",
                    themeViewModel.language.value
                )

                msg.message == "create_failed" -> AppStrings.get(
                    "create_failed",
                    themeViewModel.language.value
                )

                msg.message == "too_close" -> AppStrings.get(
                    "too_close",
                    themeViewModel.language.value
                )

                msg.message == "photo_uploaded" -> AppStrings.get(
                    "photo_uploaded",
                    themeViewModel.language.value
                )

                msg.message == "photo_failed" -> AppStrings.get(
                    "photo_failed",
                    themeViewModel.language.value
                )

                msg.message == "removed" -> AppStrings.get(
                    "poi_removed",
                    themeViewModel.language.value
                )

                handledBySheet -> null
                else -> msg.message.take(100)
            }
            if (snackbarText != null) {
                snackbarHostState.showSnackbar(snackbarText)
            }
            if (!handledBySheet || msg.message == "removed") {
                viewModel.clearActionResult()
            }
        }
    }

    LaunchedEffect(selectedPoi?.id, lifecycleOwner) {
        val poiId = selectedPoi?.id ?: return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            while (true) {
                delay(4000.milliseconds)
                viewModel.refreshPoiDetails(poiId)
            }
        }
    }

    var centerOnLocation by remember { mutableStateOf(false) }
    val searchResults by viewModel.searchResults.collectAsState()

    val notifications by notificationsViewModel.notifications.collectAsState()
    val unreadCount by notificationsViewModel.unreadCount.collectAsState()
    var showNotifications by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var isFirstLoad by remember { mutableStateOf(true) }
    var notifiedIds by remember { mutableStateOf(setOf<String>()) }
    var lastPostedUnreadCount by remember { mutableStateOf(0) }

    LaunchedEffect(notifications) {
        val settingsPrefs =
            context.getSharedPreferences("settings_prefs", android.content.Context.MODE_PRIVATE)
        val phoneAllowed = settingsPrefs.getBoolean("notifications_enabled", true)

        val unreadLikeIds = notifications
            .filter { !it.is_read && it.type == "LIKE" }
            .map { it.id }
            .toSet()

        if (isFirstLoad) {
            // Baseline on cold start: never buzz for rows that already exist.
            notifiedIds = notifications.map { it.id }.toSet()
            isFirstLoad = false
            lastPostedUnreadCount = 0
            if (unreadLikeIds.isEmpty()) {
                com.example.releaf.utils.NotificationHelper.cancelSummary(context)
            }
            return@LaunchedEffect
        }

        val freshIds = unreadLikeIds - notifiedIds
        // Remember everything currently visible, buzzing or not, so re-enabling
        // the setting later can never burst old rows.
        notifiedIds = notifiedIds + unreadLikeIds

        if (!phoneAllowed) {
            // Phone buzz is off: absorb silently and clear any live summary.
            lastPostedUnreadCount = 0
            com.example.releaf.utils.NotificationHelper.cancelSummary(context)
            return@LaunchedEffect
        }

        if (unreadLikeIds.isEmpty()) {
            if (lastPostedUnreadCount > 0) {
                com.example.releaf.utils.NotificationHelper.cancelSummary(context)
                lastPostedUnreadCount = 0
            }
            return@LaunchedEffect
        }

        val count = unreadLikeIds.size
        val text = if (count == 1) {
            t("notif_summary_one")
        } else {
            String.format(java.util.Locale.US, t("notif_summary_many"), count)
        }
        if (freshIds.isNotEmpty()) {
            // Genuinely new like(s): buzz once, text updated in place.
            com.example.releaf.utils.NotificationHelper.showOrUpdateSummary(
                context = context,
                title = t("app_name"),
                message = text,
                alert = true
            )
        } else if (count != lastPostedUnreadCount) {
            // Count changed with no new arrivals (e.g. read in-app):
            // refresh the text silently.
            com.example.releaf.utils.NotificationHelper.showOrUpdateSummary(
                context = context,
                title = t("app_name"),
                message = text,
                alert = false
            )
        }
        lastPostedUnreadCount = count
    }

    LaunchedEffect(currentUserId, lifecycleOwner) {
        val settingsPrefs =
            context.getSharedPreferences("settings_prefs", android.content.Context.MODE_PRIVATE)
        var phoneWasAllowed = settingsPrefs.getBoolean("notifications_enabled", true)
        // The in-app list always loads; the setting only gates phone buzzing.
        notificationsViewModel.loadNotifications(currentUserId)
        lifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            while (true) {
                delay(10000.milliseconds)
                notificationsViewModel.loadNotifications(currentUserId)
                val phoneAllowed = settingsPrefs.getBoolean("notifications_enabled", true)
                if (phoneWasAllowed && !phoneAllowed) {
                    // Just muted: dismiss any live summary immediately.
                    com.example.releaf.utils.NotificationHelper.cancelSummary(context)
                }
                phoneWasAllowed = phoneAllowed
            }
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding())
                    .graphicsLayer { alpha = mapAlphaAnim }
            ) {
                OsmMap(
                    pois = filteredSearchPois,
                    onPoiClick = { viewModel.selectPoi(it) },
                    centerOnLocation = centerOnLocation,
                    onCenterConsumed = { centerOnLocation = false },
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
                    Surface(
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
                                if (isSubscribed) t("pro") else t("get_pro"),
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
                                            focusPoint = org.osmdroid.util.GeoPoint(
                                                result.latitude,
                                                result.longitude
                                            )
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
                                        Text(
                                            result.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
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
                        onToggleUnverified = { viewModel.toggleUnverified() },
                        lang = lang
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
                        if (!isLocationEnabled(context)) {
                            showEnableLocationDialog = true
                            return@FloatingActionButton
                        }
                        isFetchingLocation = true
                        fetchFreshLocation(context) { lat, lng ->
                            isFetchingLocation = false
                            if (lat == 0.0 && lng == 0.0) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Couldn't determine your location. Please enable GPS/location and try again.")
                                }
                            } else {
                                currentLat = lat
                                currentLng = lng
                                showAddPoiDialog = true
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add pin")
                }
                SmallFloatingActionButton(
                    onClick = {
                        if (!isLocationEnabled(context)) {
                            showEnableLocationDialog = true
                            return@SmallFloatingActionButton
                        }
                        fetchFreshLocation(context) { lat, lng ->
                            viewModel.findNearestPois(lat, lng, "TOILET")
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        Icons.Default.Wc,
                        contentDescription = string("nearest_toilet", themeViewModel)
                    )
                }
                SmallFloatingActionButton(
                    onClick = {
                        if (!isLocationEnabled(context)) {
                            showEnableLocationDialog = true
                            return@SmallFloatingActionButton
                        }
                        fetchFreshLocation(context) { lat, lng ->
                            viewModel.findNearestPois(lat, lng, "TRASH_CAN")
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = string("nearest_trash_can", themeViewModel)
                    )
                }
                SmallFloatingActionButton(
                    onClick = { centerOnLocation = true },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = string("my_location", themeViewModel)
                    )
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
                        val navUri =
                            "google.navigation:q=${poi.latitude},${poi.longitude}&mode=w".toUri()
                        val navIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
                            setPackage("com.google.android.apps.maps")
                        }
                        try {
                            context.startActivity(navIntent)
                        } catch (_: Exception) {
                            val geoUri =
                                "geo:${poi.latitude},${poi.longitude}?q=${poi.latitude},${poi.longitude}(${poi.name})".toUri()
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, geoUri))
                            } catch (_: Exception) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("No map application is available on this device.")
                                }
                            }
                        }
                    },
                    onCommentClick = { onCommentClick(poi.id) },
                    onVerifyClick = {
                        if (isLocationEnabled(context)) {
                            val lm =
                                context.getSystemService(android.content.Context.LOCATION_SERVICE) as? LocationManager
                            val loc = try {
                                lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                    ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                            } catch (_: SecurityException) {
                                null
                            }
                            if (loc != null) {
                                viewModel.verifyPoi(
                                    poi.id,
                                    currentUserId,
                                    loc.latitude,
                                    loc.longitude
                                )
                            } else {
                                fetchFreshLocation(context) { lat, lng ->
                                    viewModel.verifyPoi(poi.id, currentUserId, lat, lng)
                                }
                            }
                        } else {
                            showEnableLocationDialog = true
                        }
                    },
                    onReportNotExist = {
                        if (isLocationEnabled(context)) {
                            val lm =
                                context.getSystemService(android.content.Context.LOCATION_SERVICE) as? LocationManager
                            val loc = try {
                                lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                    ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                            } catch (_: SecurityException) {
                                null
                            }
                            if (loc != null) {
                                viewModel.reportNotExist(
                                    poi.id,
                                    currentUserId,
                                    loc.latitude,
                                    loc.longitude
                                )
                            } else {
                                fetchFreshLocation(context) { lat, lng ->
                                    viewModel.reportNotExist(poi.id, currentUserId, lat, lng)
                                }
                            }
                        } else {
                            showEnableLocationDialog = true
                        }
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
                    actionResult = actionResult,
                    themeViewModel = themeViewModel
                )
            }
        }

        if (showPhotoSourcePickerForPoi) {
            PhotoSourcePickerDialog(
                title = t("upload_facility_photo"),
                onDismiss = { showPhotoSourcePickerForPoi = false },
                onGalleryClick = { detailPhotoPicker.launch("image/*") },
                onCameraClick = {
                    try {
                        val newUri = createCameraImageUri(context)
                        tempCameraUriForPoi = newUri
                        cameraPhotoPicker.launch(newUri)
                    } catch (_: Exception) {
                        scope.launch { snackbarHostState.showSnackbar(t("camera_unavailable")) }
                    }
                },
                lang = lang
            )
        }

        if (showSubscriptionDialog) {
            androidx.compose.ui.window.Dialog(onDismissRequest = {
                showSubscriptionDialog = false
            }) {
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
                                        listOf(
                                            Color(0xFF6A1B9A),
                                            Color(0xFFAB47BC),
                                            Color(0xFFFFD54F)
                                        )
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
                                    if (isSubscribing) return@Button
                                    isSubscribing = true
                                    scope.launch {
                                        try {
                                            // Mock subscription: flags THIS account as Pro on the server,
                                            // so it follows the account across devices/accounts.
                                            com.example.releaf.data.repository.AuthRepository()
                                                .setProStatus(currentUserId, true)
                                            isSubscribed = true
                                            snackbarHostState.showSnackbar("🎉 Welcome to Releaf Diamond Pro Membership!")
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("Could not activate Pro. Check your connection and try again.")
                                        } finally {
                                            isSubscribing = false
                                        }
                                    }
                                },
                                enabled = !isSubscribing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFF6A1B9A
                                    )
                                )
                            ) {
                                Text(
                                    t("subscribe_btn"),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
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
                                        modifier = Modifier.padding(
                                            horizontal = 16.dp,
                                            vertical = 8.dp
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        if (!dailyPointsClaimed && !isClaimingDaily) {
                                            isClaimingDaily = true
                                            scope.launch {
                                                try {
                                                    val gardenRepo =
                                                        com.example.releaf.data.repository.GardenRepository()
                                                    val authRepo =
                                                        com.example.releaf.data.repository.AuthRepository()
                                                    val garden = gardenRepo.getGarden(currentUserId)
                                                    if (garden != null) {
                                                        gardenRepo.updateGarden(
                                                            currentUserId,
                                                            com.example.releaf.data.remote.dto.GardenUpdateDto(
                                                                current_exp = garden.current_exp + 100,
                                                                exp_target = garden.exp_target,
                                                                grow_uses_left = garden.grow_uses_left,
                                                                fertilize_uses_left = garden.fertilize_uses_left,
                                                                current_points = garden.current_points + 100,
                                                                current_gems = garden.current_gems + 5
                                                            )
                                                        )
                                                    }
                                                    val profile = authRepo.getProfile(currentUserId)
                                                    if (profile != null) {
                                                        authRepo.updateTotalPoints(
                                                            currentUserId,
                                                            (profile.total_points ?: 0) + 100
                                                        )
                                                    }
                                                    markDailyRewardClaimed(
                                                        billingPrefs,
                                                        currentUserId
                                                    )
                                                    dailyPointsClaimed = true
                                                    com.example.releaf.data.remote.SupabaseModule.triggerRefresh()
                                                    snackbarHostState.showSnackbar("🎁 Claimed today's 100 Points & 5 Gems!")
                                                } catch (e: Exception) {
                                                    snackbarHostState.showSnackbar("Could not claim today's reward. Check your connection and try again.")
                                                } finally {
                                                    isClaimingDaily = false
                                                }
                                            }
                                        }
                                    },
                                    enabled = !dailyPointsClaimed && !isClaimingDaily,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(
                                            0xFFFFC107
                                        )
                                    )
                                ) {
                                    Text(
                                        when {
                                            isClaimingDaily -> "Claiming..."
                                            dailyPointsClaimed -> "Today's Reward Claimed ✓"
                                            else -> "🎁 Claim Daily +100 🪙 Points & +5 💎 Gems"
                                        },
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
                lang = lang,
                onDismiss = {
                    showAddPoiDialog = false
                    selectedPhotoUri = null
                },
                onPickPhoto = { showAddPhotoSourcePicker = true },
                onSubmit = { name, category, desc, isPaid ->
                    viewModel.createPoi(
                        name,
                        category,
                        currentLat,
                        currentLng,
                        isPaid,
                        currentUserId,
                        desc,
                        selectedPhotoUri,
                        context
                    )
                    showAddPoiDialog = false
                    selectedPhotoUri = null
                    viewModel.loadPois()
                }
            )
        }

        if (showAddPhotoSourcePicker) {
            PhotoSourcePickerDialog(
                title = t("add_new_toilet"),
                onDismiss = { showAddPhotoSourcePicker = false },
                onGalleryClick = { photoPicker.launch("image/*") },
                onCameraClick = {
                    try {
                        val newUri = createCameraImageUri(context)
                        tempCameraUriForAddPoi = newUri
                        addPoiCameraPicker.launch(newUri)
                    } catch (_: Exception) {
                        scope.launch { snackbarHostState.showSnackbar(t("camera_unavailable")) }
                    }
                },
                lang = lang
            )
        }

        if (showEnableLocationDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showEnableLocationDialog = false },
                title = { Text(t("location_off_title")) },
                text = { Text(t("location_off_text")) },
                confirmButton = {
                    TextButton(onClick = {
                        showEnableLocationDialog = false
                        try {
                            context.startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        } catch (_: Exception) {
                        }
                    }) {
                        Text(t("open_location_settings"))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEnableLocationDialog = false }) {
                        Text(t("cancel"))
                    }
                }
            )
        }

        if (showNotifications) {
            ModalBottomSheet(
                onDismissRequest = { showNotifications = false },
                // Open fully expanded: the whole notification list is visible and
                // scrollable; drag the sheet down to close it.
                sheetState = rememberModalBottomSheetState(
                    skipPartiallyExpanded = true,
                    confirmValueChange = { true }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            t("notifications"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { notificationsViewModel.markAllAsRead(currentUserId) }
                        ) {
                            Text(t("mark_all_read"))
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
                            Text(t("no_notifications"), style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(notifications, key = { it.id }) { notification ->
                                val isRead = notification.is_read
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp)
                                        .background(
                                            if (!isRead && notification.user_id != null)
                                                Color(0xFFE3F2FD) else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(12.dp)
                                        .clickable {
                                            notificationsViewModel.markAsRead(
                                                notification,
                                                currentUserId
                                            )
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
                                        tint = if (isRead) {
                                            Color(0xFFB0BEC5)
                                        } else if (notification.type == "ANNOUNCEMENT") {
                                            Color(0xFF1E88E5)
                                        } else {
                                            Color(0xFF43A047)
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            notification.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isRead) FontWeight.Normal else FontWeight.Bold,
                                            color = if (isRead) Color(0xFF9E9E9E) else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            notification.body,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isRead) Color(0xFFBDBDBD) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            com.example.releaf.data.remote.TimeFormatter.formatCommentTime(
                                                notification.created_at
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isRead) Color(0xFFBDBDBD) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
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
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PhotoSourcePickerDialog(
    title: String,
    onDismiss: () -> Unit,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
    lang: com.example.releaf.ui.viewmodel.AppLanguage = com.example.releaf.ui.viewmodel.AppLanguage.ENGLISH
) {
    fun t(key: String) = AppStrings.get(key, lang)
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onGalleryClick()
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(t("choose_gallery"), fontWeight = FontWeight.Bold)
                        Text(
                            t("gallery_hint"),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onCameraClick()
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(t("take_camera"), fontWeight = FontWeight.Bold)
                        Text(
                            t("camera_hint"),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onDismiss) {
                    Text(t("cancel"))
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
    lang: com.example.releaf.ui.viewmodel.AppLanguage,
    onDismiss: () -> Unit,
    onPickPhoto: () -> Unit,
    onSubmit: (name: String, category: String, description: String, isPaid: Boolean) -> Unit
) {
    fun tt(key: String) = AppStrings.get(key, lang)
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("TOILET") }
    var description by remember { mutableStateOf("") }
    var isPaid by remember { mutableStateOf(false) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == "TOILET") tt("add_new_toilet") else tt("add_new_trash")) },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { category = "TOILET" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (category == "TOILET") Color(0xFFE53935) else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(tt("toilet"))
                    }
                    Button(
                        onClick = { category = "TRASH_CAN" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (category == "TRASH_CAN") Color(0xFF43A047) else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(tt("trash_can"))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(tt("name_required_ast")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(tt("location_label")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tt("filter_paid") + ": ", modifier = Modifier.weight(1f))
                    Button(onClick = { isPaid = !isPaid }) {
                        Text(if (isPaid) tt("filter_paid") else tt("filter_free"))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "GPS: ${
                        String.format(
                            java.util.Locale.US,
                            "%.4f",
                            lat
                        )
                    }, ${String.format(java.util.Locale.US, "%.4f", lng)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = onPickPhoto) {
                        Text(if (photoUri != null) tt("change_photo") else tt("add_photo"))
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
            ) { Text(tt("add")) }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text(tt("cancel")) }
        }
    )
}

private fun isLocationEnabled(context: android.content.Context): Boolean {
    val lm =
        context.getSystemService(android.content.Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
    return try {
        lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    } catch (_: Exception) {
        false
    }
}

private fun fetchFreshLocation(
    context: android.content.Context,
    onResult: (lat: Double, lng: Double) -> Unit
) {
    val lm =
        context.getSystemService(android.content.Context.LOCATION_SERVICE) as? LocationManager
    val mainLooper = android.os.Looper.getMainLooper()
    val now = System.currentTimeMillis()
    val freshThreshold = 2 * 60 * 1000L

    val gps = try {
        lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
    } catch (_: SecurityException) {
        null
    }
    val network = try {
        lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    } catch (_: SecurityException) {
        null
    }

    val fresh = listOfNotNull(gps, network)
        .filter { now - it.time < freshThreshold }
        .maxByOrNull { it.time }

    if (fresh != null) {
        onResult(fresh.latitude, fresh.longitude)
        return
    }

    val bestKnown = listOfNotNull(gps, network).maxByOrNull { it.time }

    var delivered = false
    fun deliver(lat: Double, lng: Double) {
        if (delivered) return
        delivered = true
        onResult(lat, lng)
    }

    val listener = object : android.location.LocationListener {
        override fun onLocationChanged(location: android.location.Location) {
            deliver(location.latitude, location.longitude)
            try {
                lm?.removeUpdates(this)
            } catch (_: Exception) {
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {
        }

        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    val handler = android.os.Handler(mainLooper)
    val timeout = Runnable {
        try {
            lm?.removeUpdates(listener)
        } catch (_: Exception) {
        }
        // No fresh fix: fall back to the best last-known position, or (0,0) if none exists.
        deliver(bestKnown?.latitude ?: 0.0, bestKnown?.longitude ?: 0.0)
    }

    var providerRequested = false
    try {
        lm?.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            0L,
            0f,
            listener,
            mainLooper
        )
        lm?.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
            0L,
            0f,
            listener,
            mainLooper
        )
        providerRequested = true
    } catch (_: SecurityException) {
    }

    // Fallback: if no fix arrives in 5 seconds (e.g., indoors), use best last-known location
    if (providerRequested) {
        handler.postDelayed(timeout, 5000L)
    } else {
        handler.post(timeout)
    }
}

private fun dailyDateKey(): String {
    val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    fmt.timeZone = java.util.TimeZone.getTimeZone("Asia/Kuala_Lumpur")
    return fmt.format(java.util.Date())
}

private fun isDailyRewardClaimedToday(
    prefs: android.content.SharedPreferences,
    userId: String
): Boolean {
    // Per-account so switching accounts on the same device starts a fresh claim.
    return prefs.getString("daily_claimed_date_$userId", null) == dailyDateKey()
}

private fun markDailyRewardClaimed(prefs: android.content.SharedPreferences, userId: String) {
    prefs.edit {
        putBoolean("daily_claimed_$userId", true)
            .putString("daily_claimed_date_$userId", dailyDateKey())
    }
}
