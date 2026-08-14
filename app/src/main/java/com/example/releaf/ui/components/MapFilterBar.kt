package com.example.releaf.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MapFilterBar(
    enabledCategories: Set<String>,
    onToggleCategory: (String) -> Unit,
    onResetCategories: () -> Unit,
    enabledCleanliness: Set<String>,
    onToggleCleanliness: (String) -> Unit,
    onResetCleanliness: () -> Unit,
    excludedPaid: Boolean?,
    onTogglePaid: (Boolean?) -> Unit,
    showUnverified: Boolean,
    onToggleUnverified: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ToggleChip("All", enabledCategories.size == 2) { onResetCategories() }
            ToggleChip("Toilet", "TOILET" in enabledCategories) { onToggleCategory("TOILET") }
            ToggleChip("Trash can", "TRASH_CAN" in enabledCategories) { onToggleCategory("TRASH_CAN") }
            ToggleChip("Unverified", showUnverified) { onToggleUnverified() }
        }

        Row(
            modifier = Modifier
                .padding(top = 4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ToggleChip("All", enabledCleanliness.size == 3) { onResetCleanliness() }
            CleanlinessToggleChip("Clean", Color(0xFF4CAF50), "CLEAN" in enabledCleanliness) {
                onToggleCleanliness("CLEAN")
            }
            CleanlinessToggleChip("Average", Color(0xFFFFC107), "AVERAGE" in enabledCleanliness) {
                onToggleCleanliness("AVERAGE")
            }
            CleanlinessToggleChip("Dirty", Color(0xFFF44336), "DIRTY" in enabledCleanliness) {
                onToggleCleanliness("DIRTY")
            }
            ToggleChip("Paid", excludedPaid != true) {
                onTogglePaid(if (excludedPaid == true) null else true)
            }
            ToggleChip("Free", excludedPaid != false) {
                onTogglePaid(if (excludedPaid == false) null else false)
            }
        }
    }
}

@Composable
private fun ToggleChip(label: String, enabled: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = enabled,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
private fun CleanlinessToggleChip(
    label: String,
    dotColor: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = enabled,
        onClick = onClick,
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (enabled) dotColor else MaterialTheme.colorScheme.outline)
            )
        },
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

