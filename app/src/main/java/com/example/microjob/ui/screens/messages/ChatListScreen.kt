package com.example.microjob.ui.screens.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.microjob.model.Conversation
import com.example.microjob.model.User
import com.example.microjob.viewmodel.ChatViewModel

/**
 * The Messages tab: a conversation list in the style of a messenger app.
 * Each row shows the other participant's avatar (initial), name, the latest
 * message preview and the time.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    vm: ChatViewModel,
    onChatClick: (Long) -> Unit,
) {
    val conversations by vm.conversations.collectAsState()

    // Reload conversations every time this screen appears
    LaunchedEffect(Unit) {
        vm.loadConversations()
        vm.refreshUnreadCount()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Messages tab header, like the Profile page's title bar. The outer
        // Scaffold already supplies the status-bar inset, so clear our own
        // window insets to avoid a doubled gap above the title.
        androidx.compose.material3.TopAppBar(
            title = { Text("Chat") },
            windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
        )

        if (conversations.isEmpty()) {
            EmptyConversations()
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(conversations) { conv ->
                    val otherId = conv.otherParticipantId(vm.myId()) ?: return@items
                    val otherUser = vm.userById(otherId)
                    val clickTarget = conv.otherParticipantId(vm.myId())
                    ConversationRow(
                        otherUser = otherUser,
                        conversation = conv,
                        currentUserId = vm.myId(),
                        onClick = { if (clickTarget != null) onChatClick(clickTarget) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun EmptyConversations() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No conversations yet.\nTalk to a worker or job poster to get started.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ConversationRow(
    otherUser: User?,
    conversation: Conversation,
    currentUserId: Long,
    onClick: () -> Unit,
) {
    val displayName = otherUser?.name ?: "Chat"
    val unread = conversation.unreadCountFor(currentUserId)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(displayName)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (unread > 0) FontWeight.Bold else FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (unread > 0) {
                    BadgedBox(
                        badge = {
                            Badge {
                                Text("$unread")
                            }
                        }
                    ) {
                        Text(
                            text = formatTime(conversation.lastMessageAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = formatTime(conversation.lastMessageAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = conversation.lastMessagePreview,
                style = MaterialTheme.typography.bodyMedium,
                color = if (unread > 0) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (unread > 0) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** A round avatar with the first initial of the name. */
@Composable
fun Avatar(name: String, size: Int = 48) {
    val initial = name.trim().take(1).uppercase().ifEmpty { "?" }
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = (size * 0.4).sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/** Formats an ISO-8601 timestamp as HH:MM for the row. */
private fun formatTime(iso: String): String =
    try {
        val time = java.time.OffsetDateTime.parse(iso).toLocalTime()
        "%02d:%02d".format(time.hour, time.minute)
    } catch (e: Exception) {
        ""
    }
