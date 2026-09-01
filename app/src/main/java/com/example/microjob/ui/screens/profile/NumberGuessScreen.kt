package com.example.microjob.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumberGuessScreen(
    onBack: () -> Unit,
    onWin: () -> Unit,
    onNewGame: () -> Boolean = { true }
) {
    var secretNumber by remember { mutableIntStateOf(Random.nextInt(1, 11)) }
    var guess by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Guess a number between 1 and 10") }
    var attempts by remember { mutableIntStateOf(0) }
    var gameOver by remember { mutableStateOf(false) }
    var guesses by remember { mutableStateOf(listOf<Int>()) }
    var showLimitDialog by remember { mutableStateOf(false) }

    val background = MaterialTheme.colorScheme.background
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    fun checkGuess() {
        val num = guess.toIntOrNull()
        if (num == null || num !in 1..10) {
            message = "Enter a valid number (1-10)"
            return
        }
        attempts++
        guesses = guesses + num
        when {
            num == secretNumber -> {
                message = "Correct! +10 pts ($attempts attempts)"
                gameOver = true
                onWin()
            }
            num < secretNumber -> {
                message = "Too low! Try higher."
                guess = ""
            }
            else -> {
                message = "Too high! Try lower."
                guess = ""
            }
        }
    }

    fun resetGame() {
        secretNumber = Random.nextInt(1, 11)
        guess = ""
        message = "Guess a number between 1 and 10"
        attempts = 0
        gameOver = false
        guesses = emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Number Guess", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
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
                text = "\u2753",
                fontSize = 48.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = message,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = when {
                    message.contains("Correct") -> Color(0xFF10B981)
                    message.contains("Too") -> primary
                    else -> onSurface
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = guess,
                onValueChange = { guess = it },
                label = { Text("Your guess") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                enabled = !gameOver,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(10.dp)
            )

            Button(
                onClick = { checkGuess() },
                enabled = !gameOver && guess.isNotEmpty(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guess", fontSize = 14.sp)
            }

            if (guesses.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Your guesses: ${guesses.joinToString(", ")}",
                    fontSize = 13.sp,
                    color = onSurfaceVariant
                )
            }

            if (gameOver) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (onNewGame()) resetGame() else showLimitDialog = true
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Play Again", fontSize = 14.sp)
                }
            }
        }
    }

    if (showLimitDialog) {
        AlertDialog(
            onDismissRequest = { showLimitDialog = false },
            title = { Text("Daily Limit Reached") },
            text = { Text("You've used all 6 plays for today. Come back tomorrow for more!") },
            confirmButton = {
                TextButton(onClick = { showLimitDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}
