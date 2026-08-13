package com.example.microjob.model

/** Job categories shown on the Home screen. */
data class Category(
    val id: Int,
    val name: String,
    /** Emoji used as the category icon (placeholder until real icons are added). */
    val emoji: String
)
