package com.example.microjob.data

import android.content.Context
import android.net.Uri
import com.example.microjob.model.Conversation
import com.example.microjob.model.Job
import com.example.microjob.model.Message
import com.example.microjob.model.User
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

/**
 * Supabase-backed chat repository.
 *
 * Conversations and messages live in Postgres; new messages are delivered
 * in realtime through the Realtime WebSocket channel (messages publication).
 */
@Suppress("unused") // context kept for construction symmetry with LocalChatRepository
class       SupabaseChatRepository(private val context: Context) : ChatRepository {

    private val client = SupabaseClientHolder.client
    private val jobRepo by lazy { SupabaseJobRepository(context) }

    override suspend fun getConversations(userId: Long): List<Conversation> =
        client.from("conversations")
            .select {
                filter { contains("participant_ids", listOf(userId)) }
                order("last_message_at", Order.DESCENDING)
            }
            .decodeList<Conversation>()

    override suspend fun getMessages(conversationId: String): List<Message> =
        client.from("messages")
            .select {
                filter { eq("conversation_id", conversationId) }
                order("created_at", Order.ASCENDING)
            }
            .decodeList<Message>()

    override suspend fun openConversation(userA: Long, userB: Long): Conversation {
        val id = "conv_${minOf(userA, userB)}_${maxOf(userA, userB)}"
        val existing = client.from("conversations")
            .select { filter { eq("id", id) }; limit(1) }
            .decodeSingleOrNull<Conversation>()
        if (existing != null) return existing

        val input = ConversationInput(
            id = id,
            participantIds = listOf(userA, userB).sorted()
        )
        return client.from("conversations").insert(input) { select() }.decodeSingle<Conversation>()
    }

    override suspend fun sendMessage(message: Message): Message {
        // Server copy always gets a fresh m_ id (the optimistic bubble carries
        // its own pending id until the swap).
        val stored = message.copy(
            id = "m_${java.util.UUID.randomUUID()}",
            createdAt = message.createdAt.ifBlank { OffsetDateTime.now().toString() }
        )

        val input = MessageInput(
            id = stored.id,
            conversationId = stored.conversationId,
            senderId = stored.senderId,
            recipientId = stored.recipientId,
            type = stored.type,
            text = stored.text,
            images = stored.images,
            jobId = stored.jobId,
            createdAt = stored.createdAt,
            reviewRating = stored.reviewRating,
            reviewComment = stored.reviewComment
        )
        client.from("messages").insert(input)
        try {
            updateConversationPreview(stored)
        } catch (_: Exception) {
            // The message is already stored; a failed preview refresh is best-effort.
        }
        return stored
    }

    /** Recomputes the conversation's preview + unread counts (single-row table read). */
    private suspend fun updateConversationPreview(message: Message) {
        val conv = client.from("conversations")
            .select { filter { eq("id", message.conversationId) }; limit(1L) }
            .decodeSingleOrNull<Conversation>() ?: return

        val preview = when (message.type) {
            "IMAGE" -> "📷 Photo"
            "JOB_INVITE" -> "📌 Job invite"
            "PAYMENT_CARD" -> "💳 Release payment"
            "REVIEW" -> "⭐ Review prompt"
            else -> message.text
        }
        val unreadCounts = conv.unreadCounts.toMutableMap()
        if (message.recipientId > 0) {
            unreadCounts[message.recipientId] = (unreadCounts[message.recipientId] ?: 0) + 1
        }
        client.from("conversations").update(
            {
                set("last_message_preview", preview)
                set("last_message_at", message.createdAt)
                set("last_sender_id", message.senderId)
                set("unread_counts", unreadCounts)
            }
        ) {
            filter { eq("id", message.conversationId) }
        }
    }

    override suspend fun getUser(id: Long): User? = jobRepo.getUser(id)

    override suspend fun getJob(id: Int): Job? = jobRepo.getJob(id)

    override suspend fun acceptJob(jobId: Int, workerId: Long): Job? = jobRepo.acceptJob(jobId, workerId)

    override suspend fun releasePayment(jobId: Int): Job? =
        client.from("jobs")
            .update(
                { set("status", "COMPLETED"); set("payment_status", "RELEASED") }
            ) {
                filter { eq("id", jobId) }
                filter { neq("payment_status", "RELEASED") }
                select()
            }
            .decodeSingleOrNull<Job>()

    /**
     * Uploads a picked photo for an IMAGE message and returns its public URL.
     * The interface is non-suspend, but the caller invokes it from the IO
     * dispatcher, so a scoped runBlocking upload is acceptable there.
     */
    override fun savePickedPhoto(uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return uri.toString()
        val path = "chat/${System.currentTimeMillis()}-photo.jpg"
        return runBlocking(Dispatchers.IO) {
            client.storage.from("job-images").upload(path, bytes)
            supabaseFileUrl(path)
        }
    }

    override fun markAsRead(conversationId: String, userId: Long) {
        runBlocking(Dispatchers.IO) {
            val conv = client.from("conversations")
                .select { filter { eq("id", conversationId) }; limit(1L) }
                .decodeSingleOrNull<Conversation>() ?: return@runBlocking
            val unreadCounts = conv.unreadCounts.toMutableMap()
            unreadCounts.remove(userId)
            client.from("conversations").update({ set("unread_counts", unreadCounts) }) {
                filter { eq("id", conversationId) }
            }
        }
    }

    override suspend fun getUnreadCount(userId: Long): Int =
        getConversations(userId).sumOf { it.unreadCountFor(userId) }

