package me.rerere.rikkahub.ui.pages.workflow

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.OutlinedButton
import kotlinx.serialization.decodeFromString
import me.rerere.hugeicons.stroke.Pencil
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Copy01
import me.rerere.hugeicons.stroke.Download01
import me.rerere.hugeicons.stroke.Play
import me.rerere.hugeicons.stroke.Cancel01
import me.rerere.hugeicons.stroke.PlusSign
import me.rerere.rikkahub.R
import me.rerere.hugeicons.stroke.Settings03
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.data.ai.workflow.WorkflowRunner
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.datastore.findModelById
import me.rerere.rikkahub.data.model.Workflow
import me.rerere.rikkahub.data.model.WorkflowNodeType
import me.rerere.rikkahub.data.model.WorkflowNode
import me.rerere.rikkahub.data.model.WorkflowRunState
import me.rerere.rikkahub.data.model.WorkflowStep
import me.rerere.rikkahub.data.model.WorkflowStepStatus
import me.rerere.rikkahub.data.model.WorkflowStepType
import me.rerere.rikkahub.ui.components.ai.ModelSelector
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus
import org.koin.compose.koinInject

@Composable
fun WorkflowPage() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val importToaster = LocalToaster.current
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { u ->
            scope.launch {
                runCatching {
                    val bytes = context.contentResolver.openInputStream(u)?.use { it.readBytes() }
                    val json = bytes?.decodeToString() ?: error("empty")
                    val wf = kotlinx.serialization.json.Json {
                        ignoreUnknownKeys = true
                        encodeDefaults = true
                    }.decodeFromString<Workflow>(json)
                    val imported = wf.copy(id = java.util.UUID.randomUUID().toString())
                    scope.launch {
                        settingsStore.update { s -> s.copy(workflows = s.workflows + imported) }
                    }
                }.onSuccess {
                    importToaster.show(context.getString(R.string.workflow_import_success))
                }.onFailure {
                    importToaster.show(context.getString(R.string.workflow_import_failed))
                }
            }
        }
    }
    val settingsStore = koinInject<SettingsStore>()
    val runner = koinInject<WorkflowRunner>()
    val settings by settingsStore.settingsFlow.collectAsStateWithLifecycle()
    val workflows = settings.workflows
    val runningWorkflowId by runner.runningWorkflowId.collectAsStateWithLifecycle()
    val navController = LocalNavController.current

    var editing by remember { mutableStateOf<Workflow?>(null) }
    var running by remember { mutableStateOf<Workflow?>(null) }
    var expandedId by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<Workflow?>(null) }

    fun save(workflow: Workflow) {
        scope.launch {
            settingsStore.update { s ->
                val exists = s.workflows.any { it.id == workflow.id }
                s.copy(
                    workflows = if (exists) {
                        s.workflows.map { if (it.id == workflow.id) workflow else it }
                    } else {
                        s.workflows + workflow
                    }
                )
            }
        }
    }

    /** 复制工作流（新 id + 副本后缀） */
    fun duplicateWorkflow(workflow: Workflow) {
        val suffix = context.getString(R.string.workflow_duplicate_suffix)
        val copy = workflow.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = if (workflow.name.isBlank()) {
                context.getString(R.string.workflow_untitled)
            } else {
                workflow.name + suffix
            },
        )
        save(copy)
    }

    /** 导出工作流为 JSON（分享） */
    fun exportWorkflow(workflow: Workflow) {
        val json = kotlinx.serialization.json.Json { prettyPrint = true }
            .encodeToString(workflow)
        val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, json)
        }
        runCatching { context.startActivity(send) }
    }

    fun remove(id: String) {
        scope.launch {
            settingsStore.update { s -> s.copy(workflows = s.workflows.filterNot { it.id == id }) }
        }
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.workflow_page_title)) },
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
            item {
                CardGroup(modifier = Modifier.padding(horizontal = 8.dp)) {
                    if (workflows.isEmpty()) {
                        item(
                            headlineContent = { Text(stringResource(R.string.workflow_no_workflows)) },
                            supportingContent = { Text(stringResource(R.string.workflow_entry_desc)) },
                        )
                    }
                }
            }
            workflows.forEach { workflow ->
                item(key = workflow.id) {
                    var menuExpanded by remember(workflow.id) { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { navController.navigate(Screen.WorkflowCanvas(workflow.id)) }
                            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = workflow.name.ifBlank { stringResource(R.string.workflow_untitled) },
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = workflow.description.ifBlank {
                                    stringResource(R.string.workflow_steps_count, workflow.steps.size)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Switch(
                            checked = workflow.enabled,
                            onCheckedChange = { enabled -> save(workflow.copy(enabled = enabled)) },
                        )
                        IconButton(
                            onClick = { running = workflow },
                            enabled = workflow.enabled,
                        ) {
                            Icon(
                                imageVector = HugeIcons.Play,
                                contentDescription = stringResource(R.string.workflow_run),
                            )
                        }
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Text(
                                    text = "⋮",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.workflow_edit)) },
                                    leadingIcon = { Icon(HugeIcons.Settings03, null) },
                                    onClick = {
                                        menuExpanded = false
                                        editing = workflow
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.workflow_rename)) },
                                    leadingIcon = { Icon(HugeIcons.Pencil, null) },
                                    onClick = {
                                        menuExpanded = false
                                        editing = workflow
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.workflow_duplicate)) },
                                    leadingIcon = { Icon(HugeIcons.Copy01, null) },
                                    onClick = {
                                        menuExpanded = false
                                        duplicateWorkflow(workflow)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.workflow_export)) },
                                    leadingIcon = { Icon(HugeIcons.Download01, null) },
                                    onClick = {
                                        menuExpanded = false
                                        exportWorkflow(workflow)
                                    },
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(R.string.workflow_delete_confirm),
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            HugeIcons.Cancel01,
                                            null,
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        deleteTarget = workflow
                                    },
                                )
                            }
                        }
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = { editing = Workflow() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(HugeIcons.PlusSign, contentDescription = null)
                        Text(
                            text = stringResource(R.string.workflow_new),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json")) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(HugeIcons.Download01, contentDescription = null)
                        Text(
                            text = stringResource(R.string.workflow_import),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }
    }

    editing?.let { workflow ->
        WorkflowEditSheet(
            workflow = workflow,
            providers = settings.providers,
            onSave = { save(it); editing = null },
            onDismiss = { editing = null },
            onDuplicate = { duplicateWorkflow(workflow); editing = null },
            onExport = { exportWorkflow(workflow); editing = null },
        )
    }

    // 删除二次确认（红字提示）
    deleteTarget?.let { target ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.workflow_delete_title)) },
            text = {
                Text(
                    text = stringResource(
                        R.string.workflow_delete_message,
                        target.name.ifBlank { stringResource(R.string.workflow_untitled) },
                    ),
                    color = MaterialTheme.colorScheme.error,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        remove(target.id)
                        deleteTarget = null
                    },
                ) {
                    Text(
                        text = stringResource(R.string.workflow_delete_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.workflow_cancel))
                }
            },
        )
    }

    running?.let { workflow ->
        WorkflowRunSheet(
            workflow = workflow,
            runner = runner,
            onDismiss = { running = null },
        )
    }
}

/** 工作流编辑（BottomSheet） */
@Composable
private fun WorkflowEditSheet(
    workflow: Workflow,
    providers: List<me.rerere.ai.provider.ProviderSetting>,
    onSave: (Workflow) -> Unit,
    onDismiss: () -> Unit,
    onDuplicate: (() -> Unit)? = null,
    onExport: (() -> Unit)? = null,
) {
    var draft by remember { mutableStateOf(workflow) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.9f)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.workflow_edit_title),
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                value = draft.name,
                onValueChange = { draft = draft.copy(name = it) },
                label = { Text(stringResource(R.string.workflow_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = draft.description,
                onValueChange = { draft = draft.copy(description = it) },
                label = { Text(stringResource(R.string.workflow_description)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Text(
                text = stringResource(R.string.workflow_steps),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.workflow_step_prompt_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            draft.nodes.forEachIndexed { index, node ->
                WorkflowNodeEditor(
                    index = index,
                    node = node,
                    providers = providers,
                    onChange = { updated ->
                        draft = draft.copy(
                            nodes = draft.nodes.map { if (it.id == updated.id) updated else it }
                        )
                    },
                    onRemove = {
                        draft = draft.copy(nodes = draft.nodes.filterNot { it.id == node.id })
                    },
                )
            }

            TextButton(
                onClick = { draft = draft.copy(nodes = draft.nodes + WorkflowNode()) },
            ) {
                Icon(HugeIcons.PlusSign, contentDescription = null)
                Text(
                    text = stringResource(R.string.workflow_add_step),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDuplicate != null) {
                    TextButton(onClick = onDuplicate) {
                        Text(stringResource(R.string.workflow_duplicate))
                    }
                }
                if (onExport != null) {
                    TextButton(onClick = onExport) {
                        Text(stringResource(R.string.workflow_export))
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.workflow_cancel))
                }
                Button(
                    onClick = { onSave(draft) },
                    enabled = draft.nodes.isNotEmpty(),
                ) {
                    Text(stringResource(R.string.workflow_save))
                }
            }
        }
    }
}

/** 单个节点编辑（支持模型/Agent/条件分支/并行） */
@Composable
private fun WorkflowNodeEditor(
    index: Int,
    node: WorkflowNode,
    providers: List<me.rerere.ai.provider.ProviderSetting>,
    onChange: (WorkflowNode) -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.workflow_step_index, index + 1),
                style = MaterialTheme.typography.labelLarge,
            )
            IconButton(onClick = onRemove) {
                Icon(HugeIcons.Cancel01, contentDescription = null)
            }
        }
        // 节点类型（对齐业界模式：模型 / Agent / 条件分支 / 并行）
        val types = listOf(
            WorkflowNodeType.PROMPT,
            WorkflowNodeType.AGENT,
            WorkflowNodeType.IF_ELSE,
            WorkflowNodeType.PARALLEL,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            types.forEachIndexed { i, type ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(i, types.size),
                    selected = node.type == type,
                    onClick = { onChange(node.copy(type = type)) },
                ) {
                    Text(
                        text = when (type) {
                            WorkflowNodeType.PROMPT -> stringResource(R.string.workflow_step_type_prompt)
                            WorkflowNodeType.AGENT -> stringResource(R.string.workflow_step_type_agent)
                            WorkflowNodeType.IF_ELSE -> stringResource(R.string.workflow_node_type_if_else)
                            WorkflowNodeType.PARALLEL -> stringResource(R.string.workflow_node_type_parallel)
                        },
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
        OutlinedTextField(
            value = node.name,
            onValueChange = { onChange(node.copy(name = it)) },
            label = { Text(stringResource(R.string.workflow_step_name)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        if (node.type == WorkflowNodeType.IF_ELSE) {
            OutlinedTextField(
                value = node.condition,
                onValueChange = { onChange(node.copy(condition = it)) },
                label = { Text(stringResource(R.string.workflow_node_condition)) },
                supportingText = { Text(stringResource(R.string.workflow_node_condition_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            WorkflowBranchEditor(
                label = stringResource(R.string.workflow_node_true_branch),
                node = node.trueBranch,
                onNodeChange = { branch -> onChange(node.copy(trueBranch = branch)) },
            )
            WorkflowBranchEditor(
                label = stringResource(R.string.workflow_node_false_branch),
                node = node.falseBranch,
                onNodeChange = { branch -> onChange(node.copy(falseBranch = branch)) },
            )
        }
        if (node.type != WorkflowNodeType.IF_ELSE && node.type != WorkflowNodeType.PARALLEL) {
            OutlinedTextField(
                value = node.prompt,
                onValueChange = { onChange(node.copy(prompt = it)) },
                label = { Text(stringResource(R.string.workflow_step_prompt)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.workflow_step_model),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ModelSelector(
                    modelId = node.modelId,
                    providers = providers,
                    type = me.rerere.ai.provider.ModelType.CHAT,
                    allowClear = true,
                    onSelect = { model ->
                        val selectedId = model.id.takeIf { id ->
                            providers.any { p -> p.models.any { m -> m.id == id } }
                        }
                        onChange(node.copy(modelId = selectedId))
                    },
                )
            }
        }
    }
}

/** 条件分支的单个子节点编辑（名称 + 提示词） */
@Composable
private fun WorkflowBranchEditor(
    label: String,
    node: WorkflowNode?,
    onNodeChange: (WorkflowNode) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        OutlinedTextField(
            value = node?.name.orEmpty(),
            onValueChange = { onNodeChange((node ?: WorkflowNode()).copy(name = it)) },
            label = { Text(stringResource(R.string.workflow_step_name)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = node?.prompt.orEmpty(),
            onValueChange = { onNodeChange((node ?: WorkflowNode()).copy(prompt = it)) },
            label = { Text(stringResource(R.string.workflow_step_prompt)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )
    }
}

/** 工作流运行（BottomSheet） */
@Composable
private fun WorkflowRunSheet(
    workflow: Workflow,
    runner: WorkflowRunner,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var states by remember { mutableStateOf<List<WorkflowRunState>>(emptyList()) }
    var running by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.9f)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = workflow.name.ifBlank { stringResource(R.string.workflow_untitled) },
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(stringResource(R.string.workflow_run_input)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            Button(
                onClick = {
                    running = true
                    states = emptyList()
                    scope.launch {
                        runner.run(workflow, input)
                            .catch { error ->
                                states = states + WorkflowRunState(
                                    stepIndex = states.size,
                                    stepName = "",
                                    status = WorkflowStepStatus.FAILED,
                                    error = error.message ?: error.toString(),
                                )
                            }
                            .collect { state ->
                                states = states
                                    .filterNot { it.stepIndex == state.stepIndex } + state
                                if (state.status == WorkflowStepStatus.FAILED) running = false
                            }
                        running = false
                    }
                },
                enabled = !running,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.workflow_run_start))
            }

            // 树形流程图：每个步骤一个节点，实时显示状态
            if (states.isNotEmpty() || running) {
                WorkflowFlowView(
                    nodes = workflow.displayNodes(),
                    statusByNodeId = states.mapNotNull { state ->
                        state.nodeId?.let { it to state.status }
                    }.toMap(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            // 最近一步的输出（展开显示完整结果）
            states.sortedBy { it.stepIndex }.lastOrNull()?.output?.takeIf { it.isNotBlank() }?.let { output ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.workflow_run_output),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = output,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            states.firstOrNull { it.status == WorkflowStepStatus.FAILED }?.error?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/** 旧版步骤列表 → 节点列表（新版 nodes 优先，旧版 steps 自动转换） */
private fun Workflow.displayNodes(): List<WorkflowNode> =
    if (nodes.isNotEmpty()) nodes
    else steps.map { step ->
        WorkflowNode(
            id = step.id,
            name = step.name,
            type = if (step.type == WorkflowStepType.AGENT) WorkflowNodeType.AGENT else WorkflowNodeType.PROMPT,
            modelId = step.modelId,
            prompt = step.prompt,
        )
    }

/**
 * 工作流树形流程图：节点卡片 + 竖向连接线；条件分支（✓/✗）与并行分支递归展开
 */
@Composable
private fun WorkflowFlowView(
    nodes: List<WorkflowNode>,
    statusByNodeId: Map<String, WorkflowStepStatus> = emptyMap(),
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        nodes.forEachIndexed { index, node ->
            Row(verticalAlignment = Alignment.Top) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FlowNodeIndicator(
                        status = statusByNodeId[node.id] ?: WorkflowStepStatus.PENDING,
                        modifier = Modifier.width(24.dp),
                    )
                    if (index < nodes.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(24.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    WorkflowFlowNodeCard(
                        node = node,
                        status = statusByNodeId[node.id] ?: WorkflowStepStatus.PENDING,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    if (node.type == WorkflowNodeType.IF_ELSE) {
                        node.trueBranch?.let { branch ->
                            FlowBranchLabel(
                                text = stringResource(R.string.workflow_node_true_branch),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            WorkflowFlowView(
                                nodes = listOf(branch),
                                statusByNodeId = statusByNodeId,
                                modifier = Modifier.padding(start = 16.dp),
                            )
                        }
                        node.falseBranch?.let { branch ->
                            FlowBranchLabel(
                                text = stringResource(R.string.workflow_node_false_branch),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            WorkflowFlowView(
                                nodes = listOf(branch),
                                statusByNodeId = statusByNodeId,
                                modifier = Modifier.padding(start = 16.dp),
                            )
                        }
                    }
                    if (node.type == WorkflowNodeType.PARALLEL && node.children.isNotEmpty()) {
                        WorkflowFlowView(
                            nodes = node.children,
                            statusByNodeId = statusByNodeId,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

/** 分支标签（✓ 满足 / ✗ 不满足） */
@Composable
private fun FlowBranchLabel(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier.padding(vertical = 2.dp),
    )
}

/** 流程节点状态指示：灰点（待运行）/ 环形进度（运行中）/ 对勾（完成）/ 红叉（失败） */
@Composable
private fun FlowNodeIndicator(status: WorkflowStepStatus, modifier: Modifier = Modifier) {
    Box(modifier = modifier.height(24.dp), contentAlignment = Alignment.Center) {
        when (status) {
            WorkflowStepStatus.PENDING -> Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            WorkflowStepStatus.RUNNING -> CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
            )

            WorkflowStepStatus.DONE -> Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )

            WorkflowStepStatus.FAILED -> Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = HugeIcons.Cancel01,
                    contentDescription = null,
                    modifier = Modifier.size(10.dp),
                    tint = MaterialTheme.colorScheme.onError,
                )
            }
        }
    }
}

/** 单个流程节点卡片：类型徽标 + 名称 + 内容摘要 */
@Composable
private fun WorkflowFlowNodeCard(
    node: WorkflowNode,
    status: WorkflowStepStatus,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = when (status) {
            WorkflowStepStatus.RUNNING -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            WorkflowStepStatus.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = when (node.type) {
                            WorkflowNodeType.PROMPT -> stringResource(R.string.workflow_step_type_prompt)
                            WorkflowNodeType.AGENT -> stringResource(R.string.workflow_step_type_agent)
                            WorkflowNodeType.IF_ELSE -> stringResource(R.string.workflow_node_type_if_else)
                            WorkflowNodeType.PARALLEL -> stringResource(R.string.workflow_node_type_parallel)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
                Text(
                    text = node.name.ifBlank {
                        when (node.type) {
                            WorkflowNodeType.IF_ELSE -> stringResource(R.string.workflow_node_type_if_else)
                            WorkflowNodeType.PARALLEL -> stringResource(R.string.workflow_node_type_parallel)
                            else -> stringResource(R.string.workflow_untitled)
                        }
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            when (node.type) {
                WorkflowNodeType.IF_ELSE -> {
                    if (node.condition.isNotBlank()) {
                        Text(
                            text = node.condition,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                WorkflowNodeType.PARALLEL -> {
                    Text(
                        text = stringResource(R.string.workflow_parallel_count, node.children.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> {
                    Text(
                        text = node.prompt.ifBlank { "{{input}}" },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
