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

## 二、当前开发情况（截至 2026-10-31）

- **版本**：`3.0.0`（versionCode `193`）。nightly 发布命名 `3.0.0-<commit>`，正式版（stable）为纯 `3.0.0`。
- **上游同步**：已 rebase 上游 3.0.0/3.1.0 全量（128 提交）并对齐后续 2 个小提交；基线 merge `39ff6c43`（合并 `eb530b0f`，HEAD 与 upstream/master 完全一致）。fork 在上游之上共 100+ 提交。
- **构建状态**：✅ nightly 每天自动构建成功（GitHub Actions `daily-build.yml`），产物发布到固定 tag `nightly`。最近一次手动验证 `1eef58f9`（链接面板 + 子 Agent 修复 + 回复节点定位修复）构建成功。
- **定制功能**：11 项全部保留并在 3.x 架构上重新适配（详见 [02-fork定制功能.md](docs/02-fork定制功能.md)）。
- **本轮新增（2026-10-31 晚）**：
  - 对话内链接点击弹操作面板（浏览器打开 / 内嵌打开 / 分享），见 02-11 与 07-7。
  - 修复子 Agent 消息上下文污染与回复写错节点（`Conversation.mainMessages` 过滤展示消息），见 02-2 与 04-3.5。
  - **修复回复节点定位**：`runTurn` 中只有「过滤子 Agent 展示消息后的最后一条」是主 Agent 助手消息才复用其节点；用户再发新消息（末尾是 USER）必须新建节点，否则新回复会写进上一轮回复节点里（表现为"每次对话都在旧回复基础上继续改"），见 04-3.5。
  - **重试间隔策略运行时生效**：设置里的「固定 / 指数退避 / 随机抖动」不再只是摆设——`ai/.../agent/Retry.kt` 新增 `RetryBackoff` 枚举（FIXED / EXPONENTIAL_BACKOFF / JITTER），`ChatRequestFactory.retryPolicy()` 把 `NetworkSetting.rateLimitRetryMode` 映射进 `RetryPolicy.backoff`，三种策略都在运行时生效（JITTER 为 Full Jitter），并补了单元测试（见 02-4 与 04-2）。
  - **重试设置独立成页**：扩展管理新增「重试」入口（位于"其他"上方，`Screen.RetrySettings` → `RetrySettingsPage.kt`），原「其他」页里的重试设置全部搬入；**移除「自动重试」总开关**（`enableRateLimitRetry` 默认 true，规则始终生效）；重试状态只显示"重试中…"（不再显示"网络连接已断开"等具体原因）。相关字符串三语同步（含补齐 zh-rTW 的重试相关翻译），见 02-4 与 04-2。
- **会话交接**：每次会话结束时更新根目录 `NEXT_SESSION.md`（一次性交接单，可直接发给下个会话；本文件是知识库）。
- **已知待办/注意事项**：
  - 设置页 QQ 群使用 universal-share 加群链接，**`authKey` 有时效**，过期需换新链接（`SettingPage.kt` 的 `QQ_GROUPS.link`）。
  - 设置搜索交互经历过多次修正，最终形态见 [07-UI交互与代码约定.md](docs/07-UI交互与代码约定.md) 的"设置搜索"一节。
  - 构建产物因上游 3.x 引入 github/play flavor，APK 命名与路径已变（见 05）。
  - 子 Agent 展示消息被多个消费方读取（生成/压缩/标题/UI），新增消费方一律用 `mainMessages` / `isSubAgentDisplayMessage()`，不要直接遍历 `currentMessages` 发模型（见 04-3.5）。

## 三、代码逻辑索引（docs/ 目录，按模块单独成文件）

| 文件 | 内容 |
|---|---|
| [01-架构总览.md](docs/01-架构总览.md) | 模块划分、上游 3.x 关键重构（GenerationLoop 删除 / PreferencesStore 拆分 / flavors） |
| [02-fork定制功能.md](docs/02-fork定制功能.md) | fork 的 11 项定制：文件位置 + 逻辑要点（多 Agent / 工作流 / 自动允许 / 重试 / 设置搜索 / 草稿 / 节流落库 / Skill 分类 / 更新检查 / 子 Agent 视觉 / **链接操作面板**；含子 Agent 上下文隔离） |
| [03-设置与数据.md](docs/03-设置与数据.md) | 设置系统：Settings.kt + SETTINGS_FIELDS 注册机制、新增字段规范 |
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
6. 要同步上游 → 先读 `06`（rebase 流程与踩坑）。
7. **改完任何东西 → 同步更新本文档与对应逻辑文件**，保持文档与代码一致。
8. **交付约定（用户要求）**：每次开发/修复完成后，回复里必须附上最新 APK 下载链接：`https://github.com/Rune-cn/rikkahub/releases/download/nightly/app-github-arm64-v8a-release.apk`（nightly 由 Actions 自动更新，次日/构建完成后即最新）。

## 五、禁止事项

- 文档与仓库里**不要写**内部密钥/PAT/签名配置（本仓库有 `.gitignore` 与外部 secrets，文档只写"secrets 名"）。
- UI 代码中**禁用 emoji**（README/发布说明可保留上游原有风格）。
- 未经用户要求，不要随意升版本号。