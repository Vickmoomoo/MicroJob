package com.example.microjob.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.microjob.data.JobRepository
import com.example.microjob.data.LocalJobRepository
import com.example.microjob.data.SessionManager
import com.example.microjob.model.Job
import com.example.microjob.model.Review
import com.example.microjob.model.User
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ProfileUiState(
    val user: User? = null,
    val isMyProfile: Boolean = false,
    val reviews: List<Review> = emptyList(),
    val postedJobs: List<Job> = emptyList(),
    val acceptedJobs: List<Job> = emptyList(),
    val averageRating: Double? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class ProfileViewModel(
    application: Application,
    private val repository: JobRepository = LocalJobRepository(application)
) : AndroidViewModel(application) {

    @Suppress("unused")
    constructor(application: Application) : this(application, LocalJobRepository(application))

    private val sessionManager = SessionManager(application)

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    /** Loads a profile for the given user id. */
    fun loadProfile(userId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val myId = sessionManager.currentUserId
                val isMyProfile = userId == myId

                val user = withContext(Dispatchers.IO) { repository.getUser(userId) }
                if (user == null) {
                    _uiState.update { it.copy(isLoading = false, error = "User not found") }
                    return@launch
                }

                val localRepo = repository as? LocalJobRepository
                val reviews = withContext(Dispatchers.IO) {
                    localRepo?.getReviewsForUser(userId) ?: emptyList()
                }
                val postedJobs = withContext(Dispatchers.IO) {
                    localRepo?.getPostedJobs(userId) ?: emptyList()
                }
                val acceptedJobs = withContext(Dispatchers.IO) {
                    localRepo?.getAcceptedJobs(userId) ?: emptyList()
                }

                val avgRating = if (reviews.isNotEmpty()) {
                    reviews.map { it.rating }.average()
                } else null

                _uiState.update {
                    it.copy(
                        user = user,
                        isMyProfile = isMyProfile,
                        reviews = reviews,
                        postedJobs = postedJobs,
                        acceptedJobs = acceptedJobs,
                        averageRating = avgRating,
                        isLoading = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /** Updates the user's name. */
    fun updateName(newName: String) {
        val user = _uiState.value.user ?: return
        if (newName.isBlank()) return
        viewModelScope.launch {
            val updated = user.copy(name = newName.trim())
            withContext(Dispatchers.IO) { repository.updateUser(updated) }
            _uiState.update { it.copy(user = updated) }
            // Also update session if it's my profile
            if (_uiState.value.isMyProfile) {
                sessionManager.currentUserId?.let { loadProfile(it) }
            }
        }
    }

    /** Updates the user's username. */
    fun updateUsername(newUsername: String) {
        val user = _uiState.value.user ?: return
        if (newUsername.isBlank()) return
        viewModelScope.launch {
            val updated = user.copy(username = newUsername.trim())
            withContext(Dispatchers.IO) { repository.updateUser(updated) }
            _uiState.update { it.copy(user = updated) }
        }
    }

    /** Updates the user's bio. */
    fun updateBio(newBio: String) {
        val user = _uiState.value.user ?: return
        viewModelScope.launch {
            val updated = user.copy(bio = newBio.trim())
            withContext(Dispatchers.IO) { repository.updateUser(updated) }
            _uiState.update { it.copy(user = updated) }
        }
    }

    /** Updates the user's email. */
    fun updateEmail(newEmail: String) {
        val user = _uiState.value.user ?: return
        viewModelScope.launch {
            val updated = user.copy(email = newEmail.trim())
            withContext(Dispatchers.IO) { repository.updateUser(updated) }
            _uiState.update { it.copy(user = updated) }
        }
    }

    /** Persists the editable fields shown on User Details in one update. */
    fun updateUserDetails(
        name: String,
        username: String,
        bio: String,
        region: String,
        skills: List<String>,
        email: String,
        birthdate: String,
        phoneNumber: String,
    ) {
        val user = _uiState.value.user ?: return
        if (name.isBlank() || username.isBlank()) return
        viewModelScope.launch {
            val updated = user.copy(
                name = name.trim(),
                username = username.trim(),
                bio = bio.trim(),
                region = region.trim(),
                skills = skills.map(String::trim).filter(String::isNotBlank).distinct(),
                email = email.trim(),
                birthdate = birthdate.trim(),
                phoneNumber = phoneNumber.trim()
            )
            withContext(Dispatchers.IO) { repository.updateUser(updated) }
            _uiState.update { it.copy(user = updated) }
        }
    }

    /** Updates the selected profile photo URI/path. */
    fun updateAvatar(avatarUrl: String) {
        val user = _uiState.value.user ?: return
        viewModelScope.launch {
            val updated = user.copy(avatarUrl = avatarUrl)
            withContext(Dispatchers.IO) { repository.updateUser(updated) }
            _uiState.update { it.copy(user = updated) }
        }
    }

    fun updatePrivacy(showEmail: Boolean, showBirthdate: Boolean, showPhoneNumber: Boolean, showAvatar: Boolean) {
        val user = _uiState.value.user ?: return
        viewModelScope.launch {
            val updated = user.copy(
                showEmail = showEmail,
                showBirthdate = showBirthdate,
                showPhoneNumber = showPhoneNumber,
                showAvatar = showAvatar
            )
            withContext(Dispatchers.IO) { repository.updateUser(updated) }
            _uiState.update { it.copy(user = updated) }
        }
    }

    /**
     * Changes the user's password.
     * Returns null on success, or an error message on failure.
     */
    fun changePassword(oldPassword: String, newPassword: String): String? {
        val user = _uiState.value.user ?: return "User not found"
        if (oldPassword != user.password) return "Current password is incorrect"
        if (oldPassword == newPassword) return "New password cannot be the same as current password"
        if (newPassword.length < 4) return "Password must be at least 4 characters"
        viewModelScope.launch {
            val updated = user.copy(password = newPassword)
            withContext(Dispatchers.IO) { repository.updateUser(updated) }
            _uiState.update { it.copy(user = updated) }
        }
        return null
    }

    @Suppress("unused")
    /** Returns the current logged-in user id, or null. */
    fun myId(): Long? = sessionManager.currentUserId
}
