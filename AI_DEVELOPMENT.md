# AI 开发交接文档（AI Development Handoff）

> 本仓库是 [rikkahub/rikkahub](https://github.com/rikkahub/rikkahub) 的二次开发 fork（`Rune-cn/rikkahub`），
> 本文档是给**后续 AI 开发会话**看的开发指南与交接说明。
>
> **铁律：任何对项目的修改（代码 / 界面 / 行为 / 构建 / 发布），都必须同步修改本文档与 `docs/` 下对应的逻辑文件**，保证"代码逻辑说明"永远与代码一致。

---

## 一、项目概况

| 项 | 值 |
|---|---|
| 仓库 | https://github.com/Rune-cn/rikkahub（fork upstream: rikkahub/rikkahub） |
| 包名 | `app.ai.rune`（applicationId） |
| 渠道 | GitHub flavor 发布（`app-github-arm64-v8a-release.apk`） |
| 语言 | Kotlin + Jetpack Compose (Material3 Expressive) |
| 模块 | `app`（Android 主模块）、`ai`（模型/Agent 核心）、`workspace`（proot/SSH 工作区）、`web-ui`（Web 前端，编译进 APK） |

## 二、当前开发情况（截至 2026-11-14）

- **版本**：`3.1.0`（versionCode `195`）。nightly 发布命名 `3.1.0-<commit>`，正式版（stable）为纯 `3.1.0`。
- **上游同步**：fork HEAD 基于上游 `3.1.1`（merge `eff05733`）。**截至 2026-11-14，上游已到 `64452581`（3.2.0），fork 落后 173 个提交 / 121 个文件**，尚未同步；同步方案与冲突清单见下文「上游同步待办」。
- **构建状态**：最近一次成功构建为 `16d3690c`（Daily Build #342，2026-11-11）。注意：**通过 GitHub API 提交的文件不会触发 push 事件**，所以 2026-11-14 的 5 个提交没有自动构建，需要手动 `workflow_dispatch` 或在本地 `./gradlew :app:assembleGithubDebug` 验证。
- **定制功能**：12 项全部保留并在 3.x 架构上重新适配（详见 [02-fork定制功能.md](docs/02-fork定制功能.md)）。
- **本轮新增（2026-11-14）**：
  - **子 Agent 默认模型的 UI 形态**：从 `CardGroup.item` 的 `supportingContent` 手写 `Column` + `padding(top=4.dp)` 改为 `CardGroup.formItem`（标题行 + 下方整行内容），复用卡片自带内边距，`ModelSelector` 以 `fillMaxWidth` 占满整行，与设置页其它「选择模型」项对齐。见 02-2。
  - **移除内置语音服务商 AiHubMix（TTS）**：`DEFAULT_TTS_PROVIDERS` 只保留 `SystemTTS`；新增 `RETIRED_BUILT_IN_TTS_PROVIDER_IDS` 常量，并在 `withBuiltInDefaults()` 里**先 filterNot 再补默认项**（否则每次归一化都会补回来），同时修正 `selectedTTSProviderId` 悬空引用。按固定 UUID 而非名字匹配，避免误删用户自建同名提供商。见 02-12 与 03-5。
- **历史记录（2026-10-31 晚）**：
  - 对话内链接点击弹操作面板（浏览器打开 / 内嵌打开 / 分享），见 02-11 与 07-7。
  - 修复子 Agent 消息上下文污染与回复写错节点（`Conversation.mainMessages` 过滤展示消息），见 02-2 与 04-3.5。
  - **修复回复节点定位**：`runTurn` 中只有「过滤子 Agent 展示消息后的最后一条」是主 Agent 助手消息才复用其节点；用户再发新消息（末尾是 USER）必须新建节点，否则新回复会写进上一轮回复节点里（表现为"每次对话都在旧回复基础上继续改"），见 04-3.5。
  - **重试间隔策略运行时生效**：设置里的「固定 / 指数退避 / 随机抖动」不再只是摆设——`ai/.../agent/Retry.kt` 新增 `RetryBackoff` 枚举（FIXED / EXPONENTIAL_BACKOFF / JITTER），`ChatRequestFactory.retryPolicy()` 把 `NetworkSetting.rateLimitRetryMode` 映射进 `RetryPolicy.backoff`，三种策略都在运行时生效（JITTER 为 Full Jitter），并补了单元测试（见 02-4 与 04-2）。
  - **重试设置独立成页**：扩展管理新增「重试」入口（位于"其他"上方，`Screen.RetrySettings` → `RetrySettingsPage.kt`），原「其他」页里的重试设置全部搬入；**移除「自动重试」总开关**（`enableRateLimitRetry` 默认 true，规则始终生效）；重试状态只显示"重试中…"（不再显示"网络连接已断开"等具体原因）。相关字符串三语同步（含补齐 zh-rTW 的重试相关翻译），见 02-4 与 04-2。
  - **重试页总开关**：「重试」页顶部新增「自动重试」`Switch`（绑 `enableAutoRetry`）；上游「设置 → 网络」页的同字段「自动重试」开关已移除（该键字符串从 en/zh/zh-rTW 删除）。**UI 文案「多 Agent」统一改为「子 Agent」**（扩展管理开关、搜索关键词、相关注释；`multi_agent_page_title` 残留字符串值同步更新），见 02-2、02-4、04-2。
  - **扩展管理「其他」入口改名「子 Agent」**（`extensions_page_other_category` 值改为 `Sub-agents`/`子 Agent`，页面标题随入口同串变更，zh-rTW 补齐）；**移除子 Agent「共享对话上下文」**（`defaultInheritContext`/`inherit_context`/`SubAgentManager.updateMainContext` 全删，改名 `updateActiveConversation` 只记主会话 id）；**重试页新增「恢复默认」+ 优化输入体验**（全宽输入框、占位提示、自动滚入视野、`imePadding`），见 02-2、02-4、03、07。
  - **子 Agent 入口图标 `HugeIcons.AiBrain01`、副标题「主 Agent 可派生子 Agent 并行处理任务」**；**更新检查改为"API releases/latest 优先 + 网页 302 重定向兜底"双方案**（实测：匿名 API 会 403 限流，302 方案无配额，asset 直链按新版命名拼装、HEAD 不通退发布页），见 02-2、02-9。
  - **关于页新增「检查更新」手动入口**：点击完整跑一次检测并弹窗展示版本/更新内容/下载（`UpdateChecker.checkNowOnce()`），绕开聊天抽屉卡片的「暂停更新提醒」门槛，便于直接验证检测链路，见 02-9。
  - **更新检查链路运行日志埋点**：`UpdateCheck` 标签记录检查全程（方案选择/HTTP 状态/Location/版本比较/直链探测/最终结果），ChatVM 抽屉卡片门槛变化也记录；配套「设置 → 关于 → 运行日志」排查，见 02-9。
- **会话交接**：每次会话结束时更新根目录 `NEXT_SESSION.md`（一次性交接单，可直接发给下个会话；本文件是知识库）。
- **已知待办/注意事项**：
  - 设置页 QQ 群使用 universal-share 加群链接，**`authKey` 有时效**，过期需换新链接（`SettingPage.kt` 的 `QQ_GROUPS.link`）。
  - 设置搜索交互经历过多次修正，最终形态见 [07-UI交互与代码约定.md](docs/07-UI交互与代码约定.md) 的"设置搜索"一节。
  - 构建产物因上游 3.x 引入 github/play flavor，APK 命名与路径已变（见 05）。
  - 子 Agent 展示消息被多个消费方读取（生成/压缩/标题/UI），新增消费方一律用 `mainMessages` / `isSubAgentDisplayMessage()`，不要直接遍历 `currentMessages` 发模型（见 04-3.5）。
  - **上游同步待办（重要）**：fork 落后上游 173 提交 / 121 文件（merge base `dfbe9a08` → 上游 `64452581`，3.1.1 → 3.2.0）。用 **merge 不要 rebase**。冲突高发：`service/ChatService.kt`（+281/-105，含节流落库与子 Agent 上下文隔离）、`ui/pages/chat/ChatList.kt`（上游拆成 `ChatListOverlays/ChatListPreview/ChatListSwipe/ChatSelection` 四个新文件）、`ui/pages/chat/ChatPage.kt`（+144/-18）、`data/datastore/SettingsNormalization.kt`、`data/ai/ChatRequestFactory.kt`。同步后**务必**按 02-12 复核 AiHubMix 是否被上游带回来（上游 HEAD 仍在 `DEFAULT_TTS_PROVIDERS` 里内置它）。

> **提交方式注意**：经 GitHub API（MCP `gh_put_file`）提交的改动**不会触发** `daily-build.yml`（GitHub 不把 API 提交当作 push 事件）。需要验证编译时，手动 `workflow_dispatch` 或本地 `./gradlew :app:assembleGithubDebug`。

## 三、代码逻辑索引（docs/ 目录，按模块单独成文件）

| 文件 | 内容 |
|---|---|
| [01-架构总览.md](docs/01-架构总览.md) | 模块划分、上游 3.x 关键重构（GenerationLoop 删除 / PreferencesStore 拆分 / flavors） |
| [02-fork定制功能.md](docs/02-fork定制功能.md) | fork 的 12 项定制：文件位置 + 逻辑要点（多 Agent / 工作流 / 自动允许 / 重试 / 设置搜索 / 草稿 / 节流落库 / Skill 分类 / 更新检查 / 子 Agent 视觉 / **链接操作面板** / **移除内置 AiHubMix TTS**；含子 Agent 上下文隔离） |
| [03-设置与数据.md](docs/03-设置与数据.md) | 设置系统：Settings.kt + SETTINGS_FIELDS 注册机制、新增字段规范、归一化与内置项下架 |
| [04-生成循环与重试.md](docs/04-生成循环与重试.md) | 新生成架构（ChatRequestFactory / AgentLoop / RetryPolicy）、子 Agent 与工作流的适配写法 |
| [05-构建与发布.md](docs/05-构建与发布.md) | flavor 结构、版本号规则、Actions 发布流程、更新检查 |
| [06-上游同步.md](docs/06-上游同步.md) | fork 同步上游的标准流程、冲突高发区、3.x 迁移坑 |
| [07-UI交互与代码约定.md](docs/07-UI交互与代码约定.md) | 设置搜索交互、图标使用规则、组件约定、emoji 禁用 |
| [交接文档技能说明书.md](docs/%E4%BA%A4%E6%8E%A5%E6%96%87%E6%A1%A3%E6%8A%80%E8%83%BD%E8%AF%B4%E6%98%8E%E4%B9%A6.md) | 「如何写/更新交接文档」技能说明副本（配合「更新交接文档」工作流） |

## 四、给后续 AI 的开发清单（每次动工前先读）

1. 要改功能 → 先读 `docs/02` 对应小节 + 相关源码，确认"这功能当初为什么这么做"。
2. 要动设置字段 → 先读 `03`（必须注册 SETTINGS_FIELDS，否则读不到写不进）。
3. 要动生成/重试/工具 → 先读 `04`（旧 GenerationLoop API 已不存在）。
4. 要改 UI/图标 → 先读 `07`（图标要先 grep 验证存在；LazyListScope 里禁 remember；UI 代码禁 emoji）。
5. 要发版/改版本号 → 先读 `05`（版本号规则、workflow 产物路径）。
6. 要同步上游 → 先读 `06`（merge 流程与踩坑）。
7. **改完任何东西 → 同步更新本文档与对应逻辑文件**，保持文档与代码一致。
8. **交付约定（用户要求）**：每次开发/修复完成后，回复里必须附上最新 APK 下载链接：`https://github.com/Rune-cn/rikkahub/releases/download/nightly/app-github-arm64-v8a-release.apk`（nightly 由 Actions 自动更新，次日/构建完成后即最新）。

## 五、禁止事项

- 文档与仓库里**不要写**内部密钥/PAT/签名配置（本仓库有 `.gitignore` 与外部 secrets，文档只写"secrets 名"）。
- UI 代码中**禁用 emoji**（README/发布说明可保留上游原有风格）。
- 未经用户要求，不要随意升版本号。
- **不要用 raw.githubusercontent.com 判断文件是否已更新**：它有 CDN 缓存，刚提交的文件可能仍返回旧内容；验证以 GitHub API（`gh_get_file`）或按 commit SHA 拉取为准。
