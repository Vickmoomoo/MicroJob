package com.example.microjob.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.microjob.data.JobRepository
import com.example.microjob.data.LocalJobRepository
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

/** How the filtered job list is sorted. */
enum class SortOption(val label: String) {
    NONE("All"),
    PRICE_LOW_TO_HIGH("Lowest to Highest"),
    PRICE_HIGH_TO_LOW("Highest to Lowest")
}

/** All filter-panel selections bundled into one flow (combine supports ≤5 flows). */
private data class FilterState(
    val state: String? = null,
    val area: String? = null,
    val jobType: String? = null,
    val sort: SortOption = SortOption.NONE
)

class HomeViewModel(
    application: Application,
    private val repository: JobRepository = LocalJobRepository(application)
) : AndroidViewModel(application) {

    /**
     * Constructor used by Compose's default `viewModel()` factory
     * (AndroidViewModelFactory reflects on an (Application) constructor).
     */
    constructor(application: Application) : this(application, LocalJobRepository(application))

    init {
        // Seed SampleData into local JSON files on first launch.
        (repository as? LocalJobRepository)?.seedIfEmpty()
    }

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

    // --- Filter panel state (set via the bottom sheet) ---

    /** Selected state for filtering, null = all states. */
    private val _filterState = MutableStateFlow<String?>(null)
    val filterState: StateFlow<String?> = _filterState.asStateFlow()

    /** Selected area for filtering, null = all areas. */
    private val _filterArea = MutableStateFlow<String?>(null)
    val filterArea: StateFlow<String?> = _filterArea.asStateFlow()

    /** Selected work mode for filtering, null = both. "remote" or "onsite". */
    private val _filterJobType = MutableStateFlow<String?>(null)
    val filterJobType: StateFlow<String?> = _filterJobType.asStateFlow()

    /** Sort option applied to the filtered jobs. */
    private val _sortOption = MutableStateFlow(SortOption.NONE)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    /** Loading flag shown as a progress bar while fetching from the backend. */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Jobs filtered by search query, category, state, area and job type,
     * then sorted by the selected sort option.
     */
    val filteredJobs: StateFlow<List<Job>> =
        combine(
            _jobs, _searchQuery, _selectedCategory,
            combine(_filterState, _filterArea, _filterJobType, _sortOption) { state, area, jobType, sort ->
                FilterState(state, area, jobType, sort)
            }
        ) { jobs, query, category, filter ->
            jobs.filter { job ->
                // Only open (still available) jobs appear on the feed; accepted or
                // settled jobs drop off once accepted / paid out.
                val matchesQuery = job.status == "OPEN" && (
                    query.isBlank() ||
                    job.title.contains(query, ignoreCase = true) ||
                    job.description.contains(query, ignoreCase = true))
                val matchesCategory = category == null || job.category == category
                val matchesState = filter.state == null || job.state == filter.state
                val matchesArea = filter.area == null || job.area == filter.area
                val matchesJobType = filter.jobType == null || job.jobType == filter.jobType
                matchesQuery && matchesCategory && matchesState && matchesArea && matchesJobType
            }.let { filtered ->
                when (filter.sort) {
                    SortOption.PRICE_LOW_TO_HIGH -> filtered.sortedBy { it.price }
                    SortOption.PRICE_HIGH_TO_LOW -> filtered.sortedByDescending { it.price }
                    SortOption.NONE -> filtered
                }
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

    fun onFilterStateChange(state: String?) {
        _filterState.value = state
        // Changing the state resets the area to avoid impossible combos.
        _filterArea.value = null
    }

    fun onFilterAreaChange(area: String?) {
        _filterArea.value = area
    }

    fun onFilterJobTypeChange(jobType: String?) {
        _filterJobType.value = jobType
    }

    fun onSortOptionChange(option: SortOption) {
        _sortOption.value = option
    }

    /** Clears every filter panel selection (category chips are kept separate). */
    fun clearFilters() {
        _filterState.value = null
        _filterArea.value = null
        _filterJobType.value = null
        _sortOption.value = SortOption.NONE
    }
}
