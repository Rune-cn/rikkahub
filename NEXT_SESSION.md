# 交接给下个会话（Next Session Handoff）

> 本文件是**一次性交接单**：把当前情况直接发给下一个 AI 会话即可开工，不用先读别的文档。
> 完整知识库在：`AI_DEVELOPMENT.md`（主文档）+ `docs/`（按模块拆分的代码逻辑文件）。
> 更新时机：每个会话结束时更新本文件。

## 一、项目一句话

RikkaHub（rikkahub/rikkahub）的 fork `Rune-cn/rikkahub`，Android LLM 聊天客户端，Kotlin + Compose（Material3 Expressive）。包名 `app.ai.rune`，当前版本 **3.3.0**（versionCode 198）。

## 二、当前状态（截至 2026-11-21）

- **上游**：✅ **已同步到 3.3.0**（上游 `358e2931`，22 个提交 / 116 个文件），merge commit `a3f77d01`，fork 侧 201 个定制提交全部保留（merge，未 rebase）。
- **版本号**：3.2.0/196 → **3.3.0/198**（高于上游的 197，保证覆盖安装）。
- **构建**：✅ 本地 `:app:compileGithubDebugKotlin` 与 `:app:testGithubDebugUnitTest`（`SubAgentDisplayTest`）通过；本轮子 Agent 改动尚未推送。
- **本轮改动（2026-11-21）**：
  1. 同步上游 3.3.0：5 处冲突逐个按「上游架构 + fork 功能」合并（见下文）。
  2. 定制功能 UI 按上游 MD3 Expressive 规范重做（详见 `docs/02` 第 13 节）。
  3. 修复「子 Agent 工具调用不显示」并优化子 Agent 执行（详见 `docs/02` 2.1）。

### 合并时的 5 处冲突（下次同步大概率复用同一判据）

1. **`app/build.gradle.kts`**：保留 fork 的 `appVersion/appVersionCode` 机制，只改数值（3.3.0/198）。
2. **`DefaultProviders.kt`**：**以上游文件为基底**，只剔除 6 个推广商（AiHubMix、APIMart、302.AI、随想AI网关、MaruCode、STA1N API）。**不要直接 `--ours`**：上游新增了 `DEEPSEEK_PROVIDER_ID`（快速入门页迁移要用），取 ours 会把它丢掉。剔除后顺带删掉不再使用的 7 个 Compose 文本 import。
3. **`SettingsNormalization.kt`**：上游改成「内置助手可删、清空才补回」；fork 的 TTS 过滤要保留，合起来是「`filterNot(RETIRED_…)` → `ifEmpty { DEFAULT_TTS_PROVIDERS }`」。
4. **`ChatMessage.kt`**：上游新增「按段落拆分气泡」（`splitMarkdownParagraphs` + `fastForEachIndexed`），fork 的链接操作面板要**透传到每个段落**的 `MarkdownBlock(onLinkClick = …)`，不能只留外层。
5. **`SettingPage.kt`**：搜索态整页只显示结果（fork），非搜索态用上游 `ProviderConfigWarningCard`（指向 `Screen.QuickStart`）；**删掉 fork 旧的重复提示卡**（它指向 `Screen.SettingProvider`，会在页面里出现两次）。

### 本轮 UI 重做的文件（改 UI 前先看 `docs/02` 第 13 节）

| 文件 | 改成了什么 |
|---|---|
| `ui/pages/workflow/WorkflowPage.kt` | 列表改 `ListItem`（20dp 形、`listItemColors`、`animateItem`）+ `ItemActionMenu`；FAB 新建、顶栏导入；`RikkaConfirmDialog` 删除确认；Cookie9Sided 空态；`16/8` 边距 + `nestedScroll` |
| `ui/pages/workflow/WorkflowCanvas.kt` | 节点卡改 `MaterialTheme.shapes.large`、去掉阴影、`labelLargeEmphasized` |
| `ui/pages/log/RunLogPage.kt` | `SecondaryTabRow` 两个页签；每条日志一张卡片（`cardColorsOnSurfaceContainer`、`JetbrainsMono`、可点复制）；W 级颜色从硬编码改 `tertiary`；Cookie9Sided 空态 |
| `ui/pages/extensions/RetrySettingsPage.kt` | 总开关 `switchItem` + `CardGroupIcon(Refresh)`；间隔/次数/三组关键字改 `formItem`；`16/8` 边距 |
| `ui/pages/extensions/ToolCallPage.kt` | 总开关与工具列表拆成两个 `CardGroup`；`switchItem` 整行可点、图标底色随状态 |
| `ui/pages/extensions/OtherSettingsPage.kt` | 去掉 8dp 双重边距；移除与开关重复的分组标题 |
| `ui/components/message/TaskPlanCard.kt` | `Surface(onClick)` + `shapes.large` + `surfaceContainerHigh`；`labelLargeEmphasized` |
| `ui/components/message/tools/SubAgentToolUI.kt` | 运行横幅改 `shapes.large` + `surfaceContainerHigh`，去阴影 |
| `ui/components/message/ChatMessage.kt` | 子 Agent 消息卡改 `shapes.large`、`labelMediumEmphasized` |
| `ui/pages/extensions/skills/SkillDetailPage.kt` | 分类卡改 `shapes.large` + `surfaceContainerHigh` + `titleSmallEmphasized` |
| `ui/pages/extensions/skills/SkillsPage.kt` | 分组标题 `titleSmallEmphasized`；分类对话框预置项改 `selectableGroup/selectable(Role.RadioButton)` |
| `data/ai/subagent/SubAgentManager.kt` | 新增 `streamingMessages`（整条 `UIMessage`，含工具调用）；`parentConversationId`；`maxSteps=48`；`cancelForConversation()` |
| `service/ChatService.kt` | 子 Agent 消息按 `task.id` 定位、拷贝全部 parts、收尾补未执行工具；停止生成时取消本会话子 Agent |
| `data/ai/tools/ChatToolFactory.kt` | 新增 `autoApproveTools`：子 Agent 后台放行工具并剔除 `ask_user` |
| `ui/components/message/tools/SubAgentToolUI.kt` | 详情弹窗：状态胶囊 + 模型/步数/工具数/耗时 + 每次工具调用的参数与结果预览 |
| `data/ai/subagent/SubAgentDisplay.kt` | 新增纯函数 `buildSubAgentDisplayParts()`：按序拼接多条助手消息、streaming 去重、收尾补未执行工具 |
| `app/src/test/.../SubAgentDisplayTest.kt` | 上述纯函数的单元测试（5 条用例） |
| `data/model/SubAgent.kt` | 派生 `stepCount` / `toolCallCount` / `elapsedSeconds`，UI 与工具共用 |

## 三、本轮踩到的坑（务必先读）

1. **`ListItem` 在 material3 1.5.0-rc01 有两套 API**：
   - 旧：`ListItem(headlineContent = …, supportingContent = …)`，**没有** `onClick` / `shapes`；
   - 新（Expressive）：`ListItem(onClick = …, shapes = ListItemDefaults.shapes(…), leadingContent = …, supportingContent = …, trailingContent = …) { 主标题文本 }`，主内容走**尾随的 `content` 槽**，没有 `headlineContent`。
   两者混用会报 `None of the following candidates is applicable`，并连带一串「@Composable invocations can only happen…」的假错误——**先看第一个 error**。
2. **`CardGroup` 自带水平内边距**：页面 `contentPadding` 用 `16/8` 后就不要再给 `CardGroup` 加 `padding(horizontal = 8.dp)`，否则是 24dp。
3. **`stringResource` 不能进非 inline 的普通 lambda**；`ifBlank {}` 是 inline，可以，但换成自定义 lambda 就会炸。拿不准就先取字符串再判断。
4. **本地编译环境（沙盒）**：AGP 只下载 x86_64 的 aapt2，在 arm64/proot 上会 SIGILL。可用 qemu 包装：
   `-Pandroid.aapt2FromMavenOverride=/usr/local/bin/aapt2`（该文件是 exec `qemu-x86_64-static` 的 ELF 包装器）。
   `:web:buildWebUi` 需要 `pnpm`，沙盒没有时用 `-x :web:buildWebUi` 跳过（CI 会正常构建）。
5. **版本号规则**：没用户要求不升版；`versionCode` 必须单调递增（现在 **198**）。
6. **子 Agent 的展示消息千万不要只传文本**：`toText()` 会丢掉 Tool part，子 Agent 第一步常常全是工具调用，主对话就会「什么都没有」。用 `SubAgentManager.streamingMessages` 整条消息；收尾时未执行的工具要补结果，否则主对话会出现等不到审批的 pending 按钮。

## 四、已知待办 / 注意事项

1. **推送与验证**：本轮改动还在本地沙盒分支 `sync/upstream-3.3.0`，**未推送**。推送后等 `Daily Build`（push 会自动触发；没跑先看 Actions 是否被暂停）。
2. **AiHubMix 复核**：上游 `DEFAULT_TTS_PROVIDERS` 仍内置 AiHubMix，同步后确认 `grep -n "RETIRED_BUILT_IN_TTS_PROVIDER_IDS" SettingsNormalization.kt` 仍有 `filterNot`。
3. **QQ 群加群链接的 `authKey` 有时效**：`SettingPage.kt` 的 `QQ_GROUPS.link` 过期后需换新链接（用户提供）。
4. **子 Agent 展示消息隔离**：生成/压缩/标题/建议一律用 `Conversation.mainMessages` 或 `isSubAgentDisplayMessage()` 过滤，**不要再拿 `currentMessages` 直接发模型**（见 docs/04-3.5）。
5. **链接交互约定**：对话内链接必须走 `LinkActionSheet`，新增渲染入口时给 `MarkdownBlock` 透传 `onLinkClick`（见 docs/07-7）。
6. **上游新增能力**（本轮带进来的）：快速入门页 `Screen.QuickStart`、按段落拆分气泡（设置 → 显示）、DeepSeek 语音、工作区共享存储挂载、消息概览压缩点。定制页与它们有交叉时，优先复用上游组件。

## 五、给下个会话的开工步骤

1. 读 `AI_DEVELOPMENT.md` 与本文档并列对照（本文档是快照，AI_DEVELOPMENT 是知识库）。
2. 按需求类型翻 `docs/` 对应篇（设置→03，生成/工具→04，构建/版本→05，UI→07，同步→06，定制清单/UI 规范→02）。
3. 遵守铁律：**任何修改都必须同步更新 AI_DEVELOPMENT.md + 相关 docs/ 文件**；中文；不写密钥/PAT；UI 代码禁 emoji；图标用前 grep 验证；新字符串三语同步。
4. 开发后：`:app:compileGithubDebugKotlin` 自检 → 提交推送 → 等 `Daily Build` 跑完 → 失败按 docs/06 排查 → 更新本文档与知识库。
5. 交付时（每次都要）：回复里附 nightly 下载链接 —— https://github.com/Rune-cn/rikkahub/releases/download/nightly/app-github-arm64-v8a-release.apk

## 六、快速链接

- 仓库：https://github.com/Rune-cn/rikkahub
- 知识库：`AI_DEVELOPMENT.md` + `docs/`（01 架构 / 02 fork 定制 12 项 + UI 规范 / 03 设置数据 / 04 生成重试 / 05 构建发布 / 06 上游同步 / 07 UI 约定 / 交接文档技能说明书.md）
- nightly APK：见上文
