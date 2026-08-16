package com.jarvis.mark39.data.repository

import com.jarvis.mark39.data.local.CustomSkillDao
import com.jarvis.mark39.data.local.CustomSkillEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomSkillRepository @Inject constructor(
    private val dao: CustomSkillDao
) {
    fun observeAll(): Flow<List<CustomSkillEntity>> = dao.observeAll()

    suspend fun getEnabled(): List<CustomSkillEntity> = dao.getEnabled()

    suspend fun add(name: String, description: String, instructions: String): CustomSkillEntity {
        val skill = CustomSkillEntity(
            name = name.trim(),
            description = description.trim(),
            instructions = instructions.trim()
        )
        dao.insert(skill)
        return skill
    }

    suspend fun delete(id: String) = dao.delete(id)

    suspend fun setEnabled(id: String, enabled: Boolean) = dao.setEnabled(id, enabled)

    /** Inject enabled custom skills into system prompt. */
    suspend fun promptAddon(): String {
        val list = getEnabled()
        if (list.isEmpty()) return ""
        return buildString {
            appendLine()
            appendLine("[CUSTOM SKILLS — follow when relevant]")
            list.forEach { s ->
                appendLine("### ${s.name}")
                if (s.description.isNotBlank()) appendLine(s.description)
                appendLine(s.instructions)
                appendLine()
            }
        }
    }
}
