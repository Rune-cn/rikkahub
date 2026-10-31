# 交接给下个会话（Next Session Handoff）

> 本文件是**一次性交接单**：把当前情况直接发给下一个 AI 会话即可开工，不用先读别的文档。
> 完整知识库在：`AI_DEVELOPMENT.md`（主文档）+ `docs/`（按模块拆分的代码逻辑文件）。
> 更新时机：每个会话结束时更新本文件。

## 一、项目一句话

RikkaHub（rikkahub/rikkahub）的 fork `Rune-cn/rikkahub`，Android LLM 聊天客户端，Kotlin + Compose（Material3 Expressive）。包名 `app.ai.rune`，当前版本 **3.0.0**（versionCode 193）。

## 二、当前状态（截至 2026-10-31）

- **上游**：已同步至 `39ff6c43`（merge 上游 `eb530b0f`，HEAD 与 upstream/master 完全一致，`HEAD..upstream/master` = 0）。fork 在上游之上有 100+ 提交。
- **构建**：✅ nightly 自动构建成功（GitHub Actions `daily-build.yml`），APK 发布到 tag `nightly`：
  https://github.com/Rune-cn/rikkahub/releases/download/nightly/app-github-arm64-v8a-release.apk
  最近一次手动验证 `b41b17f4` 构建成功。
- **定制功能**（11 项，均在 3.x 架构上适配完成）：多 Agent、AI 工作流+无限画布、工具自动允许、可配置重试、设置搜索、输入框草稿、生成中节流落库、Skill 手动分类、更新检查指向本 fork、子 Agent 消息视觉区分、**对话内链接操作面板（浏览器/内嵌/分享）**。另：QQ 群入口（1131916400，universal-share 链接）、捐赠页新增「爱发电·Rune」(afdian.com/a/CNRune)、独立「运行日志」页。

## 三、上一次会话做了什么（本轮进展）

### A. 同步上游（收尾）
- 上一会话完成 128 提交大 rebase 后，本轮补上上游最后 2 个提交：
  - `eb530b0f` fix(markdown): 公式里 `<` 被 Jsoup 当 HTML 标签截断（MarkdownParser/LatexText/测试）
  - `20c24e7d` chore: update deps
- 本地此前已含等价 patch（`878f7825`/`6d71d498`），`git cherry` 判定 patch-id 相同 → `git merge upstream/master` **零冲突**，merge commit `39ff6c43`，普通 push 即可（无需 force）。

### B. 对话内链接操作面板（新功能）
- 新组件 `ui/components/ui/LinkActionSheet.kt`：ModalBottomSheet 三动作 = 浏览器打开（CustomTabs）/ 内嵌打开（Screen.WebView）/ 分享（ACTION_SEND）。
- 接入点：ChatMessage 正文 markdown 链接、引用角标、引用列表；ChatMessageReasoning/Translation/ContextCheckpoint 的 markdown；BuiltinToolUIs 搜索结果卡片与图片。
- `richtext/Markdown.kt` 的 `MarkdownBlock` 新增 `onLinkClick` 参数透传底层 Markdown。
- 新增 4 个字符串 `link_action_*`（en/zh/zh-rTW 三语已同步）。

### C. 修复子 Agent 消息上下文污染（bug）
- 问题：① 子 Agent 展示消息（role=ASSISTANT）被当成主 Agent 自己说的话发给模型（上下文污染）；② 主 Agent 回复被 upsert 进子 Agent 节点（"子 Agent 对话框里出现主 Agent 输出"）；③ `useAssistantAvatar=true` 时子 Agent 消息顶部误显示主 Agent 头像/名字。
- 修复：
  - `data/model/Conversation.kt`：新增 `mainMessages` + `UIMessage.isSubAgentDisplayMessage()`。
  - `service/ChatService.kt` `runTurn()`：history/updateMainContext 过滤子 Agent 消息；`replyNodeId` 跳过子 Agent 节点。
  - `data/ai/ConversationMetaGenerator.kt`：标题/建议改用 `mainMessages`。
  - `ui/components/message/ChatMessageAvatar.kt`：子 Agent 消息直接 return（顶部已有标签，不显示主 Agent 头像）。

### D. 重试间隔策略运行时生效（新功能，完成待办2）
- 之前：设置里的「固定 / 指数退避 / 随机抖动」只在 UI/设置里保留，运行时一律上游指数退避。
- 现在：
  - `ai/.../agent/Retry.kt` 新增 `RetryBackoff` 枚举（FIXED / EXPONENTIAL_BACKOFF / JITTER），`RetryPolicy` 增加 `backoff` 字段（默认 EXPONENTIAL_BACKOFF，行为不变）；`withRetry` 按策略计算等待时间，JITTER 为 Full Jitter（`[1ms, 指数退避值]` 内随机）。
  - `ChatRequestFactory.retryPolicy()` 把 `NetworkSetting.rateLimitRetryMode` 映射进 `RetryPolicy.backoff`（新增 `RetryBackoff` / `RetryMode` import）。
  - 测试：`RetryTest.kt` 新增 `fixed backoff keeps the same delay` / `jitter backoff waits within the exponential cap`。

### E. 重试设置独立成页 + 状态文案简化 + 重试页总开关（用户要求，经历两轮定稿）
- 扩展管理新增「重试」入口（`HugeIcons.Refresh` 图标，位于"其他"上方）：`Screen.RetrySettings`（RouteActivity）+ 新页面 `ui/pages/extensions/RetrySettingsPage.kt`（从 OtherSettingsPage 搬入：**自动重试总开关** / 重试间隔 / 最大次数 / 可重试状态码 / 重试关键字 / 停止重试关键字 + `RetryChipListEditor`）。
- **重试页总开关**：顶部「自动重试」`Switch` 绑 `NetworkSetting.enableAutoRetry`（`extensions_page_retry_enable`）；`enableAutoRetry=false` → `RetryPolicy.NONE`。**上游「设置 → 网络」页的同字段「自动重试」开关已移除**（`SettingPreferencesNetworkPage.kt`，`setting_page_preferences_network_auto_retry` 字符串从 en/zh/zh-rTW 删除；其他方言 locale 残留为未引用资源，不动）。
- 重试状态文案：`ChatService.retryStatus()` 只返回「重试中…」（`chat_generation_retrying_only`，三语），不再拼接「网络连接已断开 / 连接超时」等具体原因（移除 4 个网络异常 import）。
- 设置搜索：索引项 `extensions_page_retry_section` → `Screen.RetrySettings`（关键词含"自动重试"）；「其他」项去掉「自动重试」关键词。
- zh-rTW 补齐重试相关翻译（此前整块回退英文）。

### F. UI 文案「多 Agent」→「子 Agent」（用户要求）
- 扩展管理子 Agent 开关：`extensions_page_multi_agent_enable`（en "Multi-agent"→"Sub-agents"；zh "多 Agent 工作"→"子 Agent"；zh-rTW 补齐"子 Agent"）。
- 搜索关键词去掉「多 agent」；`multi_agent_page_title` 死字符串值同步更新（en/zh）；代码注释更新。

### G. 三项收尾（用户要求）
1. **重试选项输入体验**：`RetrySettingsPage` 的 `LazyColumn` 加 `imePadding()`；`RetryChipListEditor` 点「添加」后输入框改全宽 + 占位提示（`extensions_page_retry_input_hint`）+ `bringIntoViewRequester` 自动滚入视野，解决"输入时看不到"。
2. **重试页「恢复默认」**：页尾 CardGroup 条目（`HugeIcons.Refresh`），一键 `NetworkSetting.withRetryDefaults()`（定义在 `Settings.kt`，以构造函数默认值为准，不动代理等其他网络设置）。
3. **扩展管理「其他」改名「子 Agent」**：`extensions_page_other_category` = `Sub-agents`/`子 Agent`（en/zh 改值，tw 补齐）+ 搜索关键词更新；页面标题随入口同串。
4. **删除子 Agent「共享对话上下文」**：`SubAgentSetting.defaultInheritContext`、`SubAgentTask.inheritContext`、spawn_agent 的 `inherit_context` 参数、`SubAgentManager.mainContext` 与 `updateMainContext`（改名 `updateActiveConversation(conversationId)`，只记主会话 id）全部移除；`extensions_page_sub_agent_inherit_context` 字符串删除；子 Agent 提示词固定说明"不继承主对话上下文"。

### H. 子 Agent 图标副标题 + 正式版更新检测双方案（用户要求）
1. **图标/副标题**：扩展管理「子 Agent」入口图标 `HugeIcons.Settings03` → `HugeIcons.AiBrain01`；副标题 `extensions_page_other_desc` → 「主 Agent 可派生子 Agent 并行处理任务」（en/zh/tw 同步）。
2. **更新检测**（`utils/UpdateChecker.kt`，2026-10-31 命令行实测）：
   - 实测结论：`api.github.com/.../releases/latest` 匿名会 **403 限流**（60 次/h）；网页 `github.com/Rune-cn/rikkahub/releases/latest` 返回 **302**，`Location: .../releases/tag/<tag>` 无配额限制；资产命名旧（2.5.7=`app-arm64-v8a-release.apk`）新（nightly=`app-github-arm64-v8a-release.apk`）不一致，不能只靠猜。
   - 落地：API 优先（资产/日志/时间齐全）→ 失败降级 302 兜底（仅 tag 版本，资产按新版命名拼直链+HEAD 校验，不通退发布页）。nightly 是 prerelease=true，`releases/latest` 不会误判。

## 四、已知待办 / 注意事项（下个会话请留意）

1. **QQ 群加群链接的 `authKey` 有时效**：`SettingPage.kt` 的 `QQ_GROUPS.link` 过期后需换新链接（用户提供）。
2. **子 Agent 展示消息隔离**：生成/压缩/标题/建议一律用 `Conversation.mainMessages` 或 `isSubAgentDisplayMessage()` 过滤，**不要再拿 `currentMessages` 直接发模型**（见 docs/04-3.5）。
3. **链接交互约定**：对话内链接必须走 `LinkActionSheet`，新增渲染入口时给 `MarkdownBlock` 透传 `onLinkClick`，不要自己调 `context.openUrl`（见 docs/07-7）；非对话内链接（设置/关于/捐赠页）保持原行为。
4. 「运行日志」记录一切 `RunLog.log(...)`：**关键操作与异常**（应用启动、搜索、崩溃、以及任何新埋点），不限于特定几类；排查类功能优先留运行日志。
5. 版本号规则：没用户要求不升版；versionCode 必须单调递增（现在 193）。
6. 构建产物路径：`assembleGithubRelease` → `app/build/outputs/apk/github/release/`；README 下载名 `app-github-arm64-v8a-release.apk`。

## 五、给下个会话的开工步骤

1. 读 `AI_DEVELOPMENT.md` 与本文档并列对照（本文档是快照，AI_DEVELOPMENT 是知识库）。
2. 按需求类型翻 `docs/` 对应篇（设置→03，生成/工具→04，构建/版本→05，UI→07，同步→06）。
3. 遵守铁律：**任何修改都必须同步更新 AI_DEVELOPMENT.md + 相关 docs/ 文件**；中文；不写密钥/PAT；UI 代码禁 emoji；图标用前 grep 验证。
4. 开发后：本地静态自检 → 提交推送（触发 CI）→ 构建失败按 docs/06 排查套路修 → 更新本文档与知识库。
5. 交付时（每次都要）：回复里附 nightly 下载链接 —— https://github.com/Rune-cn/rikkahub/releases/download/nightly/app-github-arm64-v8a-release.apk

## 六、快速链接

- 仓库：https://github.com/Rune-cn/rikkahub
- 知识库：`AI_DEVELOPMENT.md` + `docs/`（01 架构 / 02 fork 定制 11 项 / 03 设置数据 / 04 生成重试 / 05 构建发布 / 06 上游同步 / 07 UI 约定 / 交接文档技能说明书.md）
- nightly APK：见上文