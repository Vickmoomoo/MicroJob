package com.example.microjob.ui.screens.profile

import android.content.Intent
import android.provider.CalendarContract
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
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
import com.example.microjob.data.LocalJobRepository
import com.example.microjob.model.Job
import com.example.microjob.model.Review
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyJobDetailScreen(
    jobId: Int,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var job by remember { mutableStateOf<Job?>(null) }
    var reviews by remember { mutableStateOf<List<Review>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showCalendar by remember { mutableStateOf(false) }

    LaunchedEffect(jobId) {
        val repo = LocalJobRepository(context.applicationContext)
        job = repo.getJob(jobId)
        reviews = repo.getAllReviews().filter { it.jobId == jobId.toLong() }
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                title = { Text("Job Details") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        },
        bottomBar = {
            if (job != null && job?.status == "IN_PROGRESS") {
                Button(
                    onClick = { showCalendar = true },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp)
                ) { Text("Add to Calendar") }
            }
        }
    ) { innerPadding ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        val j = job ?: run {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) { Text("Job not found") }
            return@Scaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = j.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(text = "${j.currency}%.2f".format(j.price * 0.95), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(text = j.description, style = MaterialTheme.typography.bodyMedium)
            j.scheduledAt?.let {
                Spacer(Modifier.height(4.dp))
                Text(text = "Scheduled: ${formatScheduled(it)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.HorizontalDivider()
            Text(text = "Reviews for this job", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (reviews.isEmpty()) {
                Text(text = "No reviews yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

    if (showCalendar && job != null) {
        AddToCalendarDialog(job = job!!, onDismiss = { showCalendar = false })
    }
}

private fun formatScheduled(iso: String): String = try {
    OffsetDateTime.parse(iso).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
} catch (_: Exception) { iso }

@Composable
private fun RowStars(rating: Float) {
    Row {
        val full = rating.toInt()
        val half = rating - full >= 0.49f
        repeat(full) { Text(text = "\u2605", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary) }
        if (half) Text(text = "\u00BD", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
        repeat(5 - full - if (half) 1 else 0) { Text(text = "\u2606", fontSize = 18.sp, color = MaterialTheme.colorScheme.outline) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToCalendarDialog(job: Job, onDismiss: () -> Unit) {
    val context = LocalContext.current
    // Pre-fill from job
    val initial = remember(job.scheduledAt) {
        try { job.scheduledAt?.let { OffsetDateTime.parse(it) } } catch (_: Exception) { null }
    }
    var title by remember(job.title) { mutableStateOf(job.title) }
    var dateMillis by remember(initial) { mutableStateOf(initial?.toInstant()?.toEpochMilli()) }
    var hour by remember(initial) { mutableStateOf(initial?.hour) }
    var minute by remember(initial) { mutableStateOf(initial?.minute) }

    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }

    val dateText = remember(dateMillis) {
        if (dateMillis == null) "" else {
            val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            Instant.ofEpochMilli(dateMillis!!).atZone(ZoneId.systemDefault()).format(fmt)
        }
    }
    val timeText = remember(hour, minute) {
        if (hour == null || minute == null) "" else "%02d:%02d".format(hour, minute)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Calendar") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.fillMaxWidth().clickable { showDate = true }) {
                        OutlinedTextField(
                            value = dateText, onValueChange = {}, readOnly = true,
                            enabled = false,
                            label = { Text("Date") }, placeholder = { Text("Select date") },
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Box(modifier = Modifier.fillMaxWidth().clickable { showTime = true }) {
                        OutlinedTextField(
                            value = timeText, onValueChange = {}, readOnly = true,
                            enabled = false,
                            label = { Text("Time (24h)") }, placeholder = { Text("Select time") },
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isBlank() || dateMillis == null || hour == null || minute == null) return@TextButton
                val date = Instant.ofEpochMilli(dateMillis!!).atZone(ZoneId.systemDefault()).toLocalDate()
                val time = LocalTime.of(hour!!, minute!!)
                val ldt = LocalDateTime.of(date, time)
                val begin = ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val end = begin + 60 * 60 * 1000
                val intent = Intent(Intent.ACTION_INSERT).apply {
                    data = CalendarContract.Events.CONTENT_URI
                    putExtra(CalendarContract.Events.TITLE, title)
                    putExtra(CalendarContract.Events.DESCRIPTION, "MicroJob: $title")
                    putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
                    putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
                }
                context.startActivity(intent)
                onDismiss()
            }) { Text("Add to Calendar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(onDismissRequest = { showDate = false }, confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { dateMillis = it }; showDate = false }) { Text("OK") } }, dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } }) { DatePicker(state = state) }
    }
    if (showTime) {
        var hourText by remember { mutableStateOf((hour ?: 12).toString()) }
        var minuteText by remember { mutableStateOf((minute ?: 0).toString()) }
        AlertDialog(
            onDismissRequest = { showTime = false },
            title = { Text("Select time (24h)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 2) hourText = it },
                        label = { Text("Hour (0-23)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 2) minuteText = it },
                        label = { Text("Minute (0-59)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val h = hourText.toIntOrNull()?.coerceIn(0, 23) ?: (hour ?: 12)
                    val m = minuteText.toIntOrNull()?.coerceIn(0, 59) ?: (minute ?: 0)
                    hour = h; minute = m; showTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Cancel") } }
        )
    }
}


