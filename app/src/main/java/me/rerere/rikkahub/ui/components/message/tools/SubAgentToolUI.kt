package me.rerere.rikkahub.ui.components.message.tools

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Tools
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.ai.tools.local.CLOSE_AGENT_TOOL_NAME
import me.rerere.rikkahub.data.ai.tools.local.LIST_AGENTS_TOOL_NAME
import me.rerere.rikkahub.data.ai.tools.local.SPAWN_AGENT_TOOL_NAME
import me.rerere.rikkahub.data.ai.tools.local.WAIT_AGENT_TOOL_NAME

/** 启动子 Agent：标题显示任务名 */
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
