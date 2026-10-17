package me.rerere.rikkahub.data.model

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/**
 * 工作流步骤类型（旧版线性步骤，已由 [WorkflowNode] 取代，保留兼容旧数据）
 */
@Serializable
enum class WorkflowStepType {
    /** 调用模型生成文本（不带工具，快） */
    PROMPT,

    /** 以 Agent 方式执行（带完整工具集） */
    AGENT,
}

/**
 * 旧版线性步骤（deprecated，保留用于兼容旧数据）
 *
 * 提示词支持变量：`{{input}}`（初始输入）、`{{prev}}`（上一步输出）
 */
@Serializable
data class WorkflowStep(
    val id: String = Uuid.random().toString(),
    val name: String = "",
    val type: WorkflowStepType = WorkflowStepType.PROMPT,
    val modelId: Uuid? = null,
    val prompt: String = "",
)

/**
 * 工作流节点类型（对齐业界 agentic workflow 模式）
 *
 * - [PROMPT]：模型调用（Anthropic Prompt Chaining 的基础步骤）
 * - [AGENT]：带完整工具集的 Agent 执行
 * - [IF_ELSE]：条件分支（Routing 模式），按条件走 true/false 分支
 * - [PARALLEL]：并行分支（Parallelization 模式），children 并发执行
 */
@Serializable
enum class WorkflowNodeType {
    PROMPT,
    AGENT,
    IF_ELSE,
    PARALLEL,
}

/**
 * 工作流节点（树形结构，天然支持分支与嵌套）
 *
 * 结构约定：
 * - 工作流顶层 [Workflow.nodes] 为**顺序序列**（依次执行）
 * - 普通节点执行后结束（无后续）
 * - [IF_ELSE]：条件命中走 [trueBranch]，否则走 [falseBranch]
 * - [PARALLEL]：所有 [children] 并发执行
 * - 任一分支内部可继续嵌套上述结构
 *
 * 提示词变量：`{{input}}`（初始输入）、`{{prev}}`（当前序列上一个节点的输出）
 */
@Serializable
data class WorkflowNode(
    val id: String = Uuid.random().toString(),
    val name: String = "",
    val type: WorkflowNodeType = WorkflowNodeType.PROMPT,
    /** 指定模型；null 表示用当前模型 */
    val modelId: Uuid? = null,
    val prompt: String = "",
    /** IF_ELSE 条件表达式：如 `{{prev}} contains "错误"`（不区分大小写） */
    val condition: String = "",
    /** IF_ELSE 条件命中时执行的分支 */
    val trueBranch: WorkflowNode? = null,
    /** IF_ELSE 条件未命中时执行的分支 */
    val falseBranch: WorkflowNode? = null,
    /** PARALLEL 的并行子分支 */
    val children: List<WorkflowNode> = emptyList(),
    /** 画布坐标（由自动布局或用户拖拽产生） */
    val x: Float = Float.NaN,
    val y: Float = Float.NaN,
)

/**
 * AI 工作流：把多个节点（模型调用 / Agent / 条件分支 / 并行）编排成可一键运行的流水线
 */
@Serializable
data class Workflow(
    val id: String = Uuid.random().toString(),
    val name: String = "",
    val description: String = "",
    /** 旧版线性步骤（deprecated，兼容旧数据；nodes 非空时优先用 nodes） */
    val steps: List<WorkflowStep> = emptyList(),
    /** 新版：入口节点序列（依次执行） */
    val nodes: List<WorkflowNode> = emptyList(),
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
    /** 新版节点 id（树形执行时用 nodeId 匹配状态） */
    val nodeId: String? = null,
)
