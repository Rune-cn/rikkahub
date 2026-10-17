package me.rerere.rikkahub.data.model

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/** 工作流步骤类型 */
@Serializable
enum class WorkflowStepType {
    /** 调用模型生成文本（不带工具，快） */
    PROMPT,

    /** 以 Agent 方式执行（带完整工具集） */
    AGENT,
}

/**
 * 工作流步骤
 *
 * 提示词支持变量：
 * - `{{input}}`：本次运行的初始输入
 * - `{{prev}}`：上一步的输出
 */
@Serializable
data class WorkflowStep(
    val id: String = Uuid.random().toString(),
    val name: String = "",
    val type: WorkflowStepType = WorkflowStepType.PROMPT,
    /** 指定模型；null 表示用当前模型 */
    val modelId: Uuid? = null,
    val prompt: String = "",
)

/**
 * AI 工作流：把多个步骤（模型调用 / Agent 执行）串成一条可一键运行的流水线
 */
@Serializable
data class Workflow(
    val id: String = Uuid.random().toString(),
    val name: String = "",
    val description: String = "",
    val steps: List<WorkflowStep> = emptyList(),
)

/** 工作流单步运行状态 */
@Serializable
enum class WorkflowStepStatus {
    PENDING,
    RUNNING,
    DONE,
    FAILED,
}

/** 工作流运行中的状态快照（供 UI 展示） */
data class WorkflowRunState(
    val stepIndex: Int,
    val stepName: String,
    val status: WorkflowStepStatus,
    val output: String? = null,
    val error: String? = null,
)
