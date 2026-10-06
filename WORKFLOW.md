# RikkaHub Fork 开发工作流

本仓库为 fork（`Rune-cn/rikkahub`），源仓库为 `rikkahub/rikkahub`。

## 远端配置

- `origin`   = https://github.com/Rune-cn/rikkahub.git   （你的 fork，推送到这里）
- `upstream` = https://github.com/rikkahub/rikkahub.git  （原仓库，从这里同步更新）

## 一、日常开发：提交自己的改动

```bash
# 1. 在 /workspace/rikkahub 中修改代码
# 2. 查看改了什么
git status                    # 改了哪些文件
git diff                      # 具体改动内容

# 3. 提交
git add <文件>                # 或 git add -A 添加全部
git commit -m "feat(xxx): 说明你的改动"

# 4. 推送到自己的 fork（GitHub 上即可看到）
git push origin master
```

**查看自己之前改了什么：**

```bash
git log --oneline upstream/master..HEAD   # 自己相比原仓库多出的所有提交
git log --oneline -20                      # 最近 20 条提交（含自己的和同步来的）
git show <commit-id>                       # 看某次提交的具体改动
git diff upstream/master                   # 自己本地与原仓库的完整差异
```

## 二、同步原仓库（主分支）更新

```bash
# 1. 拉取原仓库最新提交（只看不改）
git fetch upstream

# 2. 查看主分支改了什么
git log --oneline upstream/master -20          # 原仓库最近提交
git diff upstream/master...origin/master --stat # fork 与原仓库的差异

# 3. 合并到本地（推荐 rebase 保持线性历史）
git rebase upstream/master
# 或 git merge upstream/master

# 4. 推送到自己的 fork
git push origin master
```

也可以在 GitHub 网页上操作：fork 仓库页面 → **Sync fork** 按钮 → 查看 "n commits behind/ahead"。

## 三、可能出现的冲突

如果原仓库改了你正在改的文件，`git rebase upstream/master` 可能报冲突。
解决方式：
1. 打开冲突文件，保留想要的内容（`<<<<<<<` / `=======` / `>>>>>>>` 标记之间）
2. `git add <文件>`
3. `git rebase --continue`
4. 最后 `git push origin master --force-with-lease`

## 常用别名（可加到 ~/.gitconfig）

```bash
git config alias.upstream-log "log --oneline upstream/master -20"
git config alias.my-commits "log --oneline upstream/master..HEAD"
git config alias.sync "!git fetch upstream && git rebase upstream/master && git push origin master"
```
