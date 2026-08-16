package com.example.microjob.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Job categories shown on the Home screen. */
@Serializable
data class Category(
    val id: Int,
    val name: String,
    /** Emoji used as the category icon (placeholder until real icons are added). */
    val emoji: String
)
