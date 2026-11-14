# 交接给下个会话（Next Session Handoff）

> 本文件是**一次性交接单**：把当前情况直接发给下一个 AI 会话即可开工，不用先读别的文档。
> 完整知识库在：`AI_DEVELOPMENT.md`（主文档）+ `docs/`（按模块拆分的代码逻辑文件）。
> 更新时机：每个会话结束时更新本文件。

## 一、项目一句话

RikkaHub（rikkahub/rikkahub）的 fork `Rune-cn/rikkahub`，Android LLM 聊天客户端，Kotlin + Compose（Material3 Expressive）。包名 `app.ai.rune`，当前版本 **3.1.0**（versionCode 195）。

## 二、当前状态（截至 2026-11-14）

- **上游**：fork HEAD 基于上游 `3.1.1`（merge `eff05733`）。**上游已到 `64452581`（3.2.0），fork 落后 173 个提交 / 121 个文件，尚未同步。** 同步方案见下方「四、已知待办」第 6 条。
- **构建**：最近一次成功构建是 `16d3690c`（Daily Build #342，2026-11-11，schedule 触发）。
  ⚠️ **2026-11-14 的 6 个提交尚未经过 CI 验证**——通过 GitHub API/MCP 提交的改动不会触发 `push` 事件工作流（GitHub 防递归策略），需手动 `workflow_dispatch` 或本地 `./gradlew :app:assembleGithubDebug`。
- **本轮改动（2026-11-14，6 个提交）**：
  1. `refactor(sub-agent)`：子 Agent 默认模型选择从 `CardGroup.item` 的 `supportingContent`（手写 `Column` + `padding(top=4.dp)`）改为 **`CardGroup.formItem`**，整行占满并与卡片其它项对齐。
  2. `feat(tts)`：**移除内置 AiHubMix 语音服务商**，`DEFAULT_TTS_PROVIDERS` 只留 `SystemTTS`，新增 `RETIRED_BUILT_IN_TTS_PROVIDER_IDS` 常量。
  3. `fix(tts)`：`withBuiltInDefaults()` 里**先 filterNot 再补默认项**，防止 AiHubMix 每次归一化复活；同时修正 `selectedTTSProviderId` 悬空引用。
  4-6. 文档同步：`docs/02`（新增 12 节）、`docs/03`（新增归一化规范）、`AI_DEVELOPMENT.md`（版本与同步待办）。

## 三、上一轮会话的结论与踩坑（务必先读）

### A. AiHubMix 移除的两个必知点
- **只删定义不够**：`SettingsNormalization.withBuiltInDefaults()` 每次读盘/改设置都会把 `DEFAULT_TTS_PROVIDERS` 里缺失的项补回来 → 必须「先 filterNot 再补默认」。
- **必须按固定 UUID 匹配**（`e36b22ef-ca82-40ab-9e70-60cad861911c`），不能按名字 `"AiHubMix"`，否则会误删用户自建的同类提供商。
- **不要动**：`SettingProviderPage.kt` 里 `provider.name == "AiHubMix"` 的「10% 优惠」标签、`AIIconMatcher.kt` 的 `PATTERN_AIHUBMIX`——那是**聊天提供商**的通用能力，与语音服务无关。

### B. 验证文件的正确姿势（重要踩坑）
**不要用 `raw.githubusercontent.com` 判断文件是否已更新**：它有 CDN 缓存，刚提交的文件可能仍返回旧内容（本次曾因此误判「改动丢失」）。
正确做法：用 GitHub API（`gh_get_file` / `gh_get_content_sha`），或按 commit SHA 拉取。

### C. API 提交不触发 CI
经 GitHub API（MCP `gh_put_file`）提交的内容**不会**触发 `daily-build.yml`。需要构建时用 `workflow_dispatch` 或本地构建。

## 四、已知待办 / 注意事项（下个会话请留意）

1. **QQ 群加群链接的 `authKey` 有时效**：`SettingPage.kt` 的 `QQ_GROUPS.link` 过期后需换新链接（用户提供）。
2. **子 Agent 展示消息隔离**：生成/压缩/标题/建议一律用 `Conversation.mainMessages` 或 `isSubAgentDisplayMessage()` 过滤，**不要再拿 `currentMessages` 直接发模型**（见 docs/04-3.5）。
3. **链接交互约定**：对话内链接必须走 `LinkActionSheet`，新增渲染入口时给 `MarkdownBlock` 透传 `onLinkClick`，不要自己调 `context.openUrl`（见 docs/07-7）；非对话内链接（设置/关于/捐赠页）保持原行为。
4. 「运行日志」记录一切 `RunLog.log(...)`：**关键操作与异常**（应用启动、搜索、崩溃、以及任何新埋点），不限于特定几类；排查类功能优先留运行日志。
5. 版本号规则：没用户要求不升版；versionCode 必须单调递增（现在 195）。
6. **上游同步（最高优先）**：fork 落后上游 173 提交 / 121 文件（merge base `dfbe9a08` → 上游 `64452581`，3.1.1 → 3.2.0）。**用 merge 不要 rebase**（fork 有 100+ 定制提交，rebase 会逐个重放、反复解同一批冲突）。
   - 冲突高发：`service/ChatService.kt`（+281/-105，含节流落库与子 Agent 上下文隔离）、`ui/pages/chat/ChatList.kt`（上游拆成 `ChatListOverlays/ChatListPreview/ChatListSwipe/ChatSelection` 四个新文件）、`ui/pages/chat/ChatPage.kt`（+144/-18）、`data/datastore/SettingsNormalization.kt`、`data/ai/ChatRequestFactory.kt`。
   - **同步后必查**：上游 HEAD 仍在 `DEFAULT_TTS_PROVIDERS` 里内置 AiHubMix，确认它没有被带回来（`grep -n "RETIRED_BUILT_IN_TTS_PROVIDER_IDS" SettingsNormalization.kt` 应仍有 filterNot）。
7. 构建产物路径：`assembleGithubRelease` → `app/build/outputs/apk/github/release/`；README 下载名 `app-github-arm64-v8a-release.apk`。

## 五、给下个会话的开工步骤

1. 读 `AI_DEVELOPMENT.md` 与本文档并列对照（本文档是快照，AI_DEVELOPMENT 是知识库）。
2. 按需求类型翻 `docs/` 对应篇（设置→03，生成/工具→04，构建/版本→05，UI→07，同步→06）。
3. 遵守铁律：**任何修改都必须同步更新 AI_DEVELOPMENT.md + 相关 docs/ 文件**；中文；不写密钥/PAT；UI 代码禁 emoji；图标用前 grep 验证。
4. 开发后：本地静态自检 → 提交推送 → **注意 API/MCP 提交不会触发 CI**，需手动 `workflow_dispatch` 或本地构建 → 构建失败按 docs/06 排查套路修 → 更新本文档与知识库。
5. 交付时（每次都要）：回复里附 nightly 下载链接 —— https://github.com/Rune-cn/rikkahub/releases/download/nightly/app-github-arm64-v8a-release.apk

## 六、快速链接

- 仓库：https://github.com/Rune-cn/rikkahub
- 知识库：`AI_DEVELOPMENT.md` + `docs/`（01 架构 / 02 fork 定制 12 项 / 03 设置数据 / 04 生成重试 / 05 构建发布 / 06 上游同步 / 07 UI 约定 / 交接文档技能说明书.md）
- nightly APK：见上文
