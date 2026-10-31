package me.rerere.rikkahub.data.ai.tools.local

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.uuid.Uuid
import me.rerere.ai.core.InputSchema
import me.rerere.ai.core.Tool
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.model.Workflow
import me.rerere.rikkahub.data.model.WorkflowNode
import me.rerere.rikkahub.data.model.WorkflowNodeType

internal const val LIST_WORKFLOWS_TOOL_NAME = "list_workflows"
internal const val CREATE_WORKFLOW_TOOL_NAME = "create_workflow"
internal const val UPDATE_WORKFLOW_TOOL_NAME = "update_workflow"
internal const val DELETE_WORKFLOW_TOOL_NAME = "delete_workflow"

/**
 * 工作流管理工具集：让 AI 自己创建/修改/删除 AI 工作流
 *
 * - `list_workflows`：查看已有工作流（返回 nodes 树）
 * - `create_workflow`：新建工作流（可含多个节点，支持顺序/条件分支/并行）
 * - `update_workflow`：修改名称/描述/节点
 * - `delete_workflow`：删除工作流
 */
internal fun buildWorkflowTools(settingsStore: SettingsStore): List<Tool> = listOf(
    listWorkflowsTool(settingsStore),
    createWorkflowTool(settingsStore),
    updateWorkflowTool(settingsStore),
    deleteWorkflowTool(settingsStore),
)

private val NODES_DESCRIPTION = """
    Ordered nodes executed in sequence. Each node: name (optional), type ("prompt" = plain model call,
    "agent" = full toolset, "if_else" = conditional branch, "parallel" = concurrent children),
    prompt (required for prompt/agent; supports {{input}} for the initial input and {{prev}} for the previous
    node's output), model (optional model id override). For if_else: condition string like
    {{prev}} contains "keyword" (also supports {{input}} and {{node.<id>}}); true_branch/false_branch are single
    child nodes (or null). For parallel: children is a list of nodes to run concurrently.
""".trimIndent().replace("\n", " ")

private fun nodeSchema(): JsonObject = buildJsonObject {
    put("type", "object")
    put("properties", buildJsonObject {
        put("name", buildJsonObject {
            put("type", "string")
            put("description", "Node name, shown in the UI.")
        })
        put("type", buildJsonObject {
            put("type", "string")
            put("enum", buildJsonArray {
                add("prompt")
                add("agent")
                add("if_else")
                add("parallel")
            })
            put("description", "Node type: prompt (no tools), agent (full toolset), if_else (conditional branch), parallel (concurrent children).")
        })
        put("prompt", buildJsonObject {
            put("type", "string")
            put("description", "Node prompt; supports {{input}} and {{prev}}.")
        })
        put("model", buildJsonObject {
            put("type", "string")
            put("description", "Optional model id override for this node.")
        })
        put("condition", buildJsonObject {
            put("type", "string")
            put("description", "Only for if_else: expression like {{prev}} contains \"keyword\" (case-insensitive; also supports {{input}} and {{node.<id>}}).")
        })
        put("true_branch", nodeSchema())
        put("false_branch", nodeSchema())
        put("children", buildJsonObject {
            put("type", "array")
            put("description", "Only for parallel: list of nodes to run concurrently.")
            put("items", nodeSchema())
        })
    })
    put("required", buildJsonArray { add("type") })
}

private fun parseNode(element: JsonElement?): WorkflowNode? {
    val obj = element as? JsonObject ?: return null
    val type = when (obj["type"]?.jsonPrimitive?.contentOrNull?.lowercase()) {
        "if_else" -> WorkflowNodeType.IF_ELSE
        "parallel" -> WorkflowNodeType.PARALLEL
        "agent" -> WorkflowNodeType.AGENT
        else -> WorkflowNodeType.PROMPT
    }
    return WorkflowNode(
        name = obj["name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty(),
        type = type,
        prompt = obj["prompt"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty(),
        modelId = obj["model"]?.jsonPrimitive?.contentOrNull?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { Uuid.parse(it) }.getOrNull() },
        condition = obj["condition"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty(),
        trueBranch = parseNode(obj["true_branch"]),
        falseBranch = parseNode(obj["false_branch"]),
        children = obj["children"]?.jsonArray?.mapNotNull { parseNode(it) } ?: emptyList(),
    )
}

private fun WorkflowNode.toJson(): JsonObject = buildJsonObject {
    put("name", name)
    put("type", type.name.lowercase())
    if (prompt.isNotBlank()) put("prompt", prompt)
    if (condition.isNotBlank()) put("condition", condition)
    trueBranch?.let { put("true_branch", it.toJson()) }
    falseBranch?.let { put("false_branch", it.toJson()) }
    if (children.isNotEmpty()) {
        put("children", buildJsonArray { children.forEach { add(it.toJson()) } })
    }
}

private fun Workflow.toJson(): JsonObject = buildJsonObject {
    put("id", id)
    put("name", name)
    put("description", description)
    put("enabled", enabled)
    put("nodes", buildJsonArray {
        nodes.forEach { add(it.toJson()) }
    })
}

private fun listWorkflowsTool(settingsStore: SettingsStore): Tool = Tool(
    name = LIST_WORKFLOWS_TOOL_NAME,
    description = "List all AI workflows (reusable pipelines) with their id, name, description and nodes.",
    parameters = {
        InputSchema.Obj(properties = buildJsonObject { })
    },
    needsApproval = { false },
    execute = {
        val settings = settingsStore.settingsFlow.value
        val payload = buildJsonObject {
            put("workflows", buildJsonArray {
                settings.workflows.forEach { add(it.toJson()) }
            })
        }
        listOf(UIMessagePart.Text(payload.toString()))
    }
)

private fun createWorkflowTool(settingsStore: SettingsStore): Tool = Tool(
    name = CREATE_WORKFLOW_TOOL_NAME,
    description = """
        Create a new AI workflow: a reusable pipeline of nodes that runs in order.
        Use it when the user asks for a repeatable multi-step process.
        Nodes can be prompt/agent (both run the model; agent also has the full toolset),
        if_else (conditional branch) and parallel (concurrent children).
        Each node's prompt supports {{input}} (the initial input) and {{prev}} (previous node's output).
    """.trimIndent().replace("\n", " "),
    parameters = {
        InputSchema.Obj(
            properties = buildJsonObject {
                put("name", buildJsonObject {
                    put("type", "string")
                    put("description", "Workflow name.")
                })
                put("description", buildJsonObject {
                    put("type", "string")
                    put("description", "Short description of what the workflow does.")
                })
                put("nodes", buildJsonObject {
                    put("type", "array")
                    put("description", NODES_DESCRIPTION)
                    put("items", nodeSchema())
                })
            },
            required = listOf("name", "nodes")
        )
    },
    needsApproval = { false },
    execute = { args ->
        val obj = args.jsonObject
        val name = obj["name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val nodes = obj["nodes"]?.jsonArray?.mapNotNull { parseNode(it) } ?: emptyList()
        val payload = if (name.isBlank() || nodes.isEmpty()) {
            buildJsonObject {
                put("success", false)
                put("error", "name and at least one valid node are required")
            }
        } else {
            val workflow = Workflow(
                name = name,
                description = obj["description"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty(),
                nodes = nodes,
            )
            settingsStore.update { s -> s.copy(workflows = s.workflows + workflow) }
            buildJsonObject {
                put("success", true)
                put("workflow", workflow.toJson())
            }
        }
        listOf(UIMessagePart.Text(payload.toString()))
    }
)

private fun updateWorkflowTool(settingsStore: SettingsStore): Tool = Tool(
    name = UPDATE_WORKFLOW_TOOL_NAME,
    description = """
        Update an existing AI workflow by id (get ids from list_workflows).
        Only the fields you pass are changed; pass nodes to replace all nodes.
    """.trimIndent().replace("\n", " "),
    parameters = {
        InputSchema.Obj(
            properties = buildJsonObject {
                put("workflow_id", buildJsonObject {
                    put("type", "string")
                    put("description", "Id of the workflow to update.")
                })
                put("name", buildJsonObject {
                    put("type", "string")
                    put("description", "New name (optional).")
                })
                put("description", buildJsonObject {
                    put("type", "string")
                    put("description", "New description (optional).")
                })
                put("nodes", buildJsonObject {
                    put("type", "array")
                    put("description", NODES_DESCRIPTION)
                    put("items", nodeSchema())
                })
            },
            required = listOf("workflow_id")
        )
    },
    needsApproval = { false },
    execute = { args ->
        val obj = args.jsonObject
        val id = obj["workflow_id"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val existing = settingsStore.settingsFlow.value.workflows.find { it.id == id }
        val payload = if (existing == null) {
            buildJsonObject {
                put("success", false)
                put("error", "workflow '$id' not found")
            }
        } else {
            val nodes = obj["nodes"]?.jsonArray?.mapNotNull { parseNode(it) }
            val updated = existing.copy(
                name = obj["name"]?.jsonPrimitive?.contentOrNull?.trim()
                    ?.takeIf { it.isNotBlank() } ?: existing.name,
                description = obj["description"]?.jsonPrimitive?.contentOrNull?.trim()
                    ?: existing.description,
                nodes = nodes?.takeIf { it.isNotEmpty() } ?: existing.nodes,
            )
            settingsStore.update { s ->
                s.copy(workflows = s.workflows.map { if (it.id == id) updated else it })
            }
            buildJsonObject {
                put("success", true)
                put("workflow", updated.toJson())
            }
        }
        listOf(UIMessagePart.Text(payload.toString()))
    }
)

private fun deleteWorkflowTool(settingsStore: SettingsStore): Tool = Tool(
    name = DELETE_WORKFLOW_TOOL_NAME,
    description = "Delete an AI workflow by id (get ids from list_workflows).",
    parameters = {
        InputSchema.Obj(
            properties = buildJsonObject {
                put("workflow_id", buildJsonObject {
                    put("type", "string")
                    put("description", "Id of the workflow to delete.")
                })
            },
            required = listOf("workflow_id")
        )
    },
    needsApproval = { true },
    execute = { args ->
        val id = args.jsonObject["workflow_id"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val exists = settingsStore.settingsFlow.value.workflows.any { it.id == id }
        if (exists) {
            settingsStore.update { s -> s.copy(workflows = s.workflows.filterNot { it.id == id }) }
        }
        val payload = buildJsonObject {
            put("success", exists)
            if (!exists) put("error", "workflow '$id' not found")
        }
        listOf(UIMessagePart.Text(payload.toString()))
    }
)