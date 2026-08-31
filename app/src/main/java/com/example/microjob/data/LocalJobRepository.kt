package com.example.microjob.data

import android.content.Context
import android.net.Uri
import com.example.microjob.model.Category
import com.example.microjob.model.Job
import com.example.microjob.model.Review
import com.example.microjob.model.SampleData
import com.example.microjob.model.User
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Local-only repository: everything is persisted as JSON files inside the
 * app's private storage (filesDir). No network, no Supabase.
 *
 * - jobs.json / categories.json / users.json / reviews.json hold the data
 * - photos are copied into filesDir/photos/ and their file paths are stored
 *   inside the job's `images` list
 */
class LocalJobRepository(private val context: Context) : JobRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val dataDir: File get() = context.filesDir
    private val photosDir: File get() = File(dataDir, "photos").apply { mkdirs() }

    private fun dataFile(name: String) = File(dataDir, name)

    // ---------- generic JSON read/write ----------

    private inline fun <reified T> readList(fileName: String, fallback: List<T>): List<T> {
        val file = dataFile(fileName)
        if (!file.exists()) return fallback
        return try {
            json.decodeFromString<List<T>>(file.readText())
        } catch (_: Exception) {
            fallback
        }
    }

    private inline fun <reified T> writeList(fileName: String, items: List<T>) {
        dataFile(fileName).writeText(json.encodeToString(items))
    }

    // ---------- JobRepository ----------

    override suspend fun getJobs(): List<Job> = readJobs()

    override suspend fun getCategories(): List<Category> =
        readList("categories.json", SampleData.categories)

    override suspend fun getJob(id: Int): Job? =
        readJobs().firstOrNull { it.id == id }

    override suspend fun getUser(id: Long): User? =
        readList("users.json", SampleData.users).firstOrNull { it.id == id }

    /** Non-suspend helper so plain functions can read jobs too. */
    private fun readJobs(): List<Job> = readList("jobs.json", SampleData.jobs)

    private fun readUsers(): List<User> = readList("users.json", SampleData.users)

    private fun writeUsers(users: List<User>) = writeList("users.json", users)

    override suspend fun registerUser(
        username: String,
        password: String,
        email: String,
        securityQuestion: String,
        securityAnswer: String
    ): User {
        val users = readUsers()
        if (users.any { it.username.equals(username, ignoreCase = true) }) {
            throw IllegalArgumentException("Username already taken.")
        }
        if (users.any { it.email.equals(email, ignoreCase = true) }) {
            throw IllegalArgumentException("Email already registered.")
        }
        val nextId = (users.maxOfOrNull { it.id } ?: 0) + 1
        val created = User(
            id = nextId,
            // No separate full-name field at registration; the username doubles as the name.
            name = username.trim(),
            username = username.trim(),
            password = password,
            email = email.trim(),
            securityQuestion = securityQuestion,
            securityAnswer = securityAnswer.trim(),
            createdAt = java.time.OffsetDateTime.now().toString()
        )
        writeUsers(users + created)
        return created
    }

    override suspend fun login(username: String, password: String): User? =
        readUsers().firstOrNull {
            it.username.equals(username, ignoreCase = true) && it.password == password
        }

    override suspend fun resetPassword(
        usernameOrEmail: String,
        securityQuestion: String,
        securityAnswer: String,
        newPassword: String
    ): PasswordResetResult {
        val users = readUsers().toMutableList()
        val index = users.indexOfFirst {
            (it.username.equals(usernameOrEmail.trim(), ignoreCase = true) ||
                it.email.equals(usernameOrEmail.trim(), ignoreCase = true)) &&
                it.securityQuestion == securityQuestion &&
                it.securityAnswer.equals(securityAnswer.trim(), ignoreCase = true)
        }
        if (index == -1) return PasswordResetResult.INVALID_DETAILS

        if (users[index].password == newPassword) {
            return PasswordResetResult.SAME_AS_CURRENT_PASSWORD
        }

        users[index] = users[index].copy(password = newPassword)
        writeUsers(users)
        return PasswordResetResult.SUCCESS
    }

    override suspend fun acceptJob(jobId: Int, workerId: Long): Job? = synchronized(this) {
        val jobs = readJobs().toMutableList()
        val index = jobs.indexOfFirst { it.id == jobId }
        if (index == -1) return null
        val current = jobs[index]
        // Only an OPEN (not yet accepted / settled) job can be accepted — this
        // stops a second worker from claiming a job already taken by someone.
        if (current.status != "OPEN") return null
        val updated = current.copy(
            workerId = workerId,
            status = "IN_PROGRESS"
        )
        jobs[index] = updated
        writeList("jobs.json", jobs)
        return updated
    }

    /** Marks a job as fully settled: paid out to the worker and completed. */
    fun releasePayment(jobId: Int): Job? {
        val jobs = readJobs().toMutableList()
        val index = jobs.indexOfFirst { it.id == jobId }
        if (index == -1) return null
        val updated = jobs[index].copy(
            status = "COMPLETED",
            paymentStatus = "RELEASED"
        )
        jobs[index] = updated
        writeList("jobs.json", jobs)
        return updated
    }

    override suspend fun deleteJob(jobId: Int) {
        val jobs = readJobs().filterNot { it.id == jobId }
        writeList("jobs.json", jobs)
    }

    override suspend fun updateUser(user: User) {
        val users = readUsers().toMutableList()
        val index = users.indexOfFirst { it.id == user.id }
        if (index != -1) {
            users[index] = user
            writeUsers(users)
        }
    }

    override suspend fun addReview(review: Review): Review {
        val reviews = readList<Review>("reviews.json", emptyList()).toMutableList()
        val nextId = (reviews.maxOfOrNull { it.id } ?: 0) + 1
        val created = review.copy(id = nextId)
        reviews.add(created)
        writeList("reviews.json", reviews)
        return created
    }

    override suspend fun updateReview(review: Review) {
        val reviews = readList<Review>("reviews.json", emptyList()).toMutableList()
        val index = reviews.indexOfFirst { it.id == review.id }
        if (index != -1) {
            reviews[index] = review
            writeList("reviews.json", reviews)
        }
    }

    override suspend fun hasReviewed(reviewerUserId: Long, reviewedUserId: Long, jobId: Long?): Boolean {
        val reviews = readList<Review>("reviews.json", emptyList())
        return reviews.any {
            it.reviewerUserId == reviewerUserId &&
                it.reviewedUserId == reviewedUserId &&
                it.jobId == jobId
        }
    }

    override suspend fun upsertReview(review: Review): Review {
        val reviews = readList<Review>("reviews.json", emptyList()).toMutableList()
        val idx = reviews.indexOfFirst {
            it.reviewerUserId == review.reviewerUserId &&
                it.reviewedUserId == review.reviewedUserId &&
                it.jobId == review.jobId
        }
        return if (idx != -1) {
            val updated = reviews[idx].copy(
                rating = review.rating,
                comment = review.comment,
                createdAt = java.time.OffsetDateTime.now().toString()
            )
            reviews[idx] = updated
            writeList("reviews.json", reviews)
            updated
        } else {
            val nextId = (reviews.maxOfOrNull { it.id } ?: 0) + 1
            val created = review.copy(id = nextId)
            reviews.add(created)
            writeList("reviews.json", reviews)
            created
        }
    }

    override suspend fun postJob(job: Job): Job {
        val jobs = getJobs().toMutableList()
        val nextId = (jobs.maxOfOrNull { it.id } ?: 0) + 1
        val created = job.copy(id = nextId)
        jobs.add(created)
        writeList("jobs.json", jobs)
        return created
    }

    override suspend fun uploadJobImage(path: String, bytes: ByteArray): String {
        // Local variant: save into filesDir/photos and return the local file path.
        val target = File(photosDir, File(path).name)
        target.writeBytes(bytes)
        return target.absolutePath
    }

    // ---------- helpers used by the UI ----------

    /** Returns all reviews for a user (used by future profile pages). */
    override suspend fun getReviewsForUser(userId: Long): List<Review> =
        readList<Review>("reviews.json", emptyList()).filter { it.reviewedUserId == userId }

    /** Returns all reviews (for job detail). */
    override suspend fun getAllReviews(): List<Review> =
        readList<Review>("reviews.json", emptyList())

    /** Jobs posted by a user (poster history). */
    override suspend fun getPostedJobs(userId: Long): List<Job> =
        readJobs().filter { it.posterId == userId }

    /** Jobs accepted by a user (worker history). */
    override suspend fun getAcceptedJobs(userId: Long): List<Job> =
        readJobs().filter { it.workerId == userId }

    /** Seeds initial data from SampleData on first launch. */
    fun seedIfEmpty() {
        if (!dataFile("jobs.json").exists()) writeList("jobs.json", SampleData.jobs)
        if (!dataFile("categories.json").exists()) writeList("categories.json", SampleData.categories)
        if (!dataFile("users.json").exists()) writeList("users.json", SampleData.users)
        if (!dataFile("reviews.json").exists()) writeList("reviews.json", emptyList<Review>())
    }

    @Suppress("unused")
    /** Resolves a local photo path or a content Uri to an absolute path (for Coil). */
    fun resolvePhoto(uriOrPath: String): String = uriOrPath

    @Suppress("unused")
    /** Convenience for the post form: converts a picked Uri to a saved file. */
    fun savePickedPhoto(uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return uri.toString()
        val name = "photo_${System.currentTimeMillis()}.jpg"
        val target = File(photosDir, name)
        target.writeBytes(bytes)
        return target.absolutePath
    }
}
