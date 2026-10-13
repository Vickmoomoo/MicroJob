package com.example.microjob.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.microjob.data.JobRepository
import com.example.microjob.data.LocalJobRepository
import com.example.microjob.data.RepositoryProvider
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
    application: Application,
    private val repository: JobRepository = RepositoryProvider.jobRepository(application)
) : AndroidViewModel(application) {

    /**
     * Constructor used by Compose's default `viewModel()` factory
     * (AndroidViewModelFactory reflects on an (Application) constructor).
     */
    @Suppress("unused")
    constructor(application: Application) : this(application, RepositoryProvider.jobRepository(application))

    init {
        // Seed SampleData into local JSON files on first launch.
        (repository as? LocalJobRepository)?.seedIfEmpty()
    }

    /** All jobs loaded from the repository. */
    private val _jobs = MutableStateFlow<List<Job>>(emptyList())
    val jobs: StateFlow<List<Job>> = _jobs.asStateFlow()

    /** Text typed into the search bar. */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** Loading flag shown as a progress bar while fetching from the backend. */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Open jobs matching the search query. */
    val filteredJobs: StateFlow<List<Job>> =
        combine(_jobs, _searchQuery) { jobs, query ->
            jobs.filter { job ->
                // Only open (still available) jobs appear on the feed; accepted or
                // settled jobs drop off once accepted / paid out.
                val matchesQuery = job.status == "OPEN" && (
                    query.isBlank() ||
                    job.title.contains(query, ignoreCase = true) ||
                    job.description.contains(query, ignoreCase = true))
                matchesQuery
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /**
     * Loads jobs from the repository.
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
                _jobs.update { remoteJobs }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Fallback to fake data so the UI still has content to show.
                _jobs.update { SampleData.jobs }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

}
