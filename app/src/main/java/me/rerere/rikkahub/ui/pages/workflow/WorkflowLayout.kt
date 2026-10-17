package me.rerere.rikkahub.ui.pages.workflow

import me.rerere.rikkahub.data.model.WorkflowNode
import me.rerere.rikkahub.data.model.WorkflowNodeType

/** 画布节点尺寸与间距（dp 值，绘制时按 dp 转 px） */
object CanvasMetrics {
    const val NODE_W = 190f
    const val NODE_H = 76f
    const val GAP_X = 70f
    const val GAP_Y = 34f
}

/** 布局后的节点（画布坐标，单位 dp） */
data class LayoutNode(
    val node: WorkflowNode,
    val x: Float,
    val y: Float,
    val depth: Int,
)

/** 布局后的连线 */
data class LayoutEdge(
    val fromId: String,
    val toId: String,
    /** IF_ELSE 的 true/false 标签 */
    val label: String? = null,
)

data class WorkflowLayout(
    val nodes: List<LayoutNode>,
    val edges: List<LayoutEdge>,
) {
    val maxX: Float get() = nodes.maxOfOrNull { it.x + CanvasMetrics.NODE_W } ?: 0f
    val maxY: Float get() = nodes.maxOfOrNull { it.y + CanvasMetrics.NODE_H } ?: 0f
    val minX: Float get() = nodes.minOfOrNull { it.x } ?: 0f
    val minY: Float get() = nodes.minOfOrNull { it.y } ?: 0f
}

/**
 * 树形 → 画布坐标的自动布局
 *
 * 规则：
 * - 顶层节点**竖向**排成主链（x = 0）
 * - IF_ELSE / PARALLEL 的分支排到**右侧列**（x 递增）
 * - 分支内部继续按同样规则递归
 * - 主链在分支之后继续向下（y 取两者较大值，避免重叠）
 *
 * 若节点已带合法坐标（用户拖拽过），则保留其坐标。
 */
fun layoutWorkflow(rootNodes: List<WorkflowNode>): WorkflowLayout {
    val nodes = mutableListOf<LayoutNode>()
    val edges = mutableListOf<LayoutEdge>()
    var yCursor = 0f

    fun place(
        node: WorkflowNode,
        x: Float,
        y: Float,
        depth: Int,
        parentId: String?,
        edgeLabel: String?,
    ) {
        val hasCoords = !node.x.isNaN() && !node.y.isNaN()
        val px = if (hasCoords) node.x else x
        val py = if (hasCoords) node.y else y
        nodes += LayoutNode(node, px, py, depth)
        if (parentId != null) edges += LayoutEdge(parentId, node.id, edgeLabel)
    }

    /** 递归布局一条竖向链，返回该链结束后的下一个可用 y */
    fun layoutChain(
        chain: List<WorkflowNode>,
        x: Float,
        startY: Float,
        depth: Int,
        parentId: String?,
        edgeLabel: String?,
    ): Float {
        var y = startY
        var prevId = parentId
        var label = edgeLabel
        chain.forEach { node ->
            place(node, x, y, depth, prevId, label)
            val nodeY = nodes.last().y
            prevId = node.id
            label = null
            var nextY = nodeY + CanvasMetrics.NODE_H + CanvasMetrics.GAP_Y

            when (node.type) {
                WorkflowNodeType.IF_ELSE -> {
                    var branchBottom = nextY
                    node.trueBranch?.let { branch ->
                        val endY = layoutChain(
                            listOf(branch),
                            x + CanvasMetrics.NODE_W + CanvasMetrics.GAP_X,
                            nextY,
                            depth + 1,
                            node.id,
                            "true",
                        )
                        branchBottom = maxOf(branchBottom, endY)
                    }
                    node.falseBranch?.let { branch ->
                        val endY = layoutChain(
                            listOf(branch),
                            x + (CanvasMetrics.NODE_W + CanvasMetrics.GAP_X) * 2,
                            nextY,
                            depth + 1,
                            node.id,
                            "false",
                        )
                        branchBottom = maxOf(branchBottom, endY)
                    }
                    nextY = branchBottom
                }

                WorkflowNodeType.PARALLEL -> {
                    var branchBottom = nextY
                    node.children.forEachIndexed { index, child ->
                        val endY = layoutChain(
                            listOf(child),
                            x + (CanvasMetrics.NODE_W + CanvasMetrics.GAP_X) * (index + 1),
                            nextY,
                            depth + 1,
                            node.id,
                            null,
                        )
                        branchBottom = maxOf(branchBottom, endY)
                    }
                    nextY = branchBottom
                }

                else -> Unit
            }

            y = nextY
        }
        return y
    }

    val endY = layoutChain(rootNodes, 0f, yCursor, 0, null, null)
    yCursor = maxOf(yCursor, endY)
    return WorkflowLayout(nodes = nodes, edges = edges)
}
