<div align="center">
  <img src="docs/icon.png" alt="App 圖標" width="100" />
  <h1>RikkaHub</h1>

一個原生 Android LLM 聊天客戶端，支持切換不同的供應商進行聊天 🤖💬

基於 [rikkahub/rikkahub](https://github.com/rikkahub/rikkahub) 的二次開發版本，包名為 `app.ai.rune`

[English](README.md) | [简体中文](README_ZH_CN.md) | **繁體中文**

</div>

<div align="center">
  <img src="docs/img/chat.png" alt="Chat Interface" width="150" />
  <img src="docs/img/desktop.png" alt="Models Picker" width="450" />
</div>

## 📦 發布版本

自動構建的 APK 發布在 [Nightly Build](https://github.com/Rune-cn/rikkahub/releases/tag/nightly)：

| APK | 說明 |
|---|---|
| `app-arm64-v8a-release.apk` | 手機（arm64 設備） |

> 每次推送到 `master` 或每天定時（UTC 09:00 / 18:00），GitHub Actions 會自動重新構建並更新 APK。
> 預發布版本號格式：`[應用版本]-[構建 commit]`，如 `2.5.6-a3ea1cd1`。

## ✨ 功能特色

- [x] 🎨 現代化安卓APP設計（Material You / 預測性返回）和 🌙 暗色模式
- [x] 📦 工作區：基於 proot 的 Linux 智能體環境
- [x] 🖥️ Web 多端訪問支持
- [x] 🛠️ MCP 支持
- [x] 🔄 多種類型的供應商支持，自定義 API / URL / 模型（目前支持 OpenAI、Google、Anthropic）
- [x] 🖼️ 多模態輸入支持
- [x] 📝 Markdown 渲染（支持代碼高亮、數學公式、表格、Mermaid）
- [x] 🪾 消息分支
- [x] 🔍 搜索功能（Exa、Tavily、Zhipu、LinkUp、Brave、Perplexity、..）
- [x] 🧩 Prompt 變量（模型名稱、時間等）
- [x] 🤳 二維碼導出和導入提供商
- [x] 🤖 Agent 定制
- [x] 🧠 類 ChatGPT 記憶功能
- [x] 📝 AI 翻譯
- [x] 🌐 自定義 HTTP 請求頭和請求體
- [x] 💌 Silly Tavern 角色卡導入

### 本分支新增

- [x] 📱 包名改為 `app.ai.rune`（應用顯示名不變）
- [x] 🔓 工具審批「全部允許」：AI 使用所有工具（含 MCP）不再彈確認框
- [x] 🔁 限流自動重試（tpm/rpm）：支持固定間隔 / 指數退避 / 隨機抖動，可配置最大重試次數（擴展管理 → 其他）
- [x] ▶️ 繼續生成：AI 被停止或中斷後，從半截回覆接著生成（歷史中段對話也能繼續）
- [x] 🔥 Firebase 可選：沒有 `google-services.json` 也能構建運行
- [x] 🤖 GitHub Actions 自動構建並簽名 APK（Nightly Release）

## 💻 開發

本項目使用 [Android Studio](https://developer.android.com/studio) 開發。

技術棧：

- [Kotlin](https://kotlinlang.org/)（開發語言）
- [Koin](https://insert-koin.io/)（依賴注入）
- [Jetpack Compose](https://developer.android.com/jetpack/compose)（UI 框架）
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)（偏好數據
  存儲）
- [Room](https://developer.android.com/training/data-storage/room)（數據庫）
- [Coil](https://coil-kt.github.io/coil/)（圖片加載）
- [Material You](https://m3.material.io/)（界面設計）
- [Navigation 3](https://developer.android.com/guide/navigation/navigation-3)（導航）
- [Okhttp](https://square.github.io/okhttp/)（HTTP 客戶端）
- [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)（JSON 序列化）

> [!NOTE]
> 本倉庫為 [rikkahub/rikkahub](https://github.com/rikkahub/rikkahub) 的 fork，
> 同步上游更新請參考 [FORK_SYNC.md](FORK_SYNC.md)。

## 📄 許可證

採用 [GNU Affero General Public License v3.0](LICENSE)（AGPL-3.0）許可。
本倉庫為 RikkaHub 的二次開發版本，原始版權歸 RikkaHub 作者所有。