package com.example.microjob.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.microjob.data.SessionManager
import com.example.microjob.data.SupabaseCourseRepository
import com.example.microjob.model.Certificate
import com.example.microjob.model.Course
import com.example.microjob.model.CourseCategory
import com.example.microjob.model.sampleCourseCategories
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class CourseViewModel(application: Application) : AndroidViewModel(application) {

    private val session = SessionManager(application)

    private val _categories = MutableStateFlow<List<CourseCategory>>(sampleCourseCategories)
    val categories: StateFlow<List<CourseCategory>> = _categories.asStateFlow()

    private val _certificates = MutableStateFlow<List<Certificate>>(emptyList())
    val certificates: StateFlow<List<Certificate>> = _certificates.asStateFlow()

    private val _watchedEpisodes = MutableStateFlow<Map<Int, Set<Int>>>(emptyMap())
    val watchedEpisodes: StateFlow<Map<Int, Set<Int>>> = _watchedEpisodes.asStateFlow()

    private val _testCompleted = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val testCompleted: StateFlow<Map<Int, Boolean>> = _testCompleted.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var lastUserId: Long? = null
    private var loadJob: Job? = null

    init {
        loadFromSupabase()
    }

    /** Call this when the app resumes or user changes to refresh data. */
    fun refreshIfUserChanged() {
        val currentUserId = session.currentUserId
        if (currentUserId != lastUserId) {
            // Clear old data when user changes
            _watchedEpisodes.value = emptyMap()
            _testCompleted.value = emptyMap()
            _certificates.value = emptyList()
            _categories.value = sampleCourseCategories
            lastUserId = currentUserId
            loadFromSupabase()
        }
    }

    private fun loadFromSupabase() {
        loadJob?.cancel()
        val userId = session.currentUserId ?: return
        lastUserId = userId
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            // Snapshot local state BEFORE overwriting categories, so a refresh right
            // after watching doesn't lose the just-watched episodes.
            val localWatchedSnapshot = _watchedEpisodes.value
            val localTestsSnapshot = _testCompleted.value
            val localCoursesSnapshot = getAllCourses().associateBy { it.id }
            try {
                // Load categories + courses
                val cats = SupabaseCourseRepository.getCategories()
                if (cats.isNotEmpty()) {
                    _categories.value = cats
                }

                // Load user progress
                val progressList = SupabaseCourseRepository.getAllProgress(userId)
                android.util.Log.d("CourseVM", "loadFromSupabase: userId=$userId, progressCount=${progressList.size}")
                progressList.forEach { p ->
                    android.util.Log.d("CourseVM", "  progress: courseId=${p.courseId}, enrolled=${p.enrolled}, progress=${p.progress}, test=${p.testCompleted}")
                }
                // Merge DB state with local in-memory state instead of overwriting.
                // refresh() can run right after markEpisodeWatched() while the Supabase
                // write is still in flight; overwriting would revert the circle.
                val localWatched = localWatchedSnapshot
                val localTests = localTestsSnapshot
                val localCourses = localCoursesSnapshot

                val watched = mutableMapOf<Int, Set<Int>>()
                val tests = mutableMapOf<Int, Boolean>()

                progressList.forEach { p ->
                    if (p.watchedEpisodes.isNotEmpty()) {
                        watched[p.courseId] = p.watchedEpisodes.toSet()
                    }
                    if (p.testCompleted) {
                        tests[p.courseId] = true
                    }
                }
                // Union local (possibly newer) watches with DB watches.
                localWatched.forEach { (courseId, episodes) ->
                    watched[courseId] = (watched[courseId] ?: emptySet()) + episodes
                }
                localTests.forEach { (courseId, done) ->
                    if (done) tests[courseId] = true
                }
                _watchedEpisodes.value = watched
                _testCompleted.value = tests

                // Update enrolled + progress on courses.
                // enrolled = DB OR local (don't unenroll on stale refresh).
                // progress = max(DB, local, recomputed from merged watches).
                _categories.value = _categories.value.map { cat ->
                    cat.copy(
                        courses = cat.courses.map { course ->
                            val progress = progressList.find { it.courseId == course.id }
                            val local = localCourses[course.id]
                            if (progress != null || local != null) {
                                val enrolled = (progress?.enrolled == true) || (local?.enrolled == true)
                                val mergedWatchedCount = (watched[course.id] ?: emptySet()).size
                                val mergedTestDone = tests[course.id] == true
                                val videoPercent = if (course.lessons > 0) {
                                    ((mergedWatchedCount.toFloat() / course.lessons) * 100).toInt().coerceIn(0, 100)
                                } else 0
                                val recomputed = (videoPercent * 0.8 + (if (mergedTestDone) 100 else 0) * 0.2).toInt().coerceIn(0, 100)
                                val mergedProgress = maxOf(
                                    progress?.progress ?: 0,
                                    local?.progress ?: 0,
                                    if (enrolled) recomputed else 0
                                )
                                course.copy(
                                    enrolled = enrolled,
                                    progress = mergedProgress
                                )
                            } else course
                        }
                    )
                }

                // Load certificates
                _certificates.value = SupabaseCourseRepository.getCertificates(userId)
            } catch (e: Exception) {
                android.util.Log.e("CourseVM", "loadFromSupabase FAILED", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Loads only a profile owner's completed certificates for public viewing. */
    fun loadCertificatesForUser(userId: Long) {
        loadJob?.cancel()
        _certificates.value = emptyList()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                _certificates.value = SupabaseCourseRepository.getCertificates(userId)
            } catch (e: Exception) {
                android.util.Log.e("CourseVM", "loadCertificatesForUser FAILED", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun startCourse(courseId: Int) {
        val userId = session.currentUserId
        android.util.Log.d("CourseVM", "startCourse: userId=$userId, courseId=$courseId")

        // Update local state immediately
        _categories.value = _categories.value.map { cat ->
            cat.copy(
                courses = cat.courses.map { course ->
                    if (course.id == courseId && !course.enrolled) {
                        course.copy(enrolled = true, progress = 0)
                    } else course
                }
            )
        }

        // Sync to Supabase
        if (userId != null) {
            viewModelScope.launch {
                try {
                    SupabaseCourseRepository.startCourse(userId, courseId)
                    android.util.Log.d("CourseVM", "startCourse SUCCESS")
                } catch (e: Exception) {
                    android.util.Log.e("CourseVM", "startCourse FAILED", e)
                }
            }
        }
    }

    fun markEpisodeWatched(courseId: Int, episode: Int) {
        val userId = session.currentUserId
        val current = _watchedEpisodes.value[courseId] ?: emptySet()
        _watchedEpisodes.value = _watchedEpisodes.value + (courseId to (current + episode))
        recalculateProgress(courseId)

        // Sync to Supabase
        if (userId != null) {
            val course = getCourseById(courseId)
            viewModelScope.launch {
                try {
                    android.util.Log.d("CourseVM", "markEpisodeWatched: userId=$userId, courseId=$courseId, episode=$episode")
                    SupabaseCourseRepository.markEpisodeWatched(userId, courseId, episode, course?.lessons ?: 1)
                    android.util.Log.d("CourseVM", "markEpisodeWatched SUCCESS")
                } catch (e: Exception) {
                    android.util.Log.e("CourseVM", "markEpisodeWatched FAILED", e)
                }
            }
        } else {
            android.util.Log.e("CourseVM", "markEpisodeWatched: userId is NULL")
        }
    }

    fun markTestCompleted(courseId: Int) {
        val userId = session.currentUserId
        _testCompleted.value = _testCompleted.value + (courseId to true)
        recalculateProgress(courseId)

        // Sync to Supabase
        if (userId != null) {
            val course = getCourseById(courseId)
            viewModelScope.launch {
                try {
                    android.util.Log.d("CourseVM", "markTestCompleted: userId=$userId, courseId=$courseId, lessons=${course?.lessons}")
                    SupabaseCourseRepository.markTestCompleted(userId, courseId, course?.lessons ?: 1)
                    android.util.Log.d("CourseVM", "markTestCompleted SUCCESS")
                } catch (e: Exception) {
                    android.util.Log.e("CourseVM", "markTestCompleted FAILED", e)
                }
            }
        } else {
            android.util.Log.e("CourseVM", "markTestCompleted: userId is NULL")
        }
    }

    fun isTestCompleted(courseId: Int): Boolean =
        _testCompleted.value[courseId] == true

    fun getWatchedEpisodes(courseId: Int): Set<Int> =
        _watchedEpisodes.value[courseId] ?: emptySet()

    /** Single source of truth for list/detail circles: same 80/20 formula as detail screen. */
    fun getDisplayProgress(course: Course): Int {
        if (!course.enrolled) return 0
        val watchedCount = (_watchedEpisodes.value[course.id] ?: emptySet()).size
        val videoPercent = if (course.lessons > 0) {
            ((watchedCount.toFloat() / course.lessons) * 100).toInt().coerceIn(0, 100)
        } else 0
        val testPercent = if (_testCompleted.value[course.id] == true) 100 else 0
        val recomputed = (videoPercent * 0.8 + testPercent * 0.2).toInt().coerceIn(0, 100)
        // Take max so a stale course.progress never hides a newly watched episode.
        return maxOf(course.progress, recomputed)
    }

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

        val userId = session.currentUserId ?: return

        // Issue certificate via Supabase
        viewModelScope.launch {
            try {
                val cert = SupabaseCourseRepository.issueCertificate(userId, course.id)
                if (cert != null) {
                    _certificates.value = _certificates.value + cert
                }
            } catch (_: Exception) {
                // Fallback: local certificate
                val dateStr = java.time.LocalDate.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy", java.util.Locale.ENGLISH)
                )
                val catPrefix = when (course.category) {
                    "Housekeeping" -> "HK"
                    "Caregiving" -> "CG"
                    "Delivery & Transport" -> "DL"
                    "Gardening" -> "GD"
                    "Digital Literacy & Applied Technology" -> "DT"
                    "Soft Skills & Professional Ethics" -> "SS"
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
        }
    }

    fun getAllCourses(): List<Course> =
        _categories.value.flatMap { it.courses }

    fun getCourseById(courseId: Int): Course? =
        getAllCourses().find { it.id == courseId }

    /** Force reload from Supabase. */
    fun refresh() {
        loadFromSupabase()
    }
}
