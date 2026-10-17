package me.rerere.rikkahub.data.ai.tools.local

import kotlinx.serialization.json.JsonArray
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
import me.rerere.rikkahub.data.model.WorkflowStep
import me.rerere.rikkahub.data.model.WorkflowStepType

internal const val LIST_WORKFLOWS_TOOL_NAME = "list_workflows"
internal const val CREATE_WORKFLOW_TOOL_NAME = "create_workflow"
internal const val UPDATE_WORKFLOW_TOOL_NAME = "update_workflow"
internal const val DELETE_WORKFLOW_TOOL_NAME = "delete_workflow"

/**
 * 工作流管理工具集：让 AI 自己创建/修改/删除 AI 工作流
 *
 * - `list_workflows`：查看已有工作流
 * - `create_workflow`：新建工作流（可含多个步骤）
 * - `update_workflow`：修改名称/描述/步骤
 * - `delete_workflow`：删除工作流
 */
internal fun buildWorkflowTools(settingsStore: SettingsStore): List<Tool> = listOf(
    listWorkflowsTool(settingsStore),
    createWorkflowTool(settingsStore),
    updateWorkflowTool(settingsStore),
    deleteWorkflowTool(settingsStore),
)

private const val STEPS_DESCRIPTION = """
    Ordered steps. Each step: name (optional), type ("prompt" = plain model call, "agent" = full toolset),
    prompt (required; supports {{input}} for the initial input and {{prev}} for the previous step output),
    model (optional model id override).
""".trimIndent().replace("\n", " ")

private fun stepsSchema(): JsonObject = buildJsonObject {
    put("type", "array")
    put("description", STEPS_DESCRIPTION)
    put("items", buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            put("name", buildJsonObject {
                put("type", "string")
                put("description", "Step name, shown in the UI.")
            })
            put("type", buildJsonObject {
                put("type", "string")
                put("enum", buildJsonArray {
                    add("prompt")
                    add("agent")
                })
                put("description", "Step type: prompt (no tools) or agent (with tools).")
            })
            put("prompt", buildJsonObject {
                put("type", "string")
                put("description", "Step prompt; supports {{input}} and {{prev}}.")
            })
            put("model", buildJsonObject {
                put("type", "string")
                put("description", "Optional model id override for this step.")
            })
        })
        put("required", buildJsonArray { add("prompt") })
    })
}

private fun parseSteps(array: JsonArray?): List<WorkflowStep> =
    array?.mapNotNull { element ->
        val obj = element as? JsonObject ?: return@mapNotNull null
        val prompt = obj["prompt"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        if (prompt.isBlank()) return@mapNotNull null
        WorkflowStep(
            name = obj["name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty(),
            type = when (obj["type"]?.jsonPrimitive?.contentOrNull?.lowercase()) {
                "agent" -> WorkflowStepType.AGENT
                else -> WorkflowStepType.PROMPT
            },
            prompt = prompt,
            modelId = obj["model"]?.jsonPrimitive?.contentOrNull?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { runCatching { Uuid.parse(it) }.getOrNull() },
        )
    } ?: emptyList()

private fun Workflow.toJson(): JsonObject = buildJsonObject {
    put("id", id)
    put("name", name)
    put("description", description)
    put("steps", buildJsonArray {
        steps.forEach { step ->
            add(buildJsonObject {
                put("name", step.name)
                put("type", step.type.name.lowercase())
                put("prompt", step.prompt)
            })
        }
    })
}

private fun listWorkflowsTool(settingsStore: SettingsStore): Tool = Tool(
    name = LIST_WORKFLOWS_TOOL_NAME,
    description = "List all AI workflows (reusable multi-step pipelines) with their id, name and steps.",
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
        Create a new AI workflow: a reusable pipeline of steps that runs in order.
        Use it when the user asks for a repeatable multi-step process.
        Each step's prompt supports {{input}} (the initial input) and {{prev}} (previous step output).
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
                put("steps", stepsSchema())
            },
            required = listOf("name", "steps")
        )
    },
    needsApproval = { false },
    execute = { args ->
        val obj = args.jsonObject
        val name = obj["name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val steps = parseSteps(obj["steps"]?.jsonArray)
        val payload = if (name.isBlank() || steps.isEmpty()) {
            buildJsonObject {
                put("success", false)
                put("error", "name and at least one valid step (with prompt) are required")
            }
        } else {
            val workflow = Workflow(
                name = name,
                description = obj["description"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty(),
                steps = steps,
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
        Only the fields you pass are changed; pass steps to replace all steps.
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
                put("steps", stepsSchema())
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
            val steps = obj["steps"]?.jsonArray?.let { parseSteps(it) }
            val updated = existing.copy(
                name = obj["name"]?.jsonPrimitive?.contentOrNull?.trim()
                    ?.takeIf { it.isNotBlank() } ?: existing.name,
                description = obj["description"]?.jsonPrimitive?.contentOrNull?.trim()
                    ?: existing.description,
                steps = steps?.takeIf { it.isNotEmpty() } ?: existing.steps,
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
