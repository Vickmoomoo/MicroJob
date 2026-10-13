package com.example.microjob.ui.screens.reviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.microjob.data.RepositoryProvider
import com.example.microjob.model.Job
import com.example.microjob.model.Review

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewJobDetailScreen(
    jobId: Int,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var job by remember { mutableStateOf<Job?>(null) }
    var reviews by remember { mutableStateOf<List<Review>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(jobId) {
        val repo = RepositoryProvider.jobRepository(context.applicationContext)
        try {
            job = repo.getJob(jobId)
            // Load both direction reviews for this job
            val all = repo.getAllReviews().filter { it.jobId == jobId.toLong() }
            reviews = all
        } catch (_: Exception) {
            // ignore — the "Job not found" state below handles it
        }
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                title = { Text("Job Review") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { innerPadding ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        val j = job
        if (j == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) { Text("Job not found") }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = j.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(text = "${j.currency}%.2f".format(j.price * 0.95), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(text = j.description, style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(4.dp))
            Text(text = "Reviews for this job", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            if (reviews.isEmpty()) {
                Text(text = "No reviews yet for this job.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                reviews.forEach { review ->
                    val reviewerLabel = when (review.reviewerUserId) {
                        j.posterId -> "Owner Review"
                        j.workerId -> "Worker Review"
                        else -> "Review"
                    }
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = reviewerLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                            RowStars(review.rating)
                            if (review.comment.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(text = review.comment, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowStars(rating: Float) {
    androidx.compose.foundation.layout.Row {
        val full = rating.toInt()
        val half = rating - full >= 0.49f
        repeat(full) { Text(text = "\u2605", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary) }
        if (half) Text(text = "\u00BD", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
        repeat(5 - full - if (half) 1 else 0) { Text(text = "\u2606", fontSize = 18.sp, color = MaterialTheme.colorScheme.outline) }
    }
}
