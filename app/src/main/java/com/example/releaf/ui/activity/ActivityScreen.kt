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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val userQuests by viewModel.userQuests.collectAsState()
    val gardenViewModel: com.example.releaf.ui.viewmodel.GardenViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val garden by gardenViewModel.garden.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadQuests(userId)
        gardenViewModel.loadGarden(userId)
    }

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
                    .weight(1f)
                    .clickable { 
                        viewModel.loadQuests(userId)
                        gardenViewModel.loadGarden(userId)
                    }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(string("activity", themeViewModel), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(16.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Live currency display in Activity tab
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("\uD83E\uDE99", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.width(4.dp))
                Text("${garden?.current_points ?: 0}", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("\uD83D\uDC8E", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.width(4.dp))
                Text("${garden?.current_gems ?: 0}", fontWeight = FontWeight.Bold)
            }
        }
        if (userQuests.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material3.CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading daily quests...", style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(userQuests) { userQuest ->
                    QuestCard(
                        userQuest = userQuest,
                        themeViewModel = themeViewModel,
                        onActionClick = {
                            when (userQuest.status) {
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

@Composable
private fun QuestCard(
    userQuest: UserQuestDto, 
    themeViewModel: ThemeViewModel,
    onActionClick: () -> Unit
) {
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
                        if (quest.reward_label == "Gems") {
                            Text("\uD83D\uDC8E", style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        val rewardLabel = if (quest.reward_label == "Gems") string("gems", themeViewModel) 
                                          else string("points", themeViewModel)
                        Text("${quest.reward_count} $rewardLabel", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    }
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
                                "CLAIMABLE" -> "CLAIM REWARD"
                                "CLAIMED" -> "COMPLETED"
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
