package com.example.microjob.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A user of the MicroJob platform. Every user can act as BOTH a job
 * poster and a worker, so a user owns two histories:
 *  - posted jobs    (jobs where posterId == this user)
 *  - accepted jobs  (jobs where workerId == this user)
 */
@Serializable
data class User(
    val id: Long,
    val name: String,
    /** Login username. */
    val username: String = "",
    /** Login password (plain text — demo only; real apps must hash it). */
    val password: String = "",
    /** Email used for account recovery. */
    val email: String = "",
    /** Security question chosen at registration (for password reset). */
    @SerialName("security_question")
    val securityQuestion: String = "",
    /** Answer to the security question. */
    @SerialName("security_answer")
    val securityAnswer: String = "",
    /** Short self-introduction shown on the user's profile page. */
    val bio: String = "",
    /** Profile photo local path; empty string = no photo yet. */
    @SerialName("avatar_url")
    val avatarUrl: String = "",
    /** Malaysian state or territory selected by the user. */
    val region: String = "",
    /** Selected category names and user-entered skills. */
    val skills: List<String> = emptyList(),
    /** Birth date formatted for display (for example, "12 Aug 2000"). */
    val birthdate: String = "",
    /** Phone number used for contact. */
    @SerialName("phone_number")
    val phoneNumber: String = "",
    /** Privacy controls, defaulting to hidden for sensitive fields. */
    @SerialName("show_email")
    val showEmail: Boolean = false,
    @SerialName("show_birthdate")
    val showBirthdate: Boolean = false,
    @SerialName("show_phone_number")
    val showPhoneNumber: Boolean = false,
    @SerialName("show_avatar")
    val showAvatar: Boolean = true,
    /** ISO-8601 timestamp of account creation. */
    @SerialName("created_at")
    val createdAt: String = ""
)
