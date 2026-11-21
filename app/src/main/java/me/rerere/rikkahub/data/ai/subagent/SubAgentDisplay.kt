package me.rerere.rikkahub.data.ai.subagent

import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.ToolApprovalState
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.data.model.SubAgentStatus
import me.rerere.rikkahub.data.model.SubAgentTask

/**
 * 主对话里那条「子 Agent」消息应该显示哪些 part。
 *
 * 子 Agent 一次任务会产生多条助手消息：每次模型调用一条，工具结果回来后再开下一条。
 * 只取"当前这条"会让上一步的工具调用在进入下一步时凭空消失——这正是
 * 「主对话看不到子 Agent 工具调用」的根因。
 *
 * [streaming] 是正在生成、可能还没写进 [SubAgentTask.messages] 的那条消息；
 * 若它与最后一条助手消息是同一条（createdAt 相同），以 streaming 的最新版本为准，
 * 避免 combine 的两个 Flow 不同步时重复追加。
 *
 * [finished] 为 true 时，把没有结果的工具调用标成已结束，避免主对话里留下
 * 永远等不到审批的按钮。
 */
internal fun buildSubAgentDisplayParts(
    task: SubAgentTask,
    streaming: UIMessage?,
    finished: Boolean,
    noResultText: String,
): List<UIMessagePart> {
    val assistantMessages = task.messages.filter { it.role == MessageRole.ASSISTANT }
    val orderedMessages = when {
        streaming == null -> assistantMessages
        assistantMessages.lastOrNull()?.createdAt == streaming.createdAt ->
            assistantMessages.dropLast(1) + streaming
        assistantMessages.lastOrNull() == streaming -> assistantMessages
        else -> assistantMessages + streaming
    }

    return buildList {
        orderedMessages.forEach { message ->
            message.parts.forEach { part ->
                add(
                    if (finished && part is UIMessagePart.Tool && !part.isExecuted) {
                        part.copy(
                            output = listOf(UIMessagePart.Text(noResultText)),
                            approvalState = ToolApprovalState.Auto,
                        )
                    } else {
                        part
                    }
                )
            }
        }
        if (finished && task.status == SubAgentStatus.FAILED && !task.error.isNullOrBlank()) {
            add(UIMessagePart.Text("\n" + task.error))
        }
    }
}