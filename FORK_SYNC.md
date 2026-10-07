# Fork 同步说明（Fork Sync Guide）

本仓库是 [rikkahub/rikkahub](https://github.com/rikkahub/rikkahub) 的二次开发 fork，
目标：**跟随上游更新，同时保留本分支的定制功能**。

## 基本信息

| 项目 | 值 |
|---|---|
| 本仓库 | `Rune-cn/rikkahub`（https://github.com/Rune-cn/rikkahub） |
| 上游仓库 | `rikkahub/rikkahub`（https://github.com/rikkahub/rikkahub） |
| fork 基线（上游 commit） | `b64beeef`（feat(chat): 会话开始后把模型、思考级别、搜索等配置固定在会话上） |
| 最近一次同步（上游 commit） | `4a7a39c4`（2026-10-06，上游新增 2 个提交：会话列表排序、AI 消息模型显示修复） |
| 上游默认分支 | `master` |
| 本仓库默认分支 | `master` |

> ✅ 最近一次同步已完成：`git rebase upstream/master` 零冲突，落后 0 / 领先 14（定制提交），构建通过。

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

## 如何同步上游更新

```bash
cd /workspace/rikkahub

# 1. 拉取上游最新代码（只下载，不合并）
git fetch upstream

# 2. 查看上游改了什么
git log --oneline master..upstream/master        # 上游新增的提交
git diff master...upstream/master --stat         # 上游的改动概览

# 3. 合并上游到本地（推荐 rebase，历史更线性）
git rebase upstream/master
# 或：git merge upstream/master

# 4. 若冲突：手动解决（优先保留本分支定制，见上表），然后
git add <解决好的文件>
git rebase --continue     # 如果用的是 rebase
# 或 git commit           # 如果用的是 merge

# 5. 推送（rebase 后需要强推）
git push origin master --force-with-lease
```

推送到 `master` 后，GitHub Actions 会自动构建并发布新的 Nightly Release APK。

## 注意事项

- **不要**把上游的 `updates.rikka-ai.com` / `docs.rikka-ai.com` / 官网链接改回来，
  本分支已改为指向本仓库。
- 上游若改动 `NetworkSetting`、`WorkspaceTools`、`GenerationLoop`、`ChatVM` 等文件，
  合并时检查本分支的定制是否仍在。
- 会优先使用本地缓存的 JBR 21（`~/.gradle/jdks`），无需每次重新下载。

## 快速命令

```bash
git fetch upstream && git log --oneline master..upstream/master -20
```