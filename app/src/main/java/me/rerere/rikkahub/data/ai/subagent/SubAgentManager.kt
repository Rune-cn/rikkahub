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
import me.rerere.rikkahub.service.SubAgentForegroundService
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

        /** 继承主对话上下文时最多带入的消息条数（避免上下文过长） */
        const val MAX_CONTEXT_MESSAGES = 20
    }

    // 延迟解析，避免与 LocalTools / ChatToolFactory 形成 Koin 循环依赖
    private val chatToolFactory: ChatToolFactory by lazy { getKoin().get() }
    private val generationLoop: GenerationLoop by lazy { getKoin().get() }

    private val _tasks = MutableStateFlow<List<SubAgentTask>>(emptyList())
    val tasks: StateFlow<List<SubAgentTask>> = _tasks.asStateFlow()

    private val jobs = mutableMapOf<String, Job>()

    /** 主 Agent 最近的对话上下文（由 ChatService 在生成前同步），供 inherit_context 使用 */
    @Volatile
    private var mainContext: List<UIMessage> = emptyList()

    /** 由 ChatService 在开始生成前调用，同步主对话上下文 */
    fun updateMainContext(messages: List<UIMessage>) {
        mainContext = messages
    }

    /** 启动子 Agent（异步，立即返回） */
    fun spawn(
        name: String,
        prompt: String,
        modelId: Uuid? = null,
        inheritContext: Boolean? = null,
    ): Result<SubAgentTask> {
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
            inheritContext = inheritContext,
            startedAt = System.currentTimeMillis(),
        )
        _tasks.value = _tasks.value + task
        syncSubAgentService()
        jobs[name] = appScope.launch { runTask(task) }
        Log.i(TAG, "spawn sub-agent: $name (inheritContext=$inheritContext)")
        return Result.success(task)
    }

    /**
     * 给子 Agent 追加一条消息，让它继续工作（多轮对话）
     *
     * 仅支持已结束的子 Agent；运行中的需先 wait_agent。
     */
    fun sendMessage(name: String, message: String): Result<SubAgentTask> {
        val task = _tasks.value.find { it.name == name }
            ?: return Result.failure(IllegalStateException("Sub-agent '$name' not found"))
        if (!task.isFinished) {
            return Result.failure(
                IllegalStateException("Sub-agent '$name' is still running; wait for it first")
            )
        }
        val resumed = task.copy(
            status = SubAgentStatus.RUNNING,
            result = null,
            error = null,
            finishedAt = null,
            messages = task.messages + UIMessage(
                role = MessageRole.USER,
                parts = listOf(UIMessagePart.Text(message)),
            ),
        )
        updateTask(name) { resumed }
        jobs[name] = appScope.launch {
            runTask(resumed, resumeMessages = resumed.messages)
        }
        Log.i(TAG, "send message to sub-agent: $name")
        return Result.success(resumed)
    }

    private suspend fun runTask(task: SubAgentTask, resumeMessages: List<UIMessage>? = null) {
        try {
            val settings = settingsStore.settingsFlow.first { !it.init }
            val assistant = settings.getCurrentAssistant()
            // 模型优先级：任务指定 > 全局子 Agent 默认模型 > 主 Agent 当前模型
            val model = task.modelId?.let { settings.findModelById(it) }
                ?: settings.subAgentSetting.defaultModelId?.let { settings.findModelById(it) }
                ?: settings.getCurrentChatModel()
                ?: throw IllegalStateException("No chat model available for sub-agent")

            updateTask(task.name) { it.copy(modelName = model.displayName) }

            // 子 Agent 拥有与主 Agent 同源的工具集（含技能、MCP、工作区、搜索、记忆等），
            // 但排除子 Agent 管理工具本身，避免无限递归
            val tools = chatToolFactory.createTools(
                settings = settings,
                assistant = assistant,
                model = model,
                excludeSubAgentTools = true,
            )

            var messages = resumeMessages ?: buildList {
                // 可选：继承主 Agent 的最近对话上下文（任务未指定时用全局设置）
                val inheritContext = task.inheritContext ?: settings.subAgentSetting.defaultInheritContext
                // 子 Agent 身份与规则说明（让它清楚自己是谁、能力边界、结果如何上交）
                add(
                    UIMessage(
                        role = MessageRole.SYSTEM,
                        parts = listOf(
                            UIMessagePart.Text(
                                """
                                You are a SUB-AGENT spawned by the main agent Rikka to work on a delegated task.
                                - Your task name: ${task.name}
                                - You have the same toolset as the main agent, except you cannot spawn sub-agents.
                                - You cannot chat with the main agent directly; your final answer is returned to it automatically when you finish. Make it complete and self-contained (plain human-readable text, not JSON).
                                ${if (inheritContext) "- Your main conversation context is included below; use it to stay consistent with the main discussion." else "- You do NOT see the main conversation; the task description you are given is your only context."}
                                """.trimIndent()
                            )
                        )
                    )
                )
                if (inheritContext) {
                    addAll(mainContext.takeLast(MAX_CONTEXT_MESSAGES))
                }
                add(
                    UIMessage(
                        role = MessageRole.USER,
                        parts = listOf(UIMessagePart.Text(task.prompt)),
                    )
                )
            }
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
        val beforeStatus = _tasks.value.find { it.name == name }?.status
        _tasks.value = _tasks.value.map { if (it.name == name) transform(it) else it }
        val afterStatus = _tasks.value.find { it.name == name }?.status
        // 仅状态变化时同步前台服务/通知，避免频繁刷新
        if (beforeStatus != afterStatus) syncSubAgentService()
    }

    private var lastRunningSubAgents = 0

    /** 按运行中子 Agent 数量启停前台服务（保活）并刷新通知 */
    private fun syncSubAgentService() {
        val running = _tasks.value.count { it.status == SubAgentStatus.RUNNING }
        if (running > 0 && lastRunningSubAgents == 0) {
            SubAgentForegroundService.start(context)
        }
        if (running > 0) {
            SubAgentForegroundService.refresh(context)
        } else if (lastRunningSubAgents > 0) {
            SubAgentForegroundService.stop(context)
        }
        lastRunningSubAgents = running
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
