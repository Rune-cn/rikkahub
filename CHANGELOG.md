# 更新日志 / Changelog

本分支（`Rune-cn/rikkahub`）所有版本记录。格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。

## [未发布]

### 🐛 修复

- **子 Agent 工具调用不显示**：主对话里的子 Agent 消息此前只写入最终文本，工具调用全部丢失；现在整条消息（含工具调用）流式写入，并在思考链里完整展开
- **子 Agent 续跑后消息不再更新**：send_message 复用同一 task id，已收尾集合未清理导致跳过更新
- **停止生成不取消子 Agent**：现在只取消该会话派生的子 Agent，不影响其他会话

### ✨ 优化

- 子 Agent 详情弹窗重做：状态胶囊 + 模型/步数/工具调用数/耗时 + 每次工具调用的参数摘要与结果预览
- 子 Agent 后台执行强制放行工具并剔除 `ask_user`（没人能替它回答，会永久卡住）；步数上限 48
- `wait_agent` / `list_agents` 返回进度（steps / tool_calls / last_tool / elapsed_seconds）与 `timed_out`；转录截断到最后 12 条、每条 1200 字
- **主对话展示完整的子 Agent 过程**：把任务里多条助手消息按顺序拼接（此前只显示最新一条，上一步的工具调用会消失）；标题实时显示「步数 · 工具调用数 · 耗时」
- 子 Agent system prompt 补充执行规则（不许请求确认、工具失败换方案、最终回答自包含）
- 已结束的子 Agent 只保留最近 12 个，列表与内存不再无限增长
- 新增 `SubAgentDisplayTest`：跨步累加 / 同一消息去重 / 未执行工具补结果 / 失败追加错误

---

## [3.3.0] - 2026-11-21（正式版）

更新内容：

- **同步上游 3.3.0**：合并 rikkahub 上游 `358e2931`（22 个提交，含快速入门页、助手回复按段落拆分气泡、DeepSeek 语音合成/识别、工作区共享存储挂载、消息概览标记上下文压缩点、安全模式清理超大设置等）
- **定制功能 UI 对齐 Material3 Expressive**：工作流列表与画布、运行日志、重试、工具调用、子 Agent 消息/进度条统一到上游新版规范（20dp 卡片圆角、`CardGroup` 分组、`ItemActionMenu`、`ListItem` 整行点击、`SecondaryTabRow`、emphasized 字体）
- **延续去除广告提供商**：内置提供商仍不含 AiHubMix、APIMart、302.AI、随想AI网关、MaruCode、STA1N API

### 合并说明

上游本次 22 个提交与 fork 定制有 5 处冲突，按「上游架构 + fork 功能」逐个处理：

- `DefaultProviders.kt` 以上游为基底，仅剔除 6 个推广商，保留上游新增的 `DEEPSEEK_PROVIDER_ID`
- `SettingsNormalization.kt` 采用上游「内置助手可删、清空才补回」，同时保留已下架 TTS 过滤
- `ChatMessage.kt` 采用上游按段落拆分气泡，fork 的链接操作面板透传到每个段落
- `SettingPage.kt` 搜索态优先整页结果，非搜索态用上游 QuickStart 提示卡

---

## [3.2.0] - 2026-11-15（正式版）

更新内容：

- **同步上游**：合并 rikkahub 上游 3.2.0（121 个文件，含 Phone Use、自动压缩上下文、ChatList 重构等）
- **优化 MCP OAuth / MCP 认证**：上游修复动态注册按候选回退 loopback 主机名、resource 采用 PRM 声明值（解决别名主机 invalid_target）
- **优化一些界面**：延续 3.1.0 的子 Agent 模型选择样式统一、技能详情页间距调整
- **去除广告提供商**：内置提供商列表不再包含推广/返利性质的服务商（AiHubMix、APIMart、302.AI、随想AI网关、MaruCode、STA1N API）

### 合并说明

上游本次改动涉及 ChatService、ChatList、ChatPage 等 fork 深度定制区域，共 16 个文件冲突，逐个按「上游架构 + fork 功能」合并：

- 子 Agent 上下文过滤移植进上游新的 `replyTargetOf()`
- 生成中节流落库与内存心跳保留
- 厂商内置列表剔除推广商，AiHubMix 移除逻辑保持

---

## [3.1.0] - 2026-11-14（正式版）

更新内容：

### ✨ 新变化

- **移除内置语音服务商 AiHubMix**：语音服务（TTS）不再预置 AiHubMix，默认只保留系统 TTS。已保存过该配置的用户，升级后该条目会被自动清理，无需手动删除
- **子 Agent 模型选择样式统一**：子 Agent 的默认模型选择改为标准表单项（整行选择器），与设置页其它「选择模型」项样式、对齐方式一致

### ⬆️ 功能升级

- **上游同步**：已合并上游 3.1.1

### 🐛 修复

- 修复子 Agent 默认模型选择项内边距与卡片内其它条目不一致的问题

### 📚 文档

- `docs/02` 新增「移除内置 AiHubMix」章节，记录移除方式与防复活机制
- `docs/03` 新增「归一化与内置项下架」规范
- `AI_DEVELOPMENT.md` / `NEXT_SESSION.md` 更新至 3.1.0

---

## [3.0.0] - 2026-10-31（正式版）

更新内容：

### ✨ 新变化

- **同步上游 3.x 全量架构**：全新 Agent 事件流生成引擎、设置存储重构（SettingsFields）、github/play 双构建渠道、Material 3 Expressive 界面
- **运行日志页**：App 启动 / 搜索 / 崩溃自动记录，设置 → 关于 → 运行日志
- **对话内链接操作面板**：正文/引用/搜索结果里的链接点击后可选「浏览器打开 / 内嵌打开 / 分享 / 复制链接」

### ⬆️ 功能升级

- **可配置重试升级**：固定 / 指数退避 / 随机抖动三种间隔策略真正运行时生效；重试设置独立成「重试」页，新增总开关与「一键恢复默认」；状态只显示「重试中…」
- **更新检查升级**：改为正式版检测（版本号比较）；GitHub API + 302 双通道（限流自动降级）；关于页「检查更新」手动入口
- **子 Agent 完善**：消息视觉区分、统一更名「子 Agent」
- **设置搜索打磨**：顶栏直搜、整页搜索模式

### 🎨 界面与文案

- 扩展管理重组：快捷消息 / 提示词 / 技能 / 工作区 / 工具调用 / AI 工作流 / 重试 / 子 Agent
- 繁中（zh-rTW）补齐重试翻译；QQ 群入口；捐赠新增「爱发电 · Rune」

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
