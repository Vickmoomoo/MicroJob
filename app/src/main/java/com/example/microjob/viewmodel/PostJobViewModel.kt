package com.example.microjob.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.microjob.data.JobRepository
import com.example.microjob.data.LocalJobRepository
import com.example.microjob.data.SessionManager
import com.example.microjob.model.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime

/** UI state of the post-job form. */
sealed interface PostJobUiState {
    data object Idle : PostJobUiState
    data object Submitting : PostJobUiState
    /** Shown while the fake payment is "redirecting to the payment app". */
    data object RedirectingToPayment : PostJobUiState
    /** Fake payment completed successfully; the job is about to be published. */
    data object PaymentSuccess : PostJobUiState
    /** Job published; carries the new job id so the UI can offer Undo. */
    data class Success(val jobId: Int) : PostJobUiState
    data class Error(val message: String) : PostJobUiState
}

/** Snapshot of the form, kept so "Undo" can restore it after publishing. */
data class PostFormSnapshot(
    val title: String,
    val description: String,
    val price: String,
    val category: String?,
    val state: String?,
    val area: String?,
    val jobType: String,
    val paymentMethod: String,
    val bank: String,
    val requireGps: Boolean,
    val toolsRequired: String,
    val donationAmount: String,
    val addressDetail: String,
    val photoUris: List<android.net.Uri>,
    val language: String?,
)

/** Creates a PostJobViewModel backed by the local repository (avoids reflection). */
fun postJobViewModelFactory(): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
            ?: error("Application key missing from ViewModel creation extras")
        PostJobViewModel(app)
    }
}

class PostJobViewModel(
    application: Application,
    private val repository: JobRepository = LocalJobRepository(application),
    private val session: SessionManager = SessionManager(application)
) : AndroidViewModel(application) {

    /**
     * Constructor used by Compose's default `viewModel()` factory
     * (AndroidViewModelFactory reflects on an (Application) constructor).
     */
    constructor(application: Application) : this(application, LocalJobRepository(application))

    // --- Form fields (two-way bound to the UI) ---
    val title = MutableStateFlow("")
    val description = MutableStateFlow("")
    val price = MutableStateFlow("")
    val category = MutableStateFlow<String?>(null)
    val state = MutableStateFlow<String?>(null)
    val area = MutableStateFlow<String?>(null)
    val jobType = MutableStateFlow("onsite")
    val paymentMethod = MutableStateFlow("TNG eWallet")
    /** Selected bank, only used when payment method is "Online Banking". */
    val bank = MutableStateFlow("")
    val requireGps = MutableStateFlow(false)
    val toolsRequired = MutableStateFlow("")
    /**
     * Whether the poster opted in to a voluntary donation to the MicroJob fund.
     * Derived from the typed amount (donation > 0) at submit time.
     */
    val donationAmount = MutableStateFlow("")
    /** Optional street-level detail; when blank the location becomes "area, state". */
    val addressDetail = MutableStateFlow("")

    /** Recommended communication language — Chinese / English / Malay / Other. Defaults to Other. */
    val language = MutableStateFlow<String?>("Other")

    /** Selected local photos (content URIs) picked by the user. */
    private val _photoUris = MutableStateFlow<List<Uri>>(emptyList())
    val photoUris: StateFlow<List<Uri>> = _photoUris.asStateFlow()

    private val _uiState = MutableStateFlow<PostJobUiState>(PostJobUiState.Idle)
    val uiState: StateFlow<PostJobUiState> = _uiState.asStateFlow()

    /** Placeholder color used when a job has no photos. */
    private val defaultImageColor = 0xFF607D8B

    /** Platform service fee rate — 5% charged to the poster (the worker also
     *  pays 5%, so the platform takes 10% in total, split 5/5 per the SDG plan). */
    private val serviceFeeRate = 0.05

    /**
     * Platform matches user donations 1:1, capped at 2.5% of the job price
     * (per the md plan §2.1: owner / worker each match up to 2.5%, independent,
     * with no additional RM 10 cap). Example: budget RM 100 → cap RM 2.50.
     */
    val matchCap: Double
        get() = priceValue * 0.025

    /** Max number of photos a poster can attach. */
    val maxPhotos = 6

    /** Banks offered when the payment method is "Online Banking". */
    val bankOptions = listOf(
        "Maybank",
        "CIMB Bank",
        "Public Bank",
        "RHB Bank",
        "Hong Leong Bank",
        "Bank Islam",
        "Affin Bank",
        "AmBank",
    )

    // --- Length limits for text fields ---
    val maxTitleLength = 60
    val maxDescriptionLength = 500
    val maxToolsLength = 120
    val maxAddressLength = 200

    fun onTitleChange(input: String) { title.value = input.take(maxTitleLength) }
    fun onDescriptionChange(input: String) { description.value = input.take(maxDescriptionLength) }
    fun onToolsRequiredChange(input: String) { toolsRequired.value = input.take(maxToolsLength) }
    fun onAddressDetailChange(input: String) { addressDetail.value = input.take(maxAddressLength) }

    /** Current job price as Double, or 0.0 when invalid/blank. */
    val priceValue: Double get() = price.value.toDoubleOrNull() ?: 0.0

    /** Platform service fee charged to the poster (5% of the price). */
    val serviceFee: Double get() = priceValue * serviceFeeRate

    /** What the worker receives after their 5% platform fee: price × 0.95. */
    val workerReceive: Double get() = priceValue * (1.0 - serviceFeeRate)

    /**
     * The donation amount the user typed. Donating is driven by this field
     * (empty or 0 = no donation); the [donate] flag is derived from it.
     */
    val donation: Double get() = donationAmount.value.toDoubleOrNull() ?: 0.0

    /** Platform's 1:1 match, capped at [matchCap] (0 when donation is off). */
    val platformMatch: Double get() = if (donation > 0) minOf(donation, matchCap) else 0.0

    /** Total that goes to the MicroJob fund: user donation + platform match. */
    val totalToFund: Double get() = donation + platformMatch

    /** Total the poster pays: job price + service fee + donation (match is paid by platform). */
    val totalPrice: Double get() = priceValue + serviceFee + donation

    fun setPhotos(uris: List<Uri>) {
        _photoUris.value = uris.take(maxPhotos)
    }

    /**
     * Price input filter: any number of integer digits, at most one decimal
     * point, at most 2 decimal digits (e.g. "1234928734098.99" is accepted,
     * "19.999" is rejected at the third decimal digit).
     */
    fun onPriceChange(input: String) {
        if (input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            price.value = input
        }
    }

    /** Same 2-decimal filter for the donation amount field. */
    fun onDonationAmountChange(input: String) {
        if (input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            donationAmount.value = input
        }
    }

    fun removePhoto(uri: Uri) {
        _photoUris.value = _photoUris.value.filterNot { it == uri }
    }

    /** Validates the form; returns an error message or null when OK. */
    private fun validationError(): String? = when {
        title.value.isBlank() -> "Please enter a job title."
        description.value.isBlank() -> "Please enter a description."
        price.value.toDoubleOrNull() == null || price.value.toDoubleOrNull()!! <= 0 ->
            "Please enter a valid price greater than 0."
        category.value.isNullOrBlank() -> "Please select a category."
        // At least one photo is required.
        _photoUris.value.isEmpty() -> "Please add at least one photo."
        // Address fields are only required for On-site jobs.
        jobType.value == "onsite" && state.value.isNullOrBlank() -> "Please select a state."
        jobType.value == "onsite" && area.value.isNullOrBlank() -> "Please select an area."
        jobType.value == "onsite" && addressDetail.value.isBlank() -> "Please enter the address detail."
        // Online Banking needs a bank chosen.
        paymentMethod.value == "Online Banking" && bank.value.isBlank() -> "Please select a bank."
        else -> null
    }

    /**
     * Uploads the selected photos to Supabase Storage, then publishes the job
     * with the returned URLs. On success the UI navigates back.
     */
    fun submit() {
        val error = validationError()
        if (error != null) {
            _uiState.value = PostJobUiState.Error(error)
            return
        }

        val location = listOf(
            addressDetail.value.trim(),
            area.value.orEmpty(),
            state.value.orEmpty()
        ).filter { it.isNotBlank() }.joinToString(", ")

        val job = Job(
            id = 0, // server-assigned on insert
            title = title.value.trim(),
            price = price.value.toDouble(),
            category = category.value.orEmpty(),
            location = location,
            state = state.value.orEmpty(),
            area = area.value.orEmpty(),
            jobType = jobType.value,
            description = description.value.trim(),
            imageColor = defaultImageColor,
            posterId = session.currentUserId ?: 0, // current logged-in user
            status = "OPEN",
            createdAt = OffsetDateTime.now().toString(),
            requireGps = requireGps.value,
            toolsRequired = toolsRequired.value.trim(),
            paymentMethod = paymentMethod.value,
            bank = bank.value,
            paymentStatus = "ESCROWED", // escrow is simulated
            donate = donation > 0,
            donationAmount = donation,
            currency = "RM",
            language = language.value ?: ""
        )

        viewModelScope.launch {
            try {
                // 1. Simulate the payment flow: "redirect to payment app", then
                //    "payment successful", before actually publishing the job.
                _uiState.value = PostJobUiState.RedirectingToPayment
                delay(2500)
                _uiState.value = PostJobUiState.PaymentSuccess
                delay(1800)

                // 2. Upload photos (if any) and collect their local paths.
                val imageUrls = withContext(Dispatchers.IO) {
                    uploadPhotos(_photoUris.value)
                }
                // 3. Publish the job with the photo paths attached.
                val finalJob = job.copy(images = imageUrls)
                val created = withContext(Dispatchers.IO) { repository.postJob(finalJob) }
                // Keep the submitted form so "Undo" can bring it back for editing.
                saveForm()
                _uiState.value = PostJobUiState.Success(created.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = PostJobUiState.Error(
                    "Failed to publish the job. Please try again."
                )
            }
        }
    }

    /** Uploads each photo and returns its public URL, in order. */
    private suspend fun uploadPhotos(uris: List<Uri>): List<String> {
        val resolver = getApplication<Application>().contentResolver
        val result = mutableListOf<String>()
        uris.forEachIndexed { index, uri ->
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalStateException("Cannot read the selected image.")
            val path = "jobs/${System.currentTimeMillis()}-$index.jpg"
            result.add(repository.uploadJobImage(path, bytes))
        }
        return result
    }

    /** Snapshot saved right before publishing, so "Undo" can restore the form. */
    private var savedForm: PostFormSnapshot? = null

    /** Copies the current form into [savedForm] just before the job is published. */
    private fun saveForm() {
        savedForm = PostFormSnapshot(
            title = title.value,
            description = description.value,
            price = price.value,
            category = category.value,
            state = state.value,
            area = area.value,
            jobType = jobType.value,
            paymentMethod = paymentMethod.value,
            bank = bank.value,
            requireGps = requireGps.value,
            toolsRequired = toolsRequired.value,
            donationAmount = donationAmount.value,
            addressDetail = addressDetail.value,
            photoUris = _photoUris.value,
            language = language.value
        )
    }

    /** Restores the last-saved form so the user can keep editing after Undo. */
    private fun restoreForm() {
        val f = savedForm ?: return
        title.value = f.title
        description.value = f.description
        price.value = f.price
        category.value = f.category
        state.value = f.state
        area.value = f.area
        jobType.value = f.jobType
        paymentMethod.value = f.paymentMethod
        bank.value = f.bank
        requireGps.value = f.requireGps
        toolsRequired.value = f.toolsRequired
        donationAmount.value = f.donationAmount
        addressDetail.value = f.addressDetail
        _photoUris.value = f.photoUris
        language.value = f.language
    }

    /** Removes a just-published job (the "Undo" action on the snackbar). */
    fun undoPublish(jobId: Int) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { repository.deleteJob(jobId) }
                // Restore the form so the user can keep editing what they typed.
                restoreForm()
                _uiState.value = PostJobUiState.Idle
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Undo is best-effort; ignore failures.
            }
        }
    }

    /** Clears the success state and every form field after the job is
     *  published and the user has left the screen, so the next time + is
     *  pressed the form starts fresh (unless Undo restores it). */
    fun resetForm() {
        _uiState.value = PostJobUiState.Idle
        title.value = ""
        description.value = ""
        price.value = ""
        category.value = null
        state.value = null
        area.value = null
        jobType.value = "onsite"
        paymentMethod.value = "TNG eWallet"
        bank.value = ""
        requireGps.value = false
        toolsRequired.value = ""
        donationAmount.value = ""
        addressDetail.value = ""
        _photoUris.value = emptyList()
        language.value = "Other"
    }
}
