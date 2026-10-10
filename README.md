<div align="center">
  <img src="docs/icon.png" alt="App Icon" width="100" />
  <h1>RikkaHub</h1>

A native Android LLM chat client that supports switching between different providers for
conversations 🤖💬

A fork of [rikkahub/rikkahub](https://github.com/rikkahub/rikkahub) with package name `app.ai.rune`

[简体中文](README_ZH_CN.md) | [繁體中文](README_ZH_TW.md) | English
</div>

<div align="center">
  <img src="docs/img/chat.png" alt="Chat Interface" width="150" />
  <img src="docs/img/desktop.png" alt="Models Picker" width="450" />
</div>

## 📦 Releases

Built APKs are published automatically to the [Nightly Build release](https://github.com/Rune-cn/rikkahub/releases/tag/nightly):

| APK | Description |
|---|---|
| `app-arm64-v8a-release.apk` | Phones (arm64) — the only build target |

> APKs are rebuilt on every push to `master`, or twice a day (UTC 09:00 / 18:00) by GitHub Actions.
> Only the **arm64-v8a** variant is built now. In-app update check points to this fork's own Nightly Release.

## ✨ Features

- [x] 🎨 Material You Design and 🌙 Dark mode
- [x] 📦 Workspace: a proot-based Linux agent environment
- [x] 🔄 Multiple AI Provider Support: custom API / URL / models (all OpenAI, Google, Anthropic compatible api)
- [x] 🖼️ Multimodal input support (Image, Text Documentation, PDF, Docx)
- [x] 🖥️ Web access for multi-platform use
- [x] 🛠️ MCP support
- [x] 📝 Markdown Rendering (with code highlighting, Latex formulas, tables, Mermaid)
- [x] 🪾 Message Branching
- [x] 🔍 Search capabilities (Exa, Tavily, Zhipu, LinkUp, Brave, Perplexity, etc.)
- [x] 🧩 Prompt variables (model name, time, etc.)
- [x] 🤳 QR code export and import for providers
- [x] 🤖 Agent customization
- [x] 🧠 ChatGPT-like memory feature
- [x] 📝 AI Translation
- [x] 🌐 Custom HTTP request headers and request bodies
- [x] 💌 Silly Tavern character card import

### Added in this fork

- [x] 📱 Package name changed to `app.ai.rune` (display name unchanged)
- [x] 🔓 Tool approval "Allow All": AI can use every tool (including MCP) without confirmation dialogs
- [x] 🔁 Rate-limit auto retry (tpm/rpm): fixed interval / exponential backoff / jitter, configurable max retry count (Extensions → Other)
- [x] ▶️ Continue generation: after AI is stopped or interrupted, resume from the half-finished reply (including unfinished conversations from history)
- [x] 🔥 Firebase optional: builds and runs without `google-services.json`
- [x] 🤖 GitHub Actions builds and signs APKs automatically (Nightly release)

## 💻 Development

This project is developed using [Android Studio](https://developer.android.com/studio).

Technology stack:

- [Kotlin](https://kotlinlang.org/) (Development language)
- [Koin](https://insert-koin.io/) (Dependency Injection)
- [Jetpack Compose](https://developer.android.com/jetpack/compose) (UI framework)
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) (Preference data
  storage)
- [Room](https://developer.android.com/training/data-storage/room) (Database)
- [Coil](https://coil-kt.github.io/coil/) (Image loading)
- [Material You](https://m3.material.io/) (UI design)
- [Navigation 3](https://developer.android.com/guide/navigation/navigation-3) (Navigation)
- [Okhttp](https://square.github.io/okhttp/) (HTTP client)
- [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) (JSON serialization)

> [!NOTE]
> This repository is a fork of [rikkahub/rikkahub](https://github.com/rikkahub/rikkahub).
> To sync upstream updates, see [FORK_SYNC.md](FORK_SYNC.md).

## 📄 License

Licensed under the [GNU Affero General Public License v3.0](LICENSE) (AGPL-3.0).
This is a modified fork of RikkaHub; the original project is owned by RikkaHub authors.