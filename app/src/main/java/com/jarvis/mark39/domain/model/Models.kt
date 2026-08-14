package com.jarvis.mark39.domain.model

import java.util.UUID

enum class MessageRole { USER, ASSISTANT, SYSTEM }

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val attachments: List<String> = emptyList() // URIs as strings
)

enum class TaskStatus { PENDING, RUNNING, COMPLETED, FAILED, CANCELLED, PARTIAL }

data class AgentTask(
    val id: String = UUID.randomUUID().toString(),
    val goal: String,
    val status: TaskStatus = TaskStatus.PENDING,
    val steps: List<String> = emptyList(),
    val result: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class MemoryItem(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val category: String, // preference, fact, task, conversation
    val timestamp: Long = System.currentTimeMillis()
)

enum class VoiceState {
    IDLE, LISTENING, PROCESSING, SPEAKING, ERROR
}

sealed class JarvisUiEvent {
    data class SendText(val text: String) : JarvisUiEvent()
    data object StartListening : JarvisUiEvent()
    data object StopListening : JarvisUiEvent()
    data class ShareFile(val uri: String) : JarvisUiEvent()
    data class RunTask(val goal: String) : JarvisUiEvent()
    data object ClearChat : JarvisUiEvent()
}
