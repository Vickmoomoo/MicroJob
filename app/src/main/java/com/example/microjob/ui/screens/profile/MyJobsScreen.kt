package com.example.microjob.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.microjob.data.RepositoryProvider
import com.example.microjob.model.Job
import com.example.microjob.model.SampleData

private fun statusForPosted(job: Job): String = when {
    job.status == "COMPLETED" || job.paymentStatus == "RELEASED" -> "Completed"
    job.status == "IN_PROGRESS" -> "Pending"
    else -> "Awaiting Accept"
}

private fun statusForAccepted(job: Job): String = when {
    job.status == "COMPLETED" || job.paymentStatus == "RELEASED" -> "Completed"
    else -> "Pending"
}

private fun statusColor(status: String): Color = when (status) {
    "Completed" -> Color(0xFF4CAF50)
    "Pending" -> Color(0xFF2196F3)
    else -> Color(0xFFFF9800) // Awaiting Accept
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyJobsScreen(
    userId: Long,
    onBack: () -> Unit,
    onJobClick: (Int) -> Unit,
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var postedJobs by remember { mutableStateOf<List<Job>>(emptyList()) }
    var acceptedJobs by remember { mutableStateOf<List<Job>>(emptyList()) }

    LaunchedEffect(userId) {
        val repo = RepositoryProvider.jobRepository(context.applicationContext)
        try {
            postedJobs = repo.getPostedJobs(userId).sortedByDescending { it.id }
            acceptedJobs = repo.getAcceptedJobs(userId).sortedByDescending { it.id }
        } catch (_: Exception) {
            // load failure = empty lists; nothing to crash over
        }
    }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                title = { Text("My Jobs") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Posted (${postedJobs.size})") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Accepted (${acceptedJobs.size})") })
            }

            if (selectedTab == 0) {
                JobListContent(jobs = postedJobs, isPosted = true, onJobClick = onJobClick)
            } else {
                JobListContent(jobs = acceptedJobs, isPosted = false, onJobClick = onJobClick)
            }
        }
    }
}

@Composable
private fun JobListContent(
    jobs: List<Job>,
    isPosted: Boolean,
    onJobClick: (Int) -> Unit,
) {
    if (jobs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "\uD83D\uDCCB", style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.height(12.dp))
                Text(text = if (isPosted) "No posted jobs" else "No accepted jobs", style = MaterialTheme.typography.titleMedium)
            }
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(jobs, key = { it.id }) { job ->
            val status = if (isPosted) statusForPosted(job) else statusForAccepted(job)
            MyJobCard(job = job, status = status, onClick = { onJobClick(job.id) })
        }
    }
}

@Composable
private fun MyJobCard(job: Job, status: String, onClick: () -> Unit) {
    val emoji = SampleData.categories.firstOrNull { it.name == job.category }?.emoji ?: "\uD83D\uDCBC"
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color(job.imageColor)),
                contentAlignment = Alignment.Center
            ) {
                if (job.images.isNotEmpty()) {
                    AsyncImage(
                        model = job.images.first(),
                        contentDescription = job.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(text = emoji, style = MaterialTheme.typography.displaySmall)
                }
                // Status chip top-right
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(50),
                    color = statusColor(status)
                ) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = job.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${job.currency}%.2f".format(job.price * 0.95),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = job.area.ifBlank { job.state },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
