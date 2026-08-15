package com.jarvis.mark39.data.repository

import com.google.gson.Gson
import com.jarvis.mark39.data.local.MessageDao
import com.jarvis.mark39.data.local.MessageEntity
import com.jarvis.mark39.data.local.SessionDao
import com.jarvis.mark39.data.local.SessionEntity
import com.jarvis.mark39.domain.model.ChatMessage
import com.jarvis.mark39.domain.model.MessageRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val sessionDao: SessionDao,
    private val settings: SettingsRepository
) {
    private val gson = Gson()
    private val _currentSessionId = MutableStateFlow("default")
    val currentSessionId: StateFlow<String> = _currentSessionId

    val sessions: Flow<List<SessionEntity>> = sessionDao.observeSessions()

    fun observeMessages(): Flow<List<ChatMessage>> =
        _currentSessionId.flatMapLatest { sid ->
            messageDao.getMessagesForSession(sid).map { list -> list.map { it.toDomain() } }
        }

    suspend fun ensureDefaultSession() {
        if (sessionDao.get("default") == null) {
            sessionDao.insert(
                SessionEntity(id = "default", title = "Main chat", createdAt = System.currentTimeMillis())
            )
        }
        if (_currentSessionId.value.isBlank()) {
            _currentSessionId.value = "default"
        }
    }

    suspend fun createSession(title: String = "New chat"): String {
        val id = UUID.randomUUID().toString()
        sessionDao.insert(SessionEntity(id = id, title = title))
        _currentSessionId.value = id
        return id
    }

    suspend fun switchSession(id: String) {
        ensureDefaultSession()
        _currentSessionId.value = id
    }

    suspend fun renameSession(id: String, title: String) {
        sessionDao.updateTitle(id, title.ifBlank { "Chat" })
    }

    suspend fun deleteSession(id: String) {
        if (id == "default") {
            messageDao.clearSession(id)
            return
        }
        messageDao.clearSession(id)
        sessionDao.delete(id)
        if (_currentSessionId.value == id) {
            _currentSessionId.value = "default"
        }
    }

    suspend fun addMessage(message: ChatMessage) {
        if (settings.isIncognitoMode()) return
        ensureDefaultSession()
        val sid = _currentSessionId.value
        messageDao.insert(message.toEntity(sid))
        // Auto-title from first user message
        if (message.role == MessageRole.USER) {
            val recent = messageDao.getRecentForSession(sid, 3)
            if (recent.size <= 1) {
                sessionDao.updateTitle(sid, message.content.take(40))
            } else {
                sessionDao.updateTitle(sid, sessionDao.get(sid)?.title ?: "Chat")
            }
        }
    }

    suspend fun getRecent(limit: Int = 40): List<ChatMessage> =
        messageDao.getRecentForSession(_currentSessionId.value, limit).reversed().map { it.toDomain() }

    suspend fun clear() {
        messageDao.clearSession(_currentSessionId.value)
    }

    suspend fun clearAll() = messageDao.clearAll()

    private fun MessageEntity.toDomain() = ChatMessage(
        id = id,
        role = MessageRole.valueOf(role),
        content = content,
        timestamp = timestamp,
        attachments = try {
            gson.fromJson(attachmentsJson, Array<String>::class.java)?.toList() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    )

    private fun ChatMessage.toEntity(sessionId: String) = MessageEntity(
        id = id,
        sessionId = sessionId,
        role = role.name,
        content = content,
        timestamp = timestamp,
        attachmentsJson = gson.toJson(attachments)
    )
}
