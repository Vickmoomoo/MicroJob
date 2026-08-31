package com.example.microjob.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val primaryBlue = Color(0xFF2563EB)
private val green = Color(0xFF10B981)
private val red = Color(0xFFEF4444)

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int
)

fun getQuizForCourse(courseId: Int): List<QuizQuestion> = when (courseId) {
    // Housekeeping
    1 -> listOf(
        QuizQuestion("What is the correct order for cleaning a room?", listOf("Floor → Walls → Ceiling", "Ceiling → Walls → Floor", "Walls → Floor → Ceiling", "Any order is fine"), 1),
        QuizQuestion("Which product should NOT be used on marble surfaces?", listOf("pH-neutral cleaner", "Vinegar", "Microfiber cloth", "Warm water"), 1),
        QuizQuestion("How often should kitchen sponges be replaced?", listOf("Once a month", "Every week", "Every day", "When they fall apart"), 1),
        QuizQuestion("What is the best method for removing grease stains?", listOf("Cold water only", "Baking soda paste", "Dry wiping", "Sandpaper"), 1),
        QuizQuestion("Which tool is most effective for cleaning windows?", listOf("Newspaper", "Paper towels", "Squeegee with microfiber", "Old rag"), 2)
    )
    2 -> listOf(
        QuizQuestion("What should you clean first in a kitchen?", listOf("Floor", "Countertops", "Appliances", "Sink"), 1),
        QuizQuestion("Which area needs the most attention during deep cleaning?", listOf("Visible surfaces", "Behind appliances", "Decorative items", "Light fixtures"), 1),
        QuizQuestion("What is the recommended water temperature for kitchen cleaning?", listOf("Cold water", "Lukewarm water", "Hot water", "It doesn't matter"), 2),
        QuizQuestion("How long should disinfectant sit on surfaces to be effective?", listOf("10 seconds", "1 minute", "5-10 minutes", "30 minutes"), 2),
        QuizQuestion("What is the safest way to clean inside an oven?", listOf("Use bleach", "Baking soda and vinegar", "Steel wool", "Harsh chemicals only"), 1)
    )
    // Caregiving
    6 -> listOf(
        QuizQuestion("What is the first step when approaching an elderly patient?", listOf("Start tasks immediately", "Introduce yourself and explain what you'll do", "Check their temperature", "Call the nurse"), 1),
        QuizQuestion("How often should elderly patients be repositioned in bed?", listOf("Every 1 hour", "Every 2 hours", "Every 4 hours", "Only when they ask"), 1),
        QuizQuestion("What is a common sign of dehydration in elderly?", listOf("Bright yellow urine", "Dry mouth and dark urine", "Excessive sweating", "Weight gain"), 1),
        QuizQuestion("What is the proper way to help someone stand up?", listOf("Pull their arms", "Support from under the shoulders", "Grab their wrists", "Let them do it alone"), 1),
        QuizQuestion("How can you prevent pressure sores?", listOf("Use baby powder", "Regular repositioning and skin checks", "Keep them still", "Use heating pads"), 1)
    )
    10 -> listOf(
        QuizQuestion("What does CPR stand for?", listOf("Cardio Pulmonary Resuscitation", "Cardiac Pressure Recovery", "Critical Patient Response", "Chest Pressure Release"), 0),
        QuizQuestion("How many chest compressions per minute in CPR?", listOf("60-80", "80-100", "100-120", "120-140"), 2),
        QuizQuestion("What is the ratio of compressions to breaths in CPR?", listOf("15:2", "30:2", "15:1", "30:1"), 1),
        QuizQuestion("When should you call emergency services?", listOf("After performing CPR", "Before starting CPR", "Only if the person asks", "Never"), 1),
        QuizQuestion("What is the first thing to check in an emergency?", listOf("Pulse", "Breathing", "Response/Consciousness", "Temperature"), 2)
    )
    // Delivery
    12 -> listOf(
        QuizQuestion("What should you check before starting a delivery?", listOf("Social media", "Weather and route conditions", "Personal messages", "Music playlist"), 1),
        QuizQuestion("How should food be transported to maintain temperature?", listOf("Open bag", "Insulated bag with hot/cold separation", "Plastic wrap only", "Newspaper wrapping"), 1),
        QuizQuestion("What is the maximum time food should be left unattended?", listOf("30 minutes", "2 hours", "4 hours", "It doesn't matter"), 1),
        QuizQuestion("What should you do if food packaging is damaged?", listOf("Deliver anyway", "Report and do not deliver", "Fix it with tape", "Ignore it"), 1),
        QuizQuestion("Which is the safest way to carry food on a motorcycle?", listOf("Hand carry", "Backpack only", "Proper delivery box/bag", "Hang on handlebars"), 2)
    )
    // Default fallback
    else -> listOf(
        QuizQuestion("What is the most important quality for this role?", listOf("Speed", "Attention to detail", "Talking a lot", "Working alone"), 1),
        QuizQuestion("How should you handle a difficult situation?", listOf("Ignore it", "Report to supervisor", "Quit immediately", "Do nothing"), 1),
        QuizQuestion("What is the key to good service?", listOf("Doing minimum required", "Being professional and courteous", "Working fast only", "Avoiding customers"), 1),
        QuizQuestion("How often should you check your work?", listOf("Never", "Once at the end", "Regularly throughout", "Only when asked"), 2),
        QuizQuestion("What should you do if you make a mistake?", listOf("Hide it", "Acknowledge and correct it", "Blame someone else", "Ignore it"), 1)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestQuizScreen(
    courseId: Int,
    onBack: () -> Unit,
    onPassed: () -> Unit
) {
    val questions = remember(courseId) { getQuizForCourse(courseId) }
    var currentQuestion by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableIntStateOf(-1) }
    var showResult by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var quizFinished by remember { mutableStateOf(false) }

    val totalQuestions = questions.size
    val passScore = (totalQuestions * 0.6).toInt() // 60% to pass

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Knowledge Test", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (quizFinished) {
            // Quiz result screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val passed = score >= passScore

                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = if (passed) green else red,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = if (passed) "Congratulations!" else "Not Passed",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (passed) green else red
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "You scored $score / $totalQuestions",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (passed) "You passed the test!" else "You need $passScore correct answers to pass.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                if (passed) {
                    Button(
                        onClick = onPassed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = green)
                    ) {
                        Text("Claim Certificate", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            currentQuestion = 0
                            selectedAnswer = -1
                            showResult = false
                            score = 0
                            quizFinished = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
                    ) {
                        Text("Try Again", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Quiz question screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Progress indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Question ${currentQuestion + 1} of $totalQuestions",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Score: $score",
                        fontSize = 14.sp,
                        color = primaryBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
                LinearProgressIndicator(
                    progress = { (currentQuestion + 1).toFloat() / totalQuestions },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = primaryBlue,
                    trackColor = MaterialTheme.colorScheme.outlineVariant,
                )

                Spacer(Modifier.height(8.dp))

                // Question
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = questions[currentQuestion].question,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(16.dp),
                        lineHeight = 24.sp
                    )
                }

                // Options
                questions[currentQuestion].options.forEachIndexed { index, option ->
                    val isSelected = selectedAnswer == index
                    val showCorrect = showResult && index == questions[currentQuestion].correctIndex
                    val showWrong = showResult && isSelected && index != questions[currentQuestion].correctIndex

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !showResult) { selectedAnswer = index },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                showCorrect -> green.copy(alpha = 0.1f)
                                showWrong -> red.copy(alpha = 0.1f)
                                isSelected -> primaryBlue.copy(alpha = 0.1f)
                                else -> MaterialTheme.colorScheme.surface
                            }
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Option letter
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            showCorrect -> green
                                            showWrong -> red
                                            isSelected -> primaryBlue
                                            else -> MaterialTheme.colorScheme.outlineVariant
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = ('A' + index).toString(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected || showCorrect || showWrong) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = option,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            if (showCorrect) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Correct",
                                    tint = green,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else if (showWrong) {
                                Text(
                                    text = "\u2717",
                                    fontSize = 18.sp,
                                    color = red
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Submit / Next button
                Button(
                    onClick = {
                        if (!showResult) {
                            // Check answer
                            isCorrect = selectedAnswer == questions[currentQuestion].correctIndex
                            if (isCorrect) score++
                            showResult = true
                        } else {
                            // Next question
                            if (currentQuestion < totalQuestions - 1) {
                                currentQuestion++
                                selectedAnswer = -1
                                showResult = false
                            } else {
                                quizFinished = true
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!showResult) primaryBlue else {
                            if (currentQuestion < totalQuestions - 1) primaryBlue else green
                        }
                    ),
                    enabled = selectedAnswer >= 0 || showResult
                ) {
                    Text(
                        when {
                            !showResult -> "Submit Answer"
                            currentQuestion < totalQuestions - 1 -> "Next Question"
                            else -> "See Results"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
