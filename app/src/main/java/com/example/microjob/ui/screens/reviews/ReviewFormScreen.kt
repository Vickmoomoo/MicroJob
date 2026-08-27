package com.example.microjob.ui.screens.reviews

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.microjob.viewmodel.ReviewViewModel

/**
 * Full-screen review form with half-star rating.
 *
 * @param reviewedUserId the user being reviewed
 * @param jobId the job this review is about (optional)
 * @param existingReviewId if editing, the id of the existing review
 * @param vm the ReviewViewModel
 * @param onBack navigate back
 * @param onSubmitted called after successful submission
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewFormScreen(
    reviewedUserId: Long,
    jobId: Long?,
    existingReviewId: Long? = null,
    vm: ReviewViewModel,
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
) {
    val formState by vm.formState.collectAsStateWithLifecycle()
    val isEditing = existingReviewId != null

    LaunchedEffect(formState.submitted) {
        if (formState.submitted) {
            onSubmitted()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0), title = { Text(if (isEditing) "Edit Review" else "Leave a Review") },
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // --- Half-star rating ---
            Text(
                text = "Rating",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            HalfStarRating(
                rating = formState.rating,
                onRatingChange = { vm.onRatingChange(it) }
            )

            Spacer(Modifier.height(4.dp))

            // --- Comment ---
            Text(
                text = "Comment",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = formState.comment,
                onValueChange = { vm.onCommentChange(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Share your experience...") },
                minLines = 3,
                maxLines = 6
            )

            // --- Error ---
            if (formState.error != null) {
                Text(
                    text = formState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(8.dp))

            // --- Submit button ---
            Button(
                onClick = { vm.submitReview(reviewedUserId, jobId, existingReviewId) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !formState.isSubmitting
            ) {
                if (formState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (isEditing) "Update Review" else "Submit Review")
            }
        }
    }
}

/**
 * Half-star rating. Tap left half = 0.5, tap right half = 1.0.
 * Uses two half-width Box overlays for clean half-fill effect.
 */
@Composable
private fun HalfStarRating(
    rating: Float,
    onRatingChange: (Float) -> Unit,
    maxStars: Int = 5,
) {
    val filledColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..maxStars) {
            val starIndex = i - 1
            val fillLevel = when {
                rating >= i.toFloat() -> 1f
                rating >= starIndex + 0.5f -> 0.5f
                else -> 0f
            }

            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.StarBorder,
                    contentDescription = null,
                    tint = outlineColor,
                    modifier = Modifier.size(34.dp)
                )
                if (fillLevel > 0f) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = filledColor,
                        modifier = Modifier
                            .size(34.dp)
                            .drawWithContent {
                                clipRect(right = size.width * fillLevel) {
                                    this@drawWithContent.drawContent()
                                }
                            }
                    )
                }
                Row(modifier = Modifier.matchParentSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onRatingChange(starIndex + 0.5f) }
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onRatingChange(i.toFloat()) }
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        Text(
            text = if (rating == rating.toInt().toFloat()) "${rating.toInt()}\u2605" else "%.1f\u2605".format(rating),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
