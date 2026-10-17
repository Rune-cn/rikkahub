package me.rerere.rikkahub.ui.components.message.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Tools
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.ai.subagent.SubAgentManager
import me.rerere.rikkahub.data.ai.tools.local.CLOSE_AGENT_TOOL_NAME
import me.rerere.rikkahub.data.ai.tools.local.LIST_AGENTS_TOOL_NAME
import me.rerere.rikkahub.data.ai.tools.local.SEND_MESSAGE_TOOL_NAME
import me.rerere.rikkahub.data.ai.tools.local.SPAWN_AGENT_TOOL_NAME
import me.rerere.rikkahub.data.ai.tools.local.WAIT_AGENT_TOOL_NAME
import me.rerere.rikkahub.data.model.SubAgentStatus
import me.rerere.rikkahub.data.model.SubAgentTask
import org.koin.compose.koinInject

/** 启动子 Agent：标题显示任务名，详情里可查看它的完整执行过程 */
object SpawnAgentToolUI : ToolUIRenderer {
    override val toolName: String = SPAWN_AGENT_TOOL_NAME

    override fun icon(context: ToolUIContext): ImageVector = HugeIcons.Tools

    @Composable
    override fun title(context: ToolUIContext): String {
        val name = context.arguments.getStringContent("task_name")
        return if (name.isNullOrBlank()) {
            stringResource(R.string.chat_message_tool_spawn_agent)
        } else {
            stringResource(R.string.chat_message_tool_spawn_agent_with_name, name)
        }
    }

    @Composable
    override fun Preview(context: ToolUIContext, onDismissRequest: () -> Unit) {
        val manager: SubAgentManager = koinInject()
        val tasks by manager.tasks.collectAsStateWithLifecycle()
        val taskName = context.arguments.getStringContent("task_name")
        val task = tasks.find { it.name == taskName }

        Column(
            modifier = Modifier
                .fillMaxHeight(0.8f)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (task == null) {
                Text(
                    text = stringResource(R.string.sub_agent_not_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                // 标题 + 状态
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = task.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    SubAgentStatusBadge(task)
                }
                task.modelName?.let { modelName ->
                    Text(
                        text = stringResource(R.string.sub_agent_model_label, modelName),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // 完整执行过程
                SubAgentConversation(messages = task.messages)
                task.error?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

/** 子 Agent 状态徽标 */
@Composable
private fun SubAgentStatusBadge(task: SubAgentTask) {
    val (labelRes, color) = when (task.status) {
        SubAgentStatus.RUNNING -> R.string.sub_agent_status_running to MaterialTheme.colorScheme.primary
        SubAgentStatus.DONE -> R.string.sub_agent_status_done to MaterialTheme.colorScheme.primary
        SubAgentStatus.FAILED -> R.string.sub_agent_status_failed to MaterialTheme.colorScheme.error
        SubAgentStatus.CANCELLED -> R.string.sub_agent_status_cancelled to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = stringResource(labelRes),
        style = MaterialTheme.typography.labelMedium,
        color = color,
    )
}

/** 只读展示子 Agent 的完整对话（文本 + 工具调用名） */
@Composable
private fun SubAgentConversation(messages: List<UIMessage>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        messages.forEach { message ->
            val texts = message.parts.filterIsInstance<UIMessagePart.Text>()
                .map { it.text }
                .filter { it.isNotBlank() }
            val tools = message.parts.filterIsInstance<UIMessagePart.Tool>()
            if (texts.isEmpty() && tools.isEmpty()) return@forEach

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = when (message.role) {
                        MessageRole.USER -> stringResource(R.string.sub_agent_role_user)
                        MessageRole.ASSISTANT -> stringResource(R.string.sub_agent_role_assistant)
                        else -> message.role.name.lowercase()
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                texts.forEach { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                tools.forEach { tool ->
                    Text(
                        text = "· ${tool.toolName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/** 等待子 Agent */
object WaitAgentToolUI : ToolUIRenderer {
    override val toolName: String = WAIT_AGENT_TOOL_NAME

    override fun icon(context: ToolUIContext): ImageVector = HugeIcons.Tools

    @Composable
    override fun title(context: ToolUIContext): String =
        stringResource(R.string.chat_message_tool_wait_agent)
}

/** 查看子 Agent */
object ListAgentsToolUI : ToolUIRenderer {
    override val toolName: String = LIST_AGENTS_TOOL_NAME

    override fun icon(context: ToolUIContext): ImageVector = HugeIcons.Tools

    @Composable
    override fun title(context: ToolUIContext): String =
        stringResource(R.string.chat_message_tool_list_agents)
}

/** 关闭子 Agent */
object CloseAgentToolUI : ToolUIRenderer {
    override val toolName: String = CLOSE_AGENT_TOOL_NAME

    override fun icon(context: ToolUIContext): ImageVector = HugeIcons.Tools

    @Composable
    override fun title(context: ToolUIContext): String {
        val name = context.arguments.getStringContent("task_name")
        return if (name.isNullOrBlank()) {
            stringResource(R.string.chat_message_tool_close_agent)
        } else {
            stringResource(R.string.chat_message_tool_close_agent) + ": " + name
        }
    }
}

/** 给已结束的子 Agent 追加消息（多轮对话） */
object SendMessageToolUI : ToolUIRenderer {
    override val toolName: String = SEND_MESSAGE_TOOL_NAME

    override fun icon(context: ToolUIContext): ImageVector = HugeIcons.Tools

    @Composable
    override fun title(context: ToolUIContext): String {
        val name = context.arguments.getStringContent("task_name")
        return if (name.isNullOrBlank()) {
            stringResource(R.string.chat_message_tool_send_message)
        } else {
            stringResource(R.string.chat_message_tool_send_message_with_name, name)
        }
    }
}

/** 输入框上方的子 Agent 运行进度（有子 Agent 运行时显示） */
@Composable
internal fun SubAgentRunningBanner(
    count: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
            )
            Text(
                text = stringResource(R.string.chat_sub_agent_running, count),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
