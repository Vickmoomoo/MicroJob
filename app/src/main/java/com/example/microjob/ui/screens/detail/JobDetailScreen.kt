package com.example.microjob.ui.screens.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.microjob.model.Job
import com.example.microjob.model.SampleData
import com.example.microjob.model.User
import com.example.microjob.viewmodel.JobDetailUiState
import com.example.microjob.viewmodel.JobDetailViewModel
import java.time.OffsetDateTime

/** Looks up the category emoji for a job, falling back to a generic icon. */
private fun categoryEmoji(job: Job): String =
    SampleData.categories.firstOrNull { it.name == job.category }?.emoji ?: "💼"

/** Formats an ISO-8601 timestamp into "yyyy-MM-dd", or returns the raw text. */
private fun formatDate(iso: String): String =
    runCatching { OffsetDateTime.parse(iso).toLocalDate().toString() }.getOrDefault(iso)

/**
 * Full-screen job detail page. Shows every field of the job plus a
 * poster row (avatar + name). Bottom navigation is hidden on this page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailScreen(
    jobId: Int,
    onBack: () -> Unit,
    onContactPoster: (com.example.microjob.model.User?) -> Unit,
    onPosterClick: (Long) -> Unit = {},
    vm: JobDetailViewModel = viewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(jobId) {
        vm.loadJob(jobId)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Job Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            JobDetailUiState.Loading -> LoadingContent(innerPadding = innerPadding)
            JobDetailUiState.NotFound -> NotFoundContent(innerPadding = innerPadding)
            is JobDetailUiState.Success -> JobDetailContent(
                job = state.job,
                poster = state.poster,
                innerPadding = innerPadding,
                onContactPoster = onContactPoster,
                onPosterClick = onPosterClick
            )
        }
    }
}

@Composable
private fun LoadingContent(innerPadding: androidx.compose.foundation.layout.PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentAlignment = Alignment.Center
    ) {
        Text("Loading...", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun NotFoundContent(innerPadding: androidx.compose.foundation.layout.PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Job not found", style = MaterialTheme.typography.titleMedium)
            Text(
                "This job may have been removed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun JobDetailContent(
    job: Job,
    poster: User?,
    innerPadding: androidx.compose.foundation.layout.PaddingValues,
    onContactPoster: (com.example.microjob.model.User?) -> Unit,
    onPosterClick: (Long) -> Unit = {},
) {
    val context = LocalContext.current
    val hasRequirements = job.requireGps || job.toolsRequired.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        // Scrollable details column on top.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
        // Photo area: swipeable pager when photos exist, otherwise the
        // placeholder color block (same pattern as the home banner).
        if (job.images.isNotEmpty()) {
            PhotoPager(job = job)
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color(job.imageColor)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = categoryEmoji(job),
                    style = MaterialTheme.typography.displayLarge,
                )
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = job.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            // Price — worker's take-home amount (budget minus 5% platform fee).
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${job.currency}%.2f".format(job.price * 0.95),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = job.category,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))

            // Poster row (avatar + name). Clicking opens the poster profile.
            PosterRow(poster = poster, onClick = { poster?.let { onPosterClick(it.id) } })

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()

            // Location — tap to open Google Maps navigation from current position.
            InfoRow(
                icon = "📍",
                label = "Location",
                value = job.location,
                onClick = {
                    val query = Uri.encode(job.location)
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$query")
                    )
                    context.startActivity(intent)
                }
            )
            InfoRow(
                icon = "🗣️",
                label = "Recommended language",
                value = job.language.ifBlank { "Not specified" }
            )
            InfoRow(
                icon = "💳",
                label = "Payment method",
                value = job.paymentMethod
            )
            if (job.deadline != null) {
                InfoRow(
                    icon = "⏰",
                    label = "Deadline",
                    value = formatDate(job.deadline)
                )
            }

            // Requirement block — only shown when there is at least one requirement.
            if (hasRequirements) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Requirements",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (job.requireGps) {
                    RequirementRow(icon = "🛰️", text = "Location required — worker must share their location while working")
                }
                if (job.toolsRequired.isNotBlank()) {
                    RequirementRow(icon = "🧰", text = "Tools: ${job.toolsRequired}")
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Text(
                text = "Description",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = job.description,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(24.dp))
            }
        }

        // Contact the job poster — pinned to the bottom, not scrolled with content.
        Button(
            onClick = { onContactPoster(poster) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(52.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Chat,
                contentDescription = null
            )
            Spacer(Modifier.width(8.dp))
            Text("Contact Job Poster")
        }
    }
}

/**
 * Swipeable photo pager for the job detail header — same interaction as the
 * home banner, with indicator dots. Each image loads from its URL via Coil.
 */
@Composable
private fun PhotoPager(job: Job) {
    val pagerState = rememberPagerState(pageCount = { job.images.size })

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(state = pagerState) { page ->
            AsyncImage(
                model = job.images[page],
                contentDescription = "Job photo ${page + 1}",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
        }

        // Indicator dots
        if (job.images.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(job.images.size) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (selected) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun PosterRow(poster: User?, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar placeholder: circle with the first letter of the name.
        // TODO: replace with real photo from avatarUrl when images are supported.
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = (poster?.name?.firstOrNull()?.toString() ?: "?").uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = poster?.name ?: "Unknown poster",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            if (!poster?.bio.isNullOrBlank()) {
                Text(
                    text = poster!!.bio,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun InfoRow(icon: String, label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(icon, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RequirementRow(icon: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(icon, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
