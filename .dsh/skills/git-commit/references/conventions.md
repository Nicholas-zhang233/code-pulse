# code-pulse 提交信息规范

## 1. 规范来源

统计自本仓库 66 条历史提交：`type(scope): 中文摘要`（Conventional Commits 中文版），空行后接 `- ` 要点列表。

| 指标 | 历史统计 |
| --- | --- |
| type | feat 53、refactor 7、fix 2、docs 1、style 1 |
| scope | 19 个；ai 14、app 12、auth 9、chat 6、workflow 4、api 3、database 2、tools 2、langgraph4j 2，其余各 1 |
| 摘要描述长度 | 平均 14.8 汉字，范围 8–25；整行最长 38 字符 |
| 正文条数 | 平均 6.4，众数 6；5–8 条共 51/66 |
| 正文结尾标点 | 0/64 条以句号结尾 |
| trailer | 0 条（无 Co-authored-by / Signed-off-by / BREAKING CHANGE / Closes） |

## 2. 模板

```
<type>(<scope>): <中文摘要>

- 要点1
- 要点2
- 要点3
```

## 3. type 判定表

| type | 含义 | 触发场景 |
| --- | --- | --- |
| `feat` | 新增功能或能力 | 新接口、新页面、新工作流、新的 AI 生成能力 |
| `fix` | 修复缺陷 | 修 bug、修空指针、修错误返回值、修路由/缓存问题 |
| `refactor` | 不改变外部行为的重构 | 重命名、移动、删除废弃代码、同步/异步改造 |
| `docs` | 文档变更 | README、接口文档、注释性文档 |
| `style` | 纯格式或空白调整 | 缩进、空行、格式化，不改变逻辑 |

扩展类型（`test`、`chore`、`perf`、`build`、`ci`）与历史不符，使用前先确认；确认后在正文首条说明理由。

## 4. scope 目录

| scope | 覆盖范围 | 典型位置 |
| --- | --- | --- |
| `ai` | AI 代码生成、提示词装配、facade | `src/main/java/com/zwx/codepulse/ai` |
| `app` | 应用管理 | `AppController`、`AppService`、`AppServiceImpl`、前端应用页 |
| `auth` | 登录注册、权限、Sa-Token | `AuthController`、`UserController`、`access.ts` |
| `chat` | 聊天流、SSE、对话历史 | 聊天相关 controller/service/前端页面 |
| `workflow` | 业务工作流编排 | LangGraph4j 相关业务代码（非引擎包） |
| `langgraph4j` | 工作流引擎包 | `src/main/java/com/zwx/codepulse/langgraph4j` |
| `api` | 前端接口层与接口路径调整 | `code-pules-frontend/src/api` |
| `database` | 表结构、SQL、Mapper 数据变更 | `sql/`、实体类、`mapper/` |
| `tools` | AI 工具 | 文件工具、截图、Logo、图片搜索等 Tool 类 |
| `cache` | Redis 缓存 | 缓存注解、缓存配置、CacheManager |
| `ratelimit` | 限流 | `ratelimter` 包、Redisson 限流 |
| `message` | 流式消息模型与枚举 | 消息实体、消息类型枚举 |
| `parser` | 代码解析 | 解析器、解析策略类 |
| `prompt` | 提示词文档 | `src/main/resources/prompt` |
| `build` | 前端/项目构建 | `VueProjectBuilder`、npm 构建流程 |
| `frontend` | 前端模式与框架支持 | 前端页面、Vue 项目模式 |
| `header` | 顶部导航组件 | `GlobalHeader` |
| `layout` | 全局布局组件 | 布局与路由容器 |
| `service` | 服务层通用调整 | 不归属具体业务的 service 改动 |

新增 scope 规则：

1. 小写，单个英文词或用连字符连接，如 `guardrail`、`auth`；
2. 取改动最集中的目录、模块或组件，而不是“涉及到的所有目录”；
3. 优先复用上表已有 scope，避免同义重复（如不要用 `ai-service` 代替 `ai`）；
4. 新 scope 必须在正文中写清完整包名、类名或路径，便于后续检索；
5. 首次引入新 scope 后，把它补进本表并说明语义。

