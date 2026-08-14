package com.jarvis.mark39.data.repository

import com.google.gson.Gson
import com.jarvis.mark39.data.local.MessageDao
import com.jarvis.mark39.data.local.MessageEntity
import com.jarvis.mark39.domain.model.ChatMessage
import com.jarvis.mark39.domain.model.MessageRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val messageDao: MessageDao
) {
    private val gson = Gson()

    fun observeMessages(): Flow<List<ChatMessage>> =
        messageDao.getAllMessages().map { list ->
            list.map { it.toDomain() }
        }

    suspend fun addMessage(message: ChatMessage) {
        messageDao.insert(message.toEntity())
    }

    suspend fun getRecent(limit: Int = 40): List<ChatMessage> =
        messageDao.getRecentMessages(limit).reversed().map { it.toDomain() }

    suspend fun clear() = messageDao.clearAll()

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

    private fun ChatMessage.toEntity() = MessageEntity(
        id = id,
        role = role.name,
        content = content,
        timestamp = timestamp,
        attachmentsJson = gson.toJson(attachments)
    )
}
