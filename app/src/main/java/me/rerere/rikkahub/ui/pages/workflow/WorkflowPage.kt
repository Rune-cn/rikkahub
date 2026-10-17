package me.rerere.rikkahub.ui.pages.workflow

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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Cancel01
import me.rerere.hugeicons.stroke.PlusSign
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.ai.workflow.WorkflowRunner
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.datastore.findModelById
import me.rerere.rikkahub.data.model.Workflow
import me.rerere.rikkahub.data.model.WorkflowRunState
import me.rerere.rikkahub.data.model.WorkflowStep
import me.rerere.rikkahub.data.model.WorkflowStepStatus
import me.rerere.rikkahub.data.model.WorkflowStepType
import me.rerere.rikkahub.ui.components.ai.ModelSelector
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.theme.CustomColors
import org.koin.compose.koinInject

@Composable
fun WorkflowPage() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()
    val settingsStore = koinInject<SettingsStore>()
    val runner = koinInject<WorkflowRunner>()
    val settings by settingsStore.settingsFlow.collectAsStateWithLifecycle()
    val workflows = settings.workflows

    var editing by remember { mutableStateOf<Workflow?>(null) }
    var running by remember { mutableStateOf<Workflow?>(null) }

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
                    workflows.forEach { workflow ->
                        item(
                            onClick = { editing = workflow },
                            headlineContent = { Text(workflow.name.ifBlank { stringResource(R.string.workflow_untitled) }) },
                            supportingContent = {
                                Text(
                                    text = workflow.description.ifBlank {
                                        stringResource(R.string.workflow_steps_count, workflow.steps.size)
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = { running = workflow }) {
                                        Text(stringResource(R.string.workflow_run))
                                    }
                                    IconButton(onClick = { remove(workflow.id) }) {
                                        Icon(HugeIcons.Cancel01, contentDescription = null)
                                    }
                                }
                            },
                        )
                    }
                }
            }
            item {
                Button(
                    onClick = { editing = Workflow() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Icon(HugeIcons.PlusSign, contentDescription = null)
                    Text(
                        text = stringResource(R.string.workflow_new),
                        modifier = Modifier.padding(start = 8.dp),
                    )
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

            draft.steps.forEachIndexed { index, step ->
                WorkflowStepEditor(
                    index = index,
                    step = step,
                    providers = providers,
                    onChange = { updated ->
                        draft = draft.copy(
                            steps = draft.steps.map { if (it.id == updated.id) updated else it }
                        )
                    },
                    onRemove = {
                        draft = draft.copy(steps = draft.steps.filterNot { it.id == step.id })
                    },
                )
            }

            TextButton(
                onClick = { draft = draft.copy(steps = draft.steps + WorkflowStep()) },
            ) {
                Icon(HugeIcons.PlusSign, contentDescription = null)
                Text(
                    text = stringResource(R.string.workflow_add_step),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.workflow_cancel))
                }
                Button(
                    onClick = { onSave(draft) },
                    enabled = draft.steps.isNotEmpty(),
                ) {
                    Text(stringResource(R.string.workflow_save))
                }
            }
        }
    }
}

/** 单个步骤编辑 */
@Composable
private fun WorkflowStepEditor(
    index: Int,
    step: WorkflowStep,
    providers: List<me.rerere.ai.provider.ProviderSetting>,
    onChange: (WorkflowStep) -> Unit,
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
        // 步骤类型
        val types = listOf(WorkflowStepType.PROMPT, WorkflowStepType.AGENT)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            types.forEachIndexed { i, type ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(i, types.size),
                    selected = step.type == type,
                    onClick = { onChange(step.copy(type = type)) },
                ) {
                    Text(
                        when (type) {
                            WorkflowStepType.PROMPT -> stringResource(R.string.workflow_step_type_prompt)
                            WorkflowStepType.AGENT -> stringResource(R.string.workflow_step_type_agent)
                        }
                    )
                }
            }
        }
        OutlinedTextField(
            value = step.name,
            onValueChange = { onChange(step.copy(name = it)) },
            label = { Text(stringResource(R.string.workflow_step_name)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = step.prompt,
            onValueChange = { onChange(step.copy(prompt = it)) },
            label = { Text(stringResource(R.string.workflow_step_prompt)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )
        // 模型（可选）
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.workflow_step_model),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ModelSelector(
                modelId = step.modelId,
                providers = providers,
                type = me.rerere.ai.provider.ModelType.CHAT,
                allowClear = true,
                onSelect = { model ->
                    val selectedId = model.id.takeIf { id ->
                        providers.any { p -> p.models.any { m -> m.id == id } }
                    }
                    onChange(step.copy(modelId = selectedId))
                },
            )
        }
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

            states.sortedBy { it.stepIndex }.forEach { state ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = state.stepName,
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            text = when (state.status) {
                                WorkflowStepStatus.PENDING -> stringResource(R.string.workflow_status_pending)
                                WorkflowStepStatus.RUNNING -> stringResource(R.string.workflow_status_running)
                                WorkflowStepStatus.DONE -> stringResource(R.string.workflow_status_done)
                                WorkflowStepStatus.FAILED -> stringResource(R.string.workflow_status_failed)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (state.status == WorkflowStepStatus.FAILED) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        )
                    }
                    state.output?.takeIf { it.isNotBlank() }?.let { output ->
                        Text(
                            text = output,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    state.error?.let { error ->
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
}
