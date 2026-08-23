package com.example.microjob.ui.screens.reviews

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.microjob.model.Review
import com.example.microjob.model.SampleData
import com.example.microjob.viewmodel.ReviewViewModel
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * Shows all reviews for a user. Empty state when no reviews exist.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsListScreen(
    userId: Long,
    userName: String,
    vm: ReviewViewModel,
    onBack: () -> Unit,
    onWriteReview: () -> Unit,
    onEditReview: (Long) -> Unit,
) {
    val listState by vm.listState.collectAsStateWithLifecycle()
    val myId = vm.myId()

    LaunchedEffect(userId) {
        vm.loadReviews(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reviews") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (listState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (listState.reviews.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "\u2B50",
                        fontSize = 48.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "No reviews yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Reviews will appear here after completed jobs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Average rating header
            item {
                val avgRating = listState.reviews.map { it.rating }.average()
                Text(
                    text = "\u2B50 %.1f  \u00B7  %d reviews".format(avgRating, listState.reviews.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                HorizontalDivider()
            }

            items(listState.reviews) { review ->
                ReviewItem(
                    review = review,
                    reviewerName = listState.users[review.reviewerUserId]?.name ?: "Unknown",
                    isMyReview = review.reviewerUserId == myId,
                    onEdit = { onEditReview(review.id) }
                )
                HorizontalDivider()
            }

            item {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ReviewItem(
    review: Review,
    reviewerName: String,
    isMyReview: Boolean,
    onEdit: () -> Unit,
) {
    val jobTitle = review.jobId?.let { id ->
        // Try to find job title from sample data or leave blank
        null // Will be passed from the list state
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        // Reviewer row
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = reviewerName.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reviewerName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                // Date + job info
                val dateStr = formatReviewDate(review.createdAt)
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Edit button for my own review
            if (isMyReview) {
                IconButton(onClick = onEdit) {
                    Text(
                        text = "Edit",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Star rating display
        Row {
            val fullStars = review.rating.toInt()
            val hasHalf = review.rating - fullStars >= 0.49f
            repeat(fullStars) {
                Text(text = "\u2605", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
            }
            if (hasHalf) {
                Text(text = "\u00BD", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
            }
            repeat(5 - fullStars - if (hasHalf) 1 else 0) {
                Text(text = "\u2606", fontSize = 18.sp, color = MaterialTheme.colorScheme.outline)
            }
        }

        // Comment
        if (review.comment.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = review.comment,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/** Formats an ISO-8601 timestamp to a readable date. */
private fun formatReviewDate(iso: String): String {
    return try {
        val date = OffsetDateTime.parse(iso).toLocalDate()
        date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    } catch (e: Exception) {
        iso
    }
}
