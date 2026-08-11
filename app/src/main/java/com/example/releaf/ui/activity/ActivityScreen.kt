package com.example.releaf.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.releaf.ui.viewmodel.ActivityViewModel

@Composable
fun ActivityScreen(
    viewModel: ActivityViewModel,
    userId: String
) {
    val userQuests by viewModel.userQuests.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadQuests(userId)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text("Activity", style = MaterialTheme.typography.headlineLarge)
        }
        if (userQuests.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No quests available", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(userQuests) { userQuest ->
                    QuestCard(
                        userQuest = userQuest,
                        onActionClick = {
                            when (userQuest.status) {
                                "CLAIMABLE" -> viewModel.claimQuest(userQuest.quest_id, userId)
                                "IN_PROGRESS" -> viewModel.updateQuestProgress(
                                    userQuest.quest_id,
                                    userId,
                                    userQuest.progress_current + 1,
                                    userQuest.quest?.progress_target ?: 10,
                                    userQuest.status
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestCard(userQuest: UserQuestDto, onActionClick: () -> Unit) {
    val quest = userQuest.quest ?: return
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF6B8FD1))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(quest.title, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
            Text(quest.description, color = Color.White)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Reward:", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    Text("${quest.reward_label} x${quest.reward_count}", color = Color.White)
                }
                Spacer(modifier = Modifier.weight(1f))
                if (userQuest.status == "IN_PROGRESS") {
                    Text("${userQuest.progress_current}/${quest.progress_target}", color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Button(
                    onClick = onActionClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        when (userQuest.status) {
                            "CLAIMABLE" -> "Claim"
                            "CLAIMED" -> "Done"
                            else -> "Go"
                        }
                    )
                }
            }
        }
    }
}
