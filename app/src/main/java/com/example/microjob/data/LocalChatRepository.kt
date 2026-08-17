package com.example.microjob.data

import android.content.Context
import android.net.Uri
import com.example.microjob.model.Conversation
import com.example.microjob.model.Job
import com.example.microjob.model.Message
import com.example.microjob.model.User
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Local-only chat data source: conversations and messages are persisted as
 * JSON files (conversations.json / messages.json) in the app's private
 * storage, so history survives restarts with no network dependency.
 */
class LocalChatRepository(private val context: Context) : ChatRepository {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private val dataDir: File get() = context.filesDir
    private val photosDir: File get() = File(dataDir, "photos").apply { mkdirs() }

    private fun dataFile(name: String) = File(dataDir, name)

    // ---------- generic JSON helpers (mirror LocalJobRepository) ----------

    private inline fun <reified T> readList(fileName: String, fallback: List<T>): List<T> {
        val file = dataFile(fileName)
        if (!file.exists()) return fallback
        return try { json.decodeFromString<List<T>>(file.readText()) } catch (e: Exception) { fallback }
    }

    private inline fun <reified T> writeList(fileName: String, items: List<T>) {
        dataFile(fileName).writeText(json.encodeToString(items))
    }

    private fun readConversations(): List<Conversation> =
        readList("conversations.json", emptyList())

    private fun readMessages(): List<Message> =
        readList("messages.json", emptyList())

    // ---------- ChatRepository ----------

    override suspend fun getConversations(userId: Long): List<Conversation> =
        readConversations()
            .filter { userId in it.participantIds }
            .sortedByDescending { it.lastMessageAt }

    override suspend fun getMessages(conversationId: String): List<Message> =
        readMessages()
            .filter { it.conversationId == conversationId }
            .sortedBy { it.createdAt }

    override suspend fun openConversation(userA: Long, userB: Long): Conversation {
        val existing = readConversations().firstOrNull {
            userA in it.participantIds && userB in it.participantIds
        }
        if (existing != null) return existing

        val id = "conv_${minOf(userA, userB)}_${maxOf(userA, userB)}"
        val created = Conversation(
            id = id,
            participantIds = listOf(userA, userB).sorted()
        )
        writeList("conversations.json", readConversations() + created)
        return created
    }

    override suspend fun sendMessage(message: Message): Message {
        val messages = readMessages().toMutableList()
        val stored = if (message.id.isBlank()) {
            message.copy(id = "m_${System.currentTimeMillis()}")
        } else {
            message
        }
        messages.add(stored)
        writeList("messages.json", messages)

        // Update the conversation's last-message preview.
        val conversations = readConversations().toMutableList()
        val idx = conversations.indexOfFirst { it.id == message.conversationId }
        if (idx != -1) {
            val nickname = message.type
            val preview = when (message.type) {
                "IMAGE" -> "📷 Photo"
                "JOB_INVITE" -> "📌 Job invite"
                "PAYMENT_CARD" -> "💳 Release payment"
                else -> message.text
            }
            conversations[idx] = conversations[idx].copy(
                lastMessagePreview = preview,
                lastMessageAt = message.createdAt,
                lastSenderId = message.senderId
            )
            writeList("conversations.json", conversations)
        }
        return stored
    }

    override suspend fun getUser(id: Long): User? =
        LocalJobRepository(context).getUser(id)

    override suspend fun getJob(id: Int): Job? =
        LocalJobRepository(context).getJob(id)

    override suspend fun acceptJob(jobId: Int, workerId: Long): Job? =
        LocalJobRepository(context).acceptJob(jobId, workerId)

    override suspend fun releasePayment(jobId: Int): Job? =
        LocalJobRepository(context).releasePayment(jobId)

    override fun savePickedPhoto(uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return uri.toString()
        val name = "chat_${System.currentTimeMillis()}.jpg"
        val target = File(photosDir, name)
        target.writeBytes(bytes)
        return target.absolutePath
    }
}
