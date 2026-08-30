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
