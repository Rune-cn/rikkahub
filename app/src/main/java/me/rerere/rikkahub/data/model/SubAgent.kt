package me.rerere.rikkahub.data.model

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/** 子 Agent 运行状态 */
@Serializable
enum class SubAgentStatus {
    /** 运行中 */
    RUNNING,

    /** 已完成 */
    DONE,

    /** 执行失败 */
    FAILED,

    /** 被主 Agent 或用户取消 */
    CANCELLED,
}

/**
 * 子 Agent 任务
 *
 * 由主 Agent 通过 `spawn_agent` 工具创建，在独立协程里运行自己的生成循环
 * （拥有完整工具能力，可使用不同模型），完成后把结果回填到 [result]。
 */
@Serializable
data class SubAgentTask(
    val id: String,
    /** 任务名（spawn 时指定，供 wait_agent / close_agent 定位） */
    val name: String,
    /** 任务说明，作为子 Agent 的初始用户消息 */
    val prompt: String,
    /** 模型覆盖；null 表示继承主 Agent 当前模型 */
    val modelId: Uuid? = null,
    val status: SubAgentStatus = SubAgentStatus.RUNNING,
    /** 完成后的最终回答 */
    val result: String? = null,
    /** 失败原因 */
    val error: String? = null,
) {
    val isFinished: Boolean
        get() = status != SubAgentStatus.RUNNING
}
