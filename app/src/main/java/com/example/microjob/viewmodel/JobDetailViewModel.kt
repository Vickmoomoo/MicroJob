package com.example.microjob.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.microjob.data.JobRepository
import com.example.microjob.data.LocalJobRepository
import com.example.microjob.model.Job
import com.example.microjob.model.SampleData
import com.example.microjob.model.User
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** UI state for the job detail screen. */
sealed interface JobDetailUiState {
    data object Loading : JobDetailUiState
    data object NotFound : JobDetailUiState
    data class Success(val job: Job, val poster: User?) : JobDetailUiState
}

class JobDetailViewModel(
    application: Application,
    private val repository: JobRepository = LocalJobRepository(application)
) : AndroidViewModel(application) {

    /**
     * Constructor used by Compose's default `viewModel()` factory
     * (AndroidViewModelFactory reflects on an (Application) constructor).
     */
    constructor(application: Application) : this(application, LocalJobRepository(application))

    private val _uiState = MutableStateFlow<JobDetailUiState>(JobDetailUiState.Loading)
    val uiState: StateFlow<JobDetailUiState> = _uiState.asStateFlow()

    /**
     * Loads the job and its poster. On any failure it falls back to
     * SampleData so the screen is never blank.
     */
    fun loadJob(jobId: Int) {
        viewModelScope.launch {
            _uiState.value = JobDetailUiState.Loading
            try {
                val job = withContext(Dispatchers.IO) { repository.getJob(jobId) }
                if (job == null) {
                    _uiState.value = JobDetailUiState.NotFound
                    return@launch
                }
                val poster = withContext(Dispatchers.IO) { repository.getUser(job.posterId) }
                _uiState.value = JobDetailUiState.Success(job, poster)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Fallback to fake data.
                val job = SampleData.jobs.firstOrNull { it.id == jobId }
                if (job == null) {
                    _uiState.value = JobDetailUiState.NotFound
                } else {
                    val poster = SampleData.users.firstOrNull { it.id == job.posterId }
                    _uiState.value = JobDetailUiState.Success(job, poster)
                }
            }
        }
    }
}
