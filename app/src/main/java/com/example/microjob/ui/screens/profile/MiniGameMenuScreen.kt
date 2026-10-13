package com.example.microjob.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniGameMenuScreen(
    gamesPlayedToday: Int,
    maxGamesPerDay: Int,
    onBack: () -> Unit,
    onPlayTicTacToe: () -> Unit,
    onPlayNumberGuess: () -> Unit,
    onPlayMemoryFlip: () -> Unit
) {
    val background = MaterialTheme.colorScheme.background
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surface = MaterialTheme.colorScheme.surface
    val remaining = maxGamesPerDay - gamesPlayedToday

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Mini Games", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Plays remaining banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Earn 10 points per win!",
                            fontSize = 14.sp,
                            color = primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "$remaining / $maxGamesPerDay plays left",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (remaining > 0) Color(0xFF10B981) else Color(0xFFE4002B)
                    )
                }
            }

            MiniGameCard(
                icon = "\u2563\u2551",
                title = "Tic Tac Toe",
                subtitle = "Classic XOX game - beat the computer",
                reward = "10 pts",
                enabled = remaining > 0,
                onClick = onPlayTicTacToe
            )

            MiniGameCard(
                icon = "\u2753",
                title = "Number Guess",
                subtitle = "Guess the secret number (1-10)",
                reward = "10 pts",
                enabled = remaining > 0,
                onClick = onPlayNumberGuess
            )

            MiniGameCard(
                icon = "\u2728",
                title = "Memory Flip",
                subtitle = "Match the pairs to win",
                reward = "10 pts",
                enabled = remaining > 0,
                onClick = onPlayMemoryFlip
            )

            if (remaining <= 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE4002B).copy(alpha = 0.1f))
                ) {
                    Text(
                        text = "You've used all your plays today. Come back tomorrow!",
                        fontSize = 13.sp,
                        color = Color(0xFFE4002B),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MiniGameCard(
    icon: String,
    title: String,
    subtitle: String,
    reward: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val surface = MaterialTheme.colorScheme.surface
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) surface else surface.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(primary.copy(alpha = if (enabled) 0.1f else 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 22.sp, color = if (enabled) Color.Unspecified else Color.Gray)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (enabled) onSurface else onSurfaceVariant)
                Text(subtitle, fontSize = 12.sp, color = onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    reward,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) Color(0xFF10B981) else Color.Gray
                )
                if (enabled) {
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = onSurfaceVariant
                    )
                }
            }
        }
    }
}
