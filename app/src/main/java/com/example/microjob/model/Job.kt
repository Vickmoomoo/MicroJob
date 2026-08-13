package com.example.microjob.model

/**
 * A job posted by an employer/customer on the MicroJob platform.
 * In this phase the app is driven by fake data; later a real database
 * will back these fields via the repository layer.
 */
data class Job(
    val id: Int,
    val title: String,
    val price: Double,
    val distanceKm: Double,
    val category: String,
    val location: String,
    val description: String,
    /** Placeholder tint used in the card image area while there is no real photo yet. */
    val imageColor: Long
)
