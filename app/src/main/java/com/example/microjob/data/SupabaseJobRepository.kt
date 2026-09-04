package com.example.microjob.data

import android.content.Context
import com.example.microjob.model.Category
import com.example.microjob.model.Job
import com.example.microjob.model.Review
import com.example.microjob.model.User
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

/** Shared Supabase client (one connection, used by job + chat + auth repositories). */
internal object SupabaseClientHolder {
    val client by lazy {
        createSupabaseClient(
            supabaseUrl = SupabaseConfig.SUPABASE_URL,
            supabaseKey = SupabaseConfig.SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
            install(Storage)
            install(Realtime)
            install(Auth) {
                autoLoadFromStorage = true
                autoSaveToStorage = true
            }
        }
    }
}

/** Public URL for a file stored in the job-images bucket. */
internal fun supabaseFileUrl(path: String): String =
    SupabaseConfig.SUPABASE_URL.trimEnd('/') + "/storage/v1/object/public/job-images/$path"

/**
 * Supabase-backed job repository.
 *
 * Every call goes through the Supabase Data API (PostgREST) with the anon
 * key; RLS policies (see supabase_setup.sql) allow the public anon role to
 * read/insert/update/delete — this is a school demo, not a production app.
 *
 * Field names: Kotlin camelCase is automatically mapped to the database's
 * snake_case by supabase-kt's default serializer.
 */
@Suppress("unused") // context kept for construction symmetry with LocalJobRepository
class SupabaseJobRepository(private val context: Context) : JobRepository {

    private val client = SupabaseClientHolder.client

    override suspend fun getJobs(): List<Job> = client
        .from("jobs")
        .select { order("id", Order.DESCENDING) }
        .decodeList<Job>()

    override suspend fun getCategories(): List<Category> = client
        .from("categories")
        .select { order("id", Order.ASCENDING) }
        .decodeList<Category>()

    override suspend fun getJob(id: Int): Job? = client
        .from("jobs")
        .select { filter { eq("id", id) }; limit(1L) }
        .decodeSingleOrNull<Job>()

    override suspend fun getUser(id: Long): User? = client
        .from("public_profiles")
        .select { filter { eq("id", id) }; limit(1L) }
        .decodeSingleOrNull<User>()

    /** Inserts the job as a DTO without `id` (the database generates it). */
    override suspend fun postJob(job: Job): Job {
        val input = JobInput(
            title = job.title,
            price = job.price,
            category = job.category,
            location = job.location,
            state = job.state,
            area = job.area,
            jobType = job.jobType,
            description = job.description,
            imageColor = job.imageColor,
            images = job.images,
            posterId = job.posterId,
            status = job.status,
            createdAt = job.createdAt,
            scheduledAt = job.scheduledAt,
            requireGps = job.requireGps,
            toolsRequired = job.toolsRequired,
            paymentMethod = job.paymentMethod,
            bank = job.bank,
            paymentStatus = job.paymentStatus,
            donate = job.donate,
            donationAmount = job.donationAmount,
            currency = job.currency,
            language = job.language
        )
        return client.from("jobs").insert(input) { select() }.decodeSingle<Job>()
    }

    override suspend fun uploadJobImage(path: String, bytes: ByteArray): String {
        client.storage.from("job-images").upload(path, bytes)
        return supabaseFileUrl(path)
    }

