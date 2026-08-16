package com.jarvis.mark39.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "custom_skills")
data class CustomSkillEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val instructions: String,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
