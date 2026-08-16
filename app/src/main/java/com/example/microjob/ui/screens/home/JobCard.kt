package com.example.microjob.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.microjob.model.Job
import com.example.microjob.model.SampleData
import java.time.Duration
import java.time.OffsetDateTime

/** Looks up the category emoji for a job, falling back to a generic icon. */
private fun categoryEmoji(job: Job): String =
    SampleData.categories.firstOrNull { it.name == job.category }?.emoji ?: "💼"

/** Badge text on the card image: "Remote" (work from home) or "On-site". */
private fun jobTypeLabel(jobType: String): String = when (jobType) {
    "remote" -> "Remote"
    else -> "On-site"
}

/** "2026-08-16T09:30:00+08:00" → "Posted 3h ago"; blank/invalid → "". */
private fun timeAgo(iso: String): String {
    if (iso.isBlank()) return ""
    return try {
        val parsed = OffsetDateTime.parse(iso)
        val hours = Duration.between(parsed, OffsetDateTime.now()).toHours()
        when {
            hours < 1 -> "Posted just now"
            hours < 24 -> "Posted ${hours}h ago"
            hours < 24 * 30 -> "Posted ${hours / 24}d ago"
            else -> "Posted ${hours / (24 * 30)}mo ago"
        }
    } catch (e: Exception) {
        ""
    }
}

/** Small info chips row: payment method / language / GPS / tools (only what exists). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JobInfoChips(job: Job) {
    val items = buildList {
        add("💳 ${job.paymentMethod}")
        if (job.language.isNotBlank()) add("🗣️ ${job.language}")
        if (job.requireGps) add("🛰️ GPS")
        if (job.toolsRequired.isNotBlank()) add("🧰 ${job.toolsRequired}")
    }
    if (items.isEmpty()) return

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
    ) {
        items.forEach { item ->
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = item,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

/** A single job card inside the list on the Home screen. */
@Composable
fun JobCard(
    job: Job,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            // Placeholder image area (real photos come with the database later)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color(job.imageColor))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = categoryEmoji(job),
                    style = MaterialTheme.typography.displaySmall,
                )

                // Remote / On-site badge, top-right corner of the image.
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd),
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.55f)
                ) {
                    Text(
                        text = jobTypeLabel(job.jobType),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                // Title + posted time on the same row.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = job.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    val posted = timeAgo(job.createdAt)
                    if (posted.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = posted,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${job.currency}%.2f".format(job.price),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = job.area,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(8.dp))
                JobInfoChips(job)
            }
        }
    }
}
