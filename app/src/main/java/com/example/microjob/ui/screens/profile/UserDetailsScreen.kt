package com.example.microjob.ui.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import com.example.microjob.model.User
import com.example.microjob.viewmodel.ProfileViewModel
import com.example.microjob.viewmodel.ProfileUiState
import coil.compose.AsyncImage
import java.time.format.DateTimeFormatter
import java.time.OffsetDateTime
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val predefinedSkills = listOf(
    "Cleaning", "Delivery", "Digital Marketing", "Graphic Design", "Gardening",
    "Home Repairs", "Moving", "Tutoring", "Cooking", "Photography", "Pet Care",
    "IT & Programming"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailsScreen(vm: ProfileViewModel, onBack: () -> Unit) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val user = state.user ?: return
    var editing by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        vm.loadProfile(user.id)
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val profilePhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Some document providers do not support persistable permissions.
            }
            vm.updateAvatar(uri.toString())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("User Details") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (state.isMyProfile && !editing) {
                        IconButton(onClick = { editing = true }) { Icon(Icons.Filled.Edit, "Edit profile") }
                    }
                }
            )
        }
    ) { padding ->
        if (editing && state.isMyProfile) {
            EditProfileContent(
                user = user,
                modifier = Modifier.padding(padding),
                onCancel = { editing = false },
                onSave = { vm.updateProfile(it); editing = false }
            )
        } else {
             ViewProfileContent(
                 user = user,
                 state = state,
                 modifier = Modifier.padding(padding),
                 onChangePhoto = if (state.isMyProfile) {
                     { profilePhotoPicker.launch(arrayOf("image/*")) }
                 } else null,
                 onPublishActivity = vm::addActivity,
                 onDeleteActivity = vm::deleteActivity
            )
        }
    }
}

@Composable
private fun ViewProfileContent(
    user: User,
    state: ProfileUiState,
    modifier: Modifier,
    onChangePhoto: (() -> Unit)?,
    onPublishActivity: (String, String) -> Unit,
    onDeleteActivity: (Long) -> Unit
) {
    val visibleEmail = state.isMyProfile || user.showEmail
    val visibleBirthdate = state.isMyProfile || user.showBirthdate
    val visiblePhone = state.isMyProfile || user.showPhoneNumber
    val completedJobs = state.acceptedJobs.count { it.status == "COMPLETED" }
    val completeness = profileCompleteness(user)

    Column(modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).background(MaterialTheme.colorScheme.background)) {
         Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
             ProfileAvatar(user, onChangePhoto)
            Spacer(Modifier.height(10.dp))
            Text(user.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("@${user.username}", color = MaterialTheme.colorScheme.primary)
            if (user.bio.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(user.bio, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (user.region.isNotBlank()) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(3.dp))
                    Text(user.region, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Profile progress", fontWeight = FontWeight.Bold)
                    Text("${completeness.second}/${completeness.third}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator({ completeness.first }, Modifier.fillMaxWidth().height(8.dp))
                Text("Add more details to build trust with the community.", style = MaterialTheme.typography.bodySmall)
            }
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Metric("Rating", state.averageRating?.let { "%.1f".format(it) } ?: "--")
            Metric("Completed", completedJobs.toString())
            Metric("Member since", memberSince(user.createdAt))
        }
        HorizontalDivider()

        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("About ${user.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            InfoRow(Icons.Filled.Person, "Full name", user.name)
            InfoRow(Icons.Filled.Person, "Username", "@${user.username}")
            InfoRow(Icons.Filled.Work, "Skills", user.skills.joinToString().ifBlank { "No skills added" })
            InfoRow(Icons.Filled.Email, "Email", if (visibleEmail) user.email.ifBlank { "Not added" } else "Hidden by user")
            InfoRow(Icons.Filled.CalendarMonth, "Birthdate", if (visibleBirthdate) user.birthdate.ifBlank { "Not added" } else "Hidden by user")
            InfoRow(Icons.Filled.Phone, "Phone number", if (visiblePhone) user.phoneNumber.ifBlank { "Not added" } else "Hidden by user")
        }
        HorizontalDivider()

        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Activity", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (state.isMyProfile) ActivityComposer(onPublish = onPublishActivity)
            if (state.activities.isEmpty()) Text("No activities yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            state.activities.forEach { activity ->
                ActivityCard(activity.text, activity.photoUri, activity.createdAt, user.avatarUrl, user.name, state.isMyProfile) {
                    onDeleteActivity(activity.id)
                }
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditProfileContent(user: User, modifier: Modifier, onCancel: () -> Unit, onSave: (User) -> Unit) {
    var name by remember { mutableStateOf(user.name) }
    var username by remember { mutableStateOf(user.username) }
    var bio by remember { mutableStateOf(user.bio) }
    var region by remember { mutableStateOf(user.region) }
    var email by remember { mutableStateOf(user.email) }
    var birthdate by remember { mutableStateOf(user.birthdate) }
    var phone by remember { mutableStateOf(user.phoneNumber) }
    var skills by remember { mutableStateOf(user.skills) }
    var customSkill by remember { mutableStateOf("") }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Edit profile", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        ProfileField("Full name", name) { name = it }
        ProfileField("Username", username) { username = it }
        ProfileField("Bio", bio, minLines = 3) { bio = it }
        ProfileField("Region", region) { region = it }
        ProfileField("Email", email) { email = it }
        ProfileField("Birthdate", birthdate, placeholder = "For example, 12 Aug 2000") { birthdate = it }
        ProfileField("Phone number", phone) { phone = it }

        Text("Skills", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            predefinedSkills.forEach { skill ->
                FilterChip(
                    selected = skill in skills,
                    onClick = { skills = if (skill in skills) skills - skill else skills + skill },
                    label = { Text(skill) }
                )
            }
            skills.filterNot { it in predefinedSkills }.forEach { skill ->
                FilterChip(selected = true, onClick = { skills = skills - skill }, label = { Text(skill) })
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedTextField(customSkill, { customSkill = it }, label = { Text("Custom skill") }, modifier = Modifier.weight(1f), singleLine = true)
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = {
                val value = customSkill.trim()
                if (value.isNotEmpty() && value !in skills) skills = skills + value
                customSkill = ""
            }) { Text("Add") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            OutlinedButton(onClick = onCancel) { Text("Cancel") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                onSave(user.copy(name = name.trim(), username = username.trim(), bio = bio.trim(), region = region.trim(), skills = skills, email = email.trim(), birthdate = birthdate.trim(), phoneNumber = phone.trim()))
            }, enabled = name.isNotBlank() && username.isNotBlank()) { Text("Save") }
        }
    }
}

@Composable
private fun ActivityComposer(onPublish: (String, String) -> Unit) {
    var text by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf("") }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) photoUri = uri.toString()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Share an update", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                scope.launch {
                                    delay(200)
                                    bringIntoViewRequester.bringIntoView()
                                }
                            }
                        },
                    label = { Text("What's on your mind?") },
                    placeholder = { Text("Share something with your community") },
                    shape = RoundedCornerShape(14.dp),
                    minLines = 3
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    OutlinedButton(onClick = { photoPicker.launch(arrayOf("image/*")) }) {
                    Icon(Icons.Filled.Image, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (photoUri.isBlank()) "Add photo" else "Photo selected")
                    }
                    Button(onClick = { onPublish(text, photoUri); text = ""; photoUri = "" }, enabled = text.isNotBlank() || photoUri.isNotBlank()) { Text("Post") }
                }
                if (photoUri.isNotBlank()) {
                    AsyncImage(photoUri, "Selected activity photo", Modifier.fillMaxWidth().height(140.dp), contentScale = ContentScale.Crop)
                }
        }
    }
}

