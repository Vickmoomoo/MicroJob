package com.example.microjob.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.microjob.data.JobRepository
import com.example.microjob.data.SupabaseJobRepository
import com.example.microjob.model.Category
import com.example.microjob.model.Job
import com.example.microjob.model.SampleData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

class HomeViewModel(
    private val repository: JobRepository = SupabaseJobRepository()
) : ViewModel() {

    /** All jobs loaded from the repository. */
    private val _jobs = MutableStateFlow<List<Job>>(emptyList())
    val jobs: StateFlow<List<Job>> = _jobs.asStateFlow()

    /** All categories loaded from the repository. */
    private val _categories = MutableStateFlow<List<Category>>(SampleData.categories)
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    /** Text typed into the search bar. */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** Currently selected category name, null = all categories. */
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    /** Loading flag shown as a progress bar while fetching from the backend. */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Jobs filtered by the search query and the selected category.
     * combine() re-emits whenever any of the three sources changes.
     */
    val filteredJobs: StateFlow<List<Job>> =
        combine(_jobs, _searchQuery, _selectedCategory) { jobs, query, category ->
            jobs.filter { job ->
                val matchesQuery = query.isBlank() ||
                    job.title.contains(query, ignoreCase = true) ||
                    job.description.contains(query, ignoreCase = true)
                val matchesCategory = category == null || job.category == category
                matchesQuery && matchesCategory
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /**
     * Loads jobs and categories from the repository.
     * On any failure (wrong credentials, offline, table missing) it falls
     * back to SampleData so the app never shows an empty screen.
     */
    fun loadJobs() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Simulated latency, mirrors the original sample pattern.
                delay(1200.milliseconds)
                val remoteJobs = withContext(Dispatchers.IO) { repository.getJobs() }
                val remoteCategories = withContext(Dispatchers.IO) { repository.getCategories() }
                _jobs.update { remoteJobs }
                _categories.update { remoteCategories }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Fallback to fake data so the UI still has content to show.
                _jobs.update { SampleData.jobs }
                _categories.update { SampleData.categories }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelect(category: String?) {
        _selectedCategory.value = category
    }
}
