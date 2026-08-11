package com.example.releaf.ui.map

import android.Manifest
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
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.releaf.model.CleanlinessStatus
import com.example.releaf.model.PoiCategory
import com.example.releaf.ui.components.MapFilterBar
import com.example.releaf.ui.viewmodel.MapViewModel
import com.example.releaf.ui.viewmodel.PoiActionResult
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.example.releaf.data.remote.dto.PoiDto
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onDirectionClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    currentUserId: String
) {
    val pois by viewModel.filteredPois.collectAsState()
    val selectedPoi by viewModel.selectedPoi.collectAsState()
    val photos by viewModel.photos.collectAsState()
    val actionResult by viewModel.actionResult.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<PoiCategory?>(null) }
    var selectedCleanliness by remember { mutableStateOf<CleanlinessStatus?>(null) }
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

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap -> }

    val filteredSearchPois = pois.filter {
        searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true)
    }

    LaunchedEffect(Unit) {
        viewModel.loadPois()
    }

    LaunchedEffect(actionResult) {
        val msg = actionResult
        if (msg is PoiActionResult.Message) {
            snackbarHostState.showSnackbar(
                when (msg.message) {
                    "created" -> "POI created!"
                    "create_failed" -> "Failed to create POI"
                    else -> msg.message.take(100)
                }
            )
        }
    }

    var showMap by remember { mutableStateOf(true) }
    var centerOnLocation by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        OsmMap(
            pois = filteredSearchPois,
            onPoiClick = { viewModel.selectPoi(it) },
            centerOnLocation = centerOnLocation,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilledIconButton(onClick = { }) {
                    Icon(Icons.Default.Settings, contentDescription = "Map settings")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            MapFilterBar(
                selectedCategory = selectedCategory,
                onCategorySelected = {
                    selectedCategory = it
                    viewModel.setCategoryFilter(it?.name ?: "ALL")
                },
                selectedCleanliness = selectedCleanliness,
                onCleanlinessSelected = {
                    selectedCleanliness = it
                    viewModel.setCleanlinessFilter(it?.name ?: "ALL")
                }
            )
        }

        BadgedBox(
            badge = { Badge { Text("9") } },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = { },
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
                onCloseClick = { viewModel.clearSelection() },
                onDirectionClick = { onDirectionClick(poi.id) },
                onCommentClick = { onCommentClick(poi.id) },
                onVerifyClick = { viewModel.verifyPoi(poi.id, currentUserId) },
                onReportNotExist = { viewModel.reportNotExist(poi.id, currentUserId) },
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
        title = { Text("Add New Toilet") },
        text = {
            Column {
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
