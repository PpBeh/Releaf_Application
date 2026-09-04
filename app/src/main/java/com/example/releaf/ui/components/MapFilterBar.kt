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
import com.example.releaf.ui.theme.AppStrings
import com.example.releaf.ui.viewmodel.AppLanguage

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
    onToggleUnverified: () -> Unit,
    lang: AppLanguage = AppLanguage.ENGLISH
) {
    fun t(key: String) = AppStrings.get(key, lang)
    Column {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ToggleChip(t("all"), enabledCategories.size == 2) { onResetCategories() }
            ToggleChip(t("toilet"), "TOILET" in enabledCategories) { onToggleCategory("TOILET") }
            ToggleChip(
                t("trash_can"),
                "TRASH_CAN" in enabledCategories
            ) { onToggleCategory("TRASH_CAN") }
            ToggleChip(t("unverified"), showUnverified) { onToggleUnverified() }
        }

        Row(
            modifier = Modifier
                .padding(top = 4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ToggleChip(t("all"), enabledCleanliness.size == 3) { onResetCleanliness() }
            ToggleChip(t("clean"), "CLEAN" in enabledCleanliness) { onToggleCleanliness("CLEAN") }
            ToggleChip(
                t("average"),
                "AVERAGE" in enabledCleanliness
            ) { onToggleCleanliness("AVERAGE") }
            ToggleChip(t("dirty"), "DIRTY" in enabledCleanliness) { onToggleCleanliness("DIRTY") }
            ToggleChip(t("all"), excludedPaid == null) { onTogglePaid(null) }
            ToggleChip(t("filter_paid"), excludedPaid == false) {
                onTogglePaid(if (excludedPaid == false) null else false)
            }
            ToggleChip(t("filter_free"), excludedPaid == true) {
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

