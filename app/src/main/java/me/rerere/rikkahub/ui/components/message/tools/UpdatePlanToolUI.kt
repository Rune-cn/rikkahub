package me.rerere.rikkahub.ui.components.message.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import me.rerere.common.http.jsonObjectOrNull
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.LeftToRightListBullet
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.ai.tools.local.PLAN_STATUS_COMPLETED
import me.rerere.rikkahub.data.ai.tools.local.PLAN_STATUS_IN_PROGRESS
import me.rerere.rikkahub.data.ai.tools.local.PLAN_STATUS_PENDING
import me.rerere.rikkahub.data.ai.tools.local.UPDATE_PLAN_TOOL_NAME
import me.rerere.rikkahub.utils.jsonPrimitiveOrNull

/** 单个待办步骤 */
internal data class TaskPlanStep(
    val step: String,
    val status: String,
) {
    val isCompleted: Boolean get() = status == PLAN_STATUS_COMPLETED
    val isInProgress: Boolean get() = status == PLAN_STATUS_IN_PROGRESS
    val isPending: Boolean get() = status == PLAN_STATUS_PENDING
}

/** 待办清单（来自 update_plan 工具入参） */
internal data class TaskPlan(
    val explanation: String?,
    val steps: List<TaskPlanStep>,
) {
    val total: Int get() = steps.size
    val completed: Int get() = steps.count { it.isCompleted }

    /** 是否所有步骤都已完成 */
    val allCompleted: Boolean get() = steps.isNotEmpty() && steps.all { it.isCompleted }
}

/**
 * 从 update_plan 工具入参解析待办清单
 *
 * 计划内容就保存在工具入参里（与 Codex 一致），无需额外存储；
 * 解析失败或没有有效步骤时返回 null。
 */
internal fun parseTaskPlan(arguments: JsonElement?): TaskPlan? {
    val obj = arguments?.jsonObjectOrNull ?: return null
    val planArray = obj["plan"] as? JsonArray ?: return null
    val steps = planArray.mapNotNull { item ->
        val itemObj = item as? JsonObject ?: return@mapNotNull null
        val step = itemObj["step"]?.jsonPrimitiveOrNull?.contentOrNull
            ?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val status = itemObj["status"]?.jsonPrimitiveOrNull?.contentOrNull
            ?: PLAN_STATUS_PENDING
        TaskPlanStep(step = step, status = status)
    }
    if (steps.isEmpty()) return null
    val explanation = obj["explanation"]?.jsonPrimitiveOrNull?.contentOrNull
        ?.takeIf { it.isNotBlank() }
    return TaskPlan(explanation = explanation, steps = steps)
}

/**
 * 待办清单内容，供正文卡片与工具步骤摘要复用
 */
@Composable
internal fun TaskPlanContent(
    plan: TaskPlan,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        plan.steps.forEach { step ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TaskPlanStatusIndicator(step)
                Text(
                    text = step.step,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (step.isInProgress) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textDecoration = if (step.isCompleted) TextDecoration.LineThrough else null,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (!plan.explanation.isNullOrBlank()) {
            Text(
                text = stringResource(
                    R.string.chat_message_tool_update_plan_explanation,
                    plan.explanation
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp),
            )
        }
    }
}

/** 状态指示：实心=已完成 / 圆环+实心点=进行中 / 空心圆=待办 */
@Composable
private fun TaskPlanStatusIndicator(step: TaskPlanStep) {
    val indicatorSize = 12.dp
    val primary = MaterialTheme.colorScheme.primary
    when {
        step.isCompleted -> Box(
            modifier = Modifier
                .size(indicatorSize)
                .clip(CircleShape)
                .background(primary),
        )

        step.isInProgress -> Box(
            modifier = Modifier
                .size(indicatorSize)
                .clip(CircleShape)
                .border(2.dp, primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(primary),
            )
        }

        else -> Box(
            modifier = Modifier
                .size(indicatorSize)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
        )
    }
}

/**
 * update_plan 工具步骤渲染器（Codex 同款待办）
 */
object UpdatePlanToolUI : ToolUIRenderer {
    override val toolName: String = UPDATE_PLAN_TOOL_NAME

    override fun icon(context: ToolUIContext): ImageVector = HugeIcons.LeftToRightListBullet

    @Composable
    override fun title(context: ToolUIContext): String {
        val plan = parseTaskPlan(context.arguments)
        return if (plan == null) {
            stringResource(R.string.chat_message_tool_update_plan)
        } else {
            stringResource(
                R.string.chat_message_tool_update_plan_progress,
                plan.completed,
                plan.total
            )
        }
    }

    override fun hasSummary(context: ToolUIContext): Boolean =
        parseTaskPlan(context.arguments) != null

    @Composable
    override fun Summary(context: ToolUIContext) {
        val plan = parseTaskPlan(context.arguments) ?: return
        TaskPlanContent(
            plan = plan,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    @Composable
    override fun Preview(context: ToolUIContext, onDismissRequest: () -> Unit) {
        val plan = parseTaskPlan(context.arguments) ?: return
        Column(
            modifier = Modifier
                .fillMaxHeight(0.8f)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(
                    R.string.chat_message_tool_update_plan_progress,
                    plan.completed,
                    plan.total
                ),
                style = MaterialTheme.typography.headlineSmall,
            )
            TaskPlanContent(plan = plan, modifier = Modifier.fillMaxWidth())
        }
    }
}
