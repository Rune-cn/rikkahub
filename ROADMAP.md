# 功能规划与设计（Roadmap & Design）

> 记录计划中的大功能及其**可执行的设计方案**，方便后续实现与修改。
>
> 相关：[`ARCHITECTURE.md`](ARCHITECTURE.md)（现有架构）、[`CHANGELOG.md`](CHANGELOG.md)（已完成）

---

## 一、多 Agent 协同（主 Agent 指挥，子 Agent 执行）

### 目标

- **主 Agent**：负责决策、拆解任务、指挥、汇总结果
- **子 Agent**：执行具体任务，**可使用工具**，**可使用不同模型**
- 多个模型一起工作（例如：主 Agent 用强模型，子 Agent 用便宜/快的模型）

### 参考实现：Codex 的 `multi_agent_v1`

命名空间说明：`Tools for spawning and managing sub-agents.`

| 工具 | 作用 |
|---|---|
| `spawn_agent` | 启动一个子 agent，立即返回其 task 名 |
| `wait_agent` | 等待子 agent 完成（带超时） |
| `close_agent` | 终止子 agent |
| 消息 | 子 agent 之间/与父 agent 互发消息 |

`spawn_agent` 参数（Codex）：

```jsonc
{
  "task_name": "child",        // 任务名（同时作为子 agent 的标识）
  "message": "Reply done",     // 任务说明
  "fork_turns": "all",         // 继承父历史：all（默认）/ none
  "model": "gpt-5-codex",      // 可选：模型覆盖；默认继承父 agent 模型
  "agent_type": "worker"       // 可选：agent 类型（worker / explorer / 自定义角色）
}
```

要点：
- **默认继承父模型**，只有明确需要时才覆盖（Codex 的 prompt 明确要求"不要随便设 model"）
- 子 agent 有**完整工具集**，独立跑自己的循环
- 角色（agent role）可由 TOML 定义：`description`（用于 spawn 指引）、`config_file`（角色专属配置）、`nickname_candidates`

### RikkaHub 落地方案

**核心思路**：子 agent = **独立的会话 + 独立的 `GenerationLoop` 实例**，
这样天然拥有完整工具能力，且不污染主对话。

**数据模型**（新增，`data/model/SubAgent.kt`）：

```kotlin
@Serializable
enum class SubAgentStatus { RUNNING, WAITING, DONE, FAILED, CANCELLED }

@Serializable
data class SubAgentTask(
    val id: String,                 // task_id（spawn 返回）
    val name: String,               // 简短任务名，如 "research-docs"
    val prompt: String,             // 任务说明
    val modelId: Uuid? = null,      // null = 继承主 agent 模型
    val assistantId: Uuid? = null,  // 可选：用哪个助手（含其工具/提示词配置）
    val inheritHistory: Boolean = false,  // 是否把主对话历史带过去
    val status: SubAgentStatus = SubAgentStatus.RUNNING,
    val result: String? = null,     // 最终回答（完成后回填）
    val error: String? = null,
    val conversationId: Uuid? = null, // 子 agent 独立会话 id（可点开查看）
)
```

**新增工具**（`data/ai/tools/local/`，建议作为**内置工具**由助手设置开关控制）：

```kotlin
// 1. 启动子 agent（默认异步，立即返回 task_id）
spawn_agent(
    task_name: String,      // 简短任务名（用于后续 wait/close）
    message: String,        // 任务说明
    model: String? = null,  // 可选：模型覆盖（默认继承）
    wait: Boolean = false,  // true = 阻塞直到完成
) -> { task_id, status }

// 2. 等待/获取结果（可多个一起等）
wait_agent(task_names: List<String>, timeout_seconds: Int = 120)
    -> [{ name, status, result }]

// 3. 管理
list_agents()            -> 当前所有子 agent 的状态
close_agent(task_name)   -> 终止子 agent
send_message(task_name, message) -> 给运行中的子 agent 追加消息
```

**执行流程**

```
主 Agent (GenerationLoop, 主会话)
  └─ 调用 spawn_agent("research", "调研 X 并给出结论")
       ├─ 创建子会话（Conversation，标记 isSubAgent=true，不出现在会话列表）
       ├─ 构造子 GenerationLoop：
       │    模型 = model 覆盖 ?: 主 agent 模型
       │    工具 = 与主 agent 同源（ChatToolFactory），可按配置裁剪
       │    初始消息 = inheritHistory ? 主对话历史 : 仅任务说明
       ├─ 在独立协程中运行（不阻塞主 agent）
       └─ 完成后把 result 写回 SubAgentTask
  └─ 调用 wait_agent(["research"]) → 拿到结果 → 继续决策/汇总
```

**UI**

| 位置 | 内容 |
|---|---|
| 对话正文 | 子 agent 卡片（任务名 / 状态 / 结果摘要），**复用 `MessagePartBlock` 机制**（参考 `PlanBlock`） |
| 卡片交互 | 点击展开结果；点"查看"进入子 agent 的完整会话（只读或可继续） |
| 工具步骤 | `ToolUIRenderer` 渲染器：`spawn_agent`（显示任务名 + 状态徽标）、`wait_agent`（显示等待中的子 agent 列表） |
| 状态指示 | 运行中显示进度指示（复用 `ChatVM.processingStatus` 或独立状态） |

**并行与配额**（防失控）

- 限制同时运行的子 agent 数（如最多 3 个）
- 限制单个子 agent 的最大步数（`GenerationLoop.maxSteps`）
- 子 agent 默认**不允许再 spawn**（避免无限递归；可配置允许一层）
- 子 agent 的工具审批：继承全局「自动允许」设置；建议默认**不允许**危险工具（除非显式开启）

---

## 二、AI 工作流（多步骤编排）

### 目标

把「模型调用 / 工具调用 / 条件判断 / 数据处理」编排成一条可复用、可一键运行的流水线。

### 三种方案对比

| 方案 | 形态 | 优点 | 缺点 |
|---|---|---|---|
| **A. 列表式步骤** | 有序步骤列表（Step 1 → 2 → 3） | 实现简单、移动端体验好 | 无分支/循环，表达力有限 |
| **B. 节点图** | 画布 + 拖拽 + 连线（Dify 风格） | 表达力最强 | Android 上画布交互体验差、实现成本高 |
| **C. 多 Agent 编排** | 预设的 agent 分工与顺序（谁调谁） | **复用多 agent 基础设施**、天然支持工具 | 依赖多 agent 先完成 |

### 推荐：A + C 组合（分阶段）

**数据结构**（`data/model/Workflow.kt`）：

```kotlin
@Serializable
enum class WorkflowStepType {
    PROMPT,      // 调用模型（可指定模型/提示词）
    AGENT,       // 启动子 agent 执行（复用多 agent）
    TOOL,        // 直接调用某个工具
    CONDITION,   // 条件分支（按表达式/模型判断走哪条路）
}

@Serializable
data class WorkflowStep(
    val id: String,
    val type: WorkflowStepType,
    val name: String,
    val modelId: Uuid? = null,       // PROMPT/AGENT 用
    val prompt: String = "",         // 支持变量：{{input}}、{{step_1.output}}
    val toolName: String? = null,    // TOOL 用
    val condition: String? = null,   // CONDITION 用（如 contains/equals/模型判定）
    val enabled: Boolean = true,
)

@Serializable
data class Workflow(
    val id: Uuid,
    val name: String,
    val description: String = "",
    val steps: List<WorkflowStep>,
)
```

**执行器**：`WorkflowRunner`（`data/ai/workflow/`）

```
input → step1 → step2 → ... → output
          ↓ 变量表：每步输出以 stepId 存入，供后续步骤 {{step_1.output}} 引用
          ↓ CONDITION 步骤按结果选择跳过/进入后续分支
```

**UI**

- 工作流列表页（扩展管理新增入口）
- 编辑页：步骤列表 + 增删改排序（拖拽），每步选择类型/模型/提示词
- 运行页：显示每步状态与输出（类似待办卡片的进度展示）
- 快捷入口：聊天页可选择"用工作流运行"

**典型示例**（内置模板）

| 名称 | 步骤 |
|---|---|
| 深度研究 | 拆解问题 → 并行子 agent 调研 → 汇总 → 输出带引用的报告 |
| 代码评审 | 读取 diff → 分析问题 → 生成建议 → 输出报告 |
| 长文写作 | 大纲 → 分节撰写（并行子 agent）→ 合并 → 润色 |

---

## 三、实施计划（建议顺序）

| 阶段 | 内容 | 预估涉及文件 |
|---|---|---|
| **P1 多 Agent 核心** | `SubAgentTask` 模型 + `spawn_agent`/`wait_agent`/`list_agents` 工具 + 子会话执行 + 基础卡片 UI | `data/model/SubAgent.kt`、`data/ai/tools/local/SubAgentTools.kt`、`service/ChatService.kt`（子会话）、`ui/components/message/tools/SubAgentToolUI.kt`、`ChatMessageCot.kt`（卡片） |
| **P2 多 Agent 增强** | 并行/配额限制、子 agent 详情页、`send_message`、模型覆盖 UI | `SubAgentManager`、子 agent 会话页 |
| **P3 工作流** | `Workflow` 模型 + `WorkflowRunner` + 编辑/运行 UI + 内置模板 | `data/model/Workflow.kt`、`data/ai/workflow/WorkflowRunner.kt`、`ui/pages/workflow/` |

### 落地时需要注意

1. **子会话隔离**：子 agent 的会话不要出现在会话列表（加 `isSubAgent` 标记并在查询处过滤）
2. **取消传播**：用户停止生成时，应连带取消所有运行中的子 agent
3. **审批策略**：子 agent 继承全局「自动允许」；建议默认禁止子 agent 使用 `ask_user`（避免打扰）
4. **成本控制**：子 agent 用便宜模型是主要卖点，UI 上要方便设置
5. **持久化**：新增的设置字段必须同步 `PreferencesStore`（见 ARCHITECTURE.md 六-3）
