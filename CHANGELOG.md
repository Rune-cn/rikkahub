# 更新日志 / Changelog

本分支（`Rune-cn/rikkahub`）所有版本记录。格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。

## [Unreleased]

### ✨ 新增功能

- **重试间隔策略运行时生效**：「扩展管理 → 其他 → 重试」里的三种间隔策略（固定间隔 / 指数退避 / 随机抖动）此前只有指数退避在运行时生效，现在全部生效：
  - `ai` 模块新增 `RetryBackoff` 枚举（FIXED / EXPONENTIAL_BACKOFF / JITTER），`RetryPolicy` 增加 `backoff` 字段（默认指数退避，行为与之前一致）；
  - `ChatRequestFactory.retryPolicy()` 将 `NetworkSetting.rateLimitRetryMode` 映射进 `RetryPolicy.backoff`；
  - JITTER 采用 Full Jitter（在 `[1ms, 指数退避值]` 内随机），避免限流时大批请求同时重试造成"重试风暴"；
  - 新增单元测试覆盖固定间隔与抖动两种策略（`ai/src/test/.../RetryTest.kt`）。

### 🎨 界面改动

- **重试设置独立成页**：扩展管理新增「重试」入口（位于「其他」上方），重试设置（总开关 / 间隔 / 次数 / 状态码 / 关键字）全部集中在新「重试」页。
- **重试总开关**：「重试」页顶部新增「自动重试」开关；上游「设置 → 网络」页的「自动重试」开关移除（同一设置项 `enableAutoRetry`，位置从网络页挪到重试页）。
- **重试状态文案简化**：重试时只显示「重试中…」，不再显示「网络连接已断开」等具体错误原因。
- **「多 Agent」改名为「子 Agent」**：子 Agent 开关及各处 UI 文案统一为「子 Agent」。
- 设置搜索：「重试」相关搜索结果直达新的重试页。
- 补齐繁体中文（zh-rTW）的重试相关翻译。

---

## [2.5.6-1] - 2026-10-07（首个发布版）

### ✨ 新增功能

- **工具审批「全部允许」**：工作区开启后，AI 使用所有工具（含 MCP、日历、技能等）不再弹确认框，直接执行。`ask_user`（询问用户）除外——它必须保留"询问用户"的交互流程
- **限流自动重试（tpm/rpm）**：模型触发 tpm/rpm/429 限流时自动重试，支持三种重试间隔策略（固定间隔 / 指数退避 2s→4s→8s / 随机抖动），可配置最大重试次数（3/5）
- **可配置重试规则**：自动重试的触发条件改为可配置——「可重试状态码」（默认 429/500/502/503/504）、「重试关键字」（默认 rate limit / tpm / rpm / too many requests / limit exceeded）、「停止重试关键字」（默认 insufficient / quota exceeded / invalid api key / unauthorized，**优先级最高，命中立即失败**）。入口：扩展管理 → 其他 → 重试
- **「继续生成」**：AI 生成被停止或中断后，在最后一条 AI 消息下方显示「继续生成」按钮，点击后从半截回复继续生成；历史中段对话（重进会话）也能继续
- **Firebase 可选化**：没有 `google-services.json` 也能正常构建和运行（Analytics 自动降级为 null）
- **更新检查指向本仓库**：应用内检查 `Rune-cn/rikkahub` 的 Nightly Release（对比 Release 发布时间与本地安装时间），不再依赖上游 `updates.rikka-ai.com`
- **任务计划**：内置工具 `update_plan`，AI 可维护分步待办清单（`pending` / `in_progress` / `completed`）。对话正文显示可折叠的待办卡片，聊天输入框上方显示实时进度（如「任务 3/5」）。在「助手设置 → 本地工具」中启用

### 🎨 界面改动

- **扩展管理页**：新增「其他」入口（⚙️，与快捷消息/提示词/技能/工作区并列），点击进入自动重试设置页
- **工作区详情页**：工具审批下方新增「其他」分类，含「全部允许」开关
- **关于页**：移除官网入口；GitHub / 许可证链接指向本仓库
- **设置页**：帮助文档、分享文案改为本仓库链接；更新检查禁用（不再依赖上游服务）

### 📦 其他

- 包名改为 `app.ai.rune`（应用显示名不变）
- GitHub Actions 自动构建 + 签名 APK（Nightly Release）
- README 三语更新（功能清单打勾、发布版本）
- 新增 `FORK_SYNC.md`：fork 基线与上游同步方法

---

## [2.5.6] - 2026-10-06

基于上游 `rikkahub/rikkahub` `4a7a39c4` 的 fork，上游功能全部保留：

- 🎨 Material You 设计与暗色模式
- 📦 proot 工作区环境
- 🔄 多 AI 提供商支持（OpenAI / Google / Anthropic 兼容）
- 🖼️ 多模态输入、Web 访问、MCP 支持
- 📝 Markdown 渲染、消息分支、搜索、Prompt 变量
- 🤳 提供商二维码导入导出、Agent 定制、类 ChatGPT 记忆
- 📝 AI 翻译、自定义 HTTP 头、Silly Tavern 角色卡导入