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
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.microjob.viewmodel.CourseViewModel
import kotlinx.coroutines.delay

private val primaryBlue = Color(0xFF2563EB)
private val green = Color(0xFF10B981)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    courseId: Int,
    onBack: () -> Unit,
    onCompleted: () -> Unit,
    vm: CourseViewModel = viewModel()
) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    val course = remember(categories) {
        categories.flatMap { it.courses }.find { it.id == courseId }
    }

    // Auto-start course when entering detail screen
    LaunchedEffect(courseId) {
        if (course != null && !course.enrolled) {
            vm.startCourse(courseId)
        }
    }

    // Video state
    var isPlaying by remember { mutableStateOf(false) }
    var videoProgress by remember { mutableIntStateOf(0) }
    val videoDuration = 10 // seconds needed to unlock test
    val videoComplete = videoProgress >= videoDuration
    val videoPercent = ((videoProgress.toFloat() / videoDuration) * 100).toInt().coerceAtMost(100)

    // Test state
    var testCompleted by remember { mutableStateOf(false) }
    val testPercent = if (testCompleted) 100 else 0

    // Overall progress: 80% video + 20% test
    val overallProgress = when {
        course == null -> 0
        course.progress == 100 -> 100
        !course.enrolled -> 0
        else -> (videoPercent * 0.8 + testPercent * 0.2).toInt()
    }

    // Simulate video playback
    LaunchedEffect(isPlaying, videoProgress) {
        if (isPlaying && videoProgress < videoDuration) {
            delay(1000)
            videoProgress = (videoProgress + 1).coerceAtMost(videoDuration)
            if (videoProgress >= videoDuration) {
                isPlaying = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Course Details", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Fixed bottom progress bar
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding()
                ) {
                    when {
                        course == null -> { /* no-op */ }
                        !course.enrolled -> {
                            Button(
                                onClick = { vm.startCourse(courseId) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
                            ) {
                                Text("Start Course", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        course.progress == 100 -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = green.copy(alpha = 0.1f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = green,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Course Completed",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = green
                                    )
                                }
                            }
                        }
                        else -> {
                            // Progress bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Your Progress",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "$overallProgress%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (overallProgress == 100) green else primaryBlue
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { overallProgress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (overallProgress == 100) green else primaryBlue,
                                trackColor = MaterialTheme.colorScheme.outlineVariant,
                            )
                            Spacer(Modifier.height(4.dp))
                            if (overallProgress == 100) {
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        vm.updateProgress(courseId, 100)
                                        onCompleted()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = green)
                                ) {
                                    Text("Claim Certificate", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (course == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Course not found", style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            // Hero banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(primaryBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = course.emoji, fontSize = 72.sp)
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title + status
                Column {
                    Text(
                        text = course.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "\uD83D\uDCC5 ${course.lessons} lessons \u2022 ${course.duration}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    when {
                        course.progress == 100 -> AssistChip(
                            onClick = {},
                            label = { Text("Completed", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(16.dp), tint = green)
                            }
                        )
                        course.enrolled -> AssistChip(
                            onClick = {},
                            label = { Text("In Progress", fontSize = 12.sp) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = primaryBlue.copy(alpha = 0.1f))
                        )
                        else -> AssistChip(
                            onClick = {},
                            label = { Text("Not Enrolled", fontSize = 12.sp) }
                        )
                    }
                }

                HorizontalDivider()

                // Video player section (only when enrolled)
                if (course.enrolled && course.progress < 100) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column {
                            // Video area
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .clickable {
                                        if (videoProgress < videoDuration) {
                                            isPlaying = !isPlaying
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (videoProgress >= videoDuration) {
                                    // Video completed
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Completed",
                                        tint = green,
                                        modifier = Modifier.size(64.dp)
                                    )
                                } else if (isPlaying) {
                                    // Playing indicator
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(48.dp),
                                        color = primaryBlue,
                                        strokeWidth = 4.dp
                                    )
                                } else {
                                    // Play button
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = "Play",
                                        tint = primaryBlue,
                                        modifier = Modifier.size(64.dp)
                                    )
                                }
                            }
                            // Video progress bar
                            LinearProgressIndicator(
                                progress = { videoProgress.toFloat() / videoDuration },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = if (videoComplete) green else primaryBlue,
                                trackColor = MaterialTheme.colorScheme.outlineVariant,
                            )
                            // Video info
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (videoComplete) "Video watched" else "Watching... ${videoProgress}/${videoDuration}s",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!videoComplete) {
                                    Text(
                                        text = "Watch ${videoDuration - videoProgress}s more to unlock test",
                                        fontSize = 11.sp,
                                        color = primaryBlue
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Knowledge Test (locked until video watched)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            testCompleted -> green.copy(alpha = 0.05f)
                            videoComplete || course.progress == 100 -> MaterialTheme.colorScheme.surface
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when {
                                testCompleted -> "\uD83C\uDF89"
                                videoComplete || course.progress == 100 -> "\uD83D\uDD0D"
                                else -> "\uD83D\uDD12"
                            },
                            fontSize = 32.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Knowledge Test",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = when {
                                testCompleted -> "You passed! You can now claim your certificate."
                                videoComplete || course.progress == 100 -> "You're eligible! Tap below to start the test."
                                else -> "Watch the video first to unlock the test."
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { testCompleted = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            enabled = (videoComplete || course.progress == 100) && !testCompleted
                        ) {
                            Text(
                                when {
                                    testCompleted -> "Test Passed \u2714"
                                    videoComplete || course.progress == 100 -> "Start Test"
                                    else -> "Locked"
                                },
                                fontSize = 14.sp,
                                color = if (testCompleted) green else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Spacer for bottom button
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}
