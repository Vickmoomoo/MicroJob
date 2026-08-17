package com.example.microjob.data

import android.content.Context
import android.net.Uri
import com.example.microjob.model.Category
import com.example.microjob.model.Job
import com.example.microjob.model.Review
import com.example.microjob.model.SampleData
import com.example.microjob.model.User
import kotlinx.serialization.encodeToString
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
        } catch (e: Exception) {
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

    override suspend fun acceptJob(jobId: Int, workerId: Long): Job? {
        val jobs = readJobs().toMutableList()
        val index = jobs.indexOfFirst { it.id == jobId }
        if (index == -1) return null
        val updated = jobs[index].copy(
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
    fun getReviewsForUser(userId: Long): List<Review> =
        readList<Review>("reviews.json", emptyList()).filter { it.reviewedUserId == userId }

    /** Jobs posted by a user (poster history). */
    fun getPostedJobs(userId: Long): List<Job> = readJobs().filter { it.posterId == userId }

    /** Jobs accepted by a user (worker history). */
    fun getAcceptedJobs(userId: Long): List<Job> = readJobs().filter { it.workerId == userId }

    /** Seeds initial data from SampleData on first launch. */
    fun seedIfEmpty() {
        if (!dataFile("jobs.json").exists()) writeList("jobs.json", SampleData.jobs)
        if (!dataFile("categories.json").exists()) writeList("categories.json", SampleData.categories)
        if (!dataFile("users.json").exists()) writeList("users.json", SampleData.users)
        if (!dataFile("reviews.json").exists()) writeList("reviews.json", emptyList<Review>())
    }

    /** Resolves a local photo path or a content Uri to an absolute path (for Coil). */
    fun resolvePhoto(uriOrPath: String): String = uriOrPath

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
