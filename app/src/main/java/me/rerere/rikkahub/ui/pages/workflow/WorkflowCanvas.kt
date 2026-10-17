package me.rerere.rikkahub.ui.pages.workflow

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.model.WorkflowNode
import me.rerere.rikkahub.data.model.WorkflowNodeType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.context.LocalNavController
import org.koin.compose.koinInject
import me.rerere.rikkahub.ui.theme.CustomColors

/**
 * 工作流无限画布
 *
 * - 双指缩放 / 拖拽平移
 * - 点阵背景（随画布一起移动）
 * - 节点按树形自动布局，贝塞尔曲线连线（IF_ELSE 分支标注 true/false）
 */
@Composable
fun WorkflowCanvas(
    nodes: List<WorkflowNode>,
    modifier: Modifier = Modifier,
    onNodeClick: ((WorkflowNode) -> Unit)? = null,
) {
    val density = LocalDensity.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val layout = remember(nodes) { layoutWorkflow(nodes) }

    val nodeW = with(density) { CanvasMetrics.NODE_W.dp.toPx() }
    val nodeH = with(density) { CanvasMetrics.NODE_H.dp.toPx() }
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val edgeColor = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.2f, 2.5f)
                    offset += pan
                }
            }
    ) {
        // 点阵背景
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawDotGrid(offset, scale, gridColor)
        }
        // 连线层
        Canvas(modifier = Modifier.fillMaxSize()) {
            val byId = layout.nodes.associateBy { it.node.id }
            layout.edges.forEach { edge ->
                val from = byId[edge.fromId] ?: return@forEach
                val to = byId[edge.toId] ?: return@forEach
                drawEdge(from, to, scale, offset, nodeW, nodeH, edgeColor)
                edge.label?.let { label ->
                    val lx = offset.x + (from.x.dp.toPx() + nodeW) * scale + 6f
                    val ly = offset.y + (from.y.dp.toPx() + nodeH + 10f) * scale
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        lx,
                        ly,
                        android.graphics.Paint().apply {
                            color = labelColor.toArgb()
                            textSize = 10.dp.toPx() * scale
                        },
                    )
                }
            }
        }
        // 节点层
        layout.nodes.forEach { layoutNode ->
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = offset.x + layoutNode.x.dp.toPx() * scale
                        translationY = offset.y + layoutNode.y.dp.toPx() * scale
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                    .width(CanvasMetrics.NODE_W.dp)
            ) {
                CanvasNodeCard(node = layoutNode.node, onClick = { onNodeClick?.invoke(layoutNode.node) })
            }
        }
    }
}

/** 画布节点卡片 */
@Composable
private fun CanvasNodeCard(
    node: WorkflowNode,
    onClick: () -> Unit,
) {
    val (bg, accent) = when (node.type) {
        WorkflowNodeType.PROMPT ->
            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        WorkflowNodeType.AGENT ->
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        WorkflowNodeType.IF_ELSE ->
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        WorkflowNodeType.PARALLEL ->
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        onClick = onClick,
        modifier = Modifier.width(CanvasMetrics.NODE_W.dp),
        shape = RoundedCornerShape(12.dp),
        color = bg,
        tonalElevation = 2.dp,
        shadowElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = when (node.type) {
                        WorkflowNodeType.PROMPT -> stringResource(R.string.workflow_step_type_prompt)
                        WorkflowNodeType.AGENT -> stringResource(R.string.workflow_step_type_agent)
                        WorkflowNodeType.IF_ELSE -> stringResource(R.string.workflow_node_if_else)
                        WorkflowNodeType.PARALLEL -> stringResource(R.string.workflow_node_parallel)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                )
                Text(
                    text = node.name.ifBlank { stringResource(R.string.workflow_untitled) },
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val summary = when (node.type) {
                WorkflowNodeType.IF_ELSE -> node.condition
                else -> node.prompt
            }
            Text(
                text = summary.ifBlank { "{{input}}" },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 点阵背景 */
private fun DrawScope.drawDotGrid(offset: Offset, scale: Float, color: Color) {
    val step = 26.dp.toPx() * scale
    if (step < 8f) return
    val startX = offset.x % step
    val startY = offset.y % step
    var x = startX
    while (x < size.width) {
        var y = startY
        while (y < size.height) {
            drawCircle(
                color = color.copy(alpha = 0.45f),
                radius = 1.1f * scale,
                center = Offset(x, y),
            )
            y += step
        }
        x += step
    }
}

/** 贝塞尔连线（从父节点底部中点到子节点顶部中点） */
private fun DrawScope.drawEdge(
    from: LayoutNode,
    to: LayoutNode,
    scale: Float,
    offset: Offset,
    nodeW: Float,
    nodeH: Float,
    color: Color,
) {
    val startX = offset.x + from.x.dp.toPx() * scale + nodeW * scale / 2f
    val startY = offset.y + from.y.dp.toPx() * scale + nodeH * scale
    val endX = offset.x + to.x.dp.toPx() * scale + nodeW * scale / 2f
    val endY = offset.y + to.y.dp.toPx() * scale
    val midY = (startY + endY) / 2f
    val path = Path().apply {
        moveTo(startX, startY)
        cubicTo(startX, midY, endX, midY, endX, endY)
    }
    drawPath(path, color, style = Stroke(width = 1.8f * scale))
    // 箭头
    val arrow = 5f * scale
    val arrowPath = Path().apply {
        moveTo(endX, endY)
        lineTo(endX - arrow, endY - arrow * 1.6f)
        lineTo(endX + arrow, endY - arrow * 1.6f)
        close()
    }
    drawPath(arrowPath, color)
}

/** 工作流画布页（全屏查看） */
@Composable
fun WorkflowCanvasPage(
    title: String,
    nodes: List<WorkflowNode>,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { BackButton() },
                colors = CustomColors.topBarColors,
            )
        },
        containerColor = CustomColors.topBarColors.containerColor,
    ) { padding ->
        WorkflowCanvas(
            nodes = nodes,
            modifier = Modifier.padding(padding),
        )
    }
}

/** 画布路由：按 workflowId 读取工作流并展示 */
@Composable
fun WorkflowCanvasRoute(workflowId: String) {
    val settingsStore: SettingsStore = koinInject()
    val settings by settingsStore.settingsFlow.collectAsStateWithLifecycle()
    val navController = LocalNavController.current
    val workflow = settings.workflows.find { it.id == workflowId }
    WorkflowCanvasPage(
        title = workflow?.name?.ifBlank { stringResource(R.string.workflow_untitled) }
            ?: stringResource(R.string.workflow_untitled),
        nodes = workflow?.nodes ?: emptyList(),
        onBack = { navController.popBackStack() },
    )
}
