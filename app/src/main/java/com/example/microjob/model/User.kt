package com.example.microjob.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A user of the MicroJob platform. Every user can act as BOTH a job
 * poster and a worker, so a user owns two histories:
 *  - posted jobs    (jobs where poster_id == this user)
 *  - accepted jobs  (jobs where worker_id == this user)
 */
@Serializable
data class User(
    val id: Long,
    val name: String,
    /** Short self-introduction shown on the user's profile page. */
    val bio: String = "",
    /** Profile photo URL; empty string = no photo yet. */
    @SerialName("avatar_url")
    val avatarUrl: String = "",
    /** ISO-8601 timestamp of account creation. */
    @SerialName("created_at")
    val createdAt: String = ""
)
