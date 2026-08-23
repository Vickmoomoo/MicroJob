package com.example.microjob.data

import com.example.microjob.model.Category
import com.example.microjob.model.Job
import com.example.microjob.model.Review
import com.example.microjob.model.User

enum class PasswordResetResult {
    SUCCESS,
    INVALID_DETAILS,
    SAME_AS_CURRENT_PASSWORD
}

/**
 * Data source for jobs, categories and users.
 *
 * The UI talks to this interface only, so the implementation can be swapped
 * between Supabase, a Room database, or the local SampleData without touching
 * the ViewModel / UI layer.
 */
interface JobRepository {

    /** Returns all jobs (fetched from the backend). Throws on network failure. */
    suspend fun getJobs(): List<Job>

    /** Returns all job categories. Throws on network failure. */
    suspend fun getCategories(): List<Category>

    /** Returns a single job by id, or null when it does not exist. */
    suspend fun getJob(id: Int): Job?

    /** Returns a single user by id, or null when it does not exist. */
    suspend fun getUser(id: Long): User?

    /**
     * Publishes a new job. The job must already carry its final values
     * (status = "OPEN", payment_status = "ESCROWED" — escrow is simulated).
     * Returns the created job (with its server-assigned id), or throws on failure.
     */
    suspend fun postJob(job: Job): Job

    /**
     * Uploads a job photo to Supabase Storage and returns its public URL.
     * `path` should be unique per photo (e.g. "jobs/<timestamp>-<counter>.jpg").
     */
    suspend fun uploadJobImage(path: String, bytes: ByteArray): String

    /**
     * Registers a new user. Throws if the username is already taken.
     * Returns the created user (with its assigned id).
     */
    suspend fun registerUser(
        username: String,
        password: String,
        email: String,
        securityQuestion: String,
        securityAnswer: String
    ): User

    /**
     * Logs in with username + password.
     * Returns the matching user, or null when credentials are wrong.
     */
    suspend fun login(username: String, password: String): User?

    /** Resets a password after the account recovery details are verified. */
    suspend fun resetPassword(
        usernameOrEmail: String,
        securityQuestion: String,
        securityAnswer: String,
        newPassword: String
    ): PasswordResetResult

    /**
     * Accepts a job: assigns [workerId] and moves status to IN_PROGRESS.
     * Returns the updated job, or null when the job id does not exist.
     */
    suspend fun acceptJob(jobId: Int, workerId: Long): Job?

    /** Deletes a job by id (used by the publish "Undo" action). */
    suspend fun deleteJob(jobId: Int)

    /** Updates a user's profile fields (name, bio, avatar, phone, etc.). */
    suspend fun updateUser(user: User)

    /** Adds a new review. Returns the created review with its assigned id. */
    suspend fun addReview(review: Review): Review

    /** Updates an existing review (for editing). */
    suspend fun updateReview(review: Review)

    /** Returns true if the reviewer has already reviewed the given user for the given job. */
    suspend fun hasReviewed(reviewerUserId: Long, reviewedUserId: Long, jobId: Long?): Boolean

    /** Inserts or updates a review keyed by (reviewer, reviewed, jobId). Returns the persisted review. */
    suspend fun upsertReview(review: Review): Review
}
