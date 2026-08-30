package com.example.microjob.ui.screens.profile

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

private val primaryBlue = Color(0xFF2563EB)
private val green = Color(0xFF10B981)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    courseId: Int,
    episode: Int,
    totalEpisodes: Int,
    onBack: () -> Unit,
    onWatched: () -> Unit
) {
    // YouTube video IDs per course category
    val videoId = when {
        courseId <= 5 -> "kqtD5dpn9C8"   // Housekeeping
        courseId <= 11 -> "C5H31oEvhQ0"  // Caregiving
        courseId <= 13 -> "bJaptbg2MkY"  // Delivery
        courseId <= 15 -> "fahbLIyAbUQ"  // Gardening
        courseId <= 18 -> "sD0wJKRWO4o"  // Digital
        else -> "a1wqWOBZdf4"            // Soft Skills
    }

    // Countdown: 10 seconds per episode for demo
    val countdownDuration = 10
    var countdown by remember { mutableIntStateOf(countdownDuration) }
    var isCompleted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
        isCompleted = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Episode $episode / $totalEpisodes", fontSize = 16.sp) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // YouTube WebView
            YouTubeWebView(
                videoId = videoId,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )

            // Video info + countdown
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Lesson $episode: Course Video",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                // Countdown / completion status
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCompleted) green.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isCompleted) {
                            Text(
                                text = "\u2714 Video completed!",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = green
                            )
                        } else {
                            CircularProgressIndicator(
                                progress = { (countdown.toFloat() / countdownDuration) },
                                modifier = Modifier.size(24.dp),
                                color = primaryBlue,
                                strokeWidth = 3.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Please watch the full video",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Remaining: ${countdown}s",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Mark as watched button
                Button(
                    onClick = onWatched,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCompleted) green else Color.Gray
                    ),
                    enabled = isCompleted
                ) {
                    Text(
                        if (isCompleted) "Mark as Watched" else "Watch Video First",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YouTubeWebView(videoId: String, modifier: Modifier = Modifier) {
    val html = """
        <!DOCTYPE html>
        <html>
        <body style="margin:0;padding:0;background:#000;">
        <div style="position:relative;padding-bottom:56.25%;height:0;overflow:hidden;">
        <iframe 
            src="https://www.youtube.com/embed/$videoId?autoplay=1&rel=0&origin=https://www.youtube.com" 
            style="position:absolute;top:0;left:0;width:100%;height:100%;border:0;"
            allow="autoplay; encrypted-media" 
            allowfullscreen>
        </iframe>
        </div>
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
            }
        },
        modifier = modifier
    )
}
