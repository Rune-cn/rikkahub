package me.rerere.rikkahub.data.ai.subagent

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.uuid.Uuid
import me.rerere.ai.agent.AgentEvent
import me.rerere.ai.agent.runAgent
import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.AppScope
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.ai.ChatRequestFactory
import me.rerere.rikkahub.data.ai.ChatTurnContext
import me.rerere.rikkahub.data.ai.tools.ChatToolFactory
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.datastore.findModelById
import me.rerere.rikkahub.data.datastore.findProvider
import me.rerere.rikkahub.data.datastore.getCurrentAssistant
import me.rerere.rikkahub.data.datastore.getCurrentChatModel
import me.rerere.rikkahub.data.model.Conversation
import me.rerere.rikkahub.data.model.MessageNode
import me.rerere.rikkahub.data.model.SubAgentStatus
import me.rerere.rikkahub.data.model.SubAgentTask
import me.rerere.rikkahub.data.repository.MemoryRepository
import me.rerere.rikkahub.data.repository.memoryId
import me.rerere.rikkahub.service.SubAgentForegroundService
import org.koin.java.KoinJavaComponent.getKoin

/**
 * 子 Agent 管理器
 *
 * 主 Agent 通过 `spawn_agent` 工具创建子 Agent：每个子 Agent 在**独立协程**里
 * 跑自己的多步生成循环（因此拥有完整工具能力，并可指定不同模型），
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

        /** 保留多少个已结束的任务记录（超出后按结束时间淘汰最旧的，避免列表无限增长） */
        const val MAX_RETAINED_FINISHED_TASKS = 12
    }

    // 延迟解析，避免与 LocalTools / ChatToolFactory 形成 Koin 循环依赖
    private val chatToolFactory: ChatToolFactory by lazy { getKoin().get() }
    private val requestFactory: ChatRequestFactory by lazy { getKoin().get() }
    private val memoryRepository: MemoryRepository by lazy { getKoin().get() }

    private val _tasks = MutableStateFlow<List<SubAgentTask>>(emptyList())
    val tasks: StateFlow<List<SubAgentTask>> = _tasks.asStateFlow()

    private val jobs = mutableMapOf<String, Job>()

    /** 当前主会话 id（由 ChatService 同步），用于把子 Agent 输出流式写入主对话 */
    @Volatile
    var activeMainConversationId: Uuid? = null
        private set

    /** 由 ChatService 在开始生成前调用，记录当前主会话 id */
    fun updateActiveConversation(conversationId: Uuid) {
        activeMainConversationId = conversationId
    }

    /**
     * 子 Agent 当前正在生成的那条助手消息（taskName → 完整消息）
     *
     * 带上全部 part（文本 / 推理 / 工具调用），主对话据此渲染出与主 Agent 一致的
     * 过程视图；只传文本会让流式阶段看不到工具调用（历史 bug）。
     */
    private val _streamingMessages = MutableStateFlow<Map<String, UIMessage>>(emptyMap())
    val streamingMessages: StateFlow<Map<String, UIMessage>> = _streamingMessages.asStateFlow()

    /** 启动子 Agent（异步，立即返回） */
    fun spawn(
        name: String,
        prompt: String,
        modelId: Uuid? = null,
    ): Result<SubAgentTask> {
        // 清理僵尸任务：状态仍是 RUNNING 但协程已不活跃（如上游异常退出）
        val zombieNames = _tasks.value
            .filter { it.status == SubAgentStatus.RUNNING && jobs[it.name]?.isActive != true }
            .map { it.name }
        if (zombieNames.isNotEmpty()) {
            _tasks.value = _tasks.value.map {
                if (it.name in zombieNames) {
                    it.copy(status = SubAgentStatus.FAILED, error = "stale task cleaned")
                } else {
                    it
                }
            }
            zombieNames.forEach { jobs.remove(it) }
            Log.w(TAG, "cleaned stale sub-agents: $zombieNames")
        }

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
            parentConversationId = activeMainConversationId,
            startedAt = System.currentTimeMillis(),
        )
        // 已结束的任务只保留最近 N 个：列表页与内存都不该无限增长
        _tasks.value = (_tasks.value + task).let { all ->
            val finished = all.filter { it.isFinished }.sortedByDescending { it.finishedAt ?: 0L }
            if (finished.size <= MAX_RETAINED_FINISHED_TASKS) {
                all
            } else {
                val dropped = finished.drop(MAX_RETAINED_FINISHED_TASKS)
                dropped.forEach { jobs.remove(it.name) }
                all.filterNot { candidate -> dropped.any { it.id == candidate.id } }
            }
        }
        syncSubAgentService()
        jobs[name] = appScope.launch { runTask(task) }
        Log.i(TAG, "spawn sub-agent: $name")
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

            // 子 Agent 的会话标识：工具（工作区 shell 的后台任务归属）与生成请求必须用同一个
            val conversationId = Uuid.random()

            // 子 Agent 拥有与主 Agent 同源的工具集（含技能、MCP、工作区、搜索、记忆等），
            // 但排除子 Agent 管理工具本身，避免无限递归
            // 子 Agent 在后台跑，没有审批 UI：所有工具自动放行，并剔除需要用户交互的工具
            // （ask_user 之类会一直卡住等待一个永远不会出现的回答）。
            val tools = chatToolFactory.createTools(
                settings = settings,
                assistant = assistant,
                model = model,
                conversationId = conversationId,
                excludeSubAgentTools = true,
                autoApproveTools = true,
            )

            var messages = resumeMessages ?: buildList {
                // 子 Agent 身份与规则说明（不继承主对话上下文；任务描述是它唯一的上下文）
                add(
                    UIMessage(
                        role = MessageRole.SYSTEM,
                        parts = listOf(
                            UIMessagePart.Text(
                                """
                                You are a SUB-AGENT spawned by the main agent Rikka to work on a delegated task.
                                - Your task name: ${task.name}
                                - You have the same toolset as the main agent, except you cannot spawn sub-agents.

                                Working rules:
                                1. Nobody can answer questions while you run. Never ask for confirmation; make a reasonable assumption and keep going.
                                2. Your task message is your only context. It does not include the main conversation.
                                3. Work autonomously until the task is actually done. Prefer using tools over guessing; check the result of every tool call before moving on.
                                4. If a tool fails, read the error and try a different approach. Do not repeat the same failing call.
                                5. Your final message is the deliverable. It is returned to the main agent automatically, so make it complete and self-contained: what you did, the key findings (numbers, paths, URLs, identifiers), and anything the main agent still needs to do. Use plain human-readable text, not JSON, unless the task explicitly asks for JSON.
                                """.trimIndent()
                            )
                        )
                    )
                )
                add(
                    UIMessage(
                        role = MessageRole.USER,
                        parts = listOf(UIMessagePart.Text(task.prompt)),
                    )
                )
            }
            var finalText = ""

            // 用 ChatTurnContext 走完整的多步生成循环（模型覆盖以任务指定为准）
            val turn = ChatTurnContext(
                settings = settings,
                assistant = assistant,
                model = model,
                providerSetting = model.findProvider(settings.providers) ?: error("Provider not found"),
                conversationId = conversationId,
                conversationSystemPrompt = null,
                workspaceCwd = null,
                memories = if (assistant.enableMemory) memoryRepository.getFiles(assistant.memoryId) else emptyList(),
                tools = tools,
            )
            val (request, hooks) = requestFactory.createAgent(turn, messages, MutableStateFlow(null))
            // 步数预算：防止子 Agent 在工具循环里失控（AgentRequest 默认 256 步）
            runAgent(request.copy(maxSteps = MAX_SUB_AGENT_STEPS), hooks)
                .flowOn(Dispatchers.IO)
                .collect { event ->
                    when (event) {
                        is AgentEvent.MessageUpdated -> {
                            messages = messages.let { current ->
                                val last = current.lastOrNull()
                                if (last?.role == MessageRole.ASSISTANT) current.dropLast(1) + event.message
                                else current + event.message
                            }
                            // 实时同步完整对话（供详情查看）
                            updateTask(task.name) { it.copy(messages = messages) }
                            // 关键：整条消息（含工具调用）都推给主对话，而不是只取文本。
                            // 子 Agent 第一步常常全是工具调用、没有文本，只推文本会导致
                            // 主对话里看不到任何工具调用。
                            _streamingMessages.value =
                                _streamingMessages.value + (task.name to event.message)
                            event.message.toText().takeIf { it.isNotBlank() }?.let { text ->
                                finalText = text
                            }
                        }

                        is AgentEvent.Retrying -> Unit

                        is AgentEvent.Finished -> {
                            event.message.toText().takeIf { it.isNotBlank() }?.let { finalText = it }
                        }
                    }
                }
            // 任务结束：移除流式输出（ChatService 用最终结果更新消息）
            _streamingMessages.value = _streamingMessages.value - task.name

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
            _streamingMessages.value = _streamingMessages.value - task.name
            updateTask(task.name) {
                it.copy(status = SubAgentStatus.CANCELLED, finishedAt = System.currentTimeMillis())
            }
            throw e
        } catch (e: Throwable) {
            _streamingMessages.value = _streamingMessages.value - task.name
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

    /** 取消所有运行中的子 Agent */
    fun cancelAll() {
        jobs.keys.toList().forEach { close(it) }
    }

    /** 取消某个主会话派生出来的子 Agent（用户停止该会话的生成时调用） */
    fun cancelForConversation(conversationId: Uuid) {
        _tasks.value
            .filter { it.parentConversationId == conversationId && !it.isFinished }
            .forEach { close(it.name) }
    }

    fun list(): List<SubAgentTask> = _tasks.value

    /** 清理已结束的任务记录 */
    fun clearFinished() {
        val finishedNames = _tasks.value.filter { it.isFinished }.map { it.name }
        finishedNames.forEach { jobs.remove(it) }
        _tasks.value = _tasks.value.filterNot { it.isFinished }
    }
}