    /**
     * Realtime: emits Unit whenever a message row is inserted anywhere.
     * The UI already reloads by conversation id, so no server-side filter is
     * used — supabase-kt 3.5.0 drops events whose filter `PostgresJoinConfig`
     * does not match the (filter-less) server reply, which would silently
     * kill per-conversation subscriptions.
     *
     * Fix bug #3: use a unique channel name per subscription (UUID suffix) so
     * re-entering the same conversation after leaving does not collide with
     * the previous channel that may still be unsubscribing.
     */
    override fun observeMessages(conversationId: String): Flow<Unit> = channelFlow {
        val channelName = "messages-$conversationId-${java.util.UUID.randomUUID()}"
        val channel = client.channel(channelName)
        val events = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "messages"
        }
        launch {
            channel.status.collect {
                android.util.Log.d("MicroJobSB", "messages channel status=$it name=$channelName")
            }
        }
        // NOTE: postgresChangeFlow must be registered BEFORE subscribe(),
        // and subscribe() is what actually joins the WebSocket channel.
        try {
            channel.subscribe()
            android.util.Log.d("MicroJobSB", "messages channel subscribed name=$channelName")
        } catch (t: Throwable) {
            android.util.Log.d("MicroJobSB", "messages channel subscribe FAILED name=$channelName", t)
            throw t
        }
        val job = launch { events.collect { send(Unit) } }
        awaitClose {
            job.cancel()
            launch { channel.unsubscribe() }
        }
    }

    /**
     * Realtime: emits Unit whenever any job changes (accepted / status /
     * payment released), so job-invite / payment cards update on both sides.
     */
    override fun observeJobChanges(): Flow<Unit> = channelFlow {
        val channelName = "jobs-${java.util.UUID.randomUUID()}"
        val channel = client.channel(channelName)
        val events = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "jobs"
        }
        try {
            channel.subscribe()
            android.util.Log.d("MicroJobSB", "jobs channel subscribed name=$channelName")
        } catch (t: Throwable) {
            android.util.Log.d("MicroJobSB", "jobs channel subscribe FAILED name=$channelName", t)
            throw t
        }
        val job = launch { events.collect { send(Unit) } }
        awaitClose {
            job.cancel()
            launch { channel.unsubscribe() }
        }
    }

    /** Realtime inbox: emits when any message is inserted (global). */
    override fun observeAllMessages(): Flow<Unit> = channelFlow {
        val channelName = "messages-global-${java.util.UUID.randomUUID()}"
        val channel = client.channel(channelName)
        val events = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "messages"
        }
        try {
            channel.subscribe()
            android.util.Log.d("MicroJobSB", "messages-global channel subscribed name=$channelName")
        } catch (t: Throwable) {
            android.util.Log.d("MicroJobSB", "messages-global subscribe FAILED", t)
            throw t
        }
        val job = launch { events.collect { send(Unit) } }
        awaitClose {
            job.cancel()
            launch { channel.unsubscribe() }
        }
    }

    /** Realtime inbox: emits when any conversation row changes (preview/unread). */
    override fun observeConversations(): Flow<Unit> = channelFlow {
        val channelName = "conversations-global-${java.util.UUID.randomUUID()}"
        val channel = client.channel(channelName)
        val events = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "conversations"
        }
        try {
            channel.subscribe()
            android.util.Log.d("MicroJobSB", "conversations-global channel subscribed name=$channelName")
        } catch (t: Throwable) {
            android.util.Log.d("MicroJobSB", "conversations-global subscribe FAILED", t)
            throw t
        }
        val job = launch { events.collect { send(Unit) } }
        awaitClose {
            job.cancel()
            launch { channel.unsubscribe() }
        }
    }

    /**
     * Realtime inbox: emits on any message OR conversation change.
     * Single subscription covering both tables – used for unread badge + list preview.
     */
    override fun observeInbox(): Flow<Unit> = channelFlow {
        val channelName = "inbox-global-${java.util.UUID.randomUUID()}"
        val channel = client.channel(channelName)
        val msgEvents = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "messages"
        }
        val convEvents = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "conversations"
        }
        try {
            channel.subscribe()
            android.util.Log.d("MicroJobSB", "inbox channel subscribed name=$channelName")
        } catch (t: Throwable) {
            android.util.Log.d("MicroJobSB", "inbox channel subscribe FAILED", t)
            throw t
        }
        val j1 = launch { msgEvents.collect { send(Unit) } }
        val j2 = launch { convEvents.collect { send(Unit) } }
        awaitClose {
            j1.cancel()
            j2.cancel()
            launch { channel.unsubscribe() }
        }
    }

    // ------------------------------------------------------------------
    // Insert DTOs (server-generated columns handled by the database).
    // ------------------------------------------------------------------

    @Serializable
    private data class ConversationInput(
        val id: String,
        @SerialName("participant_ids")
        val participantIds: List<Long>
    )

    @Serializable
    private data class MessageInput(
        val id: String,
        @SerialName("conversation_id")
        val conversationId: String,
        @SerialName("sender_id")
        val senderId: Long,
        @SerialName("recipient_id")
        val recipientId: Long,
        val type: String,
        val text: String,
        val images: List<String>,
        @SerialName("job_id")
        val jobId: Int,
        @SerialName("created_at")
        val createdAt: String,
        @SerialName("review_rating")
        val reviewRating: Float,
        @SerialName("review_comment")
        val reviewComment: String
    )
}
