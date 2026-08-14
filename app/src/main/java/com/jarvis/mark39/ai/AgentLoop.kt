package com.jarvis.mark39.ai

import com.jarvis.mark39.data.repository.MemoryRepository
import com.jarvis.mark39.domain.model.AgentTask
import com.jarvis.mark39.domain.model.TaskStatus
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ReAct-style agent: reason → act (via ToolRegistry) → observe → repeat.
 */
@Singleton
class AgentLoop @Inject constructor(
    private val gemini: GeminiClient,
    private val tools: ToolRegistry,
    private val memory: MemoryRepository
) {
    suspend fun execute(goal: String, maxSteps: Int = 10): AgentTask {
        var task = AgentTask(goal = goal, status = TaskStatus.RUNNING)
        val steps = mutableListOf<String>()
        var state = "Goal: $goal"

        repeat(maxSteps) { stepIdx ->
            val prompt = """
You are JARVIS Task Agent. Work toward the goal using tools.

Current state:
$state

Available tools:
${tools.listToolsForPrompt()}

Respond with EXACTLY one of:
- [ACTION:tool_name|param=value]   (or [ACTION:tool_name|value] for single-arg tools)
- [COMPLETE|final answer for the user]

Be concise. One action per step. Prefer tools over guessing.
""".trimIndent()

            val response = try {
                gemini.sendMessage(prompt)
            } catch (e: Exception) {
                steps.add("Error: ${e.message}")
                return task.copy(
                    status = TaskStatus.FAILED,
                    steps = steps,
                    result = e.message,
                    updatedAt = System.currentTimeMillis()
                )
            }

            steps.add("Think ${stepIdx + 1}: $response")

            when {
                response.contains("[COMPLETE", ignoreCase = true) -> {
                    val answer = response.substringAfter("|", "").substringBefore("]").trim()
                        .ifBlank { response }
                    memory.storeFact("Task done: $goal → $answer", "task")
                    return task.copy(
                        status = TaskStatus.COMPLETED,
                        steps = steps,
                        result = answer,
                        updatedAt = System.currentTimeMillis()
                    )
                }
                response.contains("[ACTION:", ignoreCase = true) -> {
                    val parsed = tools.parseAction(response)
                    if (parsed == null) {
                        state += "\nInvalid action format: $response"
                        steps.add("Invalid action format")
                    } else {
                        val (name, params) = parsed
                        val result = tools.execute(name, params)
                        steps.add("Action: $name $params → $result")
                        state += "\nAction $name($params) → $result"
                    }
                }
                else -> {
                    state += "\n(no ACTION/COMPLETE) $response"
                    steps.add("No actionable command")
                }
            }
            delay(300)
        }

        return task.copy(
            status = TaskStatus.PARTIAL,
            steps = steps,
            result = "Reached max steps. State:\n$state",
            updatedAt = System.currentTimeMillis()
        )
    }
}
