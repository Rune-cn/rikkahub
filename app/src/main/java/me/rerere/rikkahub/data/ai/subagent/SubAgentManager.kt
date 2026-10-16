package me.rerere.rikkahub.data.ai.subagent

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.uuid.Uuid
import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.AppScope
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.ai.GenerationChunk
import me.rerere.rikkahub.data.ai.GenerationLoop
import me.rerere.rikkahub.data.ai.tools.ChatToolFactory
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.datastore.findModelById
import me.rerere.rikkahub.data.datastore.getCurrentAssistant
import me.rerere.rikkahub.data.datastore.getCurrentChatModel
import me.rerere.rikkahub.data.model.SubAgentStatus
import me.rerere.rikkahub.data.model.SubAgentTask
import org.koin.java.KoinJavaComponent.getKoin

/**
 * 子 Agent 管理器
 *
 * 主 Agent 通过 `spawn_agent` 工具创建子 Agent：每个子 Agent 在**独立协程**里
 * 跑自己的 `GenerationLoop`（因此拥有完整工具能力，并可指定不同模型），
 * 结果回填到 [SubAgentTask.result] 供主 Agent 通过 `wait_agent` 取用。
 */
class SubAgentManager(
    private val context: Context,
    private val settingsStore: SettingsStore,
    private val appScope: AppScope,
) {
    companion object {
        private const val TAG = "SubAgentManager"

        /** 单个子 Agent 的最大步数（防止失控） */
        const val MAX_SUB_AGENT_STEPS = 48

        /** 同时运行的子 Agent 上限 */
        const val MAX_CONCURRENT_SUB_AGENTS = 3
    }

    // 延迟解析，避免与 LocalTools / ChatToolFactory 形成 Koin 循环依赖
    private val chatToolFactory: ChatToolFactory by lazy { getKoin().get() }
    private val generationLoop: GenerationLoop by lazy { getKoin().get() }

    private val _tasks = MutableStateFlow<List<SubAgentTask>>(emptyList())
    val tasks: StateFlow<List<SubAgentTask>> = _tasks.asStateFlow()

    private val jobs = mutableMapOf<String, Job>()

    /** 启动子 Agent（异步，立即返回） */
    fun spawn(name: String, prompt: String, modelId: Uuid? = null): Result<SubAgentTask> {
        val runningCount = _tasks.value.count { it.status == SubAgentStatus.RUNNING }
        if (runningCount >= MAX_CONCURRENT_SUB_AGENTS) {
            return Result.failure(
                IllegalStateException("Too many running sub-agents (max $MAX_CONCURRENT_SUB_AGENTS)")
            )
        }
        if (_tasks.value.any { it.name == name }) {
            return Result.failure(IllegalStateException("Sub-agent '$name' already exists"))
        }
        val task = SubAgentTask(
            id = Uuid.random().toString(),
            name = name,
            prompt = prompt,
            modelId = modelId,
            startedAt = System.currentTimeMillis(),
        )
        _tasks.value = _tasks.value + task
        jobs[name] = appScope.launch { runTask(task) }
        Log.i(TAG, "spawn sub-agent: $name")
        return Result.success(task)
    }

    private suspend fun runTask(task: SubAgentTask) {
        try {
            val settings = settingsStore.settingsFlow.first { !it.init }
            val assistant = settings.getCurrentAssistant()
            // 模型优先级：任务指定 > 全局子 Agent 默认模型 > 主 Agent 当前模型
            val model = task.modelId?.let { settings.findModelById(it) }
                ?: settings.subAgentSetting.defaultModelId?.let { settings.findModelById(it) }
                ?: settings.getCurrentChatModel()
                ?: throw IllegalStateException("No chat model available for sub-agent")

            updateTask(task.name) { it.copy(modelName = model.displayName) }

            // 子 Agent 拥有与主 Agent 同源的工具集（含工作区、MCP、技能等）
            val tools = chatToolFactory.createTools(
                settings = settings,
                assistant = assistant,
                model = model,
            )

            var messages = listOf(
                UIMessage(
                    role = MessageRole.USER,
                    parts = listOf(UIMessagePart.Text(task.prompt)),
                )
            )
            var finalText = ""

            generationLoop.generateText(
                settings = settings,
                model = model,
                messages = messages,
                assistant = assistant,
                tools = tools,
                maxSteps = MAX_SUB_AGENT_STEPS,
                processingStatus = MutableStateFlow(null),
            ).collect { chunk ->
                if (chunk is GenerationChunk.Messages) {
                    messages = chunk.messages
                    // 实时同步完整对话（供详情查看）
                    updateTask(task.name) { it.copy(messages = chunk.messages) }
                    chunk.messages.lastOrNull()
                        ?.takeIf { it.role == MessageRole.ASSISTANT }
                        ?.let { finalText = it.toText() }
                }
            }

            updateTask(task.name) {
                it.copy(
                    status = SubAgentStatus.DONE,
                    result = finalText.ifBlank { context.getString(R.string.sub_agent_empty_result) },
                    messages = messages,
                    finishedAt = System.currentTimeMillis(),
                )
            }
            Log.i(TAG, "sub-agent finished: ${task.name}")
        } catch (e: CancellationException) {
            updateTask(task.name) {
                it.copy(status = SubAgentStatus.CANCELLED, finishedAt = System.currentTimeMillis())
            }
            throw e
        } catch (e: Throwable) {
            Log.w(TAG, "sub-agent failed: ${task.name}", e)
            updateTask(task.name) {
                it.copy(
                    status = SubAgentStatus.FAILED,
                    error = e.message ?: e.toString(),
                    finishedAt = System.currentTimeMillis(),
                )
            }
        }
    }

    private fun updateTask(name: String, transform: (SubAgentTask) -> SubAgentTask) {
        _tasks.value = _tasks.value.map { if (it.name == name) transform(it) else it }
    }

    /**
     * 等待指定子 Agent 完成
     *
     * @return 这些子 Agent 的最新状态（超时则返回当时的快照，status 仍可能是 RUNNING）
     */
    suspend fun waitFor(names: List<String>, timeoutMs: Long): List<SubAgentTask> {
        val current = _tasks.value.filter { it.name in names }
        if (current.isEmpty()) return emptyList()
        if (current.all { it.isFinished }) return current
        // 等待直到全部结束，或超时返回当时的快照
        return withTimeoutOrNull(timeoutMs) {
            _tasks.map { list -> list.filter { it.name in names } }
                .first { list -> list.isNotEmpty() && list.all { it.isFinished } }
        } ?: _tasks.value.filter { it.name in names }
    }

    /** 取消指定子 Agent */
    fun close(name: String): Boolean {
        val job = jobs.remove(name)
        job?.cancel()
        val exists = _tasks.value.any { it.name == name }
        if (exists) {
            updateTask(name) {
                if (it.isFinished) it else it.copy(status = SubAgentStatus.CANCELLED)
            }
        }
        return exists
    }

    /** 取消所有运行中的子 Agent（用户停止生成时调用） */
    fun cancelAll() {
        jobs.keys.toList().forEach { close(it) }
    }

    fun list(): List<SubAgentTask> = _tasks.value

    /** 清理已结束的任务记录 */
    fun clearFinished() {
        val finishedNames = _tasks.value.filter { it.isFinished }.map { it.name }
        finishedNames.forEach { jobs.remove(it) }
        _tasks.value = _tasks.value.filterNot { it.isFinished }
    }
}
