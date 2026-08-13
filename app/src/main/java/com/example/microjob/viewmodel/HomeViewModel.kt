package com.example.microjob.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.microjob.model.Job
import com.example.microjob.model.SampleData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class HomeViewModel : ViewModel() {

    /** All jobs loaded from the (fake) repository. */
    private val _jobs = MutableStateFlow<List<Job>>(emptyList())

    /** Text typed into the search bar. */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** Currently selected category name, null = all categories. */
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    /** Simulated network load flag, mirrors the sample project's pattern. */
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

    /** Loads the sample jobs as if fetching from a backend. */
    fun loadSampleJobs() {
        viewModelScope.launch {
            _isLoading.value = true
            delay(1200.milliseconds)
            _jobs.update { SampleData.jobs }
            _isLoading.value = false
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelect(category: String?) {
        _selectedCategory.value = category
    }
}
