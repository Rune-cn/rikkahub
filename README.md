<div align="center">
  <img src="docs/icon.png" alt="App Icon" width="100" />
  <h1>RikkaHub</h1>

A native Android LLM chat client that supports switching between different providers for
conversations 🤖💬

A fork of [rikkahub/rikkahub](https://github.com/rikkahub/rikkahub) with package name `app.ai.rune`

[简体中文](README_ZH_CN.md) | [繁體中文](README_ZH_TW.md) | English
</div>

<div align="center">
  <img src="docs/img/poster.png" alt="RikkaHub" width="800" />
</div>

## 📦 Releases

Built APKs are published automatically to the [Nightly Build release](https://github.com/Rune-cn/rikkahub/releases/tag/nightly):

| APK | Description |
|---|---|
| `app-arm64-v8a-release.apk` | Phones (arm64) |
| `app-universal-release.apk` | Universal package |
| `app-x86_64-release.apk` | Emulators (x86_64) |

<<<<<<< HEAD
> [!WARNING]
> There are many forked versions of RikkaHub. Issues with forks are unrelated to RikkaHub, so please use forks with caution to avoid privacy leaks or excessive permission requests.

## 💖 Sponsors

| Sponsor | Description |
|:---:|:---|
| <img src="docs/sponsors/aihubmix.png" alt="Aihubmix" width="50" /><br /><b>Aihubmix</b> | Thanks to <a href="https://aihubmix.com?aff=pG7r">aihubmix.com</a> for their financial support. We recommend using aihubmix as a one-stop shop for mainstream models worldwide. (OpenAI, Claude, Google Gemini, DeepSeek, Qwen, and hundreds more). |
| <img src="docs/sponsors/api-mart.png" alt="APIMart" width="50" /><br /><b><a href="https://go.apimart.ai/gh-rikkahub">APIMart</a></b> | Thanks to APIMart for sponsoring this project! APIMart is a low-cost API platform for AI image & video generation — GPT-Image-2 from $0.006/image, 160+ images per dollar. One async API covers both image and video: submit a task, get an ID, fetch results via polling or callback. Batch tens of thousands of images without timeouts, switch models without changing code. Pay-as-you-go with no monthly fee — <a href="https://go.apimart.ai/gh-rikkahub">sign up here</a> to get started. |
| <img src="docs/sponsors/suixiang.jpg" alt="随想AI中转" width="50" /><br /><b><a href="https://sui-xiang.com">随想AI中转</a></b> | 感谢<a href="https://sui-xiang.com">随想AI中转</a>对本项目的赞助！随想AI中转 是一家可靠高效的 API 中继服务提供商，提供 Claude、Codex、Gemini 等的中继服务。注重隐私的中转站·无数据倒卖·无模型掺水，隐私，透明，极速售后。新账户注册每日签到就送 0.5 元测试额度，充值额度 1:1，无需订阅，按量付费。多线路冗余、跨区域容灾、自动故障切换，长链路 SSE 不中断。99.9% 可用性，关键调用从不掉队。 |
| <img src="docs/sponsors/ztest.png" alt="真测 ztest.ai" width="50" /><br /><b><a href="https://ztest.ai">真测 ztest.ai</a></b> | 感谢<a href="https://ztest.ai">真测 ztest.ai</a>对本项目的赞助！真测 ztest.ai 是一个 AI 中转站模型检测平台，检测结果数据全公开，23 项探针覆盖协议、身份、能力、内容完整性、安全性、性能六大维度，交叉印证识别伪造与降级。作为独立第三方验证平台，实时监测 AI 中转站的模型真实性、响应质量与服务可用性。 |
| <img src="docs/sponsors/maru.png" alt="MaruCode" width="50" /><br /><b><a href="https://api.muteki.site/register?aff=Rikkahub&promo=Rikkahub">MaruCode</a></b> | <b><a href="https://api.muteki.site/register?aff=Rikkahub&promo=Rikkahub">MaruCode</a></b> 是一家偶尔做做慈善的小破站 API，自营号池，主要提供 Codex、Claude Code、GPT Image 等主流模型，支持 Websocket 协议，明码标价(Codex 0.25x, CC 1.5x)，透明汇率(1:1)，<a href="https://api.muteki.site/register?aff=Rikkahub&promo=Rikkahub">新用户注册送 2 刀</a>。<a href="https://images-2.muteki.site">生图工作台🖼️</a> |
| <img src="docs/sponsors/naapi.png" alt="NaAPI 钠API" width="50" /><br /><b><a href="https://naapi.cc/register?aff=rikkahub">NaAPI 钠API</a></b> | Thanks to <a href="https://naapi.cc/register?aff=rikkahub">NaAPI</a> (naapi.cc) for sponsoring this project! NaAPI is an OpenAI-compatible LLM gateway that reliably serves 300+ models from OpenAI, Anthropic, Google and others at discounted pricing. One base URL and one key is all you need: multi-line redundancy with automatic failover keeps long SSE streams alive, and pricing is strictly pay-as-you-go with a 1:1 top-up ratio — no subscription, no hidden markup. |

## ✨ Features

- 🎨 Material 3 Expressive design and 🌙 Dark mode
- 📦 Workspace: a proot-based Linux agent environment
- 🎬 Media generation (image and video)
- 🔄 Multiple AI Provider Support: custom API / URL / models (all OpenAI, Google, Anthropic compatible api)
- 🖼️ Multimodal input support (Image, Text Documentation, PDF, Docx)
- 🖥️ Web access for multi-platform use
- 🛠️ MCP and Skills support
- 📝 Markdown Rendering (with code highlighting, Latex formulas, tables, Mermaid)
- 🔍 Search capabilities (Exa, Tavily, Zhipu, LinkUp, Brave, Perplexity, etc.)
- 🧩 Prompt variables (model name, time, etc.)
- 🤳 QR code export and import for providers
- 🤖 Agent customization
- 🧠 File-based memory
- 📝 AI Translation
- 🌐 Custom HTTP request headers and request bodies
- 💌 Silly Tavern character card import
=======
> APKs are rebuilt on every push to `master`, or twice a day (UTC 09:00 / 18:00) by GitHub Actions.

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
>>>>>>> 018ab9e4 (feat: 二次开发收尾——其他分类移到工作区、README/关于页/许可证定制、上游同步文档)

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
- [Material 3 Expressive](https://m3.material.io/) (UI design)
- [Navigation 3](https://developer.android.com/guide/navigation/navigation-3) (Navigation)
- [Okhttp](https://square.github.io/okhttp/) (HTTP client)
- [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) (JSON serialization)

<<<<<<< HEAD
> [!TIP]
> You need a `google-services.json` file at `app` folder to build the app.

## 💰 Donate

* [Ko-fi](https://ko-fi.com/reovodev)
* [爱发电](https://afdian.com/a/reovo)

## ⭐ Star History

If you like this project, please give it a star ⭐

<a href="https://www.star-history.com/?type=date&repos=re-ovo%2Frikkahub">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/chart?repos=re-ovo/rikkahub&type=date&theme=dark&legend=top-left&sealed_token=qSytWeq7LkzQQViTjK0MYlvvA_qkfuwjOxOqgbRpLUZZwok5rO6LXhpVL7Mq-q3o89BfKpzE7g66BCy18H6eiqTsD8czD0J-HejLqmHy-npcvCTHu11wZw" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/chart?repos=re-ovo/rikkahub&type=date&legend=top-left&sealed_token=qSytWeq7LkzQQViTjK0MYlvvA_qkfuwjOxOqgbRpLUZZwok5rO6LXhpVL7Mq-q3o89BfKpzE7g66BCy18H6eiqTsD8czD0J-HejLqmHy-npcvCTHu11wZw" />
   <img alt="Star History Chart" src="https://api.star-history.com/chart?repos=re-ovo/rikkahub&type=date&legend=top-left&sealed_token=qSytWeq7LkzQQViTjK0MYlvvA_qkfuwjOxOqgbRpLUZZwok5rO6LXhpVL7Mq-q3o89BfKpzE7g66BCy18H6eiqTsD8czD0J-HejLqmHy-npcvCTHu11wZw" />
 </picture>
</a>
=======
> [!NOTE]
> This repository is a fork of [rikkahub/rikkahub](https://github.com/rikkahub/rikkahub).
> To sync upstream updates, see [FORK_SYNC.md](FORK_SYNC.md).
>>>>>>> 018ab9e4 (feat: 二次开发收尾——其他分类移到工作区、README/关于页/许可证定制、上游同步文档)

## 📄 License

Licensed under the [GNU Affero General Public License v3.0](LICENSE) (AGPL-3.0).
This is a modified fork of RikkaHub; the original project is owned by RikkaHub authors.