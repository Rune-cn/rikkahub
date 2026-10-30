# 交接给下个会话（Next Session Handoff）

> 本文件是**一次性交接单**：把当前情况直接发给下一个 AI 会话即可开工，不用先读别的文档。
> 完整知识库在：`AI_DEVELOPMENT.md`（主文档）+ `docs/`（按模块拆分的代码逻辑文件）。
> 更新时机：每个会话结束时更新本文件。

## 一、项目一句话

RikkaHub（rikkahub/rikkahub）的 fork `Rune-cn/rikkahub`，Android LLM 聊天客户端，Kotlin + Compose（Material3 Expressive）。包名 `app.ai.rune`，当前版本 **3.0.0**（versionCode 193）。

## 二、当前状态（截至 2026-10-31）

- **上游**：已同步至 `eb530b0f`（3.0.0/3.1.0 全量 128 提交已 rebase + 后续 2 个 cherry-pick）。fork 在上游之上有 100+ 提交。
- **构建**：✅ nightly 自动构建成功（GitHub Actions），APK 发布到 tag `nightly`：
  https://github.com/Rune-cn/rikkahub/releases/download/nightly/app-github-arm64-v8a-release.apk
- **定制功能**（10 项，均在 3.x 架构上适配完成）：多 Agent、AI 工作流+无限画布、工具自动允许、可配置重试、设置搜索、输入框草稿、生成中节流落库、Skill 手动分类、更新检查指向本 fork、子 Agent 消息视觉区分。另：QQ 群入口（1131916400，universal-share 链接）、捐赠页新增「爱发电·Rune」(afdian.com/a/CNRune)、独立「运行日志」页。

## 三、上一次会话做了什么（本文件创建时）

1. 同步上游 128 提交大 rebase + 冲突解决 + 构建修复至通过。
2. 版本号升 3.0.0(193)；设置页 QQ 群替换 + 移除 Discord；捐赠页挂自己的爱发电；README 下载名适配 github flavor。
3. 设置搜索大改：顶栏「设置」标题点击展开/收起（动画），搜索时**整页只显示结果**（修复"搜不出"问题），键盘「搜索」键只收键盘不收起，收起走右侧 ↓。
4. 新增「运行日志」页（`utils/RunLog.kt` + `ui/pages/log/RunLogPage.kt`）：自动记录关键操作与异常（含崩溃堆栈），支持刷新/清空/复制全部/点行复制。
5. 建立交接文档体系：`AI_DEVELOPMENT.md` + `docs/`（01-07 逻辑文件）+ 平台 skill `handoff-docs` + 平台工作流「更新交接文档」「功能开发（先读交接文档）」。
6. 目录改名 `ai-development/` → `docs/`。

## 四、已知待办 / 注意事项（下个会话请留意）

1. **QQ 群加群链接的 `authKey` 有时效**：`SettingPage.kt` 的 `QQ_GROUPS.link` 过期后需换新链接（用户提供）。
2. **重试间隔策略**（固定/抖动）只在设置里保留，运行时走上游指数退避；要完全支持需改 `ai/.../agent/Retry.kt`。
3. **平台 AI 工作流「功能开发」在平台子会话环境中可能读不到 `/workspace` 文件**（用户反馈：运行后"什么也没有"）——第一步已改成内嵌现状摘要 + 读不到文件也能继续；若仍失败，改用主会话直接派子代理（spawn_agent）完成开发，或把需求连同本文档一起发给子会话。
4. 「运行日志」记录一切 `RunLog.log(...)`：**关键操作与异常**（应用启动、搜索、崩溃、以及任何新埋点），不限于特定几类；排查类功能优先留运行日志。
5. 版本号规则：没用户要求不升版；versionCode 必须单调递增（现在 193）。
6. 构建产物路径：`assembleGithubRelease` → `app/build/outputs/apk/github/release/`；README 下载名 `app-github-arm64-v8a-release.apk`。

## 五、给下个会话的开工步骤

1. 读 `AI_DEVELOPMENT.md` 与本文档并列对照（本文档是快照，AI_DEVELOPMENT 是知识库）。
2. 按需求类型翻 `docs/` 对应篇（设置→03，生成/工具→04，构建/版本→05，UI→07，同步→06）。
3. 遵守铁律：**任何修改都必须同步更新 AI_DEVELOPMENT.md + 相关 docs/ 文件**；中文；不写密钥/PAT；UI 代码禁 emoji；图标用前 grep 验证。
4. 开发后：本地静态自检 → 提交推送（触发 CI）→ 构建失败按 docs/06 排查套路修 → 更新本文档与知识库。

## 六、快速链接

- 仓库：https://github.com/Rune-cn/rikkahub
- 知识库：`AI_DEVELOPMENT.md` + `docs/`（01 架构 / 02 fork 定制 / 03 设置数据 / 04 生成重试 / 05 构建发布 / 06 上游同步 / 07 UI 约定 / 交接文档技能说明书.md）
- nightly APK：见上文