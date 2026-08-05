package com.example.releaf.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.releaf.model.CleanlinessStatus
import com.example.releaf.model.PoiCategory

@Composable
fun MapFilterBar(
    selectedCategory: PoiCategory?,
    onCategorySelected: (PoiCategory?) -> Unit,
    selectedCleanliness: CleanlinessStatus?,
    onCleanlinessSelected: (CleanlinessStatus?) -> Unit
) {
    Column {
        // Which type of pin to show
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("All") }
            )
            FilterChip(
                selected = selectedCategory == PoiCategory.TOILET,
                onClick = { onCategorySelected(PoiCategory.TOILET) },
                label = { Text("Toilet") }
            )
            FilterChip(
                selected = selectedCategory == PoiCategory.TRASH_CAN,
                onClick = { onCategorySelected(PoiCategory.TRASH_CAN) },
                label = { Text("Trash can") }
            )
        }

        // Cleanliness rating filter — independent of category, so both can be picked together
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCleanliness == null,
                onClick = { onCleanlinessSelected(null) },
                label = { Text("All") }
            )
            CleanlinessChip("Clean", Color(0xFF4CAF50), selectedCleanliness == CleanlinessStatus.CLEAN) {
                onCleanlinessSelected(CleanlinessStatus.CLEAN)
            }
            CleanlinessChip("Average", Color(0xFFFFC107), selectedCleanliness == CleanlinessStatus.AVERAGE) {
                onCleanlinessSelected(CleanlinessStatus.AVERAGE)
            }
            CleanlinessChip("Dirty", Color(0xFFF44336), selectedCleanliness == CleanlinessStatus.DIRTY) {
                onCleanlinessSelected(CleanlinessStatus.DIRTY)
            }
        }
    }
}

@Composable
private fun CleanlinessChip(
    label: String,
    dotColor: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        },
        label = { Text(label) }
    )
}