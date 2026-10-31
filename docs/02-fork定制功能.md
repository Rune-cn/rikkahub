# 02 · Fork 定制功能（11 项，全部保留并适配 3.x）

> 同步/排查前先读这份清单：每一项都有"文件 + 逻辑要点"。
> 改功能时只动这些文件，别改上游同名功能的行为（如请求日志不属于本清单）。

## 1. 工具审批「自动允许」
- 设置：全局总开关 + 每工具开关，存在 `Settings.toolAutoAllowSetting`（`data/datastore/Settings.kt`）。
- UI：`ui/pages/extensions/ToolCallPage.kt`（扩展管理 → 工具调用）。
- 逻辑：`data/ai/tools/ChatToolFactory.kt` 的 `createTools()` 末尾：开启后把非 disabled 工具的 `needsApproval` 置为 false；**ask_user 默认不参与**（disabledTools 默认含它）。
- 旧机制 `WORKSPACE_ALLOW_ALL_APPROVAL_KEY`（工作区维度全部允许）已废弃移除，勿再引入。

## 2. 多 Agent（子 Agent）
- 文件：
  - `data/ai/subagent/SubAgentManager.kt`：任务调度、主上下文同步、前台服务保活。
  - `data/ai/tools/local/SubAgentTools.kt`：spawn_agent / wait_agent / list_agents / close_agent 工具。
  - `data/ai/tools/local/LocalTools.kt`：注册（`SUB_AGENT_TOOL_NAMES` 在此）。
  - `ui/pages/extensions/OtherSettingsPage.kt`：默认模型 / 继承上下文 / 总开关（`Settings.subAgentSetting` + `enableSubAgent`）。
  - `service/ChatService.kt`：子 Agent 输出流式写入主对话（`upsertSubAgentMessage` + `subAgentManager.updateMainContext`）。
- 关键点：
  - 生成循环已适配新 API（ChatTurnContext + ChatRequestFactory.createAgent + runAgent，见 04）。
  - `ChatToolFactory.createTools(..., excludeSubAgentTools = true)` 防止子 Agent 递归生成子 Agent。
  - 子 Agent 消息通过 `modelSnapshot.modelId = "sub-agent"` 标记，UI 据此做视觉区分（见第 10 项）。
  - **上下文隔离（重要）**：子 Agent 写入主对话的只是"展示消息"，其输出已通过 wait_agent 工具结果返回给主 Agent。组装主 Agent 模型上下文时必须过滤，否则模型会把子 Agent 的话当成自己说的（上下文污染）。
    - `Conversation.mainMessages` = `currentMessages` 过滤掉 `isSubAgentDisplayMessage()`（`data/model/Conversation.kt`），生成/压缩/标题建议统一用它。
    - `ChatService.runTurn()`：`history` 用过滤后的消息；`replyNodeId` 跳过子 Agent 节点（避免主 Agent 回复写进子 Agent 消息里）；`updateMainContext` 同样过滤（子 Agent 继承主上下文时不掺入其他子 Agent 输出）。
    - **回复节点语义（修复过）**：只有过滤后「最后一条是主 Agent 助手消息」才复用其节点；用户再发新消息（末尾是 USER）必须新建节点，否则回复会写进旧回复节点（表现为每次对话都在旧基础上继续改）。过滤后的 `history` 与 `nodes` 索引不再一一对应，定位用 `allHistory.indexOfFirst { it.id == lastMainAssistantMessage.id }` 回查原始索引再取 `nodes[i].id`；不要用 `indexOfLast { role == ASSISTANT }` 全局找最后一个助手消息。
    - `ChatMessageAssistantAvatar` 对子 Agent 消息直接 return（顶部已有子 Agent 标签行，避免误显示主 Agent 头像/名字，`useAssistantAvatar=true` 时曾踩过）。

## 3. AI 工作流 + 无限画布
- 文件：
  - `data/ai/workflow/WorkflowRunner.kt`：执行引擎（顺序/并行/条件分支，节点替换 {{input}}/{{prev}}）。
  - `data/ai/tools/local/WorkflowTools.kt`：AI 可调用的工作流工具。
  - `data/model/Workflow.kt`：数据模型（节点/步骤/状态）。
  - `ui/pages/workflow/WorkflowPage.kt` / `WorkflowCanvas.kt`：列表页 + 无限画布（网格、缩放、拖拽、编辑、类型选择）。
  - 入口：扩展管理 → AI 工作流（`Screen.Workflows`）。
- 数据：`Settings.workflows`（JSON 列表）。
- 运行：`WorkflowRunner` 用新代理 API（见 04），每步 AST 用 ChatTurnContext + createAgent + runAgent + toText 取结果。

## 4. 可配置重试（限流 tpm/rpm + 关键字）
- 设置字段（`NetworkSetting`，`Settings.kt`）：`enableAutoRetry`、`enableRateLimitRetry`、`rateLimitRetryCount`、`retryableStatusCodes`、`retryKeywords`、`stopRetryKeywords`。
- UI：扩展管理 → 其他（`OtherSettingsPage.kt`，归类为"重试"区）。
- 运行时：`ChatRequestFactory.retryPolicy()` 读取上述字段映射到 `me.rerere.ai.agent.RetryPolicy`（maxRetries + retryOn 谓词；停止关键字优先）。
- 已知差异：间隔策略（固定/抖动）字段保留在设置里，但运行时沿上游 `Retry.kt` 内建指数退避（1s/2s/4s…）。要彻底支持固定/抖动需改 `ai/.../agent/Retry.kt` 的 `withRetry`。

## 5. 设置搜索（64 项索引）
- 文件：`ui/pages/setting/SettingSearchIndex.kt`（索引 + 搜索函数 + 输入组件）与 `SettingPage.kt`（顶栏入口）。
- 交互细节见 07（经历多次迭代，照现状改，别回退）。
- 界面：点击顶部「设置」标题展开顶栏搜索框，输入即搜，结果按页面分组为原生 CardGroup 卡片组；`query` 命中标题（本地化文本）与关键词。
- 新增设置项后记得往 `SETTING_SEARCH_ITEMS` 加行（含中文关键词），否则搜不到。

## 6. 输入框草稿自动保存
- `Settings.chatDrafts: Map<String,String>`（会话 id → 草稿文本），已被系统自动读写（SETTINGS_FIELDS 注册）。
- UI：`ui/pages/chat/ChatPage.kt`（进入恢复草稿、`setTextAndPlaceCursorAtEnd`）、`ChatVM`。

## 7. 生成中节流落库
- `service/ChatService.kt` 的 `runTurn()`：`AgentEvent.MessageUpdated` 里每 1 秒 `session.flush()`（`lastStreamSaveTs`），保证进程被杀/退出不丢消息。
- 生成结束由 `finally { session.finishGeneration() }` 兜底。

## 8. Skill 手动分类
- `data/skills/SkillManager.kt`：SKILL.md frontmatter 的 `category` 字段（parseSkillDir 里读取）；`SkillMetadata.category`。
- 工具：`data/ai/tools/local/SkillCategoryTools.kt`（list_skills / set_skill_category）。
- UI：`ui/pages/extensions/skills/SkillsPage.kt`（列表 ⋮ 菜单 → 分类、分类徽标）、`SkillDetailPage.kt`（分类行）。
- 上游曾做自动分类，本 fork 用**手动分类**（带名称+描述）替代。

## 9. 更新检查指向本 fork 正式版
- `utils/UpdateChecker.kt`：请求 `https://api.github.com/repos/Rune-cn/rikkahub/releases/latest`，`Version` 比较（remote > local 才提示），下载指向本仓库。
- 不依赖上游 updates.rikka-ai.com。

## 10. 子 Agent 消息视觉区分
- `ui/components/message/ChatMessage.kt`：`isSubAgent`（modelId == "sub-agent"）时使用不同的背景/边框/标签（Material3 tonal 色阶，**无 emoji**）。

## 11. 对话内链接操作面板（浏览器 / 内嵌 / 分享）
- 组件：`ui/components/ui/LinkActionSheet.kt`（ModalBottomSheet），三个动作：
  - 浏览器打开：`utils/ContextUtil.kt` 的 `openUrl`（CustomTabs）
  - 内嵌打开：`navController.navigate(Screen.WebView(url))`（应用内 WebView 页）
  - 分享：`Intent.ACTION_SEND` 系统分享面板
- 接入点（对话内所有链接一律弹面板，不再直接跳浏览器）：
  - `ChatMessage.kt`：`pendingLinkUrl` 状态挂载面板；正文 markdown 链接（`MarkdownBlock.onLinkClick`）、引用角标（`handleClickCitation`）、引用列表（`ChatMessageCitations.onLinkClick`）。
  - `ChatMessageReasoning.kt` / `ChatMessageTranslation.kt` / `ChatMessageContextCheckpoint.kt`：各自的 `MarkdownBlock` 透传 `onLinkClick`。
  - `BuiltinToolUIs.kt`（`SearchWebPreview`）：搜索结果卡片/图片点击改弹面板（图片、URL 卡片）。
- 配套：`ui/components/richtext/Markdown.kt` 的 `MarkdownBlock` 增加 `onLinkClick: ((String) -> Unit)? = null` 参数透传给底层 `Markdown`（为 null 时维持上游默认：系统 UriHandler 打开浏览器）。
- 字符串：`link_action_*` 四个（en/zh/zh-rTW 三语已同步）。

## 其他 fork 改动（非功能）
- 关于页：GitHub/License 指向本仓库，无官网/群（群入口在设置页关于卡片，上游 Discord 已移除，替换为 QQ 群）。
- 捐赠页：保留上游 Kofi/爱发电 + 新增「爱发电 · Rune」(`afdian.com/a/CNRune`)。
- 设置页：QQ 群（QQGroup，universal-share 链接 + 群号复制兜底）。
- 运行日志（新增页）：见 `utils/RunLog.kt` + `ui/pages/log/RunLogPage.kt`，与上游"请求日志"独立。