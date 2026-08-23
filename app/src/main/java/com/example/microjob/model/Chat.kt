package com.example.microjob.model

import kotlinx.serialization.Serializable

/**
 * A private conversation between two users, one on each side.
 * Persisted locally in conversations.json (LocalChatRepository).
 */
@Serializable
data class Conversation(
    /** Stable id for this conversation (not derived from the two users). */
    val id: String,
    /** The two participant user ids, sorted so (3,1) and (1,3) are the same. */
    val participantIds: List<Long>,
    /** The latest message text used as the list-row preview. */
    val lastMessagePreview: String = "",
    /** ISO-8601 timestamp of the latest message. */
    val lastMessageAt: String = "",
    /** Id of the user who sent the last message. */
    val lastSenderId: Long = 0,
    /** Number of unread messages keyed by recipient user id. */
    val unreadCounts: Map<Long, Int> = emptyMap()
) {
    /** Returns the id of the other participant when viewed from [fromUserId]. */
    fun otherParticipantId(fromUserId: Long): Long? =
        participantIds.firstOrNull { it != fromUserId }

    fun unreadCountFor(userId: Long): Int = unreadCounts[userId] ?: 0
}

/**
 * A single message inside a conversation.
 *
 * `type` decides how the message is rendered:
 *  - TEXT          : an ordinary chat bubble
 *  - IMAGE         : a photo (path stored in the image list)
 *  - JOB_INVITE    : a job-invite card (jobId) — the worker can Accept
 *  - PAYMENT_CARD  : a "Release Payment" card (jobId) — the worker opens a settle page
 *
 * `images` carries local file paths for IMAGE type (mirrors Job.images).
 * `jobId` is set for JOB_INVITE / PAYMENT_CARD; 0 otherwise.
 */
@Serializable
data class Message(
    val id: String = "",
    val conversationId: String = "",
    val senderId: Long = 0,
    val recipientId: Long = 0,
    /** TEXT | IMAGE | JOB_INVITE | PAYMENT_CARD */
    val type: String = "TEXT",
    val text: String = "",
    /** Local file paths for IMAGE type. */
    val images: List<String> = emptyList(),
    /** Job id referenced by a JOB_INVITE / PAYMENT_CARD card. */
    val jobId: Int = 0,
    /** ISO-8601 timestamp when the message was sent. */
    val createdAt: String = ""
)
