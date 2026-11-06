# 历史提交样例

以下 message 均摘自本仓库真实提交，可作为措辞与结构的参考。

## 正例 1：feat(header)

```
feat(header): 添加管理员应用管理和对话管理菜单项

- 在 GlobalHeader 组件中导入 AppManagePage 和 ChatManagePage 页面组件
- 向管理员菜单中添加应用管理菜单项，路由为 /admin/appManage
- 向管理员菜单中添加对话管理菜单项，路由为 /admin/chatManage
- 修复 HomePage 中应用总数转换为数字类型的潜在问题
- 修复精选应用接口缓存注解导致的返回值问题
- 修复 AppServiceImpl 中方法返回值为空的问题
```

解剖：摘要 19 个汉字，用“并”连接两个要点；正文 6 条，每条落到组件名、路由或类名。

## 正例 2：feat(app)

```
feat(app): 添加专家模式支持

- 增加专家模式工作流支持，实现多阶段代码生成流程
- 添加专家模式标识显示和生成模式选择功能
- 集成 LangGraph4j 工作流引擎支持专家模式
- 优化代码生成质量检查和重试机制
- 增加工作流步骤进度展示功能
- 扩展数据库 schema 支持生成模式字段
```

解剖：摘要 8 个汉字；正文 6 条，覆盖工作流、前端展示、引擎集成、数据库。

## 正例 3：feat(database)

```
feat(database): 添加对话历史表并集成AI护轨功能

- 在数据库中新增chat_history表用于存储对话记录
- 实现RetryOutputGuardrail类提供AI输出内容验证
- 添加ExitTool工具支持AI任务完成时退出操作
- 配置AutoIncrement初始值确保数据表ID连续性
- 集成AiCodeGenTypeRoutingServiceFactory服务工厂
- 建立消息类型索引优化对话历史查询性能
```

解剖：摘要 14 个汉字，含 `AI` 且不加空格；正文 6 条，每条包含表名、类名或配置项。

## 正例 4：style(service)

```
style(service): 格式化代码中的空格

- 修复了AppServiceImpl中return语句的多余空格问题
```

解剖：纯格式调整用 `style`；正文 1 条，说明具体类与方法。这条说明“小改动可以只有 1 条正文”，但摘要仍要具体。

## 正例 5：refactor(ai)

```
refactor(ai): 移除异步Vue项目构建并改为同步执行

- 从JsonMessageStreamHandler中移除vueProjectBuilder依赖注入
- 删除流式响应完成后的异步Vue项目构建逻辑
- 在AiCodeGeneratorFacade中添加VueProjectBuilder依赖注入
- 修改processTokenStream方法签名以接收appId参数
- 在ChatResponse完成时同步执行Vue项目构建确保预览时项目已就绪
```

解剖：重构不改外部行为；摘要 14 个汉字；正文 5 条，写清“从哪里移除、在哪里添加、改了哪个方法签名”。

## 反例与修正

### 反例 1

```
first commit
```

问题：无 type、scope 和正文，无法追溯。修正：

```
feat(app): 添加应用管理基础功能

- 创建应用管理页面和对应路由
- 添加 AppController 提供应用增删改查接口
- 集成 AppService 与数据库 Mapper 完成持久化
```

### 反例 2

```
删除文件
```

问题：无 type、scope，看不出删了什么、影响是什么。修正：

```
refactor(app): 移除废弃的临时页面组件

- 删除不再使用的临时页面与对应路由
- 从菜单配置中移除失效入口
- 更新 AppServiceImpl 中受影响的引用
```

### 反例 3

```
feat(app): 添加用户功能。

- 修改了一些文件
- 优化了代码
- 完成
```

问题：摘要带句号；正文空泛、无可验证信息；条数不足。修正：

```
feat(auth): 添加用户注册与登录功能

- 新增 AuthController 提供注册、登录和登出接口
- 集成 Sa-Token 完成会话与权限校验
- 添加前端登录页和注册页并接入路由
- 在 access.ts 中补充路由访问控制
- 新增用户表及对应 Mapper 映射
```

## 结构速查

```
type(scope): 动词+对象+结果（8–25 汉字）

- 动作 + 类/方法/接口/配置（核心在前）
- 动作 + 数据表/字段/路由
- 动作 + 前端组件/页面
```
