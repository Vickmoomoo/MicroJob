package com.example.microjob.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryFlipScreen(
    onBack: () -> Unit,
    onWin: () -> Unit
) {
    val emojis = listOf("\uD83D\uDC36", "\uD83D\uDC31", "\uD83D\uDC3B", "\uD83D\uDC2F", "\uD83E\uDD81", "\uD83D\uDC38")
    var cards by remember { mutableStateOf((emojis + emojis).shuffled()) }
    var flipped by remember { mutableStateOf(List(12) { false }) }
    var matched by remember { mutableStateOf(List(12) { false }) }
    var firstPick by remember { mutableIntStateOf(-1) }
    var secondPick by remember { mutableIntStateOf(-1) }
    var lockInput by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Tap a card to flip it") }
    var gameOver by remember { mutableStateOf(false) }
    var moves by remember { mutableIntStateOf(0) }

    val background = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surface
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    LaunchedEffect(firstPick, secondPick) {
        if (firstPick >= 0 && secondPick >= 0 && firstPick != secondPick) {
            lockInput = true
            delay(800)
            if (cards[firstPick] == cards[secondPick]) {
                matched = matched.toMutableList().also {
                    it[firstPick] = true
                    it[secondPick] = true
                }
                flipped = flipped.toMutableList().also {
                    it[firstPick] = false
                    it[secondPick] = false
                }
                if (matched.all { it }) {
                    message = "You win! +200 pts ($moves moves)"
                    gameOver = true
                    onWin()
                } else {
                    message = "Matched! Keep going."
                }
            } else {
                flipped = flipped.toMutableList().also {
                    it[firstPick] = false
                    it[secondPick] = false
                }
                message = "No match. Try again!"
            }
            firstPick = -1
            secondPick = -1
            lockInput = false
        }
    }

    fun onCardClick(index: Int) {
        if (lockInput || flipped[index] || matched[index] || gameOver) return
        flipped = flipped.toMutableList().also { it[index] = true }
        if (firstPick == -1) {
            firstPick = index
            message = "Pick another card"
        } else {
            secondPick = index
            moves++
        }
    }

    fun resetGame() {
        cards = (emojis + emojis).shuffled()
        flipped = List(12) { false }
        matched = List(12) { false }
        firstPick = -1
        secondPick = -1
        lockInput = false
        message = "Tap a card to flip it"
        gameOver = false
        moves = 0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Memory Flip", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = message,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = when {
                    message.contains("win") -> Color(0xFF10B981)
                    message.contains("Matched") -> primary
                    else -> onSurface
                },
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                "Moves: $moves",
                fontSize = 14.sp,
                color = onSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            for (row in 0..2) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    for (col in 0..3) {
                        val index = row * 4 + col
                        val isRevealed = flipped[index] || matched[index]
                        Box(
                            modifier = Modifier
                                .size(75.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isRevealed) {
                                        if (matched[index]) Color(0xFF10B981).copy(alpha = 0.2f)
                                        else surface
                                    } else primary
                                )
                                .clickable { onCardClick(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isRevealed) cards[index] else "?",
                                fontSize = 28.sp,
                                color = if (isRevealed) onSurface else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { resetGame() },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primary)
            ) {
                Text("New Game", fontSize = 14.sp)
            }
        }
    }
}
