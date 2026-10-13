package com.example.microjob.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A review attached to a PERSON (not to a single job), because every job
 * is a one-off and unrelated to other jobs.
 *
 * - A worker reviews a job poster  → shown on the poster's profile page
 * - A job poster reviews a worker  → shown on the worker's profile page
 */
@Serializable
data class Review(
    val id: Long,
    /** The user being reviewed (poster or worker). */
    @SerialName("reviewed_user_id")
    val reviewedUserId: Long,
    /** The user who wrote the review. */
    @SerialName("reviewer_user_id")
    val reviewerUserId: Long,
    /** 0.5–5 star rating (supports half stars). */
    val rating: Float = 5f,
    val comment: String = "",
    /** Which job this review was left after (optional context). */
    @SerialName("job_id")
    val jobId: Long? = null,
    /** ISO-8601 timestamp when the review was left. */
    @SerialName("created_at")
    val createdAt: String = ""
)
