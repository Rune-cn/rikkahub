# 更新日志 / Changelog

本分支（`Rune-cn/rikkahub`）所有版本记录。格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。

## [2.5.6-1] - 2026-10-07（首个发布版）

### ✨ 新增功能

- **工具审批「全部允许」**：工作区开启后，AI 使用所有工具（含 MCP、日历、技能等）不再弹确认框，直接执行。`ask_user`（询问用户）除外——它必须保留"询问用户"的交互流程
- **限流自动重试（tpm/rpm）**：模型触发 tpm/rpm/429 限流时自动重试，支持三种重试间隔策略（固定间隔 / 指数退避 2s→4s→8s / 随机抖动），可配置最大重试次数（3/5）
- **「继续生成」**：AI 生成被停止或中断后，在最后一条 AI 消息下方显示「继续生成」按钮，点击后从半截回复继续生成；历史中段对话（重进会话）也能继续
- **Firebase 可选化**：没有 `google-services.json` 也能正常构建和运行（Analytics 自动降级为 null）
- **更新检查指向本仓库**：应用内检查 `Rune-cn/rikkahub` 的 Nightly Release（对比 Release 发布时间与本地安装时间），不再依赖上游 `updates.rikka-ai.com`

### 🎨 界面改动

- **扩展管理页**：新增「其他」入口（⚙️，与快捷消息/提示词/技能/工作区并列），点击进入自动重试设置页
- **工作区详情页**：工具审批下方新增「其他」分类，含「全部允许」开关
- **关于页**：移除官网入口；GitHub / 许可证链接指向本仓库
- **设置页**：帮助文档、分享文案改为本仓库链接；更新检查禁用（不再依赖上游服务）

### 📦 其他

- 包名改为 `app.ai.rune`（应用显示名不变）
- **只构建 arm64-v8a** APK（手机平台，减小构建产物）
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