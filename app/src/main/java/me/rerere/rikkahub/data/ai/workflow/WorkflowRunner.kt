package me.rerere.rikkahub.data.ai.workflow

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
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
import me.rerere.rikkahub.data.model.WorkflowRunState
import me.rerere.rikkahub.data.model.WorkflowStep
import me.rerere.rikkahub.data.model.WorkflowStepStatus
import me.rerere.rikkahub.data.model.WorkflowStepType
import org.koin.java.KoinJavaComponent.getKoin

/**
 * 工作流执行器
 *
 * 按顺序执行工作流的每个步骤，逐步发出状态供 UI 展示。
 * 步骤提示词支持 `{{input}}`（初始输入）与 `{{prev}}`（上一步输出）变量。
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
}
