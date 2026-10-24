package me.rerere.rikkahub.data.ai.tools.local

import android.content.Context
import me.rerere.ai.core.Tool
import me.rerere.rikkahub.data.ai.subagent.SubAgentManager
import me.rerere.rikkahub.data.files.SkillManager
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.event.AppEventBus
import me.rerere.tts.provider.TTSManager

class LocalTools(
    private val context: Context,
    private val eventBus: AppEventBus,
    private val ttsManager: TTSManager,
    private val settingsStore: SettingsStore,
    private val subAgentManager: SubAgentManager,
    private val skillManager: SkillManager,
) {
    val javascriptTool by lazy { buildJavascriptTool() }

    val timeTool by lazy { buildTimeInfoTool() }

    val clipboardTool by lazy { buildClipboardTool(context) }

    val ttsTool by lazy { buildTextToSpeechTool(eventBus, ttsManager, settingsStore) }

    val askUserTool by lazy { buildAskUserTool() }

    val screenTimeTool by lazy { buildScreenTimeTool(context, eventBus) }

    val calendarQueryTool by lazy { buildCalendarQueryTool(context) }

    val calendarCreateTool by lazy { buildCalendarCreateTool(context) }

    val chartDisplayTool by lazy { buildChartDisplayTool() }

    val updatePlanTool by lazy { buildUpdatePlanTool() }

    val subAgentTools by lazy { buildSubAgentTools(subAgentManager) }

    val workflowTools by lazy { buildWorkflowTools(settingsStore) }

    val skillCategoryTools by lazy { buildSkillCategoryTools(skillManager) }

    fun getTools(options: List<LocalToolOption>): List<Tool> {
        val tools = mutableListOf<Tool>()
        if (options.contains(LocalToolOption.JavascriptEngine)) {
            tools.add(javascriptTool)
        }
        if (options.contains(LocalToolOption.TimeInfo)) {
            tools.add(timeTool)
        }
        if (options.contains(LocalToolOption.Clipboard)) {
            tools.add(clipboardTool)
        }
        if (options.contains(LocalToolOption.Tts)) {
            tools.add(ttsTool)
        }
        if (options.contains(LocalToolOption.AskUser)) {
            tools.add(askUserTool)
        }
        if (options.contains(LocalToolOption.ScreenTime)) {
            tools.add(screenTimeTool)
        }
        if (options.contains(LocalToolOption.Calendar)) {
            tools.add(calendarQueryTool)
            tools.add(calendarCreateTool)
        }
        if (options.contains(LocalToolOption.ChartDisplay)) {
            tools.add(chartDisplayTool)
        }
        if (options.contains(LocalToolOption.UpdatePlan)) {
            tools.add(updatePlanTool)
        }
        // 多 Agent 工作（设置开关控制；关闭时 AI 无法派生子 Agent）
        if (settingsStore.settingsFlow.value.enableSubAgent) {
            tools.addAll(subAgentTools)
        }
        if (options.contains(LocalToolOption.Workflow)) {
            tools.addAll(workflowTools)
        }
        // Skill 分类管理：AI 可直接查看/设置分类（内置 skill 只读），走工具审批
        tools.addAll(skillCategoryTools)
        return tools
    }
}
