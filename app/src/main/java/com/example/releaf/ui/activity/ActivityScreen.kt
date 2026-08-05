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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.releaf.model.Quest
import com.example.releaf.model.QuestStatus

@Composable
fun ActivityScreen() {
    // TODO: replace with real quest data
    val quests = emptyList<Quest>()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text("Activity", style = MaterialTheme.typography.headlineLarge)
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(quests) { quest ->
                QuestCard(quest = quest, onActionClick = { /* TODO: claim or navigate for this quest */ })
            }
        }
    }
}

@Composable
private fun QuestCard(quest: Quest, onActionClick: () -> Unit) {
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
                    Text("${quest.rewardLabel} x${quest.rewardCount}", color = Color.White)
                }
                Spacer(modifier = Modifier.weight(1f))
                if (quest.status == QuestStatus.IN_PROGRESS) {
                    Text("${quest.progressCurrent}/${quest.progressTarget}", color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Button(
                    onClick = onActionClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(if (quest.status == QuestStatus.IN_PROGRESS) "Go" else "Claim")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ActivityScreenPreview() {
    ActivityScreen()
}