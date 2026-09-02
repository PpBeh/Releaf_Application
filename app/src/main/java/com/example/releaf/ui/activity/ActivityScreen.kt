package com.example.releaf.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.releaf.data.remote.dto.UserQuestDto
import com.example.releaf.ui.theme.string
import com.example.releaf.ui.viewmodel.ActivityViewModel
import com.example.releaf.ui.viewmodel.ThemeViewModel

@Composable
fun ActivityScreen(
    viewModel: ActivityViewModel,
    userId: String,
    themeViewModel: ThemeViewModel
) {
    val context = LocalContext.current
    val lang by themeViewModel.language.collectAsState()
    fun t(key: String) = com.example.releaf.ui.theme.AppStrings.get(key, lang)
    val userQuests by viewModel.userQuests.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val gardenViewModel: com.example.releaf.ui.viewmodel.GardenViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val currentExp by gardenViewModel.currentExp.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadQuests(userId)
        gardenViewModel.loadGarden(userId, context)
    }

    var refreshTick by remember { mutableIntStateOf(0) }
    var showRefreshed by remember { mutableStateOf(false) }
    LaunchedEffect(refreshTick) {
        if (refreshTick > 0) {
            showRefreshed = true
            kotlinx.coroutines.delay(2000)
            showRefreshed = false
        }
    }

    val treeStage = gardenViewModel.getTreeStage(currentExp)
    val nextTargetExp = when (treeStage) {
        1 -> 2000
        2 -> 5000
        else -> 10000
    }
    val expProgress = (currentExp.toFloat() / nextTargetExp.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .clickable {
                        refreshTick++
                        viewModel.loadQuests(userId)
                        gardenViewModel.loadGarden(userId, context)
                    }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(string("activity", themeViewModel), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    if (showRefreshed) {
                        Text(
                            t("refreshed_ok"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    } else if (isLoading && userQuests.isNotEmpty()) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🌟", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "$currentExp / $nextTargetExp EXP",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { expProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF4CAF50),
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
        when {
            isLoading && userQuests.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        androidx.compose.material3.CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(t("loading_quests"), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            errorMessage != null && userQuests.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            errorMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = {
                            viewModel.loadQuests(userId)
                            gardenViewModel.loadGarden(userId, context)
                        }) {
                            Text(t("retry"))
                        }
                    }
                }
            }
            userQuests.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(t("no_quests"), style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = {
                            viewModel.loadQuests(userId)
                            gardenViewModel.loadGarden(userId, context)
                        }) {
                            Text(t("refresh"))
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(userQuests) { userQuest ->
                        QuestCard(
                            userQuest = userQuest,
                            themeViewModel = themeViewModel,
                            onActionClick = {                                when (userQuest.status) {
                                    "CLAIMABLE" -> viewModel.claimQuest(userQuest.quest_id, userId)
                                    "IN_PROGRESS" -> { }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestCard(
    userQuest: UserQuestDto,
    themeViewModel: ThemeViewModel,
    onActionClick: () -> Unit
) {
    val lang by themeViewModel.language.collectAsState()
    fun t(key: String) = com.example.releaf.ui.theme.AppStrings.get(key, lang)
    val quest = userQuest.quest ?: return
    val cardColor = when (quest.difficulty) {
        "HARD" -> Color(0xFFE53935)
        "MEDIUM" -> Color(0xFFFB8C00)
        else -> Color(0xFF43A047)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    quest.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(quest.difficulty, color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(quest.description, color = Color.White, style = MaterialTheme.typography.bodyMedium)

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        string("reward", themeViewModel),
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🪙", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "+ ${quest.reward_count} " + t("points"),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "+ ${quest.reward_count} 🌟 " + t("exp_label"),
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (userQuest.status == "IN_PROGRESS") {
                        Text(
                            "${userQuest.progress_current}/${quest.progress_target}",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Button(
                        onClick = onActionClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = cardColor,
                            disabledContainerColor = Color.White.copy(alpha = 0.3f),
                            disabledContentColor = Color.White
                        ),
                        shape = RoundedCornerShape(50),
                        enabled = userQuest.status == "CLAIMABLE"
                    ) {
                        Text(
                            when (userQuest.status) {
                                "CLAIMABLE" -> t("quest_reward_claim")
                                "CLAIMED" -> t("quest_completed")
                                else -> "${userQuest.progress_current}/${userQuest.quest?.progress_target ?: 1}"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}