package me.rerere.rikkahub.data.ai.subagent

import kotlinx.datetime.LocalDateTime
import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.ToolApprovalState
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.data.model.SubAgentStatus
import me.rerere.rikkahub.data.model.SubAgentTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubAgentDisplayTest {

    private val t0 = LocalDateTime(2026, 1, 1, 0, 0, 0)
    private val t1 = LocalDateTime(2026, 1, 1, 0, 0, 1)
    private val t2 = LocalDateTime(2026, 1, 1, 0, 0, 2)

    private fun message(
        role: MessageRole,
        at: LocalDateTime,
        vararg parts: UIMessagePart,
    ) = UIMessage(role = role, parts = parts.toList(), createdAt = at)

    private fun tool(name: String, executed: Boolean = true) = UIMessagePart.Tool(
        toolCallId = "call-$name",
        toolName = name,
        input = """{"q":"$name"}""",
        output = if (executed) listOf(UIMessagePart.Text("ok")) else emptyList(),
        approvalState = if (executed) ToolApprovalState.Auto else ToolApprovalState.Pending,
    )

    private fun task(
        messages: List<UIMessage>,
        status: SubAgentStatus = SubAgentStatus.RUNNING,
        error: String? = null,
    ) = SubAgentTask(
        id = "task-1",
        name = "worker",
        prompt = "do the work",
        status = status,
        error = error,
        messages = messages,
        startedAt = 0L,
        finishedAt = if (status == SubAgentStatus.RUNNING) null else 1000L,
    )

    @Test
    fun `keeps tool calls from every step in order`() {
        val task = task(
            listOf(
                message(MessageRole.SYSTEM, t0, UIMessagePart.Text("sys")),
                message(MessageRole.USER, t0, UIMessagePart.Text("task")),
                message(MessageRole.ASSISTANT, t0, tool("search_web")),
                message(MessageRole.ASSISTANT, t1, tool("workspace_shell"), UIMessagePart.Text("done")),
            )
        )

        val parts = buildSubAgentDisplayParts(task, streaming = null, finished = true, noResultText = "no result")

        assertEquals(3, parts.size)
        assertEquals("search_web", (parts[0] as UIMessagePart.Tool).toolName)
        assertEquals("workspace_shell", (parts[1] as UIMessagePart.Tool).toolName)
        assertEquals("done", (parts[2] as UIMessagePart.Text).text)
    }

    @Test
    fun `streaming message updates the same assistant message instead of duplicating it`() {
        val partial = message(MessageRole.ASSISTANT, t1, UIMessagePart.Text("partial"))
        val task = task(
            listOf(
                message(MessageRole.ASSISTANT, t0, tool("search_web")),
                partial,
            )
        )
        val streaming = partial.copy(parts = listOf(UIMessagePart.Text("full text")))

        val parts = buildSubAgentDisplayParts(task, streaming = streaming, finished = false, noResultText = "no result")

        assertEquals(2, parts.size)
        assertTrue(parts[0] is UIMessagePart.Tool)
        assertEquals("full text", (parts[1] as UIMessagePart.Text).text)
    }

    @Test
    fun `streaming message of a new step is appended`() {
        val task = task(listOf(message(MessageRole.ASSISTANT, t0, UIMessagePart.Text("step 1"))))
        val streaming = message(MessageRole.ASSISTANT, t1, tool("workspace_shell"))

        val parts = buildSubAgentDisplayParts(task, streaming = streaming, finished = false, noResultText = "no result")

        assertEquals(2, parts.size)
        assertEquals("step 1", (parts[0] as UIMessagePart.Text).text)
        assertEquals("workspace_shell", (parts[1] as UIMessagePart.Tool).toolName)
    }

    @Test
    fun `unfinished tool gets a placeholder when the task ends`() {
        val task = task(
            messages = listOf(message(MessageRole.ASSISTANT, t0, tool("workspace_shell", executed = false))),
            status = SubAgentStatus.CANCELLED,
        )

        val runningParts = buildSubAgentDisplayParts(task, null, finished = false, noResultText = "no result")
        assertTrue((runningParts.single() as UIMessagePart.Tool).output.isEmpty())

        val finishedParts = buildSubAgentDisplayParts(task, null, finished = true, noResultText = "no result")
        val tool = finishedParts.single() as UIMessagePart.Tool
        assertEquals("no result", (tool.output.single() as UIMessagePart.Text).text)
        assertEquals(ToolApprovalState.Auto, tool.approvalState)
    }

    @Test
    fun `failed task appends its error`() {
        val task = task(
            messages = listOf(message(MessageRole.ASSISTANT, t0, UIMessagePart.Text("worked"))),
            status = SubAgentStatus.FAILED,
            error = "boom",
        )

        val parts = buildSubAgentDisplayParts(task, null, finished = true, noResultText = "no result")

        assertEquals(2, parts.size)
        assertEquals("\nboom", (parts[1] as UIMessagePart.Text).text)
    }
}