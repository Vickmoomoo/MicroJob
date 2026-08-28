package com.example.microjob.ui.screens.login

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Snackbar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.microjob.viewmodel.AuthUiState
import com.example.microjob.viewmodel.AuthViewModel

/**
 * Login / Register screen. After a successful login the caller navigates
 * back to wherever the user came from; registration switches back to the
 * login form with a confirmation snackbar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    vm: AuthViewModel,
    onBack: () -> Unit,
    onLoggedIn: () -> Unit,
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val currentUser by vm.currentUser.collectAsStateWithLifecycle()
    val username by vm.username.collectAsStateWithLifecycle()
    val password by vm.password.collectAsStateWithLifecycle()
    val confirmPassword by vm.confirmPassword.collectAsStateWithLifecycle()
    val email by vm.email.collectAsStateWithLifecycle()
    val securityQuestion by vm.securityQuestion.collectAsStateWithLifecycle()
    val securityAnswer by vm.securityAnswer.collectAsStateWithLifecycle()
    val isRegisterMode by vm.isRegisterMode.collectAsStateWithLifecycle()
    val isForgotPasswordMode by vm.isForgotPasswordMode.collectAsStateWithLifecycle()

    var showPassword by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // System back gesture: leave forgot-password / register mode first,
    // only leave the screen entirely from the plain login form.
    BackHandler(enabled = isForgotPasswordMode || isRegisterMode) {
        when {
            isForgotPasswordMode -> vm.cancelForgotPassword()
            isRegisterMode -> vm.toggleMode()
        }
    }

    // Navigate back once logged in.
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            onLoggedIn()
        }
    }

    // Registration finished → switch to login mode, show the confirmation
    // snackbar, and only then clear the state (clearing earlier would cancel
    // this effect and the snackbar would never appear).
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Registered) {
            vm.switchToLoginAfterRegister()
            snackbarHostState.showSnackbar("Account created! Please log in.")
            vm.clearUiState()
        } else if (uiState is AuthUiState.PasswordReset) {
            vm.switchToLoginAfterPasswordReset()
            snackbarHostState.showSnackbar("Password reset successfully.")
            vm.clearUiState()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
         snackbarHost = {
             SnackbarHost(snackbarHostState) { data ->
                 Snackbar(
                     snackbarData = data,
                     containerColor = Color.Black,
                     contentColor = Color.White,
                     shape = RoundedCornerShape(0.dp)
                 )
             }
         },
        topBar = {
            TopAppBar(
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),title = {
                    Text(
                        when {
                            isForgotPasswordMode -> "Reset Password"
                            isRegisterMode -> "Create Account"
                            else -> "Login"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when {
                            isForgotPasswordMode -> vm.cancelForgotPassword()
                            isRegisterMode -> vm.toggleMode()
                            else -> onBack()
                        }
                    }) {
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
                .imePadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = when {
                    isForgotPasswordMode -> "Recover your account"
                    isRegisterMode -> "Join MicroJob"
                    else -> "Welcome back"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            if (isRegisterMode) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { vm.email.value = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
            }

            OutlinedTextField(
                value = username,
                onValueChange = { vm.username.value = it },
                label = { Text(if (isForgotPasswordMode) "Username or email" else "Username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = password,
                onValueChange = { vm.password.value = it },
                label = { Text(if (isForgotPasswordMode) "New password" else "Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None
                    else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    TextButton(onClick = { showPassword = !showPassword }) {
                        Text(if (showPassword) "Hide" else "Show")
                    }
                }
            )

            if (isRegisterMode || isForgotPasswordMode) {
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { vm.confirmPassword.value = it },
                    label = { Text("Confirm new password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None
                        else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

            }

            if (isRegisterMode || isForgotPasswordMode) {
                // The same question list is used during registration and recovery.
                SecurityQuestionDropdown(
                    options = vm.securityQuestions,
                    current = securityQuestion,
                    onSelect = { vm.securityQuestion.value = it }
                )

                Text(
                    text = if (isForgotPasswordMode) {
                        "Choose the question you selected when you registered."
                    } else {
                        "This question will be used to reset your password if you forget it."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = securityAnswer,
                    onValueChange = { vm.securityAnswer.value = it },
                    label = { Text("Security answer") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            if (uiState is AuthUiState.Error) {
                Text(
                    text = (uiState as AuthUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = { vm.submit() },
                enabled = uiState !is AuthUiState.Submitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    when {
                        uiState is AuthUiState.Submitting -> "Please wait..."
                        isForgotPasswordMode -> "Reset Password"
                        isRegisterMode -> "Create Account"
                        else -> "Login"
                    }
                )
            }

            if (isForgotPasswordMode) {
                TextButton(
                    onClick = { vm.cancelForgotPassword() },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Back to login")
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    TextButton(
                        onClick = { vm.toggleMode() },
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(
                            if (isRegisterMode) "Already have an account? Login"
                            else "New here? Create an account"
                        )
                    }
                    if (!isRegisterMode) {
                        TextButton(
                            onClick = { vm.startForgotPassword() },
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("Forgot password?")
                        }
                    }
                }
            }
        }
    }
}

/** Dropdown for choosing a security question. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecurityQuestionDropdown(
    options: List<String>,
    current: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = current,
            onValueChange = {},
            readOnly = true,
            label = { Text("Security question") },
            placeholder = { Text("Select a question") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
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
    }
}
