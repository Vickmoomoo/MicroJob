package com.example.microjob.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileActivity(
    val id: Long = 0,
    @SerialName("user_id") val userId: Long = 0,
    val text: String = "",
    @SerialName("photo_uri") val photoUri: String = "",
    @SerialName("created_at") val createdAt: String = ""
)
