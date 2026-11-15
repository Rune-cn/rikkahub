# 交接给下个会话（Next Session Handoff）

> 本文件是**一次性交接单**：把当前情况直接发给下一个 AI 会话即可开工，不用先读别的文档。
> 完整知识库在：`AI_DEVELOPMENT.md`（主文档）+ `docs/`（按模块拆分的代码逻辑文件）。
> 更新时机：每个会话结束时更新本文件。

## 一、项目一句话

RikkaHub（rikkahub/rikkahub）的 fork `Rune-cn/rikkahub`，Android LLM 聊天客户端，Kotlin + Compose（Material3 Expressive）。包名 `app.ai.rune`，当前版本 **3.2.0**（versionCode 196）。

## 二、当前状态（截至 2026-11-15）

- **上游**：✅ **已同步到 3.2.0**（上游 `64452581`），merge 提交见 `eb2d8539`。121 个文件、16 个冲突全部解决。
- **构建**：✅ 正式版 **3.2.0（versionCode 196）已发布**（run #363 stable success）。
  - APK：https://github.com/Rune-cn/rikkahub/releases/download/3.2.0/app-github-arm64-v8a-release.apk
- **本轮改动（2026-11-15）**：同步上游 3.2.0。
  - 合并策略：**上游架构 + fork 功能**，不整段选边。
  - 子 Agent 上下文过滤移植进上游新的 `replyTargetOf()`（上游用 `history.lastIndex` 直接索引 `nodes`，但 fork 的过滤会让索引错位，必须回 `allHistory` 按 id 查）。
  - 生成中节流落库 + 内存心跳保留在上游新的 `while(true)` 生成循环里。
  - `compressConversation` 整体采用上游（改成 `launchCompressionJob` + `await`），fork 的同步实现作废。
  - 广告/推广提供商随合并一并未引入（AiHubMix、APIMart、302.AI、随想AI网关、MaruCode、STA1N API）。
  - 版本号：3.1.0/195 → **3.2.0/196**。

### 合并时踩的三个坑（下次同步会再遇到）

1. **`ChatList.kt` 的 import 别一股脑选上游**。上游把 460 行拆到了 `ChatListOverlays/Preview/Swipe` 和 `ChatSelection` 四个新文件，但 `ChatList.kt` 里**仍保留了 fork 的「继续生成」按钮**，它还要用 `OutlinedButton`/`Icon`/`HugeIcons.Play`/`stringResource`/`R`。全选上游侧 → 编译报 `Unresolved reference`。
2. **DI 参数要跟着构造函数走**。`LocalTools` 合并后是 8 个参数（新增上游 `phoneUseManager`/`filesManager`），`AppModule` 里的 `LocalTools(get(), ...)` 必须同步补齐，否则 `No value passed for parameter`。
3. **上游改了方法签名要看调用点**。`retryStatus()` 被上游改成 `retryStatus(attempt)`，定义侧不改就报 `Too many arguments`。

> 这三次错误都只靠 GitHub Actions 才暴露 —— **合并后必须跑 CI**，本地静态检查发现不了。

## 三、上一轮会话的结论与踩坑（务必先读）

### A. AiHubMix 移除的两个必知点
- **只删定义不够**：`SettingsNormalization.withBuiltInDefaults()` 每次读盘/改设置都会把 `DEFAULT_TTS_PROVIDERS` 里缺失的项补回来 → 必须「先 filterNot 再补默认」。
- **必须按固定 UUID 匹配**（`e36b22ef-ca82-40ab-9e70-60cad861911c`），不能按名字 `"AiHubMix"`，否则会误删用户自建的同类提供商。
- **不要动**：`SettingProviderPage.kt` 里 `provider.name == "AiHubMix"` 的「10% 优惠」标签、`AIIconMatcher.kt` 的 `PATTERN_AIHUBMIX`——那是**聊天提供商**的通用能力，与语音服务无关。

### B. 验证文件的正确姿势（重要踩坑）
**不要用 `raw.githubusercontent.com` 判断文件是否已更新**：它有 CDN 缓存，刚提交的文件可能仍返回旧内容（本次曾因此误判「改动丢失」）。
正确做法：用 GitHub API（`gh_get_file` / `gh_get_content_sha`），或按 commit SHA 拉取。

### C. Actions 暂停 vs API 提交（已纠正的错误结论）
**先说结论：API 提交是可以触发 `push` 构建的。**

2026-11-14 排查时一度以为「API 提交不触发 CI」，实际根因是 **GitHub 暂停了本 fork 的 Actions**：

