package me.rerere.rikkahub.ui.components.message.tools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import me.rerere.rikkahub.utils.JsonInstantPretty
import me.rerere.rikkahub.ui.theme.JetbrainsMono
import me.rerere.rikkahub.ui.components.richtext.MarkdownBlock
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.remember
import androidx.compose.material3.Icon
import androidx.compose.foundation.shape.CircleShape

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
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = task.name,
                        style = MaterialTheme.typography.headlineSmallEmphasized,
                        modifier = Modifier.weight(1f),
                    )
                    SubAgentStatusBadge(task)
                }
                // 模型 / 步数 / 工具调用 / 耗时
                val elapsedSeconds = remember(task.startedAt, task.finishedAt, task.status) {
                    ((task.finishedAt ?: System.currentTimeMillis()) - task.startedAt).coerceAtLeast(0) / 1000
                }
                val stepCount = task.messages.count { it.role == MessageRole.ASSISTANT && it.parts.isNotEmpty() }
                val toolCallCount = task.messages.sumOf { message ->
                    message.parts.count { it is UIMessagePart.Tool }
                }
                Text(
                    text = buildList {
                        task.modelName?.let { add(stringResource(R.string.sub_agent_model_label, it)) }
                        add(stringResource(R.string.sub_agent_progress, stepCount, toolCallCount, elapsedSeconds))
                    }.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // 完整执行过程（文本 + 工具调用及结果）
                SubAgentConversation(messages = task.messages)
                task.error?.let { error ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                        )
                    }
                }
            }
        }
    }
}

/** 子 Agent 状态徽标（色调容器胶囊） */
@Composable
private fun SubAgentStatusBadge(task: SubAgentTask) {
    val (labelRes, container, content) = when (task.status) {
        SubAgentStatus.RUNNING -> Triple(
            R.string.sub_agent_status_running,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        SubAgentStatus.DONE -> Triple(
            R.string.sub_agent_status_done,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        SubAgentStatus.FAILED -> Triple(
            R.string.sub_agent_status_failed,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
        SubAgentStatus.CANCELLED -> Triple(
            R.string.sub_agent_status_cancelled,
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Surface(
        shape = CircleShape,
        color = container,
        contentColor = content,
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/** 只读展示子 Agent 的完整对话（文本 + 工具调用与结果） */
@Composable
private fun SubAgentConversation(messages: List<UIMessage>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        messages.forEach { message ->
            val texts = message.parts.filterIsInstance<UIMessagePart.Text>()
                .map { it.text }
                .filter { it.isNotBlank() }
            val tools = message.parts.filterIsInstance<UIMessagePart.Tool>()
            if (texts.isEmpty() && tools.isEmpty()) return@forEach

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                    MarkdownBlock(
                        content = text,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                tools.forEach { tool ->
                    SubAgentToolCallRow(tool = tool)
                }
            }
        }
    }
}

/**
 * 子 Agent 的一次工具调用：名称 + 参数摘要 + 结果预览
 *
 * 参数与结果都可能很长，这里截断展示；完整内容仍可在主对话的工具详情里看到。
 */
@Composable
private fun SubAgentToolCallRow(tool: UIMessagePart.Tool) {
    val executed = tool.isExecuted
    val accent = if (executed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    val input = remember(tool.input) {
        runCatching { JsonInstantPretty.encodeToString(tool.inputAsJson()) }
            .getOrDefault(tool.input)
            .take(600)
    }
    val result = remember(tool.output) {
        tool.output.filterIsInstance<UIMessagePart.Text>()
            .joinToString("\n") { it.text }
            .trim()
            .take(1200)
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = HugeIcons.Tools,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = accent,
                )
                Text(
                    text = tool.toolName,
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(
                        if (executed) R.string.sub_agent_status_done else R.string.sub_agent_status_running
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                )
            }
            if (input.isNotBlank() && input != "{}") {
                Text(
                    text = input,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = JetbrainsMono,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (result.isNotBlank()) {
                Text(
                    text = result,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = JetbrainsMono,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                )
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

/** 输入框上方的子 Agent 运行进度（有子 Agent 运行时显示，可点击进入子 Agent 工作页） */
@Composable
internal fun SubAgentRunningBanner(
    count: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                style = MaterialTheme.typography.labelLargeEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
