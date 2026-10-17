package me.rerere.rikkahub.ui.pages.multiagent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.ai.subagent.SubAgentManager
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.datastore.findModelById
import me.rerere.rikkahub.data.model.SubAgentStatus
import me.rerere.rikkahub.data.model.SubAgentTask
import me.rerere.rikkahub.ui.components.ai.ModelSelector
import me.rerere.rikkahub.ui.components.multiagent.SubAgentCreatorDialog
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus
import org.koin.compose.koinInject

/**
 * 多 Agent 工作页：直接与子 Agent 对话（聊天的形式），
 * 也可设置子 Agent 的默认模型与上下文共享；主 Agent 仍可通过
 * spawn_agent 等工具向它们分派任务。
 */
@Composable
fun MultiAgentPage() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()
    val settingsStore = koinInject<SettingsStore>()
    val subAgentManager = koinInject<SubAgentManager>()
    val settings by settingsStore.settingsFlow.collectAsStateWithLifecycle()
    val tasks by subAgentManager.tasks.collectAsStateWithLifecycle()

    var chattingId by remember { mutableStateOf<String?>(null) }
    var showCreate by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.multi_agent_page_title)) },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        modifier = Modifier,
        containerColor = CustomColors.topBarColors.containerColor,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding + PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 设置
            item {
                CardGroup(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    title = { Text(stringResource(R.string.multi_agent_settings)) },
                ) {
                    item(
                        headlineContent = { Text(stringResource(R.string.extensions_page_sub_agent_model)) },
                        supportingContent = {
                            Column {
                                Text(
                                    text = stringResource(R.string.extensions_page_sub_agent_model_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                ModelSelector(
                                    modelId = settings.subAgentSetting.defaultModelId,
                                    providers = settings.providers,
                                    type = me.rerere.ai.provider.ModelType.CHAT,
                                    allowClear = true,
                                    modifier = Modifier.padding(top = 4.dp),
                                    onSelect = { model ->
                                        val selectedId = model.id.takeIf { id ->
                                            settings.findModelById(id) != null
                                        }
                                        scope.launch {
                                            settingsStore.update { s ->
                                                s.copy(
                                                    subAgentSetting = s.subAgentSetting.copy(
                                                        defaultModelId = selectedId,
                                                    )
                                                )
                                            }
                                        }
                                    },
                                )
                            }
                        },
                    )
                    item(
                        headlineContent = {
                            Text(stringResource(R.string.extensions_page_sub_agent_inherit_context))
                        },
                        supportingContent = {
                            Text(stringResource(R.string.extensions_page_sub_agent_inherit_context_desc))
                        },
                        trailingContent = {
                            Switch(
                                checked = settings.subAgentSetting.defaultInheritContext,
                                onCheckedChange = { enabled ->
                                    scope.launch {
                                        settingsStore.update { s ->
                                            s.copy(
                                                subAgentSetting = s.subAgentSetting.copy(
                                                    defaultInheritContext = enabled,
                                                )
                                            )
                                        }
                                    }
                                },
                            )
                        },
                    )
                }
            }
            // 子 Agent 列表
            item {
                CardGroup(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    title = { Text(stringResource(R.string.multi_agent_list_title)) },
                ) {
                    if (tasks.isEmpty()) {
                        item(
                            headlineContent = { Text(stringResource(R.string.multi_agent_no_tasks)) },
                        )
                    }
                    tasks.reversed().forEach { task ->
                        item(
                            onClick = { chattingId = task.id },
                            headlineContent = { Text(task.name) },
                            supportingContent = {
                                Text(
                                    text = task.modelName?.let {
                                        stringResource(R.string.sub_agent_model_label, it)
                                    } ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            },
                            trailingContent = {
                                SubAgentStatusText(task)
                            },
                        )
                    }
                }
            }
            // 新建
            item {
                Button(
                    onClick = { showCreate = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Text(stringResource(R.string.multi_agent_new))
                }
            }
        }
    }

    // 直接与子 Agent 对话
    val chattingTask = tasks.find { it.id == chattingId }
    if (chattingTask != null) {
        SubAgentChatSheet(
            task = chattingTask,
            manager = subAgentManager,
            onDismiss = { chattingId = null },
        )
    }

    // 新建子 Agent
    if (showCreate) {
        SubAgentCreatorDialog(
            onCreate = { name, prompt ->
                subAgentManager.spawn(name = name, prompt = prompt)
                showCreate = false
            },
            onDismiss = { showCreate = false },
        )
    }
}

/** 子 Agent 状态文字 */
@Composable
private fun SubAgentStatusText(task: SubAgentTask) {
    Text(
        text = when (task.status) {
            SubAgentStatus.RUNNING -> stringResource(R.string.sub_agent_status_running)
            SubAgentStatus.DONE -> stringResource(R.string.sub_agent_status_done)
            SubAgentStatus.FAILED -> stringResource(R.string.sub_agent_status_failed)
            SubAgentStatus.CANCELLED -> stringResource(R.string.sub_agent_status_cancelled)
        },
        style = MaterialTheme.typography.labelMedium,
        color = when (task.status) {
            SubAgentStatus.RUNNING -> MaterialTheme.colorScheme.primary
            SubAgentStatus.FAILED -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}

/** 与子 Agent 的聊天界面 */
@Composable
private fun SubAgentChatSheet(
    task: SubAgentTask,
    manager: SubAgentManager,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.9f)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.titleLarge,
                )
                SubAgentStatusText(task)
            }
            task.modelName?.let {
                Text(
                    text = stringResource(R.string.sub_agent_model_label, it),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // 完整对话
            task.messages.forEach { message ->
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
                            MessageRole.SYSTEM -> "system"
                            else -> message.role.name.lowercase()
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    texts.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                    tools.forEach { tool ->
                        Text(
                            text = "· ${tool.toolName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            task.error?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            // 继续对话输入
            if (task.isFinished) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.multi_agent_chat_placeholder)) },
                        singleLine = true,
                    )
                    TextButton(
                        onClick = {
                            val msg = input.trim()
                            if (msg.isNotEmpty()) {
                                manager.sendMessage(task.name, msg)
                                input = ""
                            }
                        },
                    ) {
                        Text(stringResource(R.string.multi_agent_send))
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.multi_agent_running_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
