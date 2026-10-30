# AI 开发交接文档（AI Development Handoff）

> 本仓库是 [rikkahub/rikkahub](https://github.com/rikkahub/rikkahub) 的二次开发 fork（`Rune-cn/rikkahub`），
> 本文档是给**后续 AI 开发会话**看的开发指南与交接说明。
>
> **铁律：任何对项目的修改（代码 / 界面 / 行为 / 构建 / 发布），都必须同步修改本文档与 `ai-development/` 下对应的逻辑文件**，保证"代码逻辑说明"永远与代码一致。

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
- **上游同步**：已 rebase 上游 3.0.0/3.1.0 全量（128 提交）并对齐后续 2 个小提交；基线 `eb530b0f`。fork 在上游之上共 100+ 提交。
- **构建状态**：✅ nightly 每天自动构建成功（GitHub Actions `daily-build.yml`），产物发布到固定 tag `nightly`。
- **定制功能**：10 项全部保留并在 3.x 架构上重新适配（详见 [02-fork定制功能.md](ai-development/02-fork定制功能.md)）。
- **已知待办/注意事项**：
  - 设置页 QQ 群使用 universal-share 加群链接，**`authKey` 有时效**，过期需换新链接（`SettingPage.kt` 的 `QQ_GROUPS.link`）。
  - 可配置重试的"间隔策略"（固定/抖动）在运行时仍走上游内建指数退避，仅 UI/设置保留（见 04）。
  - 设置搜索交互经历过多次修正，最终形态见 [07-UI交互与代码约定.md](ai-development/07-UI交互与代码约定.md) 的"设置搜索"一节。
  - 构建产物因上游 3.x 引入 github/play flavor，APK 命名与路径已变（见 05）。

## 三、代码逻辑索引（ai-development/ 目录，按模块单独成文件）

| 文件 | 内容 |
|---|---|
| [01-架构总览.md](ai-development/01-架构总览.md) | 模块划分、上游 3.x 关键重构（GenerationLoop 删除 / PreferencesStore 拆分 / flavors） |
| [02-fork定制功能.md](ai-development/02-fork定制功能.md) | fork 的 10 项定制：文件位置 + 逻辑要点（多 Agent / 工作流 / 自动允许 / 重试 / 设置搜索 / 草稿 / 节流落库 / Skill 分类 / 更新检查 / 子 Agent 视觉） |
| [03-设置与数据.md](ai-development/03-设置与数据.md) | 设置系统：Settings.kt + SETTINGS_FIELDS 注册机制、新增字段规范 |
| [04-生成循环与重试.md](ai-development/04-生成循环与重试.md) | 新生成架构（ChatRequestFactory / AgentLoop / RetryPolicy）、子 Agent 与工作流的适配写法 |
| [05-构建与发布.md](ai-development/05-构建与发布.md) | flavor 结构、版本号规则、Actions 发布流程、更新检查 |
| [06-上游同步.md](ai-development/06-上游同步.md) | fork 同步上游的标准流程、冲突高发区、3.x 迁移坑 |
| [07-UI交互与代码约定.md](ai-development/07-UI交互与代码约定.md) | 设置搜索交互、图标使用规则、组件约定、emoji 禁用 |

## 四、给后续 AI 的开发清单（每次动工前先读）

1. 要改功能 → 先读 `ai-development/02` 对应小节 + 相关源码，确认"这功能当初为什么这么做"。
2. 要动设置字段 → 先读 `03`（必须注册 SETTINGS_FIELDS，否则读不到写不进）。
3. 要动生成/重试/工具 → 先读 `04`（旧 GenerationLoop API 已不存在）。
4. 要改 UI/图标 → 先读 `07`（图标要先 grep 验证存在；LazyListScope 里禁 remember；UI 代码禁 emoji）。
5. 要发版/改版本号 → 先读 `05`（版本号规则、workflow 产物路径）。
6. 要同步上游 → 先读 `06`（rebase 流程与踩坑）。
7. **改完任何东西 → 同步更新本文档与对应逻辑文件**，保持文档与代码一致。

## 五、禁止事项

- 文档与仓库里**不要写**内部密钥/PAT/签名配置（本仓库有 `.gitignore` 与外部 secrets，文档只写"secrets 名"）。
- UI 代码中**禁用 emoji**（README/发布说明可保留上游原有风格）。
- 未经用户要求，不要随意升版本号。