package me.rerere.rikkahub.data.ai.workflow

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.data.ai.GenerationChunk
import me.rerere.rikkahub.data.ai.GenerationLoop
import me.rerere.rikkahub.data.ai.tools.ChatToolFactory
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.datastore.findModelById
import me.rerere.rikkahub.data.datastore.getCurrentAssistant
import me.rerere.rikkahub.data.datastore.getCurrentChatModel
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.Workflow
import me.rerere.rikkahub.data.model.WorkflowNode
import me.rerere.rikkahub.data.model.WorkflowNodeType
import me.rerere.rikkahub.data.model.WorkflowRunState
import me.rerere.rikkahub.data.model.WorkflowStep
import me.rerere.rikkahub.data.model.WorkflowStepStatus
import me.rerere.rikkahub.data.model.WorkflowStepType
import org.koin.java.KoinJavaComponent.getKoin

/**
 * 工作流执行器
 *
 * - 新版（[Workflow.nodes] 非空）：树形执行，支持顺序链 / IF_ELSE 条件分支 / PARALLEL 并行
 * - 旧版（仅 [Workflow.steps]）：线性执行（兼容）
 *
 * 提示词变量：`{{input}}`（初始输入）、`{{prev}}`（当前序列上一个节点的输出）
 * IF_ELSE 条件：`{{prev}} contains "关键词"`（也支持 {{input}} / {{node.<id>}}）
 */
class WorkflowRunner(
    private val settingsStore: SettingsStore,
) {
    companion object {
        private const val MAX_WORKFLOW_STEPS_PER_RUN = 32
    }

    private val generationLoop: GenerationLoop by lazy { getKoin().get() }
    private val chatToolFactory: ChatToolFactory by lazy { getKoin().get() }

    /** 运行工作流，逐步发出每一步的状态 */
    fun run(workflow: Workflow, input: String): Flow<WorkflowRunState> = flow {
        val settings = settingsStore.settingsFlow.first { !it.init }
        val assistant = settings.getCurrentAssistant()

        if (workflow.nodes.isNotEmpty()) {
            // ── 新版：树形执行 ──
            val outputs = mutableMapOf<String, String>()
            runSequence(
                sequence = workflow.nodes,
                input = input,
                outputs = outputs,
                settings = settings,
                assistant = assistant,
            )
        } else {
            // ── 旧版：线性 steps（兼容） ──
            var prev = ""
            for (index in workflow.steps.indices) {
                val step = workflow.steps[index]
                val name = step.name.ifBlank { "Step ${index + 1}" }
                emit(WorkflowRunState(index, name, WorkflowStepStatus.RUNNING))
                try {
                    val output = runStep(
                        step = step,
                        input = input,
                        prev = prev,
                        settings = settings,
                        assistant = assistant,
                    )
                    prev = output
                    emit(
                        WorkflowRunState(
                            stepIndex = index,
                            stepName = name,
                            status = WorkflowStepStatus.DONE,
                            output = output,
                        )
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    emit(
                        WorkflowRunState(
                            stepIndex = index,
                            stepName = name,
                            status = WorkflowStepStatus.FAILED,
                            error = e.message ?: e.toString(),
                        )
                    )
                    return@flow
                }
            }
        }
    }

    /** 顺序执行一个节点序列 */
    private suspend fun FlowCollector<WorkflowRunState>.runSequence(
        sequence: List<WorkflowNode>,
        input: String,
        outputs: MutableMap<String, String>,
        settings: Settings,
        assistant: Assistant,
    ) {
        var prev = ""
        sequence.forEach { node ->
            runNode(
                node = node,
                input = input,
                prev = prev,
                outputs = outputs,
                settings = settings,
                assistant = assistant,
            )?.let { output ->
                outputs[node.id] = output
                prev = output
            }
        }
    }

    /**
     * 执行单个节点，返回节点输出文本（IF_ELSE / PARALLEL 不产出文本，返回 null）
     */
    private suspend fun FlowCollector<WorkflowRunState>.runNode(
        node: WorkflowNode,
        input: String,
        prev: String,
        outputs: MutableMap<String, String>,
        settings: Settings,
        assistant: Assistant,
    ): String? {
        val name = node.name.ifBlank { node.type.name }
        emit(WorkflowRunState(0, name, WorkflowStepStatus.RUNNING, nodeId = node.id))
        return try {
            when (node.type) {
                WorkflowNodeType.IF_ELSE -> {
                    val cond = evaluateCondition(node.condition, input, prev, outputs)
                    emit(
                        WorkflowRunState(
                            stepIndex = 0,
                            stepName = name,
                            status = WorkflowStepStatus.DONE,
                            output = "条件 = $cond",
                            nodeId = node.id,
                        )
                    )
                    val branch = if (cond) node.trueBranch else node.falseBranch
                    if (branch != null) {
                        runSequence(listOf(branch), input, outputs, settings, assistant)
                    }
                    null
                }

                WorkflowNodeType.PARALLEL -> {
                    coroutineScope {
                        node.children.map { child ->
                            async {
                                runNode(child, input, prev, outputs, settings, assistant)
                            }
                        }.awaitAll()
                    }
                    emit(
                        WorkflowRunState(
                            stepIndex = 0,
                            stepName = name,
                            status = WorkflowStepStatus.DONE,
                            output = "并行完成 ${node.children.size} 个分支",
                            nodeId = node.id,
                        )
                    )
                    null
                }

                else -> {
                    val output = runStepNode(node, input, prev, settings, assistant)
                    emit(
                        WorkflowRunState(
                            stepIndex = 0,
                            stepName = name,
                            status = WorkflowStepStatus.DONE,
                            output = output,
                            nodeId = node.id,
                        )
                    )
                    output
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            emit(
                WorkflowRunState(
                    stepIndex = 0,
                    stepName = name,
                    status = WorkflowStepStatus.FAILED,
                    error = e.message ?: e.toString(),
                    nodeId = node.id,
                )
            )
            null
        }
    }

    private suspend fun runStepNode(
        node: WorkflowNode,
        input: String,
        prev: String,
        settings: Settings,
        assistant: Assistant,
    ): String = runStep(
        step = WorkflowStep(
            id = node.id,
            name = node.name,
            type = if (node.type == WorkflowNodeType.AGENT) WorkflowStepType.AGENT else WorkflowStepType.PROMPT,
            modelId = node.modelId,
            prompt = node.prompt,
        ),
        input = input,
        prev = prev,
        settings = settings,
        assistant = assistant,
    )

    private suspend fun runStep(
        step: WorkflowStep,
        input: String,
        prev: String,
        settings: Settings,
        assistant: Assistant,
    ): String {
        val model = step.modelId?.let { settings.findModelById(it) }
            ?: settings.getCurrentChatModel()
            ?: error("No chat model available for workflow step")

        val prompt = step.prompt
            .replace("{{input}}", input)
            .replace("{{prev}}", prev)
            .ifBlank { input }

        val tools = if (step.type == WorkflowStepType.AGENT) {
            chatToolFactory.createTools(
                settings = settings,
                assistant = assistant,
                model = model,
            )
        } else {
            emptyList()
        }

        var text = ""
        generationLoop.generateText(
            settings = settings,
            model = model,
            messages = listOf(
                UIMessage(
                    role = MessageRole.USER,
                    parts = listOf(UIMessagePart.Text(prompt)),
                )
            ),
            assistant = assistant,
            tools = tools,
            maxSteps = MAX_WORKFLOW_STEPS_PER_RUN,
            processingStatus = MutableStateFlow(null),
        ).collect { chunk ->
            if (chunk is GenerationChunk.Messages) {
                chunk.messages.lastOrNull()
                    ?.takeIf { it.role == MessageRole.ASSISTANT }
                    ?.let { text = it.toText() }
            }
        }
        return text
    }

    /**
     * 解析 IF_ELSE 条件表达式
     *
     * 支持：`{{input}} contains "关键词"`、`{{prev}} contains "关键词"`、`{{node.<id>}} contains "关键词"`
     */
    private fun evaluateCondition(
        condition: String,
        input: String,
        prev: String,
        outputs: Map<String, String>,
    ): Boolean {
        val c = condition.trim()
        if (c.isBlank()) return true
        val match = Regex("""\{\{([^}]+)\}\}\s+contains\s+"([^"]+)"""").find(c)
        if (match != null) {
            val ref = match.groupValues[1].trim()
            val keyword = match.groupValues[2].trim()
            val value = when {
                ref == "input" -> input
                ref == "prev" -> prev
                ref.startsWith("node.") -> outputs[ref.removePrefix("node.")] ?: ""
                else -> ""
            }
            return value.lowercase().contains(keyword.lowercase())
        }
        return true
    }
}