@Composable
private fun ActivityCard(
    text: String,
    photoUri: String,
    createdAt: String,
    avatarUrl: String,
    userName: String,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    var avatarLoadFailed by remember(avatarUrl) { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                if (avatarUrl.isNotBlank() && !avatarLoadFailed) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Profile photo",
                        modifier = Modifier.size(38.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        onError = { avatarLoadFailed = true }
                    )
                } else {
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Text(
                            userName.firstOrNull()?.uppercase() ?: "?",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Activity update", fontWeight = FontWeight.SemiBold)
                    Text(
                        formatActivityDate(createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (canDelete) {
                    IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Delete activity") }
                }
            }
            if (photoUri.isNotBlank()) {
                AsyncImage(photoUri, "Activity photo", Modifier.fillMaxWidth().height(180.dp), contentScale = ContentScale.Crop)
            }
            if (text.isNotBlank()) Text(text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ProfileAvatar(user: User, onChangePhoto: (() -> Unit)?) {
    var avatarLoadFailed by remember(user.avatarUrl) { mutableStateOf(false) }
    Box(
        Modifier
            .size(104.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .then(if (onChangePhoto != null) Modifier.clickable(onClick = onChangePhoto) else Modifier),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        if (user.avatarUrl.isNotBlank() && !avatarLoadFailed) {
            AsyncImage(
                user.avatarUrl,
                "Profile photo",
                Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = { avatarLoadFailed = true }
            )
        } else {
            Text(user.name.firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String, minLines: Int = 1, placeholder: String? = null, onValueChange: (String) -> Unit) {
    OutlinedTextField(value, onValueChange, modifier = Modifier.fillMaxWidth(), label = { Text(label) }, placeholder = placeholder?.let { { Text(it) } }, minLines = minLines, singleLine = minLines == 1)
}

@Composable
private fun DetailCard(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, modifier = Modifier.width(105.dp)) {
        Text(value, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun profileCompleteness(user: User): Triple<Float, Int, Int> {
    val fields = listOf(user.name, user.username, user.bio, user.region, user.skills.joinToString(), user.email, user.birthdate, user.phoneNumber)
    val completed = fields.count { it.isNotBlank() }
    return Triple(completed.toFloat() / fields.size, completed, fields.size)
}

private fun memberSince(createdAt: String): String = try {
    OffsetDateTime.parse(createdAt).year.toString()
} catch (_: Exception) {
    createdAt.take(4).ifBlank { "Unknown" }
}

private fun formatActivityDate(createdAt: String): String = try {
    OffsetDateTime.parse(createdAt).format(
        DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.getDefault())
    )
} catch (_: Exception) {
    "Just now"
}