    override suspend fun registerUser(
        username: String,
        password: String,
        email: String,
        securityQuestion: String,
        securityAnswer: String
    ): User {
        val existing = client.from("users").select {
            filter {
                or { ilike("username", username); ilike("email", email) }
            }
        }.decodeList<User>()
        if (existing.any { it.username.equals(username, ignoreCase = true) }) {
            throw IllegalArgumentException("Username already taken.")
        }
        if (existing.any { it.email.equals(email, ignoreCase = true) }) {
            throw IllegalArgumentException("Email already registered.")
        }
        var authUserId: String? = null
        try {
            client.auth.signUpWith(Email) {
                this.email = email.trim()
                this.password = password
            }
            authUserId = client.auth.currentUserOrNull()?.id
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("already registered", ignoreCase = true) || msg.contains("User already", ignoreCase = true)) {
                throw IllegalArgumentException("Email already registered.")
            }
        }
        val input = UserInput(
            name = username.trim(),
            username = username.trim(),
            password = password,
            email = email.trim(),
            securityQuestion = securityQuestion,
            securityAnswer = securityAnswer.trim(),
            createdAt = OffsetDateTime.now().toString(),
            authUserId = authUserId
        )
        client.from("users").insert(input)
        return client.from("users").select {
            filter { ilike("username", username.trim()) }
            limit(1L)
        }.decodeSingle<User>()
    }

    override suspend fun login(username: String, password: String): User? {
        val isEmail = username.contains("@")
        val email = if (isEmail) {
            username.trim()
        } else {
            val profile = client.from("users").select {
                filter { ilike("username", username.trim()) }
                limit(1L)
            }.decodeSingleOrNull<User>() ?: return null
            profile.email
        }
        try {
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
        } catch (_: Exception) {
            return null
        }
        return client.from("users").select {
            filter { ilike("email", email) }
            limit(1L)
        }.decodeSingleOrNull<User>()
    }

    override suspend fun resetPassword(
        usernameOrEmail: String,
        securityQuestion: String,
        securityAnswer: String,
        newPassword: String
    ): PasswordResetResult {
        val target = client.from("users").select {
            filter {
                and {
                    or { ilike("username", usernameOrEmail.trim()); ilike("email", usernameOrEmail.trim()) }
                    eq("security_question", securityQuestion)
                }
                ilike("security_answer", securityAnswer.trim())
            }
            limit(1L)
        }.decodeSingleOrNull<User>()
        if (target == null) return PasswordResetResult.INVALID_DETAILS
        if (target.password == newPassword) return PasswordResetResult.SAME_AS_CURRENT_PASSWORD
        try {
            client.postgrest.rpc(
                "reset_password_by_security_question",
                mapOf(
                    "p_username" to usernameOrEmail.trim(),
                    "p_question" to securityQuestion,
                    "p_answer" to securityAnswer.trim(),
                    "p_new_password" to newPassword
                )
            )
        } catch (_: Exception) {
            client.from("users").update({ set("password", newPassword) }) {
                filter { eq("id", target.id) }
            }
        }
        try {
            val currentUser = try { client.auth.currentUserOrNull() } catch (_: Exception) { null }
            if (currentUser?.email?.equals(target.email, ignoreCase = true) == true) {
                client.auth.updateUser { password = newPassword }
            }
        } catch (_: Exception) {}
        return PasswordResetResult.SUCCESS
    }

    /** Only an OPEN job can be accepted (status is part of the WHERE clause),
     *  so a second worker racing for the same job gets no row back and null. */
    override suspend fun acceptJob(jobId: Int, workerId: Long): Job? =
        client.from("jobs")
            .update(
                { set("worker_id", workerId); set("status", "IN_PROGRESS") }
            ) {
                filter { eq("id", jobId) }
                filter { eq("status", "OPEN") }
                select()
            }
            .decodeSingleOrNull<Job>()

    override suspend fun deleteJob(jobId: Int) {
        client.from("jobs").delete { filter { eq("id", jobId) } }
    }

    override suspend fun updateUser(user: User) {
        client.from("users").update(
            {
                set("name", user.name)
                set("username", user.username)
                set("password", user.password)
                set("email", user.email)
                set("security_question", user.securityQuestion)
                set("security_answer", user.securityAnswer)
                set("bio", user.bio)
                set("avatar_url", user.avatarUrl)
                set("region", user.region)
                set("skills", user.skills)
                set("birthdate", user.birthdate)
                set("phone_number", user.phoneNumber)
                set("show_email", user.showEmail)
                set("show_birthdate", user.showBirthdate)
                set("show_phone_number", user.showPhoneNumber)
                set("show_avatar", user.showAvatar)
            }
        ) {
            filter { eq("id", user.id) }
        }
    }

    override suspend fun addReview(review: Review): Review {
        val input = ReviewInput(
            reviewedUserId = review.reviewedUserId,
            reviewerUserId = review.reviewerUserId,
            rating = review.rating,
            comment = review.comment,
            jobId = review.jobId,
            createdAt = review.createdAt.ifBlank { OffsetDateTime.now().toString() }
        )
        return client.from("reviews").insert(input) { select() }.decodeSingle<Review>()
    }

    override suspend fun updateReview(review: Review) {
        client.from("reviews").update(
            { set("rating", review.rating); set("comment", review.comment) }
        ) {
            filter { eq("id", review.id) }
        }
    }

    override suspend fun hasReviewed(reviewerUserId: Long, reviewedUserId: Long, jobId: Long?): Boolean {
        val rows = client.from("reviews").select {
            filter {
                eq("reviewer_user_id", reviewerUserId)
                eq("reviewed_user_id", reviewedUserId)
                if (jobId != null) eq("job_id", jobId)
            }
        }.decodeList<Review>()
        return rows.isNotEmpty()
    }

    override suspend fun upsertReview(review: Review): Review {
        val existing = client.from("reviews").select {
            filter {
                eq("reviewer_user_id", review.reviewerUserId)
                eq("reviewed_user_id", review.reviewedUserId)
                if (review.jobId != null) eq("job_id", review.jobId)
            }
            limit(1L)
        }.decodeSingleOrNull<Review>()
        return if (existing != null) {
            client.from("reviews").update(
                { set("rating", review.rating); set("comment", review.comment) }
            ) {
                filter { eq("id", existing.id) }
                select()
            }.decodeSingle<Review>()
        } else {
            addReview(review.copy(id = 0))
        }
    }

    override suspend fun getReviewsForUser(userId: Long): List<Review> = client
        .from("reviews")
        .select {
            filter { eq("reviewed_user_id", userId) }
            order("created_at", Order.DESCENDING)
        }
        .decodeList<Review>()

    override suspend fun getAllReviews(): List<Review> = client
        .from("reviews")
        .select { order("created_at", Order.DESCENDING) }
        .decodeList<Review>()

    override suspend fun getPostedJobs(userId: Long): List<Job> = client
        .from("jobs")
        .select {
            filter { eq("poster_id", userId) }
            order("id", Order.DESCENDING)
        }
        .decodeList<Job>()

    override suspend fun getAcceptedJobs(userId: Long): List<Job> = client
        .from("jobs")
        .select {
            filter { eq("worker_id", userId) }
            order("id", Order.DESCENDING)
        }
        .decodeList<Job>()

    // ------------------------------------------------------------------
    // Insert DTOs (no server-generated columns, mirrors the teacher's
    // User/UserInput split: id / created_at come from the database).
    // ------------------------------------------------------------------

    @Serializable
    private data class JobInput(
        val title: String,
        val price: Double,
        val category: String,
        val location: String,
        val state: String,
        val area: String,
        @SerialName("job_type")
        val jobType: String,
        val description: String,
        @SerialName("image_color")
        val imageColor: Long,
        val images: List<String>,
        @SerialName("poster_id")
        val posterId: Long,
        val status: String,
        @SerialName("created_at")
        val createdAt: String,
        @SerialName("scheduled_at")
        val scheduledAt: String? = null,
        @SerialName("require_gps")
        val requireGps: Boolean,
        @SerialName("tools_required")
        val toolsRequired: String,
        @SerialName("payment_method")
        val paymentMethod: String,
        val bank: String,
        @SerialName("payment_status")
        val paymentStatus: String,
        val donate: Boolean,
        @SerialName("donation_amount")
        val donationAmount: Double,
        val currency: String,
        val language: String
    )

    @Serializable
    private data class UserInput(
        val name: String,
        val username: String,
        val password: String,
        val email: String,
        @SerialName("security_question")
        val securityQuestion: String = "",
        @SerialName("security_answer")
        val securityAnswer: String = "",
        @SerialName("created_at")
        val createdAt: String,
        @SerialName("auth_user_id")
        val authUserId: String? = null
    )

    @Serializable
    private data class ReviewInput(
        @SerialName("reviewed_user_id")
        val reviewedUserId: Long,
        @SerialName("reviewer_user_id")
        val reviewerUserId: Long,
        val rating: Float,
        val comment: String,
        @SerialName("job_id")
        val jobId: Long? = null,
        @SerialName("created_at")
        val createdAt: String
    )
}
