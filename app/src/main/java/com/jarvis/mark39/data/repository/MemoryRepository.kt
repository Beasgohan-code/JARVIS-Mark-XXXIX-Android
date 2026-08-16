package com.jarvis.mark39.data.repository

import com.jarvis.mark39.data.local.MemoryDao
import com.jarvis.mark39.data.local.MemoryEntity
import com.jarvis.mark39.domain.model.MemoryItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryRepository @Inject constructor(
    private val memoryDao: MemoryDao
) {
    suspend fun storeFact(fact: String, category: String = "fact") {
        memoryDao.insert(
            MemoryEntity(content = fact, category = category)
        )
    }

    suspend fun getContextForPrompt(limit: Int = 30): String {
        val memories = memoryDao.getRecentMemories(limit)
        if (memories.isEmpty()) return ""
        return "Relevant memory:\n" + memories.joinToString("\n") { "- [${it.category}] ${it.content}" }
    }

    suspend fun search(query: String): List<MemoryItem> {
        return memoryDao.search(query).map {
            MemoryItem(it.id, it.content, it.category, it.timestamp)
        }
    }

    suspend fun getRecent(limit: Int = 100): List<MemoryItem> =
        memoryDao.getRecentMemories(limit).map {
            MemoryItem(it.id, it.content, it.category, it.timestamp)
        }

    suspend fun delete(id: String) {
        memoryDao.deleteById(id)
    }

    suspend fun pruneOld(days: Int = 90) {
        val cutoff = System.currentTimeMillis() - days * 24L * 60 * 60 * 1000
        memoryDao.pruneOld(cutoff)
    }
}
