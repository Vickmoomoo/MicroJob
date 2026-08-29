package com.example.microjob.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A job posted by an employer/customer on the MicroJob platform.
 *
 * Stored locally as JSON (LocalJobRepository) using the Kotlin property names.
 * New fields carry default values so older JSON files still decode fine.
 */
@Serializable
data class Job(
    val id: Int,
    val title: String,
    val price: Double,
    val category: String,
    /** Full postal address shown on the detail page. */
    val location: String,
    /** State used for filtering only (e.g. "Pulau Pinang"). */
    val state: String,
    /** Area within the state used for filtering only (e.g. "George Town"). */
    val area: String,
    /**
     * Work mode: "remote" (work from home) or "onsite" (go to the location).
     * Shown as a badge on the card image.
     */
    @SerialName("job_type")
    val jobType: String = "onsite",
    val description: String,
    @SerialName("image_color")
    val imageColor: Long,
    /** Local file paths of photos saved by the poster. Empty = no photos. */
    val images: List<String> = emptyList(),
    // --- Platform flow fields (P0) ---
    /** Who posted the job (id in `users`). 0 = unknown/legacy row. */
    @SerialName("poster_id")
    val posterId: Long = 0,
    /** Who accepted the job (id in `users`). null = nobody has accepted yet. */
    @SerialName("worker_id")
    val workerId: Long? = null,
    /** OPEN → IN_PROGRESS → COMPLETED / CANCELLED. */
    val status: String = "OPEN",
    /** ISO-8601 timestamp when the job was posted. */
    @SerialName("created_at")
    val createdAt: String = "",
    /** ISO-8601 deadline; null = no deadline. */
    val deadline: String? = null,
    /** ISO-8601 scheduled date+time (24h) chosen at posting; null when not set (legacy). */
    @SerialName("scheduled_at")
    val scheduledAt: String? = null,
    // --- Trust & safety ---
    /** Poster optionally requires the worker to share their location while working. */
    @SerialName("require_gps")
    val requireGps: Boolean = false,
    /** What tools the worker must bring; empty string = no tools required. */
    @SerialName("tools_required")
    val toolsRequired: String = "",
    /** How the poster pays the worker (e.g. "TNG eWallet", "Online Banking"). */
    @SerialName("payment_method")
    val paymentMethod: String = "Cash",
    /** Selected bank when payment method is "Online Banking"; empty otherwise. */
    val bank: String = "",
    /** Whether the poster opted in to a voluntary donation to the MicroJob fund. */
    val donate: Boolean = false,
    /** The donation amount the poster chose (0 when donation is off). */
    @SerialName("donation_amount")
    val donationAmount: Double = 0.0,
    /**
     * Escrow status of the job money:
     * ESCROWED (paid & held) → RELEASED (paid out to worker) / REFUNDED (back to poster).
     */
    @SerialName("payment_status")
    val paymentStatus: String = "ESCROWED",
    // --- Exposure & display ---
    /** ISO-8601 until when the boost (exposure voucher) is active; null = no boost. */
    @SerialName("boost_until")
    val boostUntil: String? = null,
    /** Currency code, e.g. "RM". */
    val currency: String = "RM",
    /** Recommended communication language, e.g. "Bahasa Malaysia / English". */
    val language: String = ""
)
