package com.example.microjob.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.microjob.model.PointsHistoryEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PointsHistoryScreen(
    userPoints: Int,
    pointsHistory: List<PointsHistoryEntry>,
    onBack: () -> Unit
) {
    val background = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surface
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Points History", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .background(primary.copy(alpha = 0.1f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("\u2B50", fontSize = 12.sp)
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = "$userPoints pts",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (pointsHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("\u2B50", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No points yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Play mini games to earn points!",
                            fontSize = 13.sp,
                            color = onSurfaceVariant
                        )
                    }
                }
            } else {
                pointsHistory.reversed().forEach { entry ->
                    PointsHistoryItem(entry = entry)
                }
            }
        }
    }
}

@Composable
fun PointsHistoryItem(entry: PointsHistoryEntry) {
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(surface)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(entry.source, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = onSurface)
            Spacer(Modifier.height(2.dp))
            Text(entry.date, fontSize = 12.sp, color = onSurfaceVariant)
        }
        Text(
            text = if (entry.isEarned) "+${entry.points}" else "${entry.points}",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (entry.isEarned) Color(0xFF10B981) else Color(0xFFE4002B)
        )
    }
}
