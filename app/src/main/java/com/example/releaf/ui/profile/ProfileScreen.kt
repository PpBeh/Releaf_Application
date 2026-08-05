package com.example.releaf.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private data class Achievement(val id: String, val label: String)

@Composable
fun ProfileScreen(
    userId: String,
    onSettingsClick: () -> Unit
) {
    // TODO: load the real user (own profile vs. another user's) based on userId
    val isOwnProfile = userId == "me"
    val achievements = listOf(
        Achievement("1", "Expert Reviewer"),
        Achievement("2", "Expert Gardener"),
        Achievement("3", "Expert Navigator")
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Color(0xFF1E88E5))
            // TODO: swap in the real header image/pattern from the Figma export
        )

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Row(
                modifier = Modifier.offset(y = (-40).dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                    // TODO: painterResource(R.drawable.avatar_placeholder) or AsyncImage(user.avatarUrl)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text("User$userId", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Veteran Gardener", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Column(modifier = Modifier.offset(y = (-24).dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { /* TODO: navigate to this user's garden */ }) {
                        Text("View Garden")
                    }
                    if (isOwnProfile) {
                        OutlinedButton(onClick = onSettingsClick) {
                            Text("User Info")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "Achievements (${achievements.size}/55)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    achievements.forEach { achievement ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(Color(0xFF6D4C29), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            // TODO: swap in the real trophy illustration (painterResource) per achievement
                            Text(
                                achievement.label,
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    ProfileScreen(userId = "me", onSettingsClick = {})
}