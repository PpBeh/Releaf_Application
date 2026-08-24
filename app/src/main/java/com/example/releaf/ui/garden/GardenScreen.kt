package com.example.releaf.ui.garden

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.releaf.R
import com.example.releaf.ui.theme.string
import com.example.releaf.ui.viewmodel.GardenViewModel
import com.example.releaf.ui.viewmodel.ThemeViewModel

@Composable
fun GardenScreen(
    viewModel: GardenViewModel,
    userId: String,
    themeViewModel: ThemeViewModel,
    onHouseClick: () -> Unit
) {
    val context = LocalContext.current
    val statusMessage by viewModel.statusMessage.collectAsState()

    val currentExp by viewModel.currentExp.collectAsState()
    val currentGems by viewModel.currentGems.collectAsState()
    val waterUsesLeft by viewModel.waterUsesLeft.collectAsState()
    val fertilizeUsesLeft by viewModel.fertilizeUsesLeft.collectAsState()

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            viewModel.loadGarden(userId, context)
        }
    }

    val maxUses = viewModel.getMaxUsesByExp(currentExp)
    val treeStage = viewModel.getTreeStage(currentExp)

    val nextTargetExp = when (treeStage) {
        1 -> 2000
        2 -> 5000
        else -> 10000
    }
    val expProgress = (currentExp.toFloat() / nextTargetExp.toFloat()).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = string("garden", themeViewModel) + " Status",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Stage $treeStage",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE91E63), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("\uD83D\uDC8E", style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$currentGems",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Exp: $currentExp / $nextTargetExp",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { expProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF4CAF50),
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onHouseClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = string("garden_plot", themeViewModel),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Go to Garden Plot",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val treeDrawable = when (treeStage) {
                    3 -> R.drawable.ic_tree_stage_3
                    2 -> R.drawable.ic_tree_stage_2
                    else -> R.drawable.ic_tree_stage_1
                }

                Image(
                    painter = painterResource(id = treeDrawable),
                    contentDescription = "Tree Stage $treeStage",
                    modifier = Modifier.size(280.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!statusMessage.isNullOrBlank()) {
                    val isLimit = statusMessage!!.contains("limit", ignoreCase = true)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isLimit) Color(0xFFFFF3E0) else Color(0xFFE8F5E9)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = statusMessage!!,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLimit) Color(0xFFE65100) else Color(0xFF2E7D32),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GardenActionButton(
                    label = "Water",
                    icon = "\uD83D\uDCA7",
                    usesLeft = waterUsesLeft,
                    usesMax = maxUses,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.waterPlant(userId, context) }
                )
                GardenActionButton(
                    label = "Fertilize",
                    icon = "\uD83C\uDF31",
                    usesLeft = fertilizeUsesLeft,
                    usesMax = maxUses,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.fertilizePlant(userId, context) }
                )
            }
        }
    }
}

@Composable
private fun GardenActionButton(
    label: String,
    icon: String,
    usesLeft: Int,
    usesMax: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (usesLeft > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (usesLeft > 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(icon, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(label, fontWeight = FontWeight.Bold)
                Text("$usesLeft/$usesMax Left", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}