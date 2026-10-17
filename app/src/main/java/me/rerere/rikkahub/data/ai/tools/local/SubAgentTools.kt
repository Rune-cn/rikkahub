package me.rerere.rikkahub.data.ai.tools.local

import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.uuid.Uuid
import me.rerere.ai.core.InputSchema
import me.rerere.ai.core.Tool
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.data.ai.subagent.SubAgentManager

internal const val SPAWN_AGENT_TOOL_NAME = "spawn_agent"
internal const val WAIT_AGENT_TOOL_NAME = "wait_agent"
internal const val LIST_AGENTS_TOOL_NAME = "list_agents"
internal const val CLOSE_AGENT_TOOL_NAME = "close_agent"
internal const val SEND_MESSAGE_TOOL_NAME = "send_message"

/** 子 Agent 管理工具名集合（子 Agent 自身不再携带，避免无限递归） */
internal val SUB_AGENT_TOOL_NAMES = setOf(
    SPAWN_AGENT_TOOL_NAME,
    WAIT_AGENT_TOOL_NAME,
    LIST_AGENTS_TOOL_NAME,
    CLOSE_AGENT_TOOL_NAME,
    SEND_MESSAGE_TOOL_NAME,
)

/**
 * 子 Agent 协同工具集（主 Agent 用来指挥子 Agent）
 *
 * - `spawn_agent`：启动子 Agent（异步，立即返回）
 * - `wait_agent`：等待子 Agent 完成并取回结果
 * - `send_message`：给已结束的子 Agent 追加消息，继续多轮对话
 * - `list_agents`：查看所有子 Agent 状态
 * - `close_agent`：取消子 Agent
 */
internal fun buildSubAgentTools(manager: SubAgentManager): List<Tool> = listOf(
    spawnAgentTool(manager),
    waitAgentTool(manager),
    sendMessageTool(manager),
    listAgentsTool(manager),
    closeAgentTool(manager),
)

private fun spawnAgentTool(manager: SubAgentManager): Tool = Tool(
    name = SPAWN_AGENT_TOOL_NAME,
    description = """
        Spawn a sub-agent to work on a task in the background.
        The sub-agent runs its own agent loop with a full toolset and may use a different model;
        it does NOT see your conversation, so the message must contain everything it needs.
        It starts immediately and keeps running while you continue working;
        use wait_agent to collect its result.
        Delegate independent work (research, analysis, long-running tasks) so you can focus on decisions.
        - task_name: short unique name, used later by wait_agent / close_agent / send_message
        - message: full task description for the sub-agent
        - model: optional model id override; omit to inherit your current model
        - wait: defaults to true: blocks until the sub-agent finishes and returns its result + status.
          Set false only for fire-and-forget (then you MUST call wait_agent later to get the result)
        - inherit_context: set true to give the sub-agent your recent conversation history
          (default false: it only sees the message, so include everything it needs)
        - Up to ${SubAgentManager.MAX_CONCURRENT_SUB_AGENTS} sub-agents can run at the same time.
    """.trimIndent().replace("\n", " "),
    parameters = {
        InputSchema.Obj(
            properties = buildJsonObject {
                put("task_name", buildJsonObject {
                    put("type", "string")
                    put("description", "Short unique name for this sub-agent task.")
                })
                put("message", buildJsonObject {
                    put("type", "string")
                    put("description", "Full task description; the sub-agent cannot see your conversation unless inherit_context is true.")
                })
                put("model", buildJsonObject {
                    put("type", "string")
                    put("description", "Optional model id override. Omit to inherit the current model.")
                })
                put("inherit_context", buildJsonObject {
                    put("type", "boolean")
                    put("description", "Whether to pass your recent conversation history to the sub-agent (default false).")
                })
                put("wait", buildJsonObject {
                    put("type", "boolean")
                    put("description", "Set true to block until the sub-agent finishes and return its full result + transcript (default false).")
                })
            },
            required = listOf("task_name", "message")
        )
    },
    needsApproval = { false },
    execute = { args ->
        val obj = args.jsonObject
        val name = obj["task_name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val message = obj["message"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val modelId = obj["model"]?.jsonPrimitive?.contentOrNull?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { Uuid.parse(it) }.getOrNull() }
        val inheritContext = obj["inherit_context"]?.jsonPrimitive?.booleanOrNull
        val wait = obj["wait"]?.jsonPrimitive?.booleanOrNull ?: true

        val payload = if (name.isBlank() || message.isBlank()) {
            buildJsonObject {
                put("success", false)
                put("error", "task_name and message are required")
            }
        } else {
            manager.spawn(
                name = name,
                prompt = message,
                modelId = modelId,
                inheritContext = inheritContext,
            ).fold(
                onSuccess = { task ->
                    if (wait) {
                        val finished = manager.waitFor(listOf(task.name), 300_000L).firstOrNull()
                        buildJsonObject {
                            put("success", true)
                            put("task_name", task.name)
                            put("status", finished?.status?.name ?: task.status.name)
                            finished?.result?.let { put("result", it) }
                            finished?.error?.let { put("error", it) }
                        }
                    } else {
                        buildJsonObject {
                            put("success", true)
                            put("task_name", task.name)
                            put("status", task.status.name)
                        }
                    }
                },
                onFailure = { error ->
                    buildJsonObject {
                        put("success", false)
                        put("error", error.message ?: error.toString())
                    }
                },
            )
        }
        listOf(UIMessagePart.Text(payload.toString()))
    }
)

private fun waitAgentTool(manager: SubAgentManager): Tool = Tool(
    name = WAIT_AGENT_TOOL_NAME,
    description = """
        Wait for one or more sub-agents to finish, then return their status and result.
        Returns the current state immediately if all requested sub-agents already finished.
        If the timeout is reached, returns their status at that moment (which may still be RUNNING),
        in that case you can call wait_agent again later.
    """.trimIndent().replace("\n", " "),
    parameters = {
        InputSchema.Obj(
            properties = buildJsonObject {
                put("task_names", buildJsonObject {
                    put("type", "array")
                    put("description", "Sub-agent task names to wait for.")
                    put("items", buildJsonObject { put("type", "string") })
                })
                put("timeout_seconds", buildJsonObject {
                    put("type", "number")
                    put("description", "Maximum seconds to wait (default 300, max 900).")
                })
            },
            required = listOf("task_names")
        )
    },
    needsApproval = { false },
    execute = { args ->
        val obj = args.jsonObject
        val names = obj["task_names"]?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.contentOrNull?.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
        val timeoutSeconds = (obj["timeout_seconds"]?.jsonPrimitive?.intOrNull ?: 300)
            .coerceIn(1, 900)

        val tasks = if (names.isEmpty()) emptyList() else manager.waitFor(names, timeoutSeconds * 1000L)
        val payload = buildJsonObject {
            put("tasks", buildJsonArray {
                tasks.forEach { task ->
                    add(
                        buildJsonObject {
                            put("task_name", task.name)
                            put("status", task.status.name)
                            task.result?.let { put("result", it) }
                            task.error?.let { put("error", it) }
                            // 完整过程：让主 Agent 看到子 Agent 说了什么、调了哪些工具
                            put("transcript", buildJsonArray {
                                task.messages.forEach { msg ->
                                    val texts = msg.parts
                                        .filterIsInstance<UIMessagePart.Text>()
                                        .joinToString("\n") { it.text }
                                        .trim()
                                    val tools = msg.parts
                                        .filterIsInstance<UIMessagePart.Tool>()
                                        .map { it.toolName }
                                    if (texts.isNotBlank() || tools.isNotEmpty()) {
                                        add(buildJsonObject {
                                            put("role", msg.role.name.lowercase())
                                            if (texts.isNotBlank()) put("text", texts)
                                            if (tools.isNotEmpty()) {
                                                put("tools", buildJsonArray {
                                                    tools.forEach { add(it) }
                                                })
                                            }
                                        })
                                    }
                                }
                            })
                        }
                    )
                }
            })
            if (names.isNotEmpty() && tasks.isEmpty()) {
                put("error", "no matching sub-agent found")
            }
        }
        listOf(UIMessagePart.Text(payload.toString()))
    }
)

private fun sendMessageTool(manager: SubAgentManager): Tool = Tool(
    name = SEND_MESSAGE_TOOL_NAME,
    description = """
        Send a follow-up message to a finished sub-agent to continue its work (multi-turn).
        The sub-agent keeps its previous context and runs again with the new instruction;
        its result will be updated when it finishes (use wait_agent to collect it).
        Only finished sub-agents can receive messages; if it is still running, wait_agent first.
    """.trimIndent().replace("\n", " "),
    parameters = {
        InputSchema.Obj(
            properties = buildJsonObject {
                put("task_name", buildJsonObject {
                    put("type", "string")
                    put("description", "Sub-agent task name to continue.")
                })
                put("message", buildJsonObject {
                    put("type", "string")
                    put("description", "Follow-up instruction for the sub-agent.")
                })
            },
            required = listOf("task_name", "message")
        )
    },
    needsApproval = { false },
    execute = { args ->
        val obj = args.jsonObject
        val name = obj["task_name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val message = obj["message"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val payload = if (name.isBlank() || message.isBlank()) {
            buildJsonObject {
                put("success", false)
                put("error", "task_name and message are required")
            }
        } else {
            manager.sendMessage(name, message).fold(
                onSuccess = { task ->
                    buildJsonObject {
                        put("success", true)
                        put("task_name", task.name)
                        put("status", task.status.name)
                    }
                },
                onFailure = { error ->
                    buildJsonObject {
                        put("success", false)
                        put("error", error.message ?: error.toString())
                    }
                },
            )
        }
        listOf(UIMessagePart.Text(payload.toString()))
    }
)

private fun listAgentsTool(manager: SubAgentManager): Tool = Tool(
    name = LIST_AGENTS_TOOL_NAME,
    description = "List all sub-agents with their current status and result. Use it to check progress without waiting.",
    parameters = {
        InputSchema.Obj(
            properties = buildJsonObject { }
        )
    },
    needsApproval = { false },
    execute = {
        val payload = buildJsonObject {
            put("agents", buildJsonArray {
                manager.list().forEach { task ->
                    add(
                        buildJsonObject {
                            put("task_name", task.name)
                            put("status", task.status.name)
                            put("prompt", task.prompt)
                            task.result?.let { put("result", it) }
                            task.error?.let { put("error", it) }
                        }
                    )
                }
            })
        }
        listOf(UIMessagePart.Text(payload.toString()))
    }
)

private fun closeAgentTool(manager: SubAgentManager): Tool = Tool(
    name = CLOSE_AGENT_TOOL_NAME,
    description = "Cancel a running sub-agent. Use it when its work is no longer needed.",
    parameters = {
        InputSchema.Obj(
            properties = buildJsonObject {
                put("task_name", buildJsonObject {
                    put("type", "string")
                    put("description", "Sub-agent task name to cancel.")
                })
            },
            required = listOf("task_name")
        )
    },
    needsApproval = { false },
    execute = { args ->
        val name = args.jsonObject["task_name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val closed = name.isNotBlank() && manager.close(name)
        val payload = buildJsonObject {
            put("success", closed)
            if (!closed) put("error", "sub-agent '$name' not found")
        }
        listOf(UIMessagePart.Text(payload.toString()))
    }
)
