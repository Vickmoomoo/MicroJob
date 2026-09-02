package com.example.microjob.data

import com.example.microjob.model.Certificate
import com.example.microjob.model.Course
import com.example.microjob.model.CourseCategory
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class CourseCategoryRow(
    val id: Int,
    val name: String,
    val emoji: String
)

@Serializable
private data class CourseRow(
    val id: Int,
    @SerialName("category_id") val categoryId: Int,
    val title: String,
    val emoji: String,
    val lessons: Int,
    val duration: String,
    val description: String
)

@Serializable
private data class CourseProgressRow(
    val id: Long,
    @SerialName("user_id") val userId: Long,
    @SerialName("course_id") val courseId: Int,
    val enrolled: Boolean,
    val progress: Int,
    @SerialName("watched_episodes") val watchedEpisodes: List<Int>,
    @SerialName("test_completed") val testCompleted: Boolean
)

@Serializable
private data class CourseCertificateRow(
    val id: Long,
    @SerialName("user_id") val userId: Long,
    @SerialName("course_id") val courseId: Int,
    @SerialName("earned_date") val earnedDate: String,
    @SerialName("credential_id") val credentialId: String
)

object SupabaseCourseRepository {

    private val client get() = SupabaseClientHolder.client

    /** Fetch all course categories with their courses. */
    suspend fun getCategories(): List<CourseCategory> {
        val categoryRows = client.from("course_categories")
            .select()
            .decodeList<CourseCategoryRow>()

        val courseRows = client.from("courses")
            .select()
            .decodeList<CourseRow>()

        return categoryRows.map { cat ->
            CourseCategory(
                name = cat.name,
                emoji = cat.emoji,
                courses = courseRows
                    .filter { it.categoryId == cat.id }
                    .map { course ->
                        Course(
                            id = course.id,
                            title = course.title,
                            category = cat.name,
                            emoji = course.emoji,
                            lessons = course.lessons,
                            duration = course.duration,
                            description = course.description
                        )
                    }
            )
        }
    }

    /** Get or create progress for a user + course. */
    suspend fun getProgress(userId: Long, courseId: Int): CourseProgressRow? {
        return client.from("course_progress")
            .select {
                filter {
                    eq("user_id", userId)
                    eq("course_id", courseId)
                }
            }
            .decodeList<CourseProgressRow>()
            .firstOrNull()
    }

    /** Get all progress entries for a user. */
    suspend fun getAllProgress(userId: Long): List<CourseProgressRow> {
        return client.from("course_progress")
            .select {
                filter { eq("user_id", userId) }
            }
            .decodeList<CourseProgressRow>()
    }

    /** Start a course (enroll). Creates progress row if not exists. */
    suspend fun startCourse(userId: Long, courseId: Int) {
        val existing = getProgress(userId, courseId)
        if (existing == null) {
            client.from("course_progress")
                .insert(mapOf(
                    "user_id" to userId,
                    "course_id" to courseId,
                    "enrolled" to true,
                    "progress" to 0
                ))
        } else if (!existing.enrolled) {
            client.from("course_progress")
                .update(mapOf("enrolled" to true)) {
                    filter {
                        eq("user_id", userId)
                        eq("course_id", courseId)
                    }
                }
        }
    }

    /** Mark an episode as watched and update progress. */
    suspend fun markEpisodeWatched(userId: Long, courseId: Int, episode: Int, totalEpisodes: Int) {
        val existing = getProgress(userId, courseId) ?: return
        val watched = (existing.watchedEpisodes + episode).distinct()
        val videoPercent = ((watched.size.toFloat() / totalEpisodes) * 100).toInt().coerceIn(0, 100)
        val testPercent = if (existing.testCompleted) 100 else 0
        val newProgress = (videoPercent * 0.8 + testPercent * 0.2).toInt().coerceIn(0, 100)

        client.from("course_progress")
            .update(mapOf(
                "watched_episodes" to watched,
                "progress" to newProgress
            )) {
                filter {
                    eq("user_id", userId)
                    eq("course_id", courseId)
                }
            }
    }

    /** Mark test as completed and update progress. */
    suspend fun markTestCompleted(userId: Long, courseId: Int, totalEpisodes: Int) {
        val existing = getProgress(userId, courseId) ?: return
        val videoPercent = ((existing.watchedEpisodes.size.toFloat() / totalEpisodes) * 100).toInt().coerceIn(0, 100)
        val newProgress = (videoPercent * 0.8 + 100 * 0.2).toInt().coerceIn(0, 100)

        client.from("course_progress")
            .update(mapOf(
                "test_completed" to true,
                "progress" to newProgress
            )) {
                filter {
                    eq("user_id", userId)
                    eq("course_id", courseId)
                }
            }
    }

    /** Get all certificates for a user. */
    suspend fun getCertificates(userId: Long): List<Certificate> {
        val rows = client.from("course_certificates")
            .select {
                filter { eq("user_id", userId) }
            }
            .decodeList<CourseCertificateRow>()

        // Fetch course titles
        val courses = client.from("courses").select().decodeList<CourseRow>()
        val courseMap = courses.associateBy { it.id }

        return rows.map { cert ->
            Certificate(
                id = cert.id.toInt(),
                courseTitle = courseMap[cert.courseId]?.title ?: "Unknown Course",
                earnedDate = cert.earnedDate,
                credentialId = cert.credentialId
            )
        }
    }

    /** Issue a certificate for a completed course. */
    suspend fun issueCertificate(userId: Long, courseId: Int): Certificate? {
        // Check if already exists
        val existing = client.from("course_certificates")
            .select {
                filter {
                    eq("user_id", userId)
                    eq("course_id", courseId)
                }
            }
            .decodeList<CourseCertificateRow>()
        if (existing.isNotEmpty()) return null

        // Get course info
        val course = client.from("courses")
            .select { filter { eq("id", courseId) } }
            .decodeList<CourseRow>()
            .firstOrNull() ?: return null

        val dateStr = java.time.LocalDate.now().format(
            java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy", java.util.Locale.ENGLISH)
        )

        // Count existing certificates for this user
        val certCount = client.from("course_certificates")
            .select { filter { eq("user_id", userId) } }
            .decodeList<CourseCertificateRow>()
            .size

        val catPrefix = when {
            courseId <= 5 -> "HK"
            courseId <= 11 -> "CG"
            courseId <= 13 -> "DL"
            courseId <= 15 -> "GD"
            courseId <= 18 -> "DT"
            else -> "SS"
        }
        val credentialId = "MJ-${catPrefix}-2026-%03d".format(certCount + 1)

        client.from("course_certificates")
            .insert(mapOf(
                "user_id" to userId,
                "course_id" to courseId,
                "earned_date" to dateStr,
                "credential_id" to credentialId
            ))

        return Certificate(
            id = certCount + 1,
            courseTitle = course.title,
            earnedDate = dateStr,
            credentialId = credentialId
        )
    }
}
