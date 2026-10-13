package com.example.microjob.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.microjob.data.JobRepository
import com.example.microjob.data.RepositoryProvider
import com.example.microjob.data.SessionManager
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

data class ReviewFormState(
    val rating: Float = 5f,
    val comment: String = "",
    val isSubmitting: Boolean = false,
    val submitted: Boolean = false,
    val error: String? = null,
)

data class ReviewsListState(
    val reviews: List<Review> = emptyList(),
    val users: Map<Long, User> = emptyMap(),
    val myReview: Review? = null,
    val isLoading: Boolean = false,
)

class ReviewViewModel(
    application: Application,
    private val repository: JobRepository = RepositoryProvider.jobRepository(application)
) : AndroidViewModel(application) {

    @Suppress("unused")
    constructor(application: Application) : this(application, RepositoryProvider.jobRepository(application))

    private val sessionManager = SessionManager(application)

    private val _formState = MutableStateFlow(ReviewFormState())
    val formState: StateFlow<ReviewFormState> = _formState.asStateFlow()

    private val _listState = MutableStateFlow(ReviewsListState())
    val listState: StateFlow<ReviewsListState> = _listState.asStateFlow()

    /** Loads reviews for a user and checks if the current user has already reviewed for a specific job. */
    fun loadReviews(userId: Long, jobId: Long? = null) {
        viewModelScope.launch {
            _listState.update { it.copy(isLoading = true) }
            try {
                val reviews = withContext(Dispatchers.IO) {
                    repository.getReviewsForUser(userId)
                }

                // Load reviewer user names
                val userIds = reviews.map { it.reviewerUserId }.toSet()
                val users = mutableMapOf<Long, User>()
                for (id in userIds) {
                    val user = withContext(Dispatchers.IO) { repository.getUser(id) }
                    if (user != null) users[id] = user
                }

                // Check if current user already reviewed for this job
                val myId = sessionManager.currentUserId
                val myReview = if (myId != null && jobId != null) {
                    reviews.find { it.reviewerUserId == myId && it.jobId == jobId }
                } else null

                _listState.update {
                    it.copy(
                        reviews = reviews,
                        users = users,
                        myReview = myReview,
                        isLoading = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _listState.update { it.copy(isLoading = false) }
            }
        }
    }

    /** Loads an existing review for editing by looking it up from the repository. */
    fun loadReviewForEditById(userId: Long, reviewId: Long) {
        viewModelScope.launch {
            try {
                val reviews = withContext(Dispatchers.IO) {
                    repository.getReviewsForUser(userId)
                }
                val existing = reviews.find { it.id == reviewId }
                if (existing != null) {
                    loadReviewForEdit(existing)
                }
            } catch (_: Exception) {
                // ignore
            }
        }
    }

    /** Loads an existing review for editing. */
    fun loadReviewForEdit(review: Review) {
        _formState.update {
            it.copy(
                rating = review.rating,
                comment = review.comment,
                submitted = false,
                error = null
            )
        }
    }

    /** Resets the form state. */
    fun resetForm() {
        _formState.update { ReviewFormState() }
    }

    fun onRatingChange(rating: Float) {
        _formState.update { it.copy(rating = rating.coerceIn(0.5f, 5f)) }
    }

    fun onCommentChange(comment: String) {
        _formState.update { it.copy(comment = comment) }
    }

    /** Submits a new review or updates an existing one. */
    fun submitReview(
        reviewedUserId: Long,
        jobId: Long?,
        existingReviewId: Long? = null,
    ) {
        val state = _formState.value
        if (state.rating < 0.5f || state.rating > 5f) {
            _formState.update { it.copy(error = "Rating must be between 0.5 and 5") }
            return
        }

        viewModelScope.launch {
            _formState.update { it.copy(isSubmitting = true, error = null) }
            try {
                val myId = sessionManager.currentUserId ?: throw IllegalStateException("Not logged in")

                if (existingReviewId != null) {
                    // Update existing review
                    val reviews = withContext(Dispatchers.IO) {
                        repository.getReviewsForUser(reviewedUserId)
                    }
                    val existing = reviews.find { it.id == existingReviewId }
                    if (existing == null || existing.reviewerUserId != myId ||
                        existing.reviewedUserId != reviewedUserId
                    ) {
                        throw IllegalStateException("You can only edit your own review")
                    }
                    val updated = existing.copy(
                        rating = state.rating,
                        comment = state.comment.trim()
                    )
                    withContext(Dispatchers.IO) { repository.updateReview(updated) }
                } else {
                    // Create new review
                    val normalizedJobId = if (jobId == 0L) null else jobId
                    val alreadyReviewed = withContext(Dispatchers.IO) {
                        repository.hasReviewed(myId, reviewedUserId, normalizedJobId)
                    }
                    if (alreadyReviewed) {
                        throw IllegalStateException("You have already reviewed this user for this job")
                    }
                    if (state.comment.trim().length > 500) {
                        throw IllegalStateException("Comment must be 500 characters or fewer")
                    }
                    if (state.comment.trim().isEmpty() && state.rating == 0f) {
                        throw IllegalStateException("Please provide a rating or comment")
                    }
                    val review = Review(
                        id = 0,
                        reviewedUserId = reviewedUserId,
                        reviewerUserId = myId,
                        rating = state.rating,
                        comment = state.comment.trim().take(500),
                        jobId = normalizedJobId,
                        createdAt = java.time.OffsetDateTime.now().toString()
                    )
                    try {
                        withContext(Dispatchers.IO) { repository.addReview(review) }
                    } catch (e: Exception) {
                        // Handle unique constraint violation from concurrent duplicate (Postgres 23505)
                        if (e.message?.contains("duplicate", ignoreCase = true) == true ||
                            e.message?.contains("23505") == true) {
                            throw IllegalStateException("You have already reviewed this user for this job")
                        } else throw e
                    }
                }

                _formState.update { it.copy(isSubmitting = false, submitted = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _formState.update { it.copy(isSubmitting = false, error = e.message ?: "Failed to submit review") }
            }
        }
    }

    /** Returns the current logged-in user id. */
    fun myId(): Long? = sessionManager.currentUserId
}
