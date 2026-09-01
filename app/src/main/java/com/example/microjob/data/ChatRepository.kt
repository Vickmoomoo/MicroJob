package com.example.microjob.data

import android.net.Uri
import com.example.microjob.model.Conversation
import com.example.microjob.model.Job
import com.example.microjob.model.Message
import com.example.microjob.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Data source for chat conversations and messages.
 *
 * The UI talks to this interface only, mirroring [JobRepository], so the
 * implementation can later be swapped for a real backend without UI changes.
 */
interface ChatRepository {

    /** All conversations the given user participates in (newest first). */
    suspend fun getConversations(userId: Long): List<Conversation>

    /** All messages of one conversation, in chronological order. */
    suspend fun getMessages(conversationId: String): List<Message>

    /** Finds or creates a conversation between [userA] and [userB]. */
    suspend fun openConversation(userA: Long, userB: Long): Conversation

    /**
     * Appends a message to its conversation and updates the conversation's
     * last-message preview. Returns the stored message (with assigned id).
     */
    suspend fun sendMessage(message: Message): Message

    /** Looks up a user by id (delegated to the job repository/user data). */
    suspend fun getUser(id: Long): User?

    /** Looks up a job by id (delegated to the job repository). */
    suspend fun getJob(id: Int): Job?

    /** Accepts a job: assigns [workerId] and moves status to IN_PROGRESS. */
    suspend fun acceptJob(jobId: Int, workerId: Long): Job?

    /** Marks a job as fully settled (paid out + completed), so it can't be claimed twice. */
    suspend fun releasePayment(jobId: Int): Job?

    /** Persists a picked photo (for IMAGE messages) and returns its local path. */
    fun savePickedPhoto(uri: Uri): String

    /** Marks all messages in a conversation as read for the given user. */
    fun markAsRead(conversationId: String, userId: Long)

    /** Returns the total unread message count across all conversations for a user. */
    suspend fun getUnreadCount(userId: Long): Int

    /**
     * Emits Unit each time a new message lands in the conversation
     * (Supabase: Realtime WebSocket; local: a single refresh on subscribe).
     */
    fun observeMessages(conversationId: String): Flow<Unit>

    /**
     * Emits Unit each time any job row changes (accept / status / payment).
     * Lets the other side learn "invite accepted" without refreshing.
     */
    fun observeJobChanges(): Flow<Unit>

    /**
     * Emits Unit each time inbox data changes (new message or conversation update).
     * Used for realtime unread badge and conversation list preview.
     * Supabase: Realtime WebSocket on messages+conversations; local: never emits.
     */
    fun observeInbox(): Flow<Unit>

    /** Emits Unit each time any message is inserted (global, for inbox). */
    fun observeAllMessages(): Flow<Unit>

    /** Emits Unit each time any conversation row changes (global, for inbox). */
    fun observeConversations(): Flow<Unit>
}