## 5. 摘要细则

- 动词开头，常用动词：添加、新增、实现、创建、集成、修复、重构、移除、更新、优化、完善、调整、简化、替换、扩展、配置。
- 描述长度 8–25 个汉字，整行不超过 50 字符；历史整行最长 38 字符。
- 可用“并”连接两个要点，例如 `添加对话历史表并集成AI护轨功能`、`移除异步Vue项目构建并改为同步执行`。超过两个要点应拆成两次提交。
- 保留官方术语写法：`AI`、`Vue`、`SSE`、`Redis`、`COS`、`Sa-Token`、`LangGraph4j`、`API`、`ID`、`Logo`。
- 不用空泛词：`若干`、`相关`、`部分`、`一些`、`优化代码`、`更新文件`。
- 摘要末尾不加句号、问号、感叹号，不用 emoji。
- 摘要只写“做了什么”，不写“为什么”和实现细节；原因放正文。
- 中英文之间是否加空格历史两种写法并存（如 `集成 LangGraph4j 工作流` 与 `从JsonMessageStreamHandler中移除`），不强制，但同一条提交内保持一致。

## 6. 正文细则

- 与摘要之间恰好一个空行。
- 每条以 `- ` 开头；条目之间不空行。
- 条数：默认 5–8 条；少于 3 条时确认是否小改动；超过 10 条时合并同主题条目；硬限 1–12 条。
- 每条 15–60 个汉字（含英文术语不超过 100 字符），写到类名、方法名、接口路径、配置项或字段名。
- 按重要性排序，核心变更在前；同主题的改动合并成一条。
- 不重复摘要；不逐文件罗列；不写“修改了 A 文件、B 文件”。
- 每条以动词或动作开头，避免流水账（“然后…”“另外…”）。
- 不写解释性长句和背景故事；需要背景时用一句短说明。
- 条目不以句号、问号、感叹号结尾。
- 不添加 trailer（Co-authored-by、Signed-off-by、BREAKING CHANGE、Closes #）。确需关联 issue 时先与用户确认写法。

## 7. 边界与常见错误

| 错误 | 说明 | 修正 |
| --- | --- | --- |
| `first commit` | 无 type、scope、正文 | `feat(app): 添加应用管理基础功能` + 3–5 条要点 |
| `删除文件` | 无 type、scope，看不出删了什么 | `refactor(app): 移除废弃的临时页面组件` + 说明删除范围与影响 |
| `feat(app): 添加功能。` | 摘要带句号、空泛 | `feat(app): 添加专家模式支持` |
| `feat(App): 添加菜单项` | scope 大写 | `feat(header): 添加应用管理菜单项` |
| 正文直接接在摘要下一行 | 缺少空行 | 摘要后空一行再写 `- ` 条目 |
| 一条提交包含多个特性 | type/scope 无法准确概括 | 拆成多次提交，按 type+scope 分组 |
| 正文“修改了一些文件”“优化了代码” | 无法验证 | 写具体类、方法、接口、配置项 |
| 正文条目带句号 | 与历史不一致 | 去掉句号 |

其它规则：

- 用户明确指定格式、语言或风格时，用户要求优先，并说明已偏离本规范。
- 没有实际改动时停止，不编造 message。
- 大范围改动跨多个模块时，先建议拆分提交，由用户确认后再写 message。
- 提交前检查是否有不该提交的本地配置或密钥文件（如 `application-local.yml`）。

## 8. 校验脚本

```
pwsh -File .dsh/skills/git-commit/scripts/validate-commit-message.ps1 -Path temp/commit-msg.txt
pwsh -File .dsh/skills/git-commit/scripts/validate-commit-message.ps1 -Message "feat(app): 添加专家模式支持`n`n- 添加工作流支持"
```

脚本对格式错误退出 1，对合法消息退出 0；长度、条数、空格等建议只输出 WARN，不影响退出码。

消息文件可以放在仓库外（如 `$env:TEMP`）；仓库内 `temp/` 目录在当前沙箱下不可写，不要把消息文件放进去。提交时优先用 stdin：`$msg | git commit -F -`。
