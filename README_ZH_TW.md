<div align="center">
  <img src="docs/icon.png" alt="App 圖標" width="100" />
  <h1>RikkaHub</h1>

一個原生 Android LLM 聊天客戶端，支持切換不同的供應商進行聊天 🤖💬

基於 [rikkahub/rikkahub](https://github.com/rikkahub/rikkahub) 的二次開發版本，包名為 `app.ai.rune`

[English](README.md) | 繁體中文 | [简体中文](README_ZH_CN.md)

</div>

<div align="center">
  <img src="docs/img/poster.png" alt="RikkaHub" width="800" />
</div>

## 📦 發布版本

<<<<<<< HEAD
🔗 [前往官網下載](https://rikka-ai.com/download)（推薦）

🔗 [前往 Google Play 下載](https://play.google.com/store/apps/details?id=me.rerere.rikkahub)
=======
自動構建的 APK 發布在 [Nightly Build](https://github.com/Rune-cn/rikkahub/releases/tag/nightly)：
>>>>>>> 018ab9e4 (feat: 二次开发收尾——其他分类移到工作区、README/关于页/许可证定制、上游同步文档)

| APK | 說明 |
|---|---|
| `app-arm64-v8a-release.apk` | 手機（arm64 設備） |
| `app-universal-release.apk` | 通用包 |
| `app-x86_64-release.apk` | 模擬器 |

<<<<<<< HEAD
## 💖 贊助商

| 贊助商 | 介紹 |
|:---:|:---|
| <img src="docs/sponsors/aihubmix.png" alt="Aihubmix" width="50" /><br /><b>Aihubmix</b> | 感謝 <a href="https://aihubmix.com?aff=pG7r">aihubmix.com</a> 的資金支持。我們推薦使用 aihubmix 作為全球主流模型的一站式服務平台。（OpenAI、Claude、Google Gemini、DeepSeek、Qwen 以及數百種其他模型）。 |
| <img src="docs/sponsors/api-mart.png" alt="APIMart" width="50" /><br /><b><a href="https://go.apimart.ai/gh-rikkahub">APIMart</a></b> | 感謝 APIMart 贊助了本專案！APIMart 是專注 AI 圖片/影片生成的低價 API 平台，GPT-Image-2 低至 $0.006/張，1 美元可出圖 160+ 張。圖片、影片一套非同步 API 通吃，提交任務拿 ID、回呼取結果，批次生成萬張不逾時、換模型不改程式碼。按量付費、無月費，透過<a href="https://go.apimart.ai/gh-rikkahub">此註冊連結</a>註冊即可開始使用。 |
| <img src="docs/sponsors/suixiang.jpg" alt="隨想AI網關" width="50" /><br /><b>隨想AI網關</b> | 感謝隨想AI網關對本項目的贊助！隨想AI網關 是一家可靠高效的 API 中繼服務提供商，提供 Claude、Codex、Gemini 等的中繼服務。注重隱私的中轉站·無數據倒賣·無模型摻水，隱私，透明，極速售後。新帳戶註冊每日簽到就送 0.5 元測試額度，儲值額度 1:1，無需訂閱，按量付費。多線路冗餘、跨區域容災、自動故障切換，長鏈路 SSE 不中斷。99.9% 可用性，關鍵呼叫從不掉隊。 |
| <img src="docs/sponsors/maru.png" alt="MaruCode" width="50" /><br /><b><a href="https://api.muteki.site/register?aff=Rikkahub&promo=Rikkahub">MaruCode</a></b> | <b><a href="https://api.muteki.site/register?aff=Rikkahub&promo=Rikkahub">MaruCode</a></b> 是一家偶爾做做慈善的小破站 API，自營號池，主要提供 Codex、Claude Code、GPT Image 等主流模型，支援 Websocket 協定，明碼標價(Codex 0.25x, CC 1.5x)，透明匯率(1:1)，<a href="https://api.muteki.site/register?aff=Rikkahub&promo=Rikkahub">新用戶註冊送 2 刀</a>。<a href="https://images-2.muteki.site">生图工作台🖼️</a> |
| <img src="docs/sponsors/naapi.png" alt="鈉API" width="50" /><br /><b><a href="https://naapi.cc/register?aff=rikkahub">鈉API</a></b> | 感謝<a href="https://naapi.cc/register?aff=rikkahub">鈉API</a>對本項目的贊助！鈉API（naapi.cc）是 OpenAI 相容的 LLM 閘道，以優惠價格提供 OpenAI、Anthropic、Google 等 300+ 模型存取。一個 base URL 加一個 key 即可接入；多線路冗餘、自動故障切換，長鏈路 SSE 不中斷；按量付費、儲值額度 1:1，無訂閱、無隱藏加價。 |

## ✨ 功能特色

- 🎨 現代化安卓APP設計（Material 3 Expressive / 預測性返回）和 🌙 暗色模式
- 📦 工作區：基於 proot 的 Linux 智能體環境
- 🎬 媒體生成（圖片、影片）
- 🖥️ Web多端訪問支持
- 🛠️ MCP 與 Skills 支持
- 🔄 多種類型的供應商支持，自定義 API / URL / 模型（目前支持 OpenAI、Google、Anthropic）
- 🖼️ 多模態輸入支持
- 📝 Markdown 渲染（支持代碼高亮、數學公式、表格、Mermaid）
- 🔍 搜尋功能（Exa、Tavily、Zhipu、LinkUp、Brave、Perplexity、..）
- 🧩 Prompt 變量（模型名稱、時間等）
- 🤳 二維碼導出和導入提供商
- 🤖 智能體自定義
- 🧠 基於檔案的記憶
- 📝 AI翻譯
- 🌐 自定義HTTP請求頭和請求體
=======
> 每次推送到 `master` 或每天定時（UTC 09:00 / 18:00），GitHub Actions 會自動重新構建並更新以上 APK。

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
>>>>>>> 018ab9e4 (feat: 二次开发收尾——其他分类移到工作区、README/关于页/许可证定制、上游同步文档)

### 本分支新增

- [x] 📱 包名改為 `app.ai.rune`（應用顯示名不變）
- [x] 🔓 工具審批「全部允許」：AI 使用所有工具（含 MCP）不再彈確認框
- [x] 🔁 限流自動重試（tpm/rpm）：支持固定間隔 / 指數退避 / 隨機抖動，可配置最大重試次數（擴展管理 → 其他）
- [x] ▶️ 繼續生成：AI 被停止或中斷後，從半截回覆接著生成（歷史中段對話也能繼續）
- [x] 🔥 Firebase 可選：沒有 `google-services.json` 也能構建運行
- [x] 🤖 GitHub Actions 自動構建並簽名 APK（Nightly Release）

## 💻 開發

本項目使用 [Android Studio](https://developer.android.com/studio) 開發。

<<<<<<< HEAD
- [Kotlin](https://kotlinlang.org/) (開發語言)
- [Koin](https://insert-koin.io/) (依賴注入)
- [Jetpack Compose](https://developer.android.com/jetpack/compose) (UI 框架)
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore?hl=zh-cn#preferences-datastore) (
  偏好數據存儲)
- [Room](https://developer.android.com/training/data-storage/room) (數據庫)
- [Coil](https://coil-kt.github.io/coil/) (圖片加載)
- [Material 3 Expressive](https://m3.material.io/) (UI 設計)
- [Navigation 3](https://developer.android.com/guide/navigation/navigation-3) (導航)
- [Okhttp](https://square.github.io/okhttp/) (HTTP 客戶端)
- [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) (Json序列化)
=======
技術棧：
>>>>>>> 018ab9e4 (feat: 二次开发收尾——其他分类移到工作区、README/关于页/许可证定制、上游同步文档)

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

<<<<<<< HEAD
## 💰 捐贈

* [Ko-fi](https://ko-fi.com/reovodev)
* [愛發電](https://afdian.com/a/reovo)

## ⭐ Star History

如果喜歡這個項目，請給個Star ⭐

<a href="https://www.star-history.com/?type=date&repos=re-ovo%2Frikkahub">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/chart?repos=re-ovo/rikkahub&type=date&theme=dark&legend=top-left&sealed_token=qSytWeq7LkzQQViTjK0MYlvvA_qkfuwjOxOqgbRpLUZZwok5rO6LXhpVL7Mq-q3o89BfKpzE7g66BCy18H6eiqTsD8czD0J-HejLqmHy-npcvCTHu11wZw" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/chart?repos=re-ovo/rikkahub&type=date&legend=top-left&sealed_token=qSytWeq7LkzQQViTjK0MYlvvA_qkfuwjOxOqgbRpLUZZwok5rO6LXhpVL7Mq-q3o89BfKpzE7g66BCy18H6eiqTsD8czD0J-HejLqmHy-npcvCTHu11wZw" />
   <img alt="Star History Chart" src="https://api.star-history.com/chart?repos=re-ovo/rikkahub&type=date&legend=top-left&sealed_token=qSytWeq7LkzQQViTjK0MYlvvA_qkfuwjOxOqgbRpLUZZwok5rO6LXhpVL7Mq-q3o89BfKpzE7g66BCy18H6eiqTsD8czD0J-HejLqmHy-npcvCTHu11wZw" />
 </picture>
</a>
=======
> [!NOTE]
> 本倉庫為 [rikkahub/rikkahub](https://github.com/rikkahub/rikkahub) 的 fork，
> 同步上游更新請參考 [FORK_SYNC.md](FORK_SYNC.md)。
>>>>>>> 018ab9e4 (feat: 二次开发收尾——其他分类移到工作区、README/关于页/许可证定制、上游同步文档)

## 📄 許可證

採用 [GNU Affero General Public License v3.0](LICENSE)（AGPL-3.0）許可。
本倉庫為 RikkaHub 的二次開發版本，原始版權歸 RikkaHub 作者所有。