# 架构与修改指引（Architecture Guide）

> 本文档面向本 fork（`Rune-cn/rikkahub`）的二次开发：说明**代码架构、关键文件位置**和**常见改动的操作步骤**。
>
> 相关文档：
> - 日常提交 / 分支操作 → [`WORKFLOW.md`](WORKFLOW.md)
> - 同步上游更新 → [`FORK_SYNC.md`](FORK_SYNC.md)
> - 版本变更记录 → [`CHANGELOG.md`](CHANGELOG.md)
> - 仓库通用指南（上游） → [`AGENTS.md`](AGENTS.md)

---

## 一、技术栈

| 项目 | 说明 |
|---|---|
| 语言 / UI | Kotlin + Jetpack Compose（Material 3） |
| 依赖注入 | Koin |
| 本地存储 | DataStore（设置）、Room（会话/工作区）、ObjectBox |
| 网络 | OkHttp + kotlinx.serialization |
| 构建 | Gradle（Kotlin DSL）+ 版本目录 `gradle/libs.versions.toml` |
| 最低版本 | minSdk 26 / targetSdk 37 |
| 包名 | `applicationId = app.ai.rune`，代码包名 `me.rerere.rikkahub`（未改） |

---

## 二、模块划分（顶层目录）

| 目录 | 职责 |
|---|---|
| **`app/`** | Android 主模块：UI、ViewModel、业务逻辑、服务（**绝大部分改动在这里**） |
| **`ai/`** | AI SDK 抽象层：各 provider（OpenAI / Google / Anthropic 兼容）、`Tool`、`UIMessagePart` 等核心类型 |
| **`ui/`** | 可复用的 Compose 组件（不依赖 app 逻辑），如 `me.rerere.ui.charts.ChartCard` |
| **`common/`** | 通用工具：android、cache、http 扩展 |
| **`document/`** | 文档解析（PDF / DOCX / PPTX / EPUB） |
| **`highlight/`** | 代码语法高亮 |
| **`material3/`** | Material 3 颜色工具扩展 |
| **`search/`** | 搜索引擎实现（Bing / Bocha / Brave / Exa…） |
| **`speech/`** | 语音能力（ASR / TTS provider） |
| **`workspace/`** | 工作区：proot Linux 环境（`RootfsInstaller`、`ProotShellRunner`、`RootfsPatcher`） |
| **`mediagen/`** | 媒体生成（图片等） |
| **`oauth/`** | OAuth 授权支持 |
| **`build-logic/`** | Gradle 约定插件（构建配置复用） |
| **`web/` `web-ui/`** | 内置 Web 服务与前端 |
| **`docs/`** | 文档资源（图标等） |
| `.github/workflows/` | CI：`daily-build.yml`（构建 + 发布） |

---

## 三、app 模块目录结构

包根：`app/src/main/java/me/rerere/rikkahub/`

```
├── data/                    数据与业务核心
│   ├── ai/                  ★ AI 核心
│   │   ├── GenerationLoop.kt        生成循环（多轮工具调用、审批、重试）
│   │   ├── GenerationPrompts.kt     内置提示词
│   │   ├── TranslationHandler.kt    翻译
│   │   ├── tools/                   工具定义
│   │   │   ├── ChatToolFactory.kt   ★ 汇总所有工具 + 应用「自动允许」
│   │   │   ├── WorkspaceTools.kt    工作区工具（含审批默认值）
│   │   │   ├── SearchTools.kt / MemoryTools.kt / SkillsTools.kt / ConversationTools.kt
│   │   │   └── local/               ★ 本地（内置）工具
│   │   │       ├── LocalTools.kt        工具注册
│   │   │       ├── LocalToolOption.kt   工具开关枚举（助手设置里展示）
│   │   │       ├── AskUserTool.kt / CalendarTool.kt / ClipboardTool.kt
│   │   │       ├── JavascriptTool.kt / ScreenTimeTool.kt / TextToSpeechTool.kt
│   │   │       ├── TimeInfoTool.kt / ChartDisplayTool.kt / UpdatePlanTool.kt
│   │   ├── transformers/            输入/输出变换器（模板、占位符、OCR…）
│   │   ├── mcp/                     MCP 协议支持
│   │   └── prompts/                 提示词资源
│   ├── datastore/
│   │   └── PreferencesStore.kt      ★★ 全局设置：Settings 定义 + DataStore 读写
│   ├── db/                          Room 实体与 DAO
│   ├── repository/                  仓库层（Conversation/Workspace/Memory/Files…）
│   ├── model/                       数据模型（Assistant 等）
│   ├── network/                     代理、网络配置
│   ├── provider/                    provider 相关
│   ├── files/  sync/  export/  favorite/  event/  api/
├── di/                      Koin 依赖注入模块（AppModule.kt 等）
├── service/                 ★ 后台服务
│   ├── ChatService.kt               生成调度（发送、继续生成、审批处理）
│   ├── ConversationSession(.Manager).kt  会话状态与消息队列
│   ├── MessageQueue.kt              消息队列
│   ├── ChatNotificationManager.kt   通知
│   └── *ForegroundService.kt        前台服务
├── ui/
│   ├── pages/                       各页面（见下表）
│   ├── components/
│   │   ├── message/                 ★ 消息渲染
│   │   │   ├── ChatMessage.kt           消息整体渲染
│   │   │   ├── ChatMessageCot.kt        ★ 消息分段（ThinkingBlock / ChartBlock / PlanBlock…）
│   │   │   ├── ChatMessageTools.kt      工具调用步骤
│   │   │   ├── TaskPlanCard.kt          待办卡片 + 输入框上方进度
│   │   │   └── tools/                   ★ 工具专属 UI
│   │   │       ├── ToolUI.kt                ToolUIRenderer 接口 + ToolUIRegistry 注册表
│   │   │       ├── BuiltinToolUIs.kt        内置工具渲染器
│   │   │       └── UpdatePlanToolUI.kt      待办渲染器
│   │   ├── ai/  richtext/  nav/  ui/
│   ├── hooks/  theme/  activity/  context/  modifier/
├── utils/                   工具类（UpdateChecker.kt、Json.kt、ContextUtil.kt…）
└── web/                     内置 Web 服务器（routes / dto）
```

### ui/pages 页面索引

| 目录 | 内容 |
|---|---|
| `chat/` | 聊天页（`ChatPage.kt`、`ChatVM.kt`、`ChatList`、抽屉、导出） |
| `setting/` | 设置页（模型、provider、主题、搜索、语音、网络、偏好…） |
| `assistant/` | 助手管理（`AssistantPage.kt`、详情页 `detail/`，含本地工具开关） |
| `extensions/` | 扩展管理：快捷消息、提示词、技能、工作区、**`ToolCallPage.kt`（工具调用/自动允许）**、`OtherSettingsPage.kt`（重试设置） |
| `backup/` | 备份恢复（WebDAV / S3 / 提醒） |
| `translator/` `search/` `history/` `favorite/` `stats/` `log/` `debug/` `imggen/` `mediacreation/` `share/` `webview/` | 其他功能页 |

---

## 四、核心数据流

### 1. 一次对话

```
ChatPage (UI)
  └─ ChatVM.handleMessageSend()
       └─ ChatService.sendMessage()                    service/ChatService.kt
            ├─ settingsStore.settingsFlow.first()      取最新设置
            ├─ ChatToolFactory.createTools()           构建工具列表（含自动允许处理）
            └─ GenerationLoop.generateText()           data/ai/GenerationLoop.kt
                 ├─ provider 流式输出 → 更新 messages
                 ├─ 工具审批判断（needsApproval + approvalState）
                 ├─ 执行工具 → 结果写回消息
                 └─ 循环直到无工具调用
```

### 2. 设置读写（重要）

```
读：SettingsStore.settingsFlow (StateFlow，内存最新值)
      └─ UI: vm.settings.collectAsStateWithLifecycle()

写：vm.updateSettings { s -> s.copy(...) }   ← 推荐：基于最新值
      └─ SettingsStore.update(fn)
           ├─ settingsFlow.value = 新值          （立即生效）
           └─ persistSettings(dataStore, 新值)   （落盘，PreferencesStore.kt）
```

> ⚠️ **不要**用 `vm.updateSettings(settings.copy(...))`（UI 快照整体写回），
> 快照过期时会覆盖掉其他并发修改（详见「踩坑记录」第 3 条）。

---

## 五、本 fork 定制功能 → 文件索引

| 功能 | 关键文件 |
|---|---|
| **包名** `app.ai.rune` | `app/build.gradle.kts`（`applicationId`） |
| **版本号** | `app/build.gradle.kts` 的 `val appVersion`（如 `2.5.8`）与 `val appVersionCode`（如 `192`）<br>→ `versionName` = `2.5.8-<commit>`（正式版为 `2.5.8`）<br>→ `versionCode` 每次发版**手动 +1**（必须单调递增） |
| **工具调用页 + 自动允许** | `ui/pages/extensions/ToolCallPage.kt`（UI）<br>`data/datastore/PreferencesStore.kt`（`ToolAutoAllowSetting`）<br>`data/ai/tools/ChatToolFactory.kt`（把 `needsApproval` 置 false）<br>`ui/pages/extensions/ExtensionsPage.kt`（入口顺序） |
| **工作区工具审批** | `data/ai/tools/WorkspaceTools.kt`（`WorkspaceToolDefaultApprovals`） |
| **限流自动重试** | `data/datastore/PreferencesStore.kt`（`NetworkSetting` 新增字段）<br>`data/ai/GenerationLoop.kt`（`awaitNetworkRetryOrThrow`）<br>`ui/pages/extensions/OtherSettingsPage.kt`（设置 UI） |
| **继续生成** | `service/ChatService.kt`（`continueGeneration()`）<br>`ui/pages/chat/ChatVM.kt`（`canContinueGeneration`）<br>`ui/pages/chat/`（`ChatList` 的按钮，判断 `finishedAt == null`） |
| **任务计划（待办）** | `data/ai/tools/local/UpdatePlanTool.kt`（工具定义）<br>`data/ai/tools/local/LocalToolOption.kt` + `LocalTools.kt`（注册/开关）<br>`ui/components/message/tools/UpdatePlanToolUI.kt`（工具步骤渲染）<br>`ui/components/message/TaskPlanCard.kt`（正文卡片 + 输入框上方进度）<br>`ui/components/message/ChatMessageCot.kt`（`PlanBlock` 分段）<br>`ui/pages/chat/ChatPage.kt`（进度条位置、数据提取） |
| **更新检查指向本仓库** | `utils/UpdateChecker.kt`<br>`ui/components/ui/UpdateCard.kt` |
| **Firebase 可选化** | `di/AppModule.kt`、`ui/pages/chat/ChatVM.kt`（`lazy { runCatching { Firebase.analytics } }`）<br>`app/build.gradle.kts`（有 `google-services.json` 才启用插件） |
| **关于页 / 链接** | `ui/pages/setting/SettingAboutPage.kt`、`SettingPage.kt`、`utils/ContextUtil.kt` |
| **CI / 发布** | `.github/workflows/daily-build.yml` |

---

## 六、常见修改场景

### 1. 改版本号 / 发版

```kotlin
// app/build.gradle.kts —— 只改这两处
val appVersion = "2.5.8"      // 应用版本号
val appVersionCode = 192      // 每次发版 +1（单调递增）
```

| 目标 | 操作 |
|---|---|
| 预发布（Beta） | push 到 `master` → 自动构建，版本号 = `2.5.7-<commit>`，tag `nightly`（覆盖式） |
| 正式版 | Actions → **Daily Build** → Run workflow → 选 `stable` → 版本号 = `2.5.7`，tag = `2.5.7`（**需先改 `appVersion`**） |

### 2. 新增一个内置（本地）工具

1. **工具实现**：`data/ai/tools/local/XxxTool.kt`
   ```kotlin
   internal fun buildXxxTool(): Tool = Tool(
       name = "xxx",
       description = "……（写清楚用法，模型靠它决定何时调用）",
       parameters = { InputSchema.Obj(properties = buildJsonObject { /* JSON Schema */ }, required = listOf("...")) },
       needsApproval = { false },          // 有副作用才需要审批
       execute = { args -> listOf(UIMessagePart.Text("...")) },
   )
   ```
2. **开关枚举**：`data/ai/tools/local/LocalToolOption.kt`
   ```kotlin
   @Serializable @SerialName("xxx") data object Xxx : LocalToolOption()
   ```
3. **注册**：`data/ai/tools/local/LocalTools.kt`
   ```kotlin
   val xxxTool by lazy { buildXxxTool() }
   // getTools() 内：if (options.contains(LocalToolOption.Xxx)) tools.add(xxxTool)
   ```
4. **助手设置开关**：`ui/pages/assistant/detail/AssistantLocalToolPage.kt` 加一个 `item { Switch(...) }`
5. **文案**：`res/values/strings.xml` + `res/values-zh/strings.xml`
6. **（可选）专属 UI**：见下面第 4 条
7. **（可选）自动允许列表**：`ui/pages/extensions/ToolCallPage.kt` 的 `BUILT_IN_TOOL_NAMES`

### 3. 新增一个设置项 ⚠️ 三步缺一不可

```kotlin
// ① 定义字段（data class Settings，必须有默认值）
@Serializable
data class XxxSetting(val enabled: Boolean = false)
// Settings 内：val xxxSetting: XxxSetting = XxxSetting(),
```

```kotlin
// ② 持久化（PreferencesStore.kt）—— 漏了就会「重启后设置丢失」
val XXX_SETTING = stringPreferencesKey("xxx_setting")              // 加 key
preferences[XXX_SETTING] = JsonInstant.encodeToString(settings.xxxSetting)   // persistSettings 里保存
xxxSetting = JsonInstant.decodeFromString(preferences[XXX_SETTING] ?: "{}"), // 读取构造里加载
```

```kotlin
// ③ UI 更新（基于最新值，不要传快照）
vm.updateSettings { s -> s.copy(xxxSetting = s.xxxSetting.copy(enabled = true)) }
```

### 4. 给工具加专属 UI（渲染器）

1. 实现接口（`ui/components/message/tools/`，可参考 `UpdatePlanToolUI.kt`）：
   ```kotlin
   object XxxToolUI : ToolUIRenderer {
       override val toolName: String = "xxx"
       override fun icon(context: ToolUIContext): ImageVector = HugeIcons.Tools
       @Composable override fun title(context: ToolUIContext): String = "..."
       override fun hasSummary(context: ToolUIContext): Boolean = true
       @Composable override fun Summary(context: ToolUIContext) { /* 折叠时内联摘要 */ }
       @Composable override fun Preview(context: ToolUIContext, onDismissRequest: () -> Unit) { /* 详情 BottomSheet */ }
   }
   ```
2. 注册到 `ToolUI.kt` 的 `ToolUIRegistry.renderers` 列表。

> 若要在**正文里**显示成独立卡片（像图表、待办），另需：
> - `ui/components/message/ChatMessageCot.kt`：加 `MessagePartBlock.XxxBlock` + 在 `groupMessageParts()` 分流
> - `ui/components/message/ChatMessage.kt` 与 `ui/pages/chat/ConversationExport.kt`：渲染该 block

### 5. 新增界面文案

| 语言 | 文件 |
|---|---|
| 英文（默认） | `app/src/main/res/values/strings.xml` |
| 简体中文 | `app/src/main/res/values-zh/strings.xml` |
| 繁体中文 | `app/src/main/res/values-zh-rTW/strings.xml`（可留空，自动回退） |

> 本 fork 约定：**新增字符串只加中英文**即可。

---

## 七、踩坑记录（本项目实际遇到）

1. **`ndk.abiFilters` 与 `splits.abi` 冲突**
   `Conflicting configuration: 'arm64-v8a' in ndk abiFilters cannot be present when splits abi filters are set`
   → 只保留 `splits { abi { include("arm64-v8a"); isUniversalApk = false } }`，删掉 `ndk.abiFilters`。

2. **设置字段忘记持久化 → 重启后丢失**
   `Settings` 里加了字段，但 `PreferencesStore.persistSettings` / 读取构造没同步（`toolAutoAllowSetting` 就踩过）。
   → 加设置项**必须**走「六-3」三步；改完可用脚本核对字段是否齐全。

3. **用 UI 旧快照整体写回设置 → 覆盖并发修改**
   `vm.updateSettings(settings.copy(x = y))` 会把过期的整个 `Settings` 写回，重置期间的其他改动。
   → 统一用 `vm.updateSettings { s -> s.copy(x = y) }`（7 个 VM 均已提供 lambda 重载）。

4. **`public` 函数暴露 `internal` 类型**（编译错误）
   `'public' function exposes its 'internal' parameter type 'TaskPlan'`
   → 函数改成 `internal`，或把类型改 public。

5. **Navigation3 路由缺 `@Serializable` → 切后台崩溃**
   新增 `Screen.XXX` 路由时务必加 `@Serializable`（`4dc73db0` 修复过）。

6. **`LAZY` 协程必须手动启动**
   `launchGenerationJob` 返回 `CoroutineStart.LAZY` 的 Job，需 `session.setJob(job)` 才会跑。

7. **GitHub Actions 表达式里空字符串是 falsy**
   `${{ cond && '' || 'x' }}` 永远输出 `x`（因为 `''` 为假）。
   → 写 `${{ cond && 'a' || 'b' }}` 两个非空分支。

8. **fork 仓库默认禁用 Issues**
   页面上显示 "Issue creation is restricted in this repository"。
   → Settings → Features 勾选 Issues，或 `PATCH /repos/{owner}/{repo} {"has_issues": true}`。

9. **`allWarningsAsErrors` 未开启**：未使用的 import / 变量只报警告，不会阻断构建。

---

## 八、构建与发布速查

```bash
# 本地构建（需要签名配置，见 local.properties / CI secrets）
./gradlew assembleRelease          # 产物：app/build/outputs/apk/release/app-arm64-v8a-release.apk
./gradlew assembleDebug            # Debug 包

# 只构建 arm64（当前配置）；正式版版本号不带 commit：
./gradlew assembleRelease -PversionWithSha=false
```

**CI（`.github/workflows/daily-build.yml`）**

| 触发 | 行为 |
|---|---|
| push 到 `master` | 构建并发布 Beta 预览版（tag `nightly`，版本号 `appVersion-<commit>`） |
| 每天 UTC 09:00 / 18:00 | 同上（24 小时内无新提交则跳过） |
| 手动 `workflow_dispatch` | 可选 `nightly`（预览）/ `stable`（正式版） |

**下载地址**

- 预览版：`https://github.com/Rune-cn/rikkahub/releases/download/nightly/app-arm64-v8a-release.apk`
- 正式版：`https://github.com/Rune-cn/rikkahub/releases/download/<版本号>/app-arm64-v8a-release.apk`
- Release 列表：https://github.com/Rune-cn/rikkahub/releases

**签名密钥**：备份在 `/workspace/keystore-backup.zip`（alias `rune`）；GitHub Secrets：`KEY_BASE64`、`SIGNING_CONFIG`。

---

## 九、上游同步

见 [`FORK_SYNC.md`](FORK_SYNC.md)。要点：

```bash
git fetch upstream
git rebase upstream/master          # 冲突时以本分支定制为准
# 验证定制功能 → 推送
git push origin master --force-with-lease
```

同步后**务必检查**：`app/build.gradle.kts`（applicationId / versionName / splits）、
`PreferencesStore.kt`（设置字段是否被上游改动）、`ChatToolFactory.kt`（自动允许逻辑）、
`GenerationLoop.kt`（限流重试）、`ChatService.kt`（继续生成）。
