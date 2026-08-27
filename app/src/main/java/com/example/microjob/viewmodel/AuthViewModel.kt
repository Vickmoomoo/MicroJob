package com.example.microjob.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.microjob.data.JobRepository
import com.example.microjob.data.LocalJobRepository
import com.example.microjob.data.PasswordResetResult
import com.example.microjob.data.SessionManager
import com.example.microjob.model.SampleData
import com.example.microjob.model.User
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** UI state of the auth screen. */
sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Submitting : AuthUiState
    data class Error(val message: String) : AuthUiState
    /** Registration finished successfully (switch back to login mode). */
    data object Registered : AuthUiState
    /** Password reset finished successfully (switch back to login mode). */
    data object PasswordReset : AuthUiState
}

class AuthViewModel(
    application: Application,
    private val repository: JobRepository = LocalJobRepository(application),
    private val session: SessionManager = SessionManager(application)
) : AndroidViewModel(application) {

    @Suppress("unused")
    constructor(application: Application) : this(application, LocalJobRepository(application))

    /** Form fields. */
    val username = MutableStateFlow("")
    val password = MutableStateFlow("")
    val confirmPassword = MutableStateFlow("")
    val email = MutableStateFlow("")
    val securityQuestion = MutableStateFlow("")
    val securityAnswer = MutableStateFlow("")
    val isRegisterMode = MutableStateFlow(false)
    val isForgotPasswordMode = MutableStateFlow(false)

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /** The currently logged-in user (re-read on every login/logout). */
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    @Suppress("unused")
    val isLoggedIn: Boolean get() = session.isLoggedIn

    /** Security questions reloaded every time the screen opens. */
    val securityQuestions: List<String> get() = SampleData.securityQuestions

    /** Loads the current user from the session if any. */
    fun loadCurrentUser() {
        val id = session.currentUserId ?: return
        viewModelScope.launch {
            try {
                val user = withContext(Dispatchers.IO) { repository.getUser(id) }
                if (user != null) {
                    _currentUser.value = user
                } else {
                    // The session points at a user that no longer exists —
                    // clear the stale session so login checks work again.
                    session.currentUserId = null
                    _currentUser.value = null
                }
            } catch (_: Exception) {
                // session may point at a deleted user; ignore
                _currentUser.value = null
            }
        }
    }

    fun submit() {
        val error = when {
            username.value.isBlank() -> "Please enter a username."
            !isForgotPasswordMode.value && username.value.contains(" ") -> "Username cannot contain spaces."
            password.value.isBlank() -> "Please enter a password."
            isForgotPasswordMode.value && confirmPassword.value != password.value -> "Passwords do not match."
            isForgotPasswordMode.value && securityQuestion.value.isBlank() ->
                "Please choose a security question."
            isForgotPasswordMode.value && securityAnswer.value.isBlank() ->
                "Please answer the security question."
            !isRegisterMode.value -> null
            confirmPassword.value != password.value -> "Passwords do not match."
            !isValidEmail(email.value) -> "Please enter a valid email address."
            securityQuestion.value.isBlank() -> "Please choose a security question."
            securityAnswer.value.isBlank() -> "Please answer the security question."
            else -> null
        }
        if (error != null) {
            _uiState.value = AuthUiState.Error(error)
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Submitting
            try {
                if (isForgotPasswordMode.value) {
                    val reset = withContext(Dispatchers.IO) {
                        repository.resetPassword(
                            usernameOrEmail = username.value,
                            securityQuestion = securityQuestion.value,
                            securityAnswer = securityAnswer.value,
                            newPassword = password.value
                        )
                    }
                    when (reset) {
                        PasswordResetResult.SUCCESS -> {
                            _uiState.value = AuthUiState.PasswordReset
                        }
                        PasswordResetResult.INVALID_DETAILS -> {
                            throw IllegalArgumentException(
                                "The account details or security answer are incorrect."
                            )
                        }
                        PasswordResetResult.SAME_AS_CURRENT_PASSWORD -> {
                            throw IllegalArgumentException(
                                "New password cannot be the same as your current password."
                            )
                        }
                    }
                } else if (isRegisterMode.value) {
                    // Register, then switch back to login mode with feedback.
                    withContext(Dispatchers.IO) {
                        repository.registerUser(
                            username = username.value.trim(),
                            password = password.value,
                            email = email.value.trim(),
                            securityQuestion = securityQuestion.value,
                            securityAnswer = securityAnswer.value.trim()
                        )
                    }
                    _uiState.value = AuthUiState.Registered
                } else {
                    val user = withContext(Dispatchers.IO) {
                        repository.login(username.value.trim(), password.value)
                            ?: throw IllegalArgumentException("Wrong username or password.")
                    }
                    session.currentUserId = user.id
                    _currentUser.value = user
                    _uiState.value = AuthUiState.Idle
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Login failed.")
            }
        }
    }

    /**
     * Called after a successful registration to reset the form for login.
     * NOTE: does NOT touch [uiState] — the caller (LoginScreen) resets it
     * AFTER the confirmation snackbar has been shown, otherwise changing the
     * state here would restart the LaunchedEffect and cancel the snackbar.
     */
    fun switchToLoginAfterRegister() {
        isRegisterMode.value = false
        confirmPassword.value = ""
        email.value = ""
        securityQuestion.value = ""
        securityAnswer.value = ""
        password.value = ""
    }

    /** Returns to the login form after the password reset confirmation. */
    fun switchToLoginAfterPasswordReset() {
        isForgotPasswordMode.value = false
        username.value = ""
        password.value = ""
        confirmPassword.value = ""
        securityQuestion.value = ""
        securityAnswer.value = ""
    }

    /** Opens the password recovery form. */
    fun startForgotPassword() {
        isForgotPasswordMode.value = true
        isRegisterMode.value = false
        username.value = ""
        password.value = ""
        confirmPassword.value = ""
        securityQuestion.value = ""
        securityAnswer.value = ""
        _uiState.value = AuthUiState.Idle
    }

    /** Leaves password recovery without changing the account. */
    fun cancelForgotPassword() {
        isForgotPasswordMode.value = false
        username.value = ""
        password.value = ""
        confirmPassword.value = ""
        securityQuestion.value = ""
        securityAnswer.value = ""
        _uiState.value = AuthUiState.Idle
    }

    /** Clears any pending auth state (called after the registered snackbar). */
    fun clearUiState() {
        _uiState.value = AuthUiState.Idle
    }

    /**
     * Switches between login and register mode and clears every form field,
     * so switching modes never carries leftover input into the other form.
     */
    fun toggleMode() {
        isRegisterMode.value = !isRegisterMode.value
        isForgotPasswordMode.value = false
        username.value = ""
        password.value = ""
        confirmPassword.value = ""
        email.value = ""
        securityQuestion.value = ""
        securityAnswer.value = ""
        _uiState.value = AuthUiState.Idle
    }

    fun logout() {
        session.logout()
        _currentUser.value = null
        // Clear every form field so the login/register screens start empty
        // the next time they are opened.
        username.value = ""
        password.value = ""
        confirmPassword.value = ""
        email.value = ""
        securityQuestion.value = ""
        securityAnswer.value = ""
        isRegisterMode.value = false
        isForgotPasswordMode.value = false
        _uiState.value = AuthUiState.Idle
    }

    /**
     * Email must contain an "@" and a "." after it (basic check),
     * plus the stricter Android email pattern.
     */
    private fun isValidEmail(email: String): Boolean {
        val hasAt = email.contains("@")
        val dotAfterAt = email.indexOf('@') in 0 until email.lastIndexOf('.')
        return hasAt && dotAfterAt && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
