---
name: git-commit
description: "按本仓库既有规范编写、校验并创建中文 Conventional Commits 提交信息（type(scope): 中文摘要 + 要点列表正文）。"
whenToUse: "用户要求提交代码、编写或修改 commit message、amend 提交，或输入中出现“提交”“commit”“git commit”“写提交信息”时。"
---

# git-commit：按 code-pulse 既有规范提交

本技能是本仓库 commit message 的唯一规范来源。用户没有明确要求提交时，只生成并展示 message，不执行 `git commit`。

## 硬性格式

```
<type>(<scope>): <中文摘要>

- 要点1
- 要点2
```

- **type** 仅限 `feat`、`fix`、`refactor`、`docs`、`style`。新增其它类型（test/chore/perf/build/ci）前先征得用户同意。
- **scope** 必填、小写、单词或连字符；优先从 `references/conventions.md` 的 scope 目录中选择。
- **摘要**：动词开头（添加/新增/实现/创建/集成/修复/重构/移除/更新/优化/完善/调整/简化/替换/扩展/配置），8–25 个汉字，整行 ≤ 50 字符；不以句号、问号、感叹号或 emoji 结尾。
- **正文**：与摘要之间恰好一个空行；每条以 `- ` 开头，1–12 条（默认 5–8 条），条目之间不空行，每条不以句号结尾。
- **具体**：正文写到类名、方法名、接口路径、配置项或字段名，避免“修改若干文件”“优化相关逻辑”这类空话。
- **术语**：Vue、SSE、Redis、COS、Sa-Token、LangGraph4j、API、ID、Logo 等按官方写法保留；中英文之间是否加空格历史两种写法并存，同一条提交内保持一致即可。
- **不写 trailer**：不加 Co-authored-by、Signed-off-by、BREAKING CHANGE、Closes # 等尾注。

## 五步流程

1. **收集证据**：`git status --short`；已暂存看 `git diff --cached`，未暂存看 `git diff`。没有改动就停止并告诉用户。
2. **定 type**：新功能 feat / 缺陷修复 fix / 行为不变重构 refactor / 文档 docs / 纯格式 style。
3. **定 scope**：取改动最集中的目录、模块或组件；不确定时查 `references/conventions.md`。
4. **写摘要 + 要点**：先写摘要，再从 diff 提炼 5–8 条要点，按重要性排序，核心在前。
5. **自检**：逐条对照上面的硬性格式；需要时运行同目录脚本：
   `pwsh -File .dsh/skills/git-commit/scripts/validate-commit-message.ps1 -Path <消息文件>`（或 `-Message "<文本>"`）。

## 用户要求提交时

1. 展示完整 message 并等待用户确认（用户已明确说“直接提交”时可跳过确认，但仍先输出 message）。
2. 只暂存本次改动涉及的文件；不要 `git add -A` 或把无关文件带进提交。
3. 用 stdin 传递消息，避免临时文件：在同一命令中执行 `$msg | git commit -F -`；用户已有消息文件时用 `git commit -F <文件>`。不要写入 `temp/`（本机沙箱会拒绝写该目录）；确需文件时写到 `$env:TEMP`。
4. amend 同理：`$msg | git commit --amend -F -`。
5. 提交失败（hook、身份未配置、冲突）如实报告，不绕过 hook、不 force push、不自行修改全局配置。

## 参考

- 完整判定表、19 个 scope 的语义、边界与反例：同目录 `references/conventions.md`
- 历史提交原文样例：同目录 `references/examples.md`