> Workflows are not currently being run on this fork due to the scale of GitHub Actions usage.

- 暂停期间：push / schedule 全部不跑；`rerun` 返回成功但 `build` 会因 24 小时检查被 skip；**REST API 无法解除**，只能仓库维护者在 Actions 页面点启用。
- 恢复后：API 提交立刻正常触发 `push` 构建（实测提交 `382d59c` → run #343 → success）。

所以遇到"提交了却没构建"，先看 Actions 页面有没有暂停横幅，不要先怀疑提交方式。

## 四、已知待办 / 注意事项（下个会话请留意）

1. **QQ 群加群链接的 `authKey` 有时效**：`SettingPage.kt` 的 `QQ_GROUPS.link` 过期后需换新链接（用户提供）。
2. **子 Agent 展示消息隔离**：生成/压缩/标题/建议一律用 `Conversation.mainMessages` 或 `isSubAgentDisplayMessage()` 过滤，**不要再拿 `currentMessages` 直接发模型**（见 docs/04-3.5）。
3. **链接交互约定**：对话内链接必须走 `LinkActionSheet`，新增渲染入口时给 `MarkdownBlock` 透传 `onLinkClick`，不要自己调 `context.openUrl`（见 docs/07-7）；非对话内链接（设置/关于/捐赠页）保持原行为。
4. 「运行日志」记录一切 `RunLog.log(...)`：**关键操作与异常**（应用启动、搜索、崩溃、以及任何新埋点），不限于特定几类；排查类功能优先留运行日志。
5. 版本号规则：没用户要求不升版；versionCode 必须单调递增（现在 195）。
6. **上游同步（最高优先）**：fork 落后上游 173 提交 / 121 文件（merge base `dfbe9a08` → 上游 `64452581`，3.1.1 → 3.2.0）。**用 merge 不要 rebase**（fork 有 100+ 定制提交，rebase 会逐个重放、反复解同一批冲突）。
   - 冲突高发：`service/ChatService.kt`（+281/-105，含节流落库与子 Agent 上下文隔离）、`ui/pages/chat/ChatList.kt`（上游拆成 `ChatListOverlays/ChatListPreview/ChatListSwipe/ChatSelection` 四个新文件）、`ui/pages/chat/ChatPage.kt`（+144/-18）、`data/datastore/SettingsNormalization.kt`、`data/ai/ChatRequestFactory.kt`。
   - **同步后必查**：上游 HEAD 仍在 `DEFAULT_TTS_PROVIDERS` 里内置 AiHubMix，确认它没有被带回来（`grep -n "RETIRED_BUILT_IN_TTS_PROVIDER_IDS" SettingsNormalization.kt` 应仍有 filterNot）。
   - **本轮改动与上游重叠情况（已核实）**：`OtherSettingsPage.kt`、`skills/SkillDetailPage.kt`、`components/ui/CardGroup.kt` 上游都没动，**不会冲突**；`components/ai/ModelList.kt` 上游只改了 `ModelList` 里的收藏按钮逻辑（约 677 行），与 `ModelField`（269 行）无关，**不影响本轮改动**。
7. 构建产物路径：`assembleGithubRelease` → `app/build/outputs/apk/github/release/`；README 下载名 `app-github-arm64-v8a-release.apk`。

## 五、给下个会话的开工步骤

1. 读 `AI_DEVELOPMENT.md` 与本文档并列对照（本文档是快照，AI_DEVELOPMENT 是知识库）。
2. 按需求类型翻 `docs/` 对应篇（设置→03，生成/工具→04，构建/版本→05，UI→07，同步→06）。
3. 遵守铁律：**任何修改都必须同步更新 AI_DEVELOPMENT.md + 相关 docs/ 文件**；中文；不写密钥/PAT；UI 代码禁 emoji；图标用前 grep 验证。
4. 开发后：本地静态自检 → 提交推送 → 等 `Daily Build` 跑完（push 会自动触发；若没跑，先查 Actions 是否被暂停）→ 构建失败按 docs/06 排查套路修 → 更新本文档与知识库。
5. 交付时（每次都要）：回复里附 nightly 下载链接 —— https://github.com/Rune-cn/rikkahub/releases/download/nightly/app-github-arm64-v8a-release.apk

## 六、快速链接

- 仓库：https://github.com/Rune-cn/rikkahub
- 知识库：`AI_DEVELOPMENT.md` + `docs/`（01 架构 / 02 fork 定制 12 项 / 03 设置数据 / 04 生成重试 / 05 构建发布 / 06 上游同步 / 07 UI 约定 / 交接文档技能说明书.md）
- nightly APK：见上文
