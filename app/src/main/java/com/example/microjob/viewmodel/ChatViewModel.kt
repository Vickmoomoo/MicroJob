package com.example.microjob.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.microjob.data.ChatRepository
import com.example.microjob.data.LocalChatRepository
import com.example.microjob.data.SessionManager
import com.example.microjob.model.Conversation
import com.example.microjob.model.Job
import com.example.microjob.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime

/**
 * Drives the chat screens: the conversation list and a single conversation's
 * messages. Uses the local chat repository for persistence and the session
 * manager to know who "I" am.
 */
class ChatViewModel(
    application: Application,
    private val repository: ChatRepository = LocalChatRepository(application),
    private val session: SessionManager = SessionManager(application)
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, LocalChatRepository(application))

    /** The conversation list for the logged-in user, newest first. */
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    /** Messages of the currently open conversation. */
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    /** The currently open conversation, or null. */
    private val _activeConversation = MutableStateFlow<Conversation?>(null)
    val activeConversation: StateFlow<Conversation?> = _activeConversation.asStateFlow()

    /** The other user in the open conversation, for the top bar. */
    private val _otherUser = MutableStateFlow<com.example.microjob.model.User?>(null)
    val otherUser: StateFlow<com.example.microjob.model.User?> = _otherUser.asStateFlow()

    /** The job referenced by the most recent JOB_INVITE / PAYMENT_CARD action. */
    private val _cardJob = MutableStateFlow<Job?>(null)
    val cardJob: StateFlow<Job?> = _cardJob.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** Total unread message count for the current user (for bottom bar badge). */
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    /** All demo/local users, used to resolve names & avatars in the chat screens. */
    private val _users = MutableStateFlow<List<com.example.microjob.model.User>>(emptyList())
    val users: StateFlow<List<com.example.microjob.model.User>> = _users.asStateFlow()

    init {
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            // User list lives in the job repository's users.json; iterate via getUser
            // for the known demo ids is fragile, so read them through the repository's
            // job data source by exposing one lookup per known user. For simplicity the
            // local chat repo delegates to LocalJobRepository, so we reuse its getUser.
            val me = session.currentUserId ?: return@launch
            // Build a set of ids from current conversations, then resolve users lazily.
            resolveUsersFor(me)
        }
    }

    private suspend fun resolveUsersFor(me: Long) {
        // Load all users that appear in my conversations, plus demo users 1..4.
        val ids = repository.getConversations(me)
            .flatMap { it.participantIds }
            .toMutableSet()
        ids.addAll(listOf(1L, 2L, 3L, 4L))
        ids.remove(me)
        _users.value = ids.mapNotNull { withContext(Dispatchers.IO) { repository.getUser(it) } }
    }

    /** User lookup helper for chat rows & headers. */
    fun userById(id: Long): com.example.microjob.model.User? =
        _users.value.firstOrNull { it.id == id }

    /** The logged-in user's id, or 0 when nobody is logged in. */
    fun myId(): Long = session.currentUserId ?: 0L

    /** The current user's posted jobs, used for job-invite / payment pickers. */
    private val _myPostedJobs = MutableStateFlow<List<Job>>(emptyList())
    val myPostedJobs: StateFlow<List<Job>> = _myPostedJobs.asStateFlow()

    /** Reloads the current user's posted jobs from the local job repository. */
    fun refreshMyJobs() {
        viewModelScope.launch {
            val me = session.currentUserId ?: return@launch
            val jobRepo = com.example.microjob.data.LocalJobRepository(getApplication())
            _myPostedJobs.value = withContext(Dispatchers.IO) {
                jobRepo.getPostedJobs(me).sortedByDescending { it.id }
            }
        }
    }

    /** Calls back with the resulting conversation id after opening/creating a chat. */
    fun loadConversations() {
        val me = session.currentUserId ?: return
        viewModelScope.launch {
            _conversations.value = withContext(Dispatchers.IO) {
                repository.getConversations(me).filterNot { 0L in it.participantIds }
            }
            // Re-resolve participant users (a new account may have joined since
            // the initial load), so every row shows a real name, not "Chat".
            resolveUsersFor(me)
        }
    }

    /** Opens (or creates) a conversation with [otherUserId]; returns its id. */
    fun openConversation(otherUserId: Long, onOpened: (String) -> Unit) {
        viewModelScope.launch {
            val me = session.currentUserId ?: return@launch
            val conv = withContext(Dispatchers.IO) { repository.openConversation(me, otherUserId) }
            _activeConversation.value = conv
            onOpened(conv.id)
            loadMessagesInto(conv.id)
            _otherUser.value = withContext(Dispatchers.IO) { repository.getUser(otherUserId) }
            // Mark conversation as read
            withContext(Dispatchers.IO) { repository.markAsRead(conv.id, me) }
            refreshUnreadCount()
        }
    }

    /** Loads the messages of a conversation into [messages]. */
    fun loadMessages(conversationId: String) {
        loadMessagesInto(conversationId)
    }

    private fun loadMessagesInto(conversationId: String) {
        viewModelScope.launch {
            _messages.value = withContext(Dispatchers.IO) {
                repository.getMessages(conversationId)
            }
        }
    }

    /** Sends a plain text message. */
    fun sendText(conversationId: String, recipientId: Long, text: String) {
        if (text.isBlank()) return
        send(Message(text = text.trim(), type = "TEXT"), conversationId, recipientId)
    }

    /** Sends a photo message from a content uri. */
    fun sendImage(conversationId: String, recipientId: Long, uri: Uri) {
        viewModelScope.launch {
            try {
                val path = withContext(Dispatchers.IO) { repository.savePickedPhoto(uri) }
                send(Message(images = listOf(path), type = "IMAGE"), conversationId, recipientId)
            } catch (e: Exception) {
                _error.value = "Could not send the photo."
            }
        }
    }

    /** Sends a job-invite card. */
    fun sendJobInvite(conversationId: String, recipientId: Long, jobId: Int) {
        send(Message(jobId = jobId, type = "JOB_INVITE", text = "Job invite"), conversationId, recipientId)
    }

    /** Sends a release-payment card. Optionally carries an owner → worker review (hidden on the card). */
    fun sendPaymentCard(
        conversationId: String,
        recipientId: Long,
        jobId: Int,
        reviewRating: Float = 0f,
        reviewComment: String = ""
    ) {
        send(
            Message(
                jobId = jobId,
                type = "PAYMENT_CARD",
                text = "Release payment",
                reviewRating = reviewRating,
                reviewComment = reviewComment
            ),
            conversationId,
            recipientId
        )
    }

    private fun send(message: Message, conversationId: String, recipientId: Long) {
        val me = session.currentUserId ?: return
        viewModelScope.launch {
            try {
                val stored = withContext(Dispatchers.IO) {
                    repository.sendMessage(
                        message.copy(
                            conversationId = conversationId,
                            senderId = me,
                            recipientId = recipientId,
                            createdAt = OffsetDateTime.now().toString()
                        )
                    )
                }
                _messages.value = _messages.value + stored
                _conversations.value = withContext(Dispatchers.IO) { repository.getConversations(me).filterNot { 0L in it.participantIds } }
            } catch (e: Exception) {
                _error.value = "Failed to send the message."
            }
        }
    }

    /** Fetches a job for a card (invite / payment) so the worker sees its details. */
    fun loadCardJob(jobId: Int) {
        viewModelScope.launch {
            _cardJob.value = withContext(Dispatchers.IO) { repository.getJob(jobId) }
        }
    }

    /** Cache of jobs referenced by invite / payment cards, keyed by job id. */
    private val _jobCache = MutableStateFlow<Map<Int, Job>>(emptyMap())
    val jobCache: StateFlow<Map<Int, Job>> = _jobCache.asStateFlow()

    /** Loads a batch of job ids (dedup) into the cache for rendering cards. */
    fun ensureJobsLoaded(ids: Collection<Int>) {
        val missing = ids - _jobCache.value.keys
        if (missing.isEmpty()) return
        viewModelScope.launch {
            val fetched = missing.mapNotNull { id ->
                withContext(Dispatchers.IO) { repository.getJob(id)?.let { id to it } }
            }.toMap()
            _jobCache.value = _jobCache.value + fetched
        }
    }

    /** Accepts a job as the current user (worker). */
    fun acceptJob(jobId: Int, onAccepted: () -> Unit) {
        val me = session.currentUserId ?: return
        viewModelScope.launch {
            try {
                val updated = withContext(Dispatchers.IO) { repository.acceptJob(jobId, me) }
                if (updated != null) {
                    // Put the fresh IN_PROGRESS job into the cache so invite cards
                    // immediately drop the Accept button and show "accepted".
                    _jobCache.value = _jobCache.value + (jobId to updated)
                    onAccepted()
                } else {
                    // Another worker already claimed it — refresh the cached job so
                    // the card reflects that (switching to the "accepted" state).
                    val fresh = withContext(Dispatchers.IO) { repository.getJob(jobId) }
                    if (fresh != null) {
                        _jobCache.value = _jobCache.value + (jobId to fresh)
                        _error.value = "This job has already been accepted."
                    }
                }
            } catch (e: Exception) {
                _error.value = "Could not accept the job."
            }
        }
    }

    /** Marks a job's payment as released (worker claims it); can't be claimed again.
     *  Also persists the two-sided reviews: worker → owner (from the settle sheet) and
     *  owner → worker (the newest PAYMENT_CARD's attached review wins, no matter which card was tapped).
     */
    fun releaseJobPayment(
        jobId: Int,
        workerRating: Float = 0f,
        workerComment: String = "",
        onReleased: () -> Unit,
        onJobCompleted: (jobTitle: String, jobPrice: Int) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                val updated = withContext(Dispatchers.IO) { repository.releasePayment(jobId) }
                if (updated != null) {
                    _jobCache.value = _jobCache.value - jobId

                    // Persist reviews (best-effort, never block the payment itself).
                    try {
                        val me = session.currentUserId
                        if (me != null) {
                            val localJobRepo = com.example.microjob.data.LocalJobRepository(getApplication())

                            // 1) Worker → owner review from the settle sheet.
                            if (workerRating > 0f || workerComment.isNotBlank()) {
                                val posterId = updated.posterId
                                if (posterId != 0L && posterId != me) {
                                    localJobRepo.upsertReview(
                                        com.example.microjob.model.Review(
                                            id = 0,
                                            reviewedUserId = posterId,
                                            reviewerUserId = me,
                                            rating = if (workerRating > 0f) workerRating else 5f,
                                            comment = workerComment.trim(),
                                            jobId = jobId.toLong(),
                                            createdAt = OffsetDateTime.now().toString()
                                        )
                                    )
                                }
                            }

                            // 2) Owner → worker review: newest PAYMENT_CARD for this job wins.
                            val convId = _activeConversation.value?.id
                            if (convId != null) {
                                val msgs = withContext(Dispatchers.IO) { repository.getMessages(convId) }
                                val latestCard = msgs
                                    .filter { it.type == "PAYMENT_CARD" && it.jobId == jobId && it.reviewRating > 0f }
                                    .maxByOrNull { it.createdAt }
                                if (latestCard != null) {
                                    val ownerId = latestCard.senderId
                                    if (ownerId != 0L && ownerId != me) {
                                        localJobRepo.upsertReview(
                                            com.example.microjob.model.Review(
                                                id = 0,
                                                reviewedUserId = me,
                                                reviewerUserId = ownerId,
                                                rating = latestCard.reviewRating,
                                                comment = latestCard.reviewComment.trim(),
                                                jobId = jobId.toLong(),
                                                createdAt = OffsetDateTime.now().toString()
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {
                        // Review persistence is secondary; ignore failures.
                    }

                    onJobCompleted(updated.title, updated.price)
                    onReleased()
                }
            } catch (e: Exception) {
                _error.value = "Could not release the payment."
            }
        }
    }

    /** Refreshes the unread message count for the current user. */
    fun refreshUnreadCount() {
        viewModelScope.launch {
            val me = session.currentUserId ?: return@launch
            _unreadCount.value = withContext(Dispatchers.IO) { repository.getUnreadCount(me) }
        }
    }
}
