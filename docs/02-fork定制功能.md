# 02 · Fork 定制功能（12 项，全部保留并适配 3.x）

> 同步/排查前先读这份清单：每一项都有"文件 + 逻辑要点"。
> 改功能时只动这些文件，别改上游同名功能的行为（如请求日志不属于本清单）。

## 1. 工具审批「自动允许」
- 设置：全局总开关 + 每工具开关，存在 `Settings.toolAutoAllowSetting`（`data/datastore/Settings.kt`）。
- UI：`ui/pages/extensions/ToolCallPage.kt`（扩展管理 → 工具调用）。
  - 形态：两个 `CardGroup`（总开关 / 工具列表），总开关用 `switchItem` + `CardGroupIcon(HugeIcons.Tools)`，图标底色随开关状态变化；工具行同样走 `switchItem`（整行可点）；页面边距 `16/8`。
- 逻辑：`data/ai/tools/ChatToolFactory.kt` 的 `createTools()` 末尾：开启后把非 disabled 工具的 `needsApproval` 置为 false；**ask_user 默认不参与**（disabledTools 默认含它）。
- 旧机制 `WORKSPACE_ALLOW_ALL_APPROVAL_KEY`（工作区维度全部允许）已废弃移除，勿再引入。

## 2. 子 Agent（多 Agent 协同）
> UI 文案统一为「子 Agent」（2026-10-31：原「多 Agent」字样全部改为「子 Agent」，含扩展管理开关与搜索关键词；英文 `Sub-agents`）。功能本质不变：主 Agent 派生子 Agent 并行执行任务。
> 2026-10-31：**「共享对话上下文」功能已按用户要求移除**——`SubAgentSetting.defaultInheritContext` 字段、`SubAgentTask.inheritContext`、spawn_agent 的 `inherit_context` 参数、`SubAgentManager.mainContext`/`updateMainContext`（改名 `updateActiveConversation`，只记录主会话 id）全部删除；子 Agent 不再继承主对话上下文（任务描述是它唯一的上下文）。
- 文件：
  - `data/ai/subagent/SubAgentManager.kt`：任务调度、前台服务保活。
  - `data/ai/tools/local/SubAgentTools.kt`：spawn_agent / wait_agent / list_agents / close_agent 工具。
  - `data/ai/tools/local/LocalTools.kt`：注册（`SUB_AGENT_TOOL_NAMES` 在此）。
  - `ui/pages/extensions/OtherSettingsPage.kt`：子 Agent 总开关 / 默认模型（`Settings.subAgentSetting` + `enableSubAgent`）。入口显示为「子 Agent」（`extensions_page_other_category`，原"其他"页，2026-10-31 改名；入口图标 `HugeIcons.AiBrain01`，副标题「主 Agent 可派生子 Agent 并行处理任务」）。
  - **默认模型的 UI 形态**：必须用 `CardGroup.formItem`（标题行 + 下方整行内容），不要塞进 `item` 的 `supportingContent` 里手写 `Column` + 额外 `padding`——`formItem` 自带 `padding(horizontal=16.dp, vertical=12.dp)` 与 `spacedBy(12.dp)`，和设置页其它表单项对齐。`ModelSelector` 用 `Modifier.fillMaxWidth()` 占满整行，标题在 `headlineContent`、说明文案在 `supportingContent`。
  - `service/ChatService.kt`：子 Agent 输出流式写入主对话（`upsertSubAgentMessage` + `subAgentManager.updateActiveConversation`）。
- 关键点：
  - 生成循环已适配新 API（ChatTurnContext + ChatRequestFactory.createAgent + runAgent，见 04）。
  - `ChatToolFactory.createTools(..., excludeSubAgentTools = true)` 防止子 Agent 递归生成子 Agent。
  - 子 Agent 消息通过 `modelSnapshot.modelId = "sub-agent"` 标记，UI 据此做视觉区分（见第 10 项）。
  - **上下文隔离（重要）**：子 Agent 写入主对话的只是"展示消息"，其输出已通过 wait_agent 工具结果返回给主 Agent。组装主 Agent 模型上下文时必须过滤，否则模型会把子 Agent 的话当成自己说的（上下文污染）。
    - `Conversation.mainMessages` = `currentMessages` 过滤掉 `isSubAgentDisplayMessage()`（`data/model/Conversation.kt`），生成/压缩/标题建议统一用它。
    - `ChatService.runTurn()`：`history` 用过滤后的消息；`replyNodeId` 跳过子 Agent 节点（避免主 Agent 回复写进子 Agent 消息里）。
    - **回复节点语义（修复过）**：只有过滤后「最后一条是主 Agent 助手消息」才复用其节点；用户再发新消息（末尾是 USER）必须新建节点，否则回复会写进旧回复节点（表现为每次对话都在旧基础上继续改）。过滤后的 `history` 与 `nodes` 索引不再一一对应，定位用 `allHistory.indexOfFirst { it.id == lastMainAssistantMessage.id }` 回查原始索引再取 `nodes[i].id`；不要用 `indexOfLast { role == ASSISTANT }` 全局找最后一个助手消息。
    - `ChatMessageAssistantAvatar` 对子 Agent 消息直接 return（顶部已有子 Agent 标签行，避免误显示主 Agent 头像/名字，`useAssistantAvatar=true` 时曾踩过）。

### 2.1 流式展示与执行约束（2026-11-21）

- **主对话里的子 Agent 消息必须带工具调用**：`SubAgentManager.streamingMessages` 推送的是整条 `UIMessage`（含 `UIMessagePart.Tool`），ChatService 拷贝全部 parts 写入主对话。此前只推 `toText()`，而子 Agent 第一步常常全是工具调用、没有文本，于是主对话里完全看不到工具调用（用户报的 bug 即此）。
- **定位用 `SubAgentTask.id`，不要用任务名**：任务名允许复用（send_message 续跑也复用同一 id），用名字会写错节点或丢更新；续跑重新进入 RUNNING 时把它从 `completedSubAgentTasks` 移除，否则不会再更新消息。
- **收尾清理**：完成/取消时，没有结果的工具调用补一条 `sub_agent_tool_no_result` 文本并把 `approvalState` 置为 `Auto`，避免主对话里出现永远等不到审批的按钮。
- **展示层**：`ChatMessage` 对 `modelId == "sub-agent"` 的消息把思考/工具链**全部展开**（主 Agent 仍折叠）；`SubAgentToolUI` 详情弹窗渲染完整转录（文本 + 每次工具调用的名称/参数摘要/结果预览），并显示「模型 · 步数 · 工具调用数 · 耗时」与状态胶囊。
- **后台执行约束**：子 Agent 没有审批 UI，`ChatToolFactory.createTools(autoApproveTools = true)` 强制放行并剔除 `ask_user`；`maxSteps = 48`；并发上限 3；用户停止某会话生成时 `cancelForConversation()` 只取消该会话派生的任务。
- **协议对齐**（参考 Codex multi-agent 与 DeepSeek Harness）：主对话只承载**展示消息**，模型上下文仍由 `Conversation.mainMessages` 过滤；`wait_agent` 返回 `progress`（steps / tool_calls / last_tool / elapsed_seconds）与 `timed_out`，转录截断为最后 12 条、每条 1200 字，避免把子 Agent 全文灌进主上下文（Codex 只回传 FINAL_ANSWER 的同一思路）。
- **多条助手消息要拼起来，不能只显示"当前这条"**：子 Agent 每一步（一次模型调用）产生一条助手消息，工具结果回来后再开下一条。只把最新一条写进主对话，会让上一步的工具调用在进入下一步时消失——这才是「不显示工具调用」的完整根因。组装逻辑已抽成纯函数 `buildSubAgentDisplayParts()`（`data/ai/subagent/SubAgentDisplay.kt`），并用 `SubAgentDisplayTest` 覆盖：跨步累加、streaming 同一条消息去重、未执行工具补结果、失败追加错误。
- **进度写进标题**：`Sub-Agent · name · 运行中 · 3 步 · 5 次工具调用 · 42 秒`（`sub_agent_progress`），折叠状态下也能判断它卡住还是在干活。
- **执行规则写进 system prompt**：没人能回答提问（不许请求确认）、任务描述是唯一上下文、工具失败要换方案而不是重复调用、最终回答必须自包含。
- **已结束任务只保留最近 12 个**（`MAX_RETAINED_FINISHED_TASKS`），列表与内存不会无限增长。

### 2.2 参考实现（Codex / DeepSeek Harness）

- **Codex**（`openai.com/index/unrolling-the-codex-agent-loop`）：一个 turn 内是单 Agent 循环——模型请求工具 → 执行并把输出追加进上下文 → 重新推理，直到模型给出助手消息为止；上下文窗口由 Agent 自己管理，工具执行受审批与沙箱约束。Codex CLI 没有"子 Agent"原语，本 fork 的 `spawn_agent` 相当于把同一套循环放进独立协程/独立上下文，再用 `wait_agent` 收结果。
- **DeepSeek Harness（DSH）**（`github.com/ZSeven-W/dsh-crew`、npm `@deepseek-ai/dsh-workflow-ptc`）：子 Agent 是**独立 session/上下文**，结果以 JSON 回到父级；并发与总量有上限（`maxConcurrentAgents` / `maxTotalAgents`）、嵌套深度受限（crew 限 3 层）；取消是 **caller-owned**，取消时立即中止子级并清理；进度上报包含 running step、tool calls、token usage；子 Agent 普通失败对父级表现为可处理的 `null`，而不是抛异常。
- 本 fork 已对齐的点：独立上下文、并发上限 3、步数上限 48、按会话取消、进度（steps/tool calls/elapsed）、结果文本自包含、失败写入 `error`；**未做**：token 统计与层级深度（子 Agent 已禁用 `spawn_agent`，深度固定为 1）。

## 3. AI 工作流 + 无限画布
- 文件：
  - `data/ai/workflow/WorkflowRunner.kt`：执行引擎（顺序/并行/条件分支，节点替换 {{input}}/{{prev}}）。
  - `data/ai/tools/local/WorkflowTools.kt`：AI 可调用的工作流工具。
  - `data/model/Workflow.kt`：数据模型（节点/步骤/状态）。
  - `ui/pages/workflow/WorkflowPage.kt` / `WorkflowCanvas.kt`：列表页 + 无限画布（网格、缩放、拖拽、编辑、类型选择）。
  - **列表页形态（3.3.0 起）**：独立卡片用 `ListItem`（`ListItemDefaults.shapes(20.dp)` + `CustomColors.listItemColors` + `animateItem()`），整行点击进画布；尾随 `Switch` + `ItemActionMenu`（运行/编辑/复制/导出/删除）；新建走 `ExtendedFloatingActionButton`，导入移到顶栏动作；删除用 `RikkaConfirmDialog`；空态用 `MaterialShapes.Cookie9Sided` 图标块 + `headlineSmallEmphasized`。
  - 入口：扩展管理 → AI 工作流（`Screen.Workflows`）。
- 数据：`Settings.workflows`（JSON 列表）。
- 运行：`WorkflowRunner` 用新代理 API（见 04），每步 AST 用 ChatTurnContext + createAgent + runAgent + toText 取结果。

## 4. 可配置重试（限流 tpm/rpm + 关键字）
- 设置字段（`NetworkSetting`，`Settings.kt`）：`enableAutoRetry`、`enableRateLimitRetry`、`rateLimitRetryCount`、`retryableStatusCodes`、`retryKeywords`、`stopRetryKeywords`。
- UI：**独立「重试」页**（`ui/pages/extensions/RetrySettingsPage.kt`，扩展管理 → 重试，位于"其他"上方；原"其他"页里的重试设置已全部搬入，入口图标 `HugeIcons.Refresh`）。
  - **总开关在重试页顶部**：`Switch(enableAutoRetry)`（「自动重试」，`extensions_page_retry_enable`）；上游「设置 → 网络」页里同字段的「自动重试」开关已移除（`setting_page_preferences_network_auto_retry` 字符串删除，其他方言 locale 的该键为未引用残留，不处理）。
  - `enableRateLimitRetry` 字段保留（默认 true），与规则开关合并为总开关逻辑：`enableAutoRetry=false` → 不重试；否则按下方规则。
  - **UI 形态（3.3.0 起）**：总开关用 `switchItem` + `CardGroupIcon(HugeIcons.Refresh)`；间隔 / 最大次数 / 三组关键字编辑器全部用 `formItem`（标题行 + 下方整行内容，不用 `supportingContent` 手写），页面边距 `16/8`。
  - 设置搜索索引指向 `Screen.RetrySettings`（`SettingSearchIndex.kt`）。
- 运行时：`ChatRequestFactory.retryPolicy()` 读取上述字段映射到 `me.rerere.ai.agent.RetryPolicy`（maxRetries + retryOn 谓词；停止关键字优先）。
- 状态提示：重试时只显示"重试中…"（`ChatService.retryStatus()` → `chat_generation_retrying_only`），不再拼接"网络连接已断开"等具体错误原因（`chat_generation_network_*` 字符串不再被引用，保留为上游资源）。
- 间隔策略（固定/指数退避/随机抖动）：`rateLimitRetryMode` 在 `retryPolicy()` 映射为 `RetryPolicy.backoff`（`ai/.../agent/Retry.kt` 的 `RetryBackoff` 枚举），运行时生效：
  - `FIXED`：每次都等待 `initialDelay`（固定间隔）；
  - `EXPONENTIAL_BACKOFF`：1s、2s、4s…（默认，等价上游原始行为）；
  - `JITTER`：Full Jitter，在 `[1ms, 指数退避值]` 内随机，避免大量请求同时重试造成"重试风暴"。
  - 测试：`ai/src/test/java/me/rerere/ai/agent/RetryTest.kt`（fixed / jitter 各一条）。
- **「恢复默认」**：重试页底部一键把重试相关设置恢复为默认（`NetworkSetting.withRetryDefaults()`，定义在 `Settings.kt`，默认值以 `NetworkSetting` 构造函数为准，不清代理等其他网络设置）。
- **输入体验**：状态码/关键字编辑框（`RetryChipListEditor`）点「添加」后输入框全宽显示并自动滚入视野（`bringIntoViewRequester`），页面 `LazyColumn` 加 `imePadding()` 防键盘遮挡，带占位提示「输入后回车确认」。

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
  - 3.3.0 起：分类行卡片用 `MaterialTheme.shapes.large` + `surfaceContainerHigh` + `titleSmallEmphasized`；分类对话框的预置项改为 `selectableGroup()` + `selectable(role = RadioButton)` 整行可选（触控区 ≥48dp），Radio 自身不再接收点击。
- 上游曾做自动分类，本 fork 用**手动分类**（带名称+描述）替代。

## 9. 更新检查指向本 fork 正式版
- `utils/UpdateChecker.kt`：检查**最新正式版（stable）**，`Version` 比较（remote > local 才提示），下载指向本仓库。不依赖上游 updates.rikka-ai.com。
- **双方案（2026-10-31 实测落地）**：
  1. GitHub API `releases/latest`（数据全：版本名/时间/日志/资产直链）；匿名限流 60 次/小时，设备共用 IP 易踩 403。
  2. 网页 `https://github.com/Rune-cn/rikkahub/releases/latest` 的 **302 重定向**做兜底（无 API 配额）：从 `Location` 头取最新 tag，资产名按当前规范拼 `releases/download/<tag>/app-github-arm64-v8a-release.apk`，HEAD 探测不通（旧版命名如 2.5.7 的 `app-arm64-v8a-release.apk`）则退到发布页链接。
- nightly 由 workflow 标记为 prerelease=true，`releases/latest` 不会误当正式版。
- **手动检查**：设置 → 关于 →「检查更新」（`SettingAboutPage.kt` 的 `about_page_check_update` 条目，图标 `HugeIcons.Refresh`）：点按即 `UpdateChecker.checkNowOnce()` 完整跑一次检查并弹窗显示版本/更新内容/下载按钮；已最新或失败走 Toast。不受聊天页抽屉卡片的「暂停更新提醒」与 `settings.init` 门槛影响，适合排查"卡片不显示"。
- **运行日志埋点**：更新检查全链路写入运行日志（标签 `UpdateCheck`，见「设置 → 关于 → 运行日志」）：开始/API 结果（含 HTTP 码）/302 兜底/Location tag/版本比较结论/下载直链探测/最终结果；聊天抽屉卡片启用门槛变化也记录（init 与暂停至时间）。排查"不显示更新"时先让用户看 `UpdateCheck` 行。
- **302 兜底也有更新内容**：404→302 路径拿不到真实 changelog 时，用 `update_check_fallback_changelog`（三语）填充"X 已发布 + 发布页链接"，保证卡片/弹窗的"更新内容"区域不为空（2026-10-31 实测：用户设备 API 403 触发兜底，卡片曾显示空内容的根因）。

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

## 12. 移除内置语音服务商 AiHubMix（TTS）
- 上游在 `data/datastore/DefaultSettings.kt` 的 `DEFAULT_TTS_PROVIDERS` 里内置了一个 AiHubMix（OpenAI 兼容 TTS，`baseUrl = https://aihubmix.com/v1`），本 fork **不再内置**。
- 改动（两处，缺一不可）：
  1. `DefaultSettings.kt`：从 `DEFAULT_TTS_PROVIDERS` 只保留 `SystemTTS`；新增常量 `RETIRED_BUILT_IN_TTS_PROVIDER_IDS`（固定 UUID `e36b22ef-ca82-40ab-9e70-60cad861911c`）记录已下架项。
  2. `data/datastore/SettingsNormalization.kt` 的 `withBuiltInDefaults()`：**先**按 ID 过滤掉 `RETIRED_BUILT_IN_TTS_PROVIDER_IDS`，**再**补内置项。
- **为什么要在归一化里删**：`withBuiltInDefaults()` 每次读盘和每次改设置都会跑，只要 `DEFAULT_TTS_PROVIDERS` 里还有该项就会自动补回；而且老用户 DataStore 里已经落库的旧条目不会自己消失。所以必须显式过滤，否则会「复活」。
- **按 ID 而不是按名字匹配**：用户可能自己新建一个叫 AiHubMix 的提供商（自定义 baseUrl/apiKey），按名字删会误删。固定 UUID 只命中上游内置的那一条。
- 选中项悬空处理：如果 `selectedTTSProviderId` 指向被删的项，回落到列表第一项（默认即系统 TTS）。
- 不受影响、**不要删**的地方：`ui/pages/setting/SettingProviderPage.kt` 里 `provider.name == "AiHubMix"` 的「10% 优惠」标签、`utils/AIIconMatcher.kt` 的 `PATTERN_AIHUBMIX` 图标匹配——这两处是给**聊天提供商**用的通用能力（用户自建同名 provider 时显示），与语音服务无关。
- 上游同步注意：这两处是上游代码，每次同步上游后都要确认 AiHubMix 没有被 `DEFAULT_TTS_PROVIDERS` 带回来（见 06）。

## 13. UI 规范对齐（3.3.0 起，新页面照此写）

上游已整体转向 Material3 Expressive，fork 定制页也在 3.3.0 同步时统一重做。新增/改动页面时按下面几条执行：

- **页面骨架**：`Scaffold` + `LargeFlexibleTopAppBar`，`Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)`；`LazyColumn` 的 `contentPadding = innerPadding + PaddingValues(horizontal = 16.dp, vertical = 8.dp)`，`verticalArrangement = spacedBy(16.dp)`。不要再给 `CardGroup` 额外加 `horizontal = 8.dp`（会变成 16+8 双重内边距）。
- **设置/表单**：一律 `CardGroup` 分组；整行可点的开关用 `switchItem`，标题+下方整行内容（选择器、分段按钮、标签编辑器）用 `formItem`；入口行带 `CardGroupIcon`（40dp 圆底），图标底色随开关状态在 `primaryContainer` / `surfaceContainerHigh` 之间切换。
- **独立卡片列表**（互不相关的条目）：`ListItem` + `ListItemDefaults.shapes(RoundedCornerShape(20.dp))` + `CustomColors.listItemColors` + `animateItem()`；次要操作统一放 `ItemActionMenu`（删除放最后并 `destructive = true`），不要手写 `DropdownMenu` 或文本「⋮」。
- **底部主操作**：列表页用 `ExtendedFloatingActionButton`，`contentPadding` 底部留 96dp；次要动作（导入等）放顶栏 `IconButton`。
- **空态**：`MaterialShapes.Cookie9Sided.toShape()` 图标块（112–128dp，`primaryContainer`）+ `headlineSmallEmphasized` 标题 + `bodyMedium` 说明；不要只放一行灰字。
- **标签页**：用 `SecondaryTabRow` / `SecondaryScrollableTabRow`（`containerColor = CustomColors.topBarColors.containerColor`），不要用旧的 `TabRow`。
- **字体**：标题用 `headlineSmallEmphasized` / `titleMediumEmphasized` / `titleSmallEmphasized`，行内强调用 `labelLargeEmphasized` / `labelMediumEmphasized`。
- **颜色**：只用 `MaterialTheme.colorScheme` / `CustomColors`，禁止硬编码色值（运行日志的 W 级从 `0xFFB25C00` 改为 `tertiary`）。
- **确认弹窗**：统一 `RikkaConfirmDialog`（`me.rerere.ui.components`），不要自己拼 `AlertDialog`。
- **等宽文本**：日志/代码用 `me.rerere.rikkahub.ui.theme.JetbrainsMono`，不要 `FontFamily.Monospace`。

## 其他 fork 改动（非功能）
- 关于页：GitHub/License 指向本仓库，无官网/群（群入口在设置页关于卡片，上游 Discord 已移除，替换为 QQ 群）。
- 捐赠页：保留上游 Kofi/爱发电 + 新增「爱发电 · Rune」(`afdian.com/a/CNRune`)。
- 设置页：QQ 群（QQGroup，universal-share 链接 + 群号复制兜底）。
- 运行日志（新增页）：见 `utils/RunLog.kt` + `ui/pages/log/RunLogPage.kt`，与上游"请求日志"独立。3.3.0 起：`SecondaryTabRow` 两个页签，每条日志一张 `CustomColors.cardColorsOnSurfaceContainer` 卡片（等宽字体、可点击复制），空态用 Cookie9Sided 图标块。
