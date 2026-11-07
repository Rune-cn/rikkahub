# Fork 同步说明（Fork Sync Guide）

本仓库是 [rikkahub/rikkahub](https://github.com/rikkahub/rikkahub) 的二次开发 fork，
目标：**跟随上游更新，同时保留本分支的定制功能**。

## 基本信息

| 项目 | 值 |
|---|---|
| 本仓库 | `Rune-cn/rikkahub`（https://github.com/Rune-cn/rikkahub） |
| 上游仓库 | `rikkahub/rikkahub`（https://github.com/rikkahub/rikkahub） |
| 上游默认分支 | `master` |
| 本仓库默认分支 | `master` |
| 同步方式 | `git merge upstream/master`（见下节） |

> ⚠️ **不要把基线 commit 与「领先/落后多少个提交」写死在本文件**：每次同步后必然过期（本文件此前
> 就因为写死而失效）。需要时用文末「快速命令」现场查询。

## 本分支的定制改动（同步时注意保持）

以下文件的改动是本分支自定义的，**合并上游时若冲突，以本分支为准**：

1. **包名**：`app/build.gradle.kts` → `applicationId = "app.ai.rune"`
2. **工具审批「全部允许」**：
   - `app/.../data/ai/tools/WorkspaceTools.kt`（`WORKSPACE_ALLOW_ALL_APPROVAL_KEY`）
   - `app/.../data/ai/tools/ChatToolFactory.kt`（所有工具免审批）
   - `app/.../ui/pages/extensions/workspace/WorkspaceDetailPage.kt`（「其他」分类 UI）
3. **限流自动重试（tpm/rpm）**：
   - `app/.../data/ai/GenerationLoop.kt`（重试逻辑、重试间隔策略）
   - `app/.../data/datastore/PreferencesStore.kt`（`NetworkSetting` 新增字段）
   - `app/.../ui/pages/extensions/workspace/WorkspaceDetailPage.kt`（设置 UI）
4. **Firebase 可选化**：
   - `app/build.gradle.kts`（条件启用 google-services / crashlytics 插件）
   - `app/.../di/AppModule.kt`（移除 Firebase 单例）
   - `app/.../ui/pages/chat/ChatVM.kt`（`analytics` 内部懒加载）
   - `app/.../di/ViewModelModule.kt`（移除 `analytics = get()`）
5. **关于/设置页**：去掉官网、GitHub 指向本仓库、更新检查禁用
   - `app/.../ui/pages/setting/SettingAboutPage.kt`
   - `app/.../ui/pages/setting/SettingPage.kt`
   - `app/.../utils/UpdateChecker.kt`
6. **构建配置**：
   - `.github/workflows/daily-build.yml`（签名、Nightly Release、push 触发）
   - `gradle/gradle-daemon-jvm.properties`（JBR 21 直链，绕开 foojay 服务故障）
   - 签名密钥通过 GitHub Secrets（`KEY_BASE64`、`SIGNING_CONFIG`）注入

7. **移除内置广告提供商**（返利/推广项，**冲突时不要合回来**）：
   - `app/.../data/datastore/DefaultProviders.kt`（删除 AiHubMix / APIMart / 随想AI网关 / MaruCode / 302.AI）
   - `app/.../data/datastore/RecommendedProviders.kt`（整个「推荐提供商」Sheet 已删除）
   - `app/.../ui/pages/setting/SettingProviderPage.kt`（移除入口按钮与列表项）
   - 判据：带返利参数（`?aff=` / `?promo=` / 跳转短链）或纯推广文案的内置提供商一律不加回；
     正规厂商（OpenAI / Gemini / DeepSeek / OpenRouter / 硅基流动 / 阿里云百炼 等）保留

## 如何同步上游更新

**用 merge，不要用 rebase。** 本分支有 160+ 个定制提交，且与上游大量改动同一批文件；rebase 要逐个
回放这些提交、对同一批文件反复解冲突，还要强推改写历史。merge 只需一轮冲突解决，且推送是普通提交、
不需要 `--force`。

```bash
cd /workspace/rikkahub

# 1. 拉取上游最新代码（只下载，不合并）
git fetch upstream

# 2. 先看上游改了什么
git log --oneline master..upstream/master        # 上游新增的提交
git diff master...upstream/master --stat         # 上游的改动概览

# 3. 合并上游（本地解决冲突后再提交）
git merge upstream/master

# 4. 冲突处理：按上文「本分支的定制改动」逐项判断，保留本分支定制
git add <解决好的文件>
git commit

# 5. 推送（merge 不需要 --force）
git push origin master
```

> 冲突集中在少数文件（上次 35 个双方都改的文件中只有 14 个真正冲突）。解决后**必须**做两件事：
> 1. 全文搜索 `<<<<<<<` 确认没有残留标记；
> 2. 搜索被本分支删除但上游仍引用的符号（例如上游的归一化逻辑会引用 `RECOMMENDED_PROVIDERS`，
>    本分支已删除该文件）——这类引用不会产生冲突，只会静默导致编译失败。

推送到 `master` 后，GitHub Actions 会自动构建；**构建失败时不会发布**（发布步骤以构建成功为前提），
所以坏提交不会污染 Release。

## 注意事项

- **不要**把上游的 `updates.rikka-ai.com` / `docs.rikka-ai.com` / 官网链接改回来，
  本分支已改为指向本仓库。
- 上游若改动 `NetworkSetting`、`WorkspaceTools`、`GenerationLoop`、`ChatVM` 等文件，
  合并时检查本分支的定制是否仍在。
- `app/build.gradle.kts` 的版本号：上游用硬编码 `versionCode` / `versionName`，本分支用
  `val appVersion` / `val appVersionCode` 变量（CI 依赖）。冲突时**保留变量形式**，并把数值推进到
  不小于上游的值（如上游 3.1.1 / 194），否则用户无法覆盖安装。
- 会优先使用本地缓存的 JBR 21（`~/.gradle/jdks`），无需每次重新下载。

## 快速命令

```bash
git fetch upstream && git log --oneline master..upstream/master -20
```