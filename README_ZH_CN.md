<div align="center">
  <img src="docs/icon.png" alt="App Icon" width="100" />
  <h1>RikkaHub</h1>

原生 Android LLM 聊天客户端，支持在不同提供商之间切换对话 🤖💬

基于 [rikkahub/rikkahub](https://github.com/rikkahub/rikkahub) 的二次开发版本，包名为 `app.ai.rune`

[English](README.md) | **简体中文** | [繁體中文](README_ZH_TW.md)
</div>

<div align="center">
  <img src="docs/img/chat.png" alt="Chat Interface" width="150" />
  <img src="docs/img/desktop.png" alt="Models Picker" width="450" />
</div>

## 📦 发布版本

自动构建的 APK 发布在 [Nightly Build](https://github.com/Rune-cn/rikkahub/releases/tag/nightly)：

| APK | 说明 |
|---|---|
| `app-arm64-v8a-release.apk` | 手机（arm64 设备） |

> 每次推送到 `master` 或每天定时（UTC 09:00 / 18:00），GitHub Actions 会自动重新构建并更新 APK。
> 预发布版本号格式：`[应用版本]-[构建 commit]`，如 `2.5.6-a3ea1cd1`。

## ✨ 功能特色

- [x] 🎨 现代化安卓APP设计（Material You / 预测性返回）和 🌙 暗色模式
- [x] 📦 工作区：基于 proot 的 Linux 智能体环境
- [x] 🖥️ Web多端访问支持
- [x] 🛠️ MCP 支持
- [x] 🔄 多种类型的供应商支持，自定义 API / URL / 模型（目前支持 OpenAI、Google、Anthropic）
- [x] 🖼️ 多模态输入支持
- [x] 📝 Markdown 渲染（支持代码高亮、数学公式、表格、Mermaid）
- [x] 🪾 消息分支
- [x] 🔍 搜索功能（Exa、Tavily、Zhipu、LinkUp、Brave、Perplexity、..）
- [x] 🧩 Prompt 变量（模型名称、时间等）
- [x] 🤳 二维码导出和导入提供商
- [x] 🤖 Agent 定制
- [x] 🧠 类 ChatGPT 记忆功能
- [x] 📝 AI 翻译
- [x] 🌐 自定义 HTTP 请求头和请求体
- [x] 💌 Silly Tavern 角色卡导入

### 本分支新增

- [x] 📱 包名改为 `app.ai.rune`（应用显示名不变）
- [x] 🔓 工具审批「全部允许」：AI 使用所有工具（含 MCP）不再弹确认框
- [x] 🔁 限流自动重试（tpm/rpm）：支持固定间隔 / 指数退避 / 随机抖动，可配置最大重试次数（扩展管理 → 其他）
- [x] ▶️ 继续生成：AI 被停止或中断后，从半截回复接着生成（历史中段对话也能继续）
- [x] 🔥 Firebase 可选：没有 `google-services.json` 也能构建运行
- [x] 🤖 GitHub Actions 自动构建并签名 APK（Nightly Release）

## 💻 开发

本项目使用 [Android Studio](https://developer.android.com/studio) 开发。

技术栈：

- [Kotlin](https://kotlinlang.org/)（开发语言）
- [Koin](https://insert-koin.io/)（依赖注入）
- [Jetpack Compose](https://developer.android.com/jetpack/compose)（UI 框架）
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)（偏好数据
  存储）
- [Room](https://developer.android.com/training/data-storage/room)（数据库）
- [Coil](https://coil-kt.github.io/coil/)（图片加载）
- [Material You](https://m3.material.io/)（界面设计）
- [Navigation 3](https://developer.android.com/guide/navigation/navigation-3)（导航）
- [Okhttp](https://square.github.io/okhttp/)（HTTP 客户端）
- [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)（JSON 序列化）

> [!NOTE]
> 本仓库为 [rikkahub/rikkahub](https://github.com/rikkahub/rikkahub) 的 fork，
> 同步上游更新请参考 [FORK_SYNC.md](FORK_SYNC.md)。

## 📄 许可证

采用 [GNU Affero General Public License v3.0](LICENSE)（AGPL-3.0）许可。
本仓库为 RikkaHub 的二次开发版本，原始版权归 RikkaHub 作者所有。