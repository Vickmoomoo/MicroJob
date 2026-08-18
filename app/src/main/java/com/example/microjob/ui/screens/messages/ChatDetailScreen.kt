package com.example.microjob.ui.screens.messages

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.microjob.model.Job
import com.example.microjob.model.Message
import com.example.microjob.viewmodel.ChatViewModel

/**
 * The chat detail screen: a full conversation between the logged-in user and
 * one other user. Shows messages as bubbles, plus a composer with a "+" menu
 * to attach a photo, send a job invite, or send a release-payment card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    otherUserId: Long,
    onBack: () -> Unit,
    vm: ChatViewModel = viewModel(),
) {
    val messages by vm.messages.collectAsState()
    val otherUser by vm.otherUser.collectAsState()
    val myId = vm.myId()
    val listState = rememberLazyListState()

    // Open the conversation once; capture the conversation id for sending.
    var convId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        vm.openConversation(otherUserId) { id ->
            convId = id
        }
    }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.lastIndex)
        }
    }
    // Pre-load any job referenced by invite / payment cards for rendering.
    val jobIds = remember { mutableStateOf(setOf<Int>()) }
    LaunchedEffect(messages) {
        val ids = messages.mapNotNull { it.jobId.takeIf { job -> job > 0 } }.toSet()
        if (ids != jobIds.value) {
            jobIds.value = ids
            vm.ensureJobsLoaded(ids)
        }
    }
    val jobCache by vm.jobCache.collectAsState()
    // Re-fetch any card job that isn't loaded yet, so invite/payment cards never
    // stay stuck on "Loading..." (e.g. right after an accept updates the cache).
    LaunchedEffect(messages, jobCache) {
        val need = messages
            .mapNotNull { it.jobId.takeIf { job -> job > 0 } }
            .toSet()
        val missing = need - jobCache.keys
        if (missing.isNotEmpty()) vm.ensureJobsLoaded(missing)
    }

    var text by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var showJobPicker by remember { mutableStateOf(false) }
    var pickerKind by remember { mutableStateOf(JobPickerKind.INVITE) }
    var showSettleDialog by remember { mutableStateOf(false) }
    var acceptedJob by remember { mutableStateOf<Job?>(null) }
    var acceptingJob by remember { mutableStateOf<Int?>(null) }
    var confirmAcceptJob by remember { mutableStateOf<Job?>(null) }
    var acceptedJobIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var fullScreenImage by remember { mutableStateOf<String?>(null) }

    val myJobs by vm.myPostedJobs.collectAsState()

    val pickImage =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                val cid = convId ?: vm.activeConversation.value?.id
                if (cid != null) vm.sendImage(cid, otherUserId, uri)
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(otherUser?.name ?: "Chat") },
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
                .imePadding()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    val isAlreadyAccepted =
                        message.jobId in acceptedJobIds ||
                            (jobCache[message.jobId]?.status != null && jobCache[message.jobId]?.status != "OPEN")
                    MessageBubble(
                        message = message,
                        isMine = message.senderId == myId,
                        otherUserId = otherUserId,
                        job = jobCache[message.jobId],
                        alreadyAccepted = isAlreadyAccepted,
                        accepting = acceptingJob == message.jobId,
                        onImageClick = { fullScreenImage = it },
                        // First tap shows a double-confirm dialog; accepting happens there.
                        onAcceptJob = {
                            if (acceptingJob == null) {
                                confirmAcceptJob = jobCache[message.jobId]
                            }
                        },
                        onOpenPayment = {
                            vm.loadCardJob(message.jobId)
                            showSettleDialog = true
                        }
                    )
                }
            }

            // ---- Composer ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // "+" menu
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Attach")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Photo") },
                            leadingIcon = { Icon(Icons.Filled.PhotoCamera, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                pickImage.launch("image/*")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Job Invite") },
                            leadingIcon = { Icon(Icons.Filled.Work, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                pickerKind = JobPickerKind.INVITE
                                vm.refreshMyJobs()
                                showJobPicker = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Release Payment") },
                            leadingIcon = { Icon(Icons.Filled.AttachMoney, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                pickerKind = JobPickerKind.PAYMENT
                                vm.refreshMyJobs()
                                showJobPicker = true
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message") },
                    maxLines = 4
                )
                IconButton(
                    onClick = {
                        if (text.isNotBlank()) {
                            val cid = convId ?: vm.activeConversation.value?.id
                            if (cid != null) {
                                vm.sendText(cid, otherUserId, text)
                                text = ""
                            }
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }

    // ---- Job picker (choose which job to send as invite / payment card) ----
    if (showJobPicker) {
        JobPickerDialog(
            // Job Invite: only OPEN jobs (accepted/settled ones are hidden).
            // Release Payment: only jobs a worker has already accepted.
            jobs = if (pickerKind == JobPickerKind.INVITE) myJobs.filter { it.status == "OPEN" }
                    else myJobs.filter { it.status == "IN_PROGRESS" },
            emptyMessage = if (pickerKind == JobPickerKind.INVITE)
                    "You have no posted jobs."
                else "No accepted jobs yet. Send a job invite and wait for a worker to accept first.",
            title = if (pickerKind == JobPickerKind.INVITE) "Send Job Invite" else "Release Payment",
            onPick = { job ->
                showJobPicker = false
                val cid = convId ?: vm.activeConversation.value?.id ?: return@JobPickerDialog
                if (pickerKind == JobPickerKind.INVITE) {
                    vm.sendJobInvite(cid, otherUserId, job.id)
                } else {
                    vm.sendPaymentCard(cid, otherUserId, job.id)
                }
            },
            onDismiss = { showJobPicker = false }
        )
    }

    // ---- Settle (release payment) dialog shown from a payment card ----
    if (showSettleDialog) {
        val job by vm.cardJob.collectAsState()
        SettlePaymentDialog(
            job = job,
            currentUserId = myId,
            onClaim = { jobId -> vm.releaseJobPayment(jobId) { showSettleDialog = false } },
            onDismiss = { showSettleDialog = false }
        )
    }

    // ---- Full-screen image viewer (tap a chat photo) ----
    fullScreenImage?.let { path ->
        Dialog(onDismissRequest = { fullScreenImage = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = path,
                    contentDescription = "Photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { fullScreenImage = null }
                )
            }
        }
    }

    // ---- Double-confirm before accepting a job ----
    confirmAcceptJob?.let { job ->
        AlertDialog(
            onDismissRequest = { confirmAcceptJob = null },
            title = { Text("Confirm Accept") },
            text = {
                Column {
                    Text("Are you sure you want to accept \"${job.title}\"?")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        buildString {
                            append("You'll receive RM%.2f once the poster releases payment. ")
                            append("This job can only be accepted by one worker.")
                        }.format(job.price * 0.95),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmAcceptJob = null
                    val id = job.id
                    acceptingJob = id
                    vm.acceptJob(id) {
                        acceptedJob = jobCache[id]
                        acceptedJobIds = acceptedJobIds + id
                        acceptingJob = null
                    }
                }) { Text("Accept") }
            },
            dismissButton = {
                TextButton(onClick = { confirmAcceptJob = null }) { Text("Cancel") }
            }
        )
    }

    // ---- "You accepted the job" confirmation ----
    acceptedJob?.let { job ->
        AlertDialog(
            onDismissRequest = { acceptedJob = null },
            title = { Text("Job accepted") },
            text = {
                Column {
                    Text("You accepted \"${job.title}\".")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        buildString {
                            append("You'll receive RM%.2f (after the 5%% platform fee). The poster ")
                            append("can now release payment when the job is done.")
                        }.format(job.price * 0.95),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = { Button(onClick = { acceptedJob = null }) { Text("OK") } }
        )
    }
}

enum class JobPickerKind { INVITE, PAYMENT }

/** A chat bubble: text or image on the left/right, cards rendered wider. */
@Composable
private fun MessageBubble(
    message: Message,
    isMine: Boolean,
    otherUserId: Long,
    job: Job?,
    alreadyAccepted: Boolean,
    accepting: Boolean,
    onImageClick: (String) -> Unit,
    onAcceptJob: () -> Unit,
    onOpenPayment: () -> Unit,
) {
    val alignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor =
        if (isMine) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant

    when (message.type) {
        "IMAGE" -> {
            val path = message.images.firstOrNull() ?: return
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
                AsyncImage(
                    model = path,
                    contentDescription = "Photo",
                    modifier = Modifier
                        .widthIn(max = 240.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = { onImageClick(path) }),
                    contentScale = ContentScale.Crop
                )
            }
        }
        "JOB_INVITE" -> Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
            // A card I sent, or one already accepted, has no Accept button.
            JobInviteCard(
                job = job,
                canAccept = !isMine,
                alreadyAccepted = alreadyAccepted,
                accepting = accepting,
                onAcceptJob = onAcceptJob
            )
        }
        "PAYMENT_CARD" -> Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
            PaymentCardBubble(job = job, isMine = isMine, onOpenPayment = onOpenPayment)
        }
        "SYSTEM" -> Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        else -> Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
            Text(
                text = message.text,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(bubbleColor)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    // Permanent status line under invite / payment cards so both sides can see
    // the outcome (accepted / released) at a glance.
    when (message.type) {
        "JOB_INVITE" -> if (alreadyAccepted) {
            CardStatusLine("This job invite has been accepted.")
        }
        "PAYMENT_CARD" -> if (job?.paymentStatus == "RELEASED") {
            CardStatusLine("This release payment has been paid out.")
        }
    }
}

/** A permanent small status caption shown under invite / payment cards. */
@Composable
private fun CardStatusLine(text: String) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}

/** A job-invite card showing the job and (for the receiver) an Accept button. */
@Composable
private fun JobInviteCard(
    job: Job?,
    canAccept: Boolean,
    alreadyAccepted: Boolean,
    accepting: Boolean,
    onAcceptJob: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.75f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📌 Job Invite", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(8.dp))
        if (job != null) {
            Text(job.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(
                "You'll receive RM%.2f (after 5%% fee)".format(job.price * 0.95),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text("Loading...", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(12.dp))
        when {
            alreadyAccepted -> Text(
                "✓ Accepted",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            canAccept -> Button(
                onClick = onAcceptJob,
                enabled = !accepting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (accepting) "Accepting..." else "Accept Job")
            }
            else -> Text(
                "You sent this invite",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** A release-payment card the worker can open to settle. */
@Composable
private fun PaymentCardBubble(job: Job?, isMine: Boolean, onOpenPayment: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.75f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(enabled = !isMine, onClick = onOpenPayment)
            .padding(14.dp)
    ) {
        Text("💳 Release Payment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        if (job != null) {
            Text(job.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(
                "RM%.2f will be released (after 5%% fee)".format(job.price * 0.95),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (isMine) "You sent this payment card" else "Open to receive payment for this job",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/** Dialog to pick one of the user's posted jobs via a dropdown + OK/Cancel. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JobPickerDialog(
    jobs: List<Job>,
    title: String,
    emptyMessage: String,
    onPick: (Job) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedJob by remember { mutableStateOf<Job?>(null) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (jobs.isEmpty()) {
                    Text(emptyMessage)
                } else {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedJob?.title ?: "",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            label = { Text(if (selectedJob == null) "Select a job (none)" else "Selected job") },
                            placeholder = { Text("Select a job (none)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            jobs.forEach { job ->
                                DropdownMenuItem(
                                    text = { Text(job.title) },
                                    trailingIcon = { Text("RM%.2f".format(job.price * 0.95)) },
                                    onClick = {
                                        selectedJob = job
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    Text(
                        "Price shown is before platform fee.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedJob?.let(onPick) },
                enabled = selectedJob != null
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** Full settlement page shown from a release-payment card.
 *
 *  Lets the worker confirm receiving payment for an accepted job, and
 *  optionally donate part of it to the MicroJob Fund (worker-side match:
 *  1:1, capped at 2.5% of the job price — md plan §2.1).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettlePaymentDialog(
    job: Job?,
    currentUserId: Long,
    onClaim: (Int) -> Unit,
    workerMatchRate: Double = 0.025,
    onDismiss: () -> Unit,
) {
    var donationInput by remember { mutableStateOf("") }
    var showDonationInfo by remember { mutableStateOf(false) }

    if (job == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Release Payment") },
            text = { Text("Loading job...") },
            confirmButton = { Button(onClick = onDismiss) { Text("Cancel") } }
        )
        return
    }

    // Already claimed → cannot claim again.
    if (job.paymentStatus == "RELEASED") {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Release Payment") },
            text = { Text("This payment has already been claimed.") },
            confirmButton = { Button(onClick = onDismiss) { Text("Close") } }
        )
        return
    }

    // Secure the payment: only the worker this release is meant for can open it.
    val isNotMyPayment = job.workerId != null && job.workerId != currentUserId
    if (isNotMyPayment) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Release Payment") },
            text = { Text("This release payment is not addressed to you.") },
            confirmButton = { Button(onClick = onDismiss) { Text("Close") } }
        )
        return
    }

    val grossPay = job.price * 0.95 // worker keeps 95% (5% platform fee)
    val donation = donationInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val workerMatchCap = job.price * workerMatchRate
    val platformMatch = minOf(donation, workerMatchCap)
    val netPay = grossPay - donation

    // Wrap the settlement UI in a Dialog so it has its own scrim background
    // and does not just float over the chat screen.
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
        // Job posted date
        Text(
            "Posted " + formatSettleDate(job.createdAt),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(job.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        Text(
            "Your payment: RM%.2f (after 5%% platform fee)".format(grossPay),
            style = MaterialTheme.typography.bodyMedium
        )
        HorizontalDivider()

        // Donation row (worker side)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Donate to MicroJob Fund (optional)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showDonationInfo = true }) {
                Icon(Icons.Filled.HelpOutline, contentDescription = "Donation info")
            }
        }
        OutlinedTextField(
            value = donationInput,
            onValueChange = {
                // Donation can't exceed what the worker actually takes home,
                // or their payment would go negative.
                if (it.matches(Regex("^\\d*\\.?\\d{0,2}")) ) {
                    val typed = it.toDoubleOrNull() ?: 0.0
                    if (typed <= grossPay) donationInput = it
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Donation amount (RM)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        Text(
            if (donation > 0)
                "MicroJob matches RM%.2f (1:1 up to 2.5%% of the job). To the fund: RM%.2f"
                    .format(platformMatch, donation + platformMatch)
            else
                "MicroJob matches your donation 1:1, up to 2.5% of the job.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider()
        Text(
            "You'll receive RM%.2f".format(netPay),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )

        // Confirm / receive
        Button(
            onClick = { onClaim(job.id) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Confirm & Receive RM%.2f".format(netPay))
        }
        }
    }

    if (showDonationInfo) {
        AlertDialog(
            onDismissRequest = { showDonationInfo = false },
            title = { Text("Donate to MicroJob Fund") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Donations fund free courses for people who want to learn new skills."
                    )
                    Text(
                        "MicroJob matches your donation 1:1, capped at 2.5% of this job's price."
                    )
                }
            },
            confirmButton = { Button(onClick = { showDonationInfo = false }) { Text("Got it") } }
        )
    }
}

/** Formats an ISO-8601 timestamp as a short date, e.g. "12 Aug". */
private fun formatSettleDate(iso: String): String =
    try {
        val dt = java.time.OffsetDateTime.parse(iso)
        java.time.format.DateTimeFormatter.ofPattern("d MMM").format(dt)
    } catch (e: Exception) {
        ""
    }
