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
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicTacToeScreen(
    onBack: () -> Unit,
    onWin: () -> Unit
) {
    var board by remember { mutableStateOf(List(9) { "" }) }
    var isPlayerTurn by remember { mutableStateOf(true) }
    var gameOver by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Your turn (X)") }

    fun checkWinner(b: List<String>): String? {
        val lines = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
            listOf(0, 4, 8), listOf(2, 4, 6)
        )
        for (line in lines) {
            val (a, c, e) = line
            if (b[a] != "" && b[a] == b[c] && b[c] == b[e]) return b[a]
        }
        if (b.all { it != "" }) return "Draw"
        return null
    }

    fun computerMove() {
        val empty = board.mapIndexedNotNull { i, v -> if (v == "") i else null }
        if (empty.isEmpty()) return
        val move = empty[Random.nextInt(empty.size)]
        board = board.toMutableList().also { it[move] = "O" }
        val result = checkWinner(board)
        when (result) {
            "O" -> { message = "Computer wins!"; gameOver = true }
            "Draw" -> { message = "It's a draw!"; gameOver = true }
            null -> { isPlayerTurn = true; message = "Your turn (X)" }
        }
    }

    fun playerMove(index: Int) {
        if (board[index] != "" || gameOver || !isPlayerTurn) return
        board = board.toMutableList().also { it[index] = "X" }
        val result = checkWinner(board)
        when (result) {
            "X" -> {                 message = "You win! +200 pts"; gameOver = true; onWin() }
            "Draw" -> { message = "It's a draw!"; gameOver = true }
            null -> { isPlayerTurn = false; message = "Computer's turn..."; computerMove() }
        }
    }

    fun resetGame() {
        board = List(9) { "" }
        isPlayerTurn = true
        gameOver = false
        message = "Your turn (X)"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tic Tac Toe", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF9FAFB))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = message,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    message.contains("You win") -> Color(0xFF10B981)
                    message.contains("Computer wins") -> Color(0xFFE4002B)
                    message.contains("draw") -> Color.Gray
                    else -> Color.Black
                },
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Game board
            for (row in 0..2) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    for (col in 0..2) {
                        val index = row * 3 + col
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White)
                                .clickable { playerMove(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = board[index],
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (board[index] == "X") Color(0xFF2563EB) else Color(0xFFE4002B)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { resetGame() },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text("New Game", fontSize = 14.sp)
            }
        }
    }
}
