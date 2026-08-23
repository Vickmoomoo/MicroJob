package com.example.microjob.ui.screens.profile

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.microjob.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: ProfileViewModel,
    onBack: () -> Unit,
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val user = state.user ?: return
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var name by remember(user.name) { mutableStateOf(user.name) }
    var email by remember(user.email) { mutableStateOf(user.email) }

    // Change password state
    var showChangePassword by remember { mutableStateOf(false) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var currentPasswordError by remember { mutableStateOf<String?>(null) }
    var newPasswordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(onClick = {
                    // Save any pending changes before going back
                    if (name != user.name) vm.updateName(name)
                    if (email != user.email) vm.updateEmail(email)
                    onBack()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- Account section ---
                Text(
                    text = "Account",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                SettingsField(label = "Name", value = name, onValueChange = { name = it })
                SettingsField(label = "Email", value = email, onValueChange = { email = it })

                HorizontalDivider()

                // --- Security section ---
                Text(
                    text = "Security",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Change Password — expandable
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showChangePassword = !showChangePassword
                            // Reset fields when collapsing
                            if (!showChangePassword) {
                                currentPassword = ""
                                newPassword = ""
                                confirmPassword = ""
                                currentPasswordError = null
                                newPasswordError = null
                                confirmPasswordError = null
                            }
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Change Password",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(visible = showChangePassword) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Current password
                        PasswordField(
                            label = "Current Password",
                            value = currentPassword,
                            onValueChange = {
                                currentPassword = it
                                currentPasswordError = null
                            },
                            showPassword = showCurrentPassword,
                            onToggleShow = { showCurrentPassword = !showCurrentPassword },
                            error = currentPasswordError
                        )

                        // New password
                        PasswordField(
                            label = "New Password",
                            value = newPassword,
                            onValueChange = {
                                newPassword = it
                                newPasswordError = null
                            },
                            showPassword = showNewPassword,
                            onToggleShow = { showNewPassword = !showNewPassword },
                            error = newPasswordError,
                            supportingText = "At least 4 characters"
                        )

                        // Confirm new password
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                confirmPasswordError = null
                            },
                            label = { Text("Confirm New Password") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            isError = confirmPasswordError != null,
                            supportingText = if (confirmPasswordError != null) {
                                { Text(confirmPasswordError!!, color = MaterialTheme.colorScheme.error) }
                            } else null,
                            colors = if (confirmPasswordError != null) {
                                OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.error,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.error
                                )
                            } else {
                                OutlinedTextFieldDefaults.colors()
                            }
                        )

                        // Change button
                        androidx.compose.material3.Button(
                            onClick = {
                                // Validate
                                var hasError = false

                                if (currentPassword.isBlank()) {
                                    currentPasswordError = "Please enter your current password"
                                    hasError = true
                                }

                                if (newPassword.length < 4) {
                                    newPasswordError = "Password must be at least 4 characters"
                                    hasError = true
                                }

                                if (newPassword != confirmPassword) {
                                    confirmPasswordError = "Passwords do not match"
                                    hasError = true
                                }

                                if (!hasError) {
                                    val error = vm.changePassword(currentPassword, newPassword)
                                    if (error != null) {
                                        currentPasswordError = error
                                    } else {
                                        // Success
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Password changed successfully")
                                        }
                                        currentPassword = ""
                                        newPassword = ""
                                        confirmPassword = ""
                                        showChangePassword = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Change Password")
                        }
                    }
                }

                HorizontalDivider()

                // --- App section ---
                Text(
                    text = "App",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Theme", style = MaterialTheme.typography.bodyLarge)
                    Text("System default", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Language", style = MaterialTheme.typography.bodyLarge)
                    Text("English", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                HorizontalDivider()

                // --- About section ---
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Version", style = MaterialTheme.typography.bodyLarge)
                    Text("1.0", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(Modifier.height(32.dp))
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    showPassword: Boolean,
    onToggleShow: () -> Unit,
    error: String?,
    supportingText: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleShow) {
                Icon(
                    imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (showPassword) "Hide password" else "Show password"
                )
            }
        },
        isError = error != null,
        supportingText = when {
            error != null -> {{ Text(error, color = MaterialTheme.colorScheme.error) }}
            supportingText != null -> {{ Text(supportingText, color = MaterialTheme.colorScheme.onSurfaceVariant) }}
            else -> null
        },
        colors = if (error != null) {
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.error,
                unfocusedBorderColor = MaterialTheme.colorScheme.error
            )
        } else {
            OutlinedTextFieldDefaults.colors()
        }
    )
}

@Composable
private fun SettingsField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}
