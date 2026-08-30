package com.example.microjob.viewmodel

import androidx.lifecycle.ViewModel
import com.example.microjob.model.Certificate
import com.example.microjob.model.Course
import com.example.microjob.model.CourseCategory
import com.example.microjob.model.sampleCourseCategories
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class CourseViewModel : ViewModel() {

    private val _categories = MutableStateFlow(sampleCourseCategories)
    val categories: StateFlow<List<CourseCategory>> = _categories.asStateFlow()

    private val _certificates = MutableStateFlow<List<Certificate>>(emptyList())
    val certificates: StateFlow<List<Certificate>> = _certificates.asStateFlow()

    // Track watched episodes per course: courseId -> set of episode numbers
    private val _watchedEpisodes = MutableStateFlow<Map<Int, Set<Int>>>(emptyMap())
    val watchedEpisodes: StateFlow<Map<Int, Set<Int>>> = _watchedEpisodes.asStateFlow()

    // Track test completion per course: courseId -> Boolean
    private val _testCompleted = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val testCompleted: StateFlow<Map<Int, Boolean>> = _testCompleted.asStateFlow()

    fun startCourse(courseId: Int) {
        _categories.value = _categories.value.map { cat ->
            cat.copy(
                courses = cat.courses.map { course ->
                    if (course.id == courseId && !course.enrolled) {
                        course.copy(enrolled = true, progress = 0)
                    } else course
                }
            )
        }
    }

    fun markEpisodeWatched(courseId: Int, episode: Int) {
        val current = _watchedEpisodes.value[courseId] ?: emptySet()
        _watchedEpisodes.value = _watchedEpisodes.value + (courseId to (current + episode))
        recalculateProgress(courseId)
    }

    fun markTestCompleted(courseId: Int) {
        _testCompleted.value = _testCompleted.value + (courseId to true)
        recalculateProgress(courseId)
    }

    fun isTestCompleted(courseId: Int): Boolean =
        _testCompleted.value[courseId] == true

    private fun recalculateProgress(courseId: Int) {
        val course = getCourseById(courseId) ?: return
        val watched = (_watchedEpisodes.value[courseId] ?: emptySet()).size
        val total = course.lessons
        val testDone = _testCompleted.value[courseId] == true

        val videoPercent = ((watched.toFloat() / total) * 100).toInt().coerceIn(0, 100)
        val testPercent = if (testDone) 100 else 0
        val newProgress = (videoPercent * 0.8 + testPercent * 0.2).toInt().coerceIn(0, 100)

        updateProgress(courseId, newProgress)
    }

    fun getWatchedEpisodes(courseId: Int): Set<Int> =
        _watchedEpisodes.value[courseId] ?: emptySet()

    fun updateProgress(courseId: Int, newProgress: Int) {
        val clampedProgress = newProgress.coerceIn(0, 100)
        _categories.value = _categories.value.map { cat ->
            cat.copy(
                courses = cat.courses.map { course ->
                    if (course.id == courseId && course.enrolled) {
                        val updated = course.copy(progress = clampedProgress)
                        if (clampedProgress == 100 && course.progress != 100) {
                            addCertificate(updated)
                        }
                        updated
                    } else course
                }
            )
        }
    }

    private fun addCertificate(course: Course) {
        val exists = _certificates.value.any { it.courseTitle == course.title }
        if (exists) return

        val dateStr = LocalDate.now().format(
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
        )
        val catPrefix = when (course.category) {
            "Housekeeping" -> "HK"
            "Caregiving" -> "CG"
            "Delivery" -> "DL"
            "Gardening" -> "GD"
            "Digital" -> "DT"
            "Soft Skills" -> "SS"
            else -> "GN"
        }
        val cert = Certificate(
            id = _certificates.value.size + 1,
            courseTitle = course.title,
            earnedDate = dateStr,
            credentialId = "MJ-${catPrefix}-2026-%03d".format(_certificates.value.size + 1)
        )
        _certificates.value = _certificates.value + cert
    }

    fun getAllCourses(): List<Course> =
        _categories.value.flatMap { it.courses }

    fun getCourseById(courseId: Int): Course? =
        getAllCourses().find { it.id == courseId }
}
