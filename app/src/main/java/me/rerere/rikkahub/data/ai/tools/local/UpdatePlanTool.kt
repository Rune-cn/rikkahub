package me.rerere.rikkahub.data.ai.tools.local

import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import me.rerere.ai.core.InputSchema
import me.rerere.ai.core.Tool
import me.rerere.ai.ui.UIMessagePart

internal const val UPDATE_PLAN_TOOL_NAME = "update_plan"

/** 待办步骤状态（与 Codex 的 update_plan 工具保持一致） */
internal const val PLAN_STATUS_PENDING = "pending"
internal const val PLAN_STATUS_IN_PROGRESS = "in_progress"
internal const val PLAN_STATUS_COMPLETED = "completed"

/**
 * 待办清单工具（Codex 同款 update_plan）
 *
 * 工具本身没有副作用：计划内容就写在入参里，UI 直接解析入参渲染成 checklist，
 * 因此无需额外存储，也不需要用户审批。
 */
internal fun buildUpdatePlanTool(): Tool = Tool(
    name = UPDATE_PLAN_TOOL_NAME,
    description = """
        Updates the task plan: a checklist of steps shown to the user.
        Provide an optional explanation and a list of plan items, each with a step and a status
        (pending, in_progress or completed).
        Rules:
        - Keep each step short: one sentence, about 5-7 words.
        - At most one step can be in_progress at a time.
        - Never jump a step from pending straight to completed; mark it in_progress first.
        - When the plan changes, call this tool again and provide an explanation.
        - Do not repeat the full plan in your reply; the UI already displays it.
        - Do not use a plan for simple or single-step requests.
        - Finish with all steps completed.
    """.trimIndent().replace("\n", " "),
    parameters = {
        InputSchema.Obj(
            properties = buildJsonObject {
                put("explanation", buildJsonObject {
                    put("type", "string")
                    put("description", "Optional explanation for this plan update.")
                })
                put("plan", buildJsonObject {
                    put("type", "array")
                    put("description", "The list of steps.")
                    put("items", buildJsonObject {
                        put("type", "object")
                        put("properties", buildJsonObject {
                            put("step", buildJsonObject {
                                put("type", "string")
                                put("description", "Task step text.")
                            })
                            put("status", buildJsonObject {
                                put("type", "string")
                                put("enum", buildJsonArray {
                                    add(PLAN_STATUS_PENDING)
                                    add(PLAN_STATUS_IN_PROGRESS)
                                    add(PLAN_STATUS_COMPLETED)
                                })
                                put("description", "Step status.")
                            })
                        })
                        put("required", buildJsonArray {
                            add("step")
                            add("status")
                        })
                    })
                })
            },
            required = listOf("plan")
        )
    },
    needsApproval = { false },
    execute = {
        // 计划通过工具入参直接渲染到 UI，这里只需返回确认文本
        listOf(UIMessagePart.Text("Plan updated"))
    }
)
