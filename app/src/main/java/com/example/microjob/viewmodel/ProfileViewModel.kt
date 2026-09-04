package com.example.microjob.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.microjob.data.JobRepository
import com.example.microjob.data.RepositoryProvider
import com.example.microjob.data.SessionManager
import com.example.microjob.data.SupabaseClientHolder
import com.example.microjob.data.SupabaseConfig
import com.example.microjob.data.SupabaseJobRepository
import com.example.microjob.data.SupabaseProfileActivityRepository
import com.example.microjob.model.Job
import com.example.microjob.model.ProfileActivity
import com.example.microjob.model.Review
import com.example.microjob.model.User
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

data class ProfileUiState(
    val user: User? = null,
    val isMyProfile: Boolean = false,
    val reviews: List<Review> = emptyList(),
    val postedJobs: List<Job> = emptyList(),
    val acceptedJobs: List<Job> = emptyList(),
    val averageRating: Double? = null,
    val activities: List<ProfileActivity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

class ProfileViewModel(
    application: Application,
    private val repository: JobRepository = RepositoryProvider.jobRepository(application)
) : AndroidViewModel(application) {

    @Suppress("unused")
    constructor(application: Application) : this(application, RepositoryProvider.jobRepository(application))

    private val sessionManager = SessionManager(application)
    private val activityPreferences = application.getSharedPreferences("profile_activities", 0)
    private val activityJson = Json { ignoreUnknownKeys = true }
    private val cloudActivities = if (SupabaseConfig.isConfigured) SupabaseProfileActivityRepository(application) else null

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    /** Ids deleted during this session; a stale in-flight cloud fetch must not re-add them. */
    private val deletedActivityIds = mutableSetOf<Long>()

    /** Loads a profile for the given user id. */
    fun loadProfile(userId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val myId = sessionManager.currentUserId
                val isMyProfile = userId == myId

                val loadedUser = withContext(Dispatchers.IO) { repository.getUser(userId) }
                if (loadedUser == null) {
                    _uiState.update { it.copy(isLoading = false, error = "User not found") }
                    return@launch
                }

                // public_profiles can mask email during a session restore. For
                // my own profile, prefer the registration email from Auth and
                // retain it locally as a fallback across rotation/recreation.
                val authEmail = if (isMyProfile && SupabaseConfig.isConfigured) {
                    try {
                        withContext(Dispatchers.IO) {
                            SupabaseClientHolder.client.auth.currentUserOrNull()?.email
                        }
                    } catch (_: Exception) {
                        null
                    }
                } else {
                    null
                }
                val registeredEmail = authEmail ?: sessionManager.currentUserEmail
                if (!registeredEmail.isNullOrBlank()) {
                    sessionManager.currentUserEmail = registeredEmail
                }
                val user = if (isMyProfile && !registeredEmail.isNullOrBlank()) {
                    loadedUser.copy(email = registeredEmail)
                } else {
                    loadedUser
                }

                val reviews = withContext(Dispatchers.IO) {
                    repository.getReviewsForUser(userId)
                }
                val postedJobs = withContext(Dispatchers.IO) {
                    repository.getPostedJobs(userId)
                }
                val acceptedJobs = withContext(Dispatchers.IO) {
                    repository.getAcceptedJobs(userId)
                }
                val activities = withContext(Dispatchers.IO) {
                    try {
                        cloudActivities?.getForUser(userId) ?: readActivities(userId)
                    } catch (_: Exception) {
                        // Keep the profile usable while the migration is being applied.
                        readActivities(userId)
                    }
                }.sortedByDescending { it.id }

                val avgRating = if (reviews.isNotEmpty()) {
                    reviews.map { it.rating }.average()
                } else null

                _uiState.update {
                    // Cloud fetch may be stale (started before the user posted).
                    // Union it with anything already on screen that the fetched
                    // list doesn't contain yet, instead of overwriting — otherwise
                    // a just-posted activity disappears until re-entering.
                    val cloudIds = activities.map { it.id }.toSet()
                    val deletedIds = deletedActivityIds.toSet()
                    val localOnly = it.activities.filter { a ->
                        a.userId == userId && a.id !in cloudIds && a.id !in deletedIds
                    }
                    val visibleCloud = activities.filterNot { a -> a.id in deletedIds }
                    val merged = (localOnly + visibleCloud).sortedByDescending { a -> a.id }
                    it.copy(
                        user = user,
                        isMyProfile = isMyProfile,
                        reviews = reviews,
                        postedJobs = postedJobs,
                        acceptedJobs = acceptedJobs,
                        averageRating = avgRating,
                        activities = merged,
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

    /** Updates the selected profile photo URI/path. */
    fun updateAvatar(avatarUrl: String) {
        val user = _uiState.value.user ?: return
        viewModelScope.launch {
            try {
                val sharedAvatarUrl = if (avatarUrl.startsWith("content://") && repository is SupabaseJobRepository) {
                    val uri = Uri.parse(avatarUrl)
                    val bytes = getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IllegalArgumentException("Unable to read selected profile photo")
                    val extension = getApplication<Application>().contentResolver.getType(uri)
                        ?.substringAfterLast('/')
                        ?.takeIf { it.isNotBlank() }
                        ?: "jpg"
                    withContext(Dispatchers.IO) {
                        repository.uploadProfileImage(user.id, bytes, extension)
                    }
                } else {
                    avatarUrl
                }
                val updated = user.copy(avatarUrl = sharedAvatarUrl)
                withContext(Dispatchers.IO) { repository.updateUser(updated) }
                _uiState.update { it.copy(user = updated) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Unable to update profile photo") }
            }
        }
    }

    fun addActivity(text: String, photoUri: String) {
        val userId = _uiState.value.user?.id ?: sessionManager.currentUserId ?: return
        if (text.isBlank() && photoUri.isBlank()) return
        val activity = ProfileActivity(
            id = System.currentTimeMillis(),
            userId = userId,
            text = text.trim(),
            photoUri = photoUri,
            createdAt = java.time.OffsetDateTime.now().toString()
        )

        // Update the visible profile before waiting for Supabase or local storage.
        _uiState.update {
            it.copy(activities = listOf(activity) + it.activities, error = null)
        }

        viewModelScope.launch {
            try {
                if (cloudActivities != null) {
                    // A content:// URI only works on this device. Upload it so
                    // everyone (including yourself after restart) loads a public URL.
                    var finalPhoto = activity.photoUri
                    if (finalPhoto.startsWith("content://")) {
                        val uri = Uri.parse(finalPhoto)
                        val bytes = withContext(Dispatchers.IO) {
                            getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        } ?: throw IllegalArgumentException("Unable to read selected activity photo")
                        val extension = getApplication<Application>().contentResolver.getType(uri)
                            ?.substringAfterLast('/')
                            ?.takeIf { it.isNotBlank() }
                            ?: "jpg"
                        finalPhoto = withContext(Dispatchers.IO) {
                            cloudActivities.uploadActivityImage(bytes, extension)
                        }
                    }
                    val savedActivity = cloudActivities.add(activity.copy(photoUri = finalPhoto))
                    _uiState.update { state ->
                        val hasTemp = state.activities.any { it.id == activity.id }
                        val alreadyHasSaved = state.activities.any { it.id == savedActivity.id }
                        val merged = when {
                            hasTemp -> state.activities.map {
                                if (it.id == activity.id) savedActivity else it
                            }
                            // loadProfile wiped the optimistic item while upload was in
                            // flight: prepend so this post is visible without re-entering.
                            alreadyHasSaved -> state.activities
                            else -> listOf(savedActivity) + state.activities
                        }
                        state.copy(
                            activities = merged.sortedByDescending { it.id },
                            error = null
                        )
                    }
                } else {
                    writeActivities(userId, listOf(activity) + readActivities(userId))
                }
                // Keep the optimistic activity visible; the state already reflects the post.
            } catch (e: Exception) {
                // Upload/cloud failed: drop the optimistic item so no broken
                // content:// image stays on screen.
                android.util.Log.e("ProfileVM", "addActivity FAILED", e)
                _uiState.update { state ->
                    state.copy(
                        activities = state.activities.filterNot { it.id == activity.id },
                        error = e.message ?: "Unable to publish activity"
                    )
                }
            }
        }
    }

    fun deleteActivity(activityId: Long) {
        val userId = sessionManager.currentUserId ?: return
        // Remember immediately so a stale loadProfile finishing later can't resurrect it.
        deletedActivityIds.add(activityId)
        viewModelScope.launch {
            try {
                if (cloudActivities != null) {
                    cloudActivities.delete(activityId)
                } else {
                    writeActivities(userId, readActivities(userId).filterNot { it.id == activityId })
                }
                _uiState.update { it.copy(activities = it.activities.filterNot { activity -> activity.id == activityId }) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Unable to delete activity") }
            }
        }
    }

    private fun readActivities(userId: Long): List<ProfileActivity> = try {
        activityPreferences.getString("activities_$userId", null)?.let {
            activityJson.decodeFromString<List<ProfileActivity>>(it)
        } ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    private fun writeActivities(userId: Long, activities: List<ProfileActivity>) {
        activityPreferences.edit()
            .putString("activities_$userId", activityJson.encodeToString(activities))
            .apply()
    }

    /** Saves the editable profile fields in one operation. */
    fun updateProfile(updated: User) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { repository.updateUser(updated) }
                if (updated.id == sessionManager.currentUserId) {
                    sessionManager.currentUserEmail = updated.email
                }
                _uiState.update { it.copy(user = updated, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Unable to update profile") }
            }
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
