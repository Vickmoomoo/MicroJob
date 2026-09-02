package com.example.microjob.ui.screens.post

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.microjob.model.MalaysianRegions
import com.example.microjob.model.SampleData
import com.example.microjob.viewmodel.PostJobUiState
import com.example.microjob.viewmodel.PostJobViewModel
import com.example.microjob.viewmodel.postJobViewModelFactory

private val paymentOptions = listOf("TNG eWallet", "Online Banking")

/**
 * Post a Job screen: a form to publish a new job with state/area selection,
 * photo upload, tools/GPS requirements and a simulated escrow payment on submit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostJobScreen(
    onBack: () -> Unit,
    onPublished: (Int) -> Unit,
    vm: PostJobViewModel = viewModel(factory = postJobViewModelFactory()),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val photoUris by vm.photoUris.collectAsStateWithLifecycle()

    // Collect every form field so typing actually re-renders the text fields.
    val title by vm.title.collectAsStateWithLifecycle()
    val description by vm.description.collectAsStateWithLifecycle()
    val price by vm.price.collectAsStateWithLifecycle()
    val category by vm.category.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    val area by vm.area.collectAsStateWithLifecycle()
    val jobType by vm.jobType.collectAsStateWithLifecycle()
    val paymentMethod by vm.paymentMethod.collectAsStateWithLifecycle()
    val bank by vm.bank.collectAsStateWithLifecycle()
    val toolsRequired by vm.toolsRequired.collectAsStateWithLifecycle()
    val donationAmount by vm.donationAmount.collectAsStateWithLifecycle()
    val addressDetail by vm.addressDetail.collectAsStateWithLifecycle()
    val language by vm.language.collectAsStateWithLifecycle()

    var showDonateInfo by remember { mutableStateOf(false) }

    // Photo picker (system Photo Picker, no permission needed on modern Android).
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(vm.maxPhotos)
    ) { uris ->
        if (uris.isNotEmpty()) {
            vm.setPhotos(uris)
        }
    }

    // Navigate back when the publish succeeds.
    LaunchedEffect(uiState) {
        if (uiState is PostJobUiState.Success) {
            onPublished((uiState as PostJobUiState.Success).jobId)
        }
    }

    val inPaymentFlow =
        uiState is PostJobUiState.RedirectingToPayment || uiState is PostJobUiState.PaymentSuccess

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            // No top bar during the payment flow — the user cannot pause/back out.
            if (!inPaymentFlow) {
                TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0), title = { Text("Job Posting") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        when (uiState) {
            PostJobUiState.RedirectingToPayment -> PaymentStatusScreen(
                innerPadding = innerPadding,
                title = "Redirecting to payment app/website...",
                subtitle = "Please wait",
                isSuccess = false
            )
            PostJobUiState.PaymentSuccess -> PaymentStatusScreen(
                innerPadding = innerPadding,
                title = "Payment successful!",
                subtitle = "Publishing your job...",
                isSuccess = true
            )
            else -> androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Photos come first — large square preview of the job photos.
            Text(
                text = "Photos (up to ${vm.maxPhotos})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            PhotoPickerRow(
                photoUris = photoUris,
                onPick = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onRemove = vm::removePhoto,
                canAddMore = photoUris.size < vm.maxPhotos
            )

            OutlinedTextField(
                value = title,
                onValueChange = vm::onTitleChange,
                label = { Text("Job title *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = vm::onDescriptionChange,
                label = { Text("Description *") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = price,
                onValueChange = vm::onPriceChange,
                label = { Text("Job Budget (RM) *") },
                supportingText = {
                    Text("Workers see RM%.2f (after 5%% platform fee)".format(price.toDoubleOrNull()?.times(0.95) ?: 0.0))
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                )
            )

            // Category
            PostDropdown(
                label = "Category *",
                current = category,
                options = SampleData.categories.map { it.name },
                onSelect = { vm.category.value = it }
            )

            // Job type — shown BEFORE the address fields, because On-site jobs
            // need them while Remote jobs do not.
            PostDropdown(
                label = "Job type",
                current = if (jobType == "remote") "Remote" else "On-site",
                options = listOf("Remote", "On-site"),
                onSelect = { selected ->
                    vm.jobType.value = if (selected == "Remote") "remote" else "onsite"
                }
            )

            // Address fields only make sense for On-site jobs.
            if (jobType == "onsite") {
                // State
                PostDropdown(
                    label = "State *",
                    current = state,
                    options = MalaysianRegions.stateNames,
                    onSelect = { selected ->
                        vm.state.value = selected
                        vm.area.value = null // reset area when state changes
                    }
                )

                // Area (only when a state is chosen)
                if (state != null) {
                    PostDropdown(
                        label = "Area *",
                        current = area,
                        options = MalaysianRegions.areasOf(state ?: ""),
                        onSelect = { vm.area.value = it }
                    )
                }

                // Address detail
                OutlinedTextField(
                    value = addressDetail,
                    onValueChange = vm::onAddressDetailChange,
                    label = { Text("Address detail *") },
                    placeholder = { Text("e.g. 88, Jalan Batu Ferringhi") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Payment method
            PostDropdown(
                label = "Payment method",
                current = paymentMethod,
                options = paymentOptions,
                onSelect = { vm.paymentMethod.value = it }
            )

            // Bank selection — only for Online Banking.
            if (paymentMethod == "Online Banking") {
                PostDropdown(
                    label = "Select bank",
                    current = bank,
                    options = vm.bankOptions,
                    onSelect = { vm.bank.value = it }
                )
            }

            // Recommended language — Chinese / English / Malay / Other (matches job detail display).
            PostDropdown(
                label = "Recommended language",
                current = language,
                options = listOf("Chinese", "English", "Malay", "Other"),
                onSelect = { vm.language.value = it }
            )

            // Scheduled date & time (24h) — required
            ScheduledDateTimePicker(
                dateMillis = vm.scheduledDateMillis.collectAsStateWithLifecycle().value,
                hour = vm.scheduledHour.collectAsStateWithLifecycle().value,
                minute = vm.scheduledMinute.collectAsStateWithLifecycle().value,
                onDateChange = { vm.scheduledDateMillis.value = it },
                onTimeChange = { h, m -> vm.scheduledHour.value = h; vm.scheduledMinute.value = m }
            )

            // Voluntary donation to the MicroJob fund (funds free courses) —
            // same style as the GPS row.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Donate to MicroJob Fund", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Your donation funds free courses for everyone",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { showDonateInfo = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = "What is this?",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Donation amount input — always visible; leave empty or 0 to skip.
            OutlinedTextField(
                value = donationAmount,
                onValueChange = vm::onDonationAmountChange,
                label = { Text("Donation amount (optional)") },
                prefix = { Text("RM ") },
                placeholder = { Text("0.00") },
                isError = vm.donation > vm.matchCap,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                )
            )
            if (vm.donation > vm.matchCap) {
                Text(
                    text = "Donation over the match limit (RM ${"%.2f".format(vm.matchCap)} = " +
                        "2.5% of the budget) — the excess is still donated but not matched.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Donation explanation dialog.
            if (showDonateInfo) {
                AlertDialog(
                    onDismissRequest = { showDonateInfo = false },
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
                    confirmButton = {
                        TextButton(onClick = { showDonateInfo = false }) {
                            Text("Got it")
                        }
                    }
                )
            }

            // Tools required
            OutlinedTextField(
                value = toolsRequired,
                onValueChange = vm::onToolsRequiredChange,
                label = { Text("Tools required (optional)") },
                placeholder = { Text("e.g. Cleaning gloves") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Error message
            if (uiState is PostJobUiState.Error) {
                Text(
                    text = (uiState as PostJobUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(8.dp))

            // Price breakdown card: job price + service fee + donation = total held.
            // Values are passed in from collected state so the card recomposes
            // live as the user types the price / donation.
            PriceBreakdownCard(
                price = price.toDoubleOrNull() ?: 0.0,
                donation = donationAmount.toDoubleOrNull() ?: 0.0,
                matchCap = vm.matchCap
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { vm.submit() },
                enabled = uiState !is PostJobUiState.Submitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    if (uiState is PostJobUiState.Submitting) "Publishing..." else "Post Job"
                )
            }
            Text(
                text = "Your payment is held in escrow and released to the worker when the job is completed.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "If the job is pulled down or no one accepts it, the full amount is returned to you.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            }
            }
        }
        }
    }
}

/** Full-screen payment status page shown during the fake payment flow. */
@Composable
private fun PaymentStatusScreen(
    innerPadding: androidx.compose.foundation.layout.PaddingValues,
    title: String,
    subtitle: String,
    isSuccess: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isSuccess) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp)
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.size(56.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Shows the price breakdown aligned with the Create Job design:
 *  Job Budget / Platform Fee / Total Payment, plus the price shown on the
 *  home page (worker's take-home, budget minus 5%). */
@Composable
private fun PriceBreakdownCard(price: Double, donation: Double, matchCap: Double) {
    val serviceFee = price * 0.05            // 5% charged to the poster
    val workerReceive = price * 0.95         // worker keeps 95% of the posted price
    val donationMatch = minOf(donation, matchCap) // platform 1:1 match, capped at 2.5%
    val total = price + serviceFee + donation

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
            Spacer(Modifier.height(6.dp))

            PriceRow("Job Budget", price)
            PriceRow("Platform Fee (5%)", serviceFee)
            if (donation > 0) {
                PriceRow("Donation", donation)
                PriceRow("MicroJob match (1:1)", donationMatch)
            }

            androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Total Payment",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "RM%.2f".format(total),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "ℹ Workers see RM%.2f (their 5%% platform fee is deducted)".format(workerReceive),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** One line of the price breakdown: label on the left, amount on the right. */
@Composable
private fun PriceRow(label: String, amount: Double) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "RM%.2f".format(amount),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * A dropdown styled like an OutlinedTextField (Material 3 ExposedDropdownMenuBox)
 * with a visible trailing arrow so it reads as a selector, not an input.
 * When the list has more than 4 options, the menu is capped at ~4.5 rows so the
 * user can see there is more content to scroll.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostDropdown(
    label: String,
    current: String?,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = current ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            placeholder = { Text("Select...") },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // Cap tall lists so users see that there are more options to scroll.
            if (options.size > 4) {
                Column(
                    modifier = Modifier
                        .heightIn(max = 216.dp) // ~4.5 rows
                        .verticalScroll(rememberScrollState())
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onSelect(option)
                                expanded = false
                            }
                        )
                    }
                }
            } else {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * Large square preview of the picked job photos (same width as the text
 * fields, taller than them). Multiple photos swipe like a pager, with
 * indicator dots; an "add photo" tile is shown when there is room.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoPickerRow(
    photoUris: List<Uri>,
    onPick: () -> Unit,
    onRemove: (Uri) -> Unit,
    canAddMore: Boolean,
) {
    if (photoUris.isEmpty()) {
        // No photos yet: a full-width rectangular "add photo" tile.
        OutlinedButton(
            onClick = onPick,
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text("Add photos")
            }
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { photoUris.size })

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(state = pagerState) { page ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = photoUris[page],
                    contentDescription = "Selected photo ${page + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Remove button on the top-right corner of each photo (no background).
                IconButton(
                    onClick = { onRemove(photoUris[page]) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Remove photo",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Indicator dots
        if (photoUris.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(photoUris.size) { index ->
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

        // Button below the pager. Picking again REPLACES the current selection
        // (the photo picker returns a fresh set), so the label says "Replace".
        if (canAddMore) {
            OutlinedButton(
                onClick = onPick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                Text("Replace photo")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduledDateTimePicker(
    dateMillis: Long?,
    hour: Int?,
    minute: Int?,
    onDateChange: (Long) -> Unit,
    onTimeChange: (Int, Int) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateText = remember(dateMillis) {
        if (dateMillis == null) "" else {
            val fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
            java.time.Instant.ofEpochMilli(dateMillis).atZone(java.time.ZoneId.systemDefault()).format(fmt)
        }
    }
    val timeText = remember(hour, minute) {
        if (hour == null || minute == null) "" else "%02d:%02d".format(hour, minute)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // FIX: Box(clickable) + readOnly TextField never fires because the TextField
        // consumes touch for focus. Make the field disabled so it doesn't intercept,
        // and keep normal colors via OutlinedTextFieldDefaults.colors.
        Box(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }) {
            OutlinedTextField(
                value = dateText,
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("Date *") },
                placeholder = { Text("Select date") },
                trailingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Box(modifier = Modifier.fillMaxWidth().clickable { showTimePicker = true }) {
            OutlinedTextField(
                value = timeText,
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("Time (24h) *") },
                placeholder = { Text("Select time") },
                trailingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showDatePicker) {
        val todayMillis = remember {
            java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.of("UTC")).toInstant().toEpochMilli()
        }
        val state = rememberDatePickerState(
            initialSelectedDateMillis = dateMillis,
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= todayMillis
                override fun isSelectableYear(year: Int): Boolean = year >= java.time.Year.now().value
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onDateChange(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }

    if (showTimePicker) {
        var hourText by remember { mutableStateOf((hour ?: 12).toString()) }
        var minuteText by remember { mutableStateOf((minute ?: 0).toString()) }
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select time (24h)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 2) hourText = it },
                        label = { Text("Hour (0-23)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 2) minuteText = it },
                        label = { Text("Minute (0-59)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val h = hourText.toIntOrNull()?.coerceIn(0, 23) ?: (hour ?: 12)
                    val m = minuteText.toIntOrNull()?.coerceIn(0, 59) ?: (minute ?: 0)
                    onTimeChange(h, m)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
        )
    }
}
