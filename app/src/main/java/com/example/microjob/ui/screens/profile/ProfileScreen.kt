package com.example.microjob.ui.screens.profile

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.microjob.model.User
import com.example.microjob.viewmodel.ProfileViewModel
import com.example.microjob.viewmodel.ProfileUiState

/**
 * Profile screen — works for both "my profile" (from bottom bar, no back button)
 * and "public profile" (from job detail / chat, with back button).
 *
 * When [onBack] is null → bottom bar is visible (main tab, no TopAppBar).
 * When [onBack] is provided → full-screen with TopAppBar + back arrow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: Long,
    vm: ProfileViewModel,
    onBack: (() -> Unit)? = null,
    onNavigateToSettings: () -> Unit,
    onNavigateToPostedJobs: () -> Unit,
    onNavigateToAcceptedJobs: () -> Unit,
    onNavigateToMyJobs: () -> Unit,
    onNavigateToReviews: () -> Unit,
    onNavigateToCertificates: () -> Unit,
    onNavigateToSocialImpact: () -> Unit,
    onNavigateToMiniGames: () -> Unit,
    onNavigateToPointsHistory: () -> Unit,
    onNavigateToChat: (Long) -> Unit,
    onLogout: () -> Unit,
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(userId) {
        vm.loadProfile(userId)
    }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (state.error != null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = state.error!!, color = MaterialTheme.colorScheme.error)
        }
        return
    }

    val user = state.user ?: return
    val isMyProfile = state.isMyProfile

    var isEditingBio by remember { mutableStateOf(false) }
    var bioText by remember(user.bio) { mutableStateOf(user.bio) }
    var showEditUsernameDialog by remember { mutableStateOf(false) }

    val content: @Composable () -> Unit = {
        ProfileContent(
            user = user,
            isMyProfile = isMyProfile,
            state = state,
            bioText = bioText,
            onBioTextChange = { bioText = it },
            isEditingBio = isEditingBio,
            onEditingBioChange = { isEditingBio = it },
            showEditUsernameDialog = showEditUsernameDialog,
            onShowEditUsernameDialog = { showEditUsernameDialog = it },
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToPostedJobs = onNavigateToPostedJobs,
            onNavigateToAcceptedJobs = onNavigateToAcceptedJobs,
            onNavigateToMyJobs = onNavigateToMyJobs,
            onNavigateToReviews = onNavigateToReviews,
            onNavigateToCertificates = onNavigateToCertificates,
            onNavigateToSocialImpact = onNavigateToSocialImpact,
            onNavigateToMiniGames = onNavigateToMiniGames,
            onNavigateToPointsHistory = onNavigateToPointsHistory,
            onNavigateToChat = onNavigateToChat,
            onLogout = onLogout,
            onUpdateBio = { vm.updateBio(it) },
            onUpdateUsername = { vm.updateUsername(it) }
        )
    }

    if (onBack != null) {
        // Public profile: full-screen with TopAppBar + back button
        Scaffold(
            topBar = {
                TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0), title = { Text("User Profile") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                content()
            }
        }
    } else {
        // My profile: no TopAppBar, bottom bar is the exit
        content()
    }
}

// ==================== Profile content (shared by both modes) ====================

@Suppress("UNUSED_PARAMETER")
@Composable
private fun ProfileContent(
    user: User,
    isMyProfile: Boolean,
    state: ProfileUiState,
    bioText: String,
    onBioTextChange: (String) -> Unit,
    isEditingBio: Boolean,
    onEditingBioChange: (Boolean) -> Unit,
    showEditUsernameDialog: Boolean,
    onShowEditUsernameDialog: (Boolean) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPostedJobs: () -> Unit,
    onNavigateToAcceptedJobs: () -> Unit,
    onNavigateToMyJobs: () -> Unit,
    onNavigateToReviews: () -> Unit,
    onNavigateToCertificates: () -> Unit,
    onNavigateToSocialImpact: () -> Unit,
    onNavigateToMiniGames: () -> Unit,
    onNavigateToPointsHistory: () -> Unit,
    onNavigateToChat: (Long) -> Unit,
    onLogout: () -> Unit,
    onUpdateBio: (String) -> Unit,
    onUpdateUsername: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- Top section: Avatar + Username + Email + Rating + Badge ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .then(
                        if (isMyProfile) Modifier.clickable { /* TODO: avatar picker */ }
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(Modifier.width(16.dp))

            // Username + Email + Rating
            Column(modifier = Modifier.weight(1f)) {
                // Username — tap to edit (my profile) or tap to chat (public profile)
                Text(
                    text = user.username,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = if (isMyProfile) {
                        Modifier.clickable { onShowEditUsernameDialog(true) }
                    } else {
                        Modifier.clickable { onNavigateToChat(user.id) }
                    }
                )

                // Email
                Text(
                    text = user.email.ifBlank { "No email" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Rating
                Spacer(Modifier.height(4.dp))
                if (state.averageRating != null) {
                    Text(
                        text = "\u2B50 %.1f".format(state.averageRating),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = "No reviews yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Badge icon (top-right)
            IconButton(onClick = { onNavigateToPointsHistory() }) {
                Icon(
                    imageVector = Icons.Filled.Badge,
                    contentDescription = "Badge",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // --- Bio card (inline edit) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bio",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isMyProfile && !isEditingBio) {
                        IconButton(
                            onClick = { onEditingBioChange(true) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit bio",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))

                if (isEditingBio) {
                    // Inline editing mode
                    OutlinedTextField(
                        value = bioText,
                        onValueChange = onBioTextChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Write something about yourself...") },
                        minLines = 2,
                        maxLines = 5
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            onBioTextChange(user.bio)
                            onEditingBioChange(false)
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = {
                            onUpdateBio(bioText)
                            onEditingBioChange(false)
                        }) {
                            Icon(Icons.Filled.Check, contentDescription = "Save",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                } else {
                    // Display mode
                    Text(
                        text = user.bio.ifBlank { "No bio yet." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (user.bio.isNotBlank()) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // --- Features list (Posted + Accepted combined into My Jobs) ---
        if (isMyProfile) {
            HorizontalDivider()
            ProfileMenuItem(Icons.Filled.Work, "My Jobs") { onNavigateToMyJobs() }
            ProfileMenuItem(Icons.Filled.Star, "Reviews") { onNavigateToReviews() }
            ProfileMenuItem(Icons.Filled.School, "Certificates") { onNavigateToCertificates() }
            ProfileMenuItem(Icons.Filled.Favorite, "Social Impact") { onNavigateToSocialImpact() }
            ProfileMenuItem(Icons.Filled.Star, "Mini Games") { onNavigateToMiniGames() }
            ProfileMenuItem(Icons.Filled.Settings, "Settings") { onNavigateToSettings() }
        } else {
            ProfileMenuItem(Icons.Filled.Star, "Reviews") { onNavigateToReviews() }
            ProfileMenuItem(Icons.Filled.School, "Certificates") { onNavigateToCertificates() }
            ProfileMenuItem(Icons.Filled.Favorite, "Social Impact") { onNavigateToSocialImpact() }
        }

        // --- Logout (my profile only) ---
        if (isMyProfile) {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Logout")
            }
        }
    }

    // --- Edit Username Dialog ---
    if (showEditUsernameDialog) {
        EditFieldDialog(
            title = "Edit Username",
            initialValue = user.username,
            onConfirm = { onUpdateUsername(it); onShowEditUsernameDialog(false) },
            onDismiss = { onShowEditUsernameDialog(false) }
        )
    }
}

// ==================== Reusable components ====================

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Suppress("SameParameterValue")
@Composable
private fun EditFieldDialog(
    title: String,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initialValue) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { onConfirm(text) }) {
                Text("Save")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
