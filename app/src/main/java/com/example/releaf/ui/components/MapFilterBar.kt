package com.example.releaf.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
            ToggleChip("Clean", "CLEAN" in enabledCleanliness) { onToggleCleanliness("CLEAN") }
            ToggleChip("Average", "AVERAGE" in enabledCleanliness) { onToggleCleanliness("AVERAGE") }
            ToggleChip("Dirty", "DIRTY" in enabledCleanliness) { onToggleCleanliness("DIRTY") }
            ToggleChip("All", excludedPaid == null) { onTogglePaid(null) }
            ToggleChip("Paid", excludedPaid == false) {
                onTogglePaid(if (excludedPaid == false) null else false)
            }
            ToggleChip("Free", excludedPaid == true) {
                onTogglePaid(if (excludedPaid == true) null else true)
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

