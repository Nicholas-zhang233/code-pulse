package com.zwx.codepulse.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zwx.codepulse.ai.AiCodeGenTypeRoutingService;
import com.zwx.codepulse.ai.AiCodeGenTypeRoutingServiceFactory;
import com.zwx.codepulse.ai.core.AiCodeGeneratorFacade;
import com.zwx.codepulse.ai.core.builder.VueProjectBuilder;
import com.zwx.codepulse.ai.core.handler.StreamHandlerExecutor;
import com.zwx.codepulse.common.DeleteRequest;
import com.zwx.codepulse.constant.AppConstant;
import com.zwx.codepulse.exception.BusinessException;
import com.zwx.codepulse.exception.ErrorCode;
import com.zwx.codepulse.exception.ThrowUtils;
import com.zwx.codepulse.langgraph4j.CodeGenWorkflow;
import com.zwx.codepulse.langgraph4j.model.WorkflowEvent;
import com.zwx.codepulse.langgraph4j.state.WorkflowContext;
import com.zwx.codepulse.mapper.AppMapper;
import com.zwx.codepulse.model.dto.AppAddRequest;
import com.zwx.codepulse.model.dto.AppUpdateRequest;
import com.zwx.codepulse.model.entity.App;
import com.zwx.codepulse.model.entity.User;
import com.zwx.codepulse.model.enums.ChatHistoryMessageTypeEnum;
import com.zwx.codepulse.model.enums.CodeGenTypeEnum;
import com.zwx.codepulse.model.enums.GenModeEnum;
import com.zwx.codepulse.model.vo.*;
import com.zwx.codepulse.service.AppService;
import com.zwx.codepulse.service.ChatHistoryService;
import com.zwx.codepulse.service.ScreenshotService;
import com.zwx.codepulse.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author: 张伟旭
 * @Create: 2026-09-15 16:42
 * @description:
 **/
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    /**
     * 专家模式写入对话历史的代码内容长度上限
     * <p>
     * 超过该长度时只写入流程摘要，避免对话历史表被整份代码撑爆
     */
    private static final int EXPERT_HISTORY_MAX_LENGTH = 8000;

    /**
     * 专家模式工作流（无状态，与 WorkflowSseController 保持一致的直接实例化方式）
     */
    private final CodeGenWorkflow codeGenWorkflow = new CodeGenWorkflow();


    @Resource
    private UserService userService;

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;
    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private AiCodeGenTypeRoutingServiceFactory aiCodeGenTypeRoutingServiceFactory;

    @Override
    public Flux<String> chatToGenCode(Long appId, String message, LoginUserVO loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证用户是否有权限访问该应用，仅本人可以生成代码
        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        // 4. 获取应用的代码生成类型
        String codeGenTypeStr = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenTypeStr);
        ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型");

        // 5. 通过校验后，添加用户消息到对话历史
        chatHistoryService.addChatMessage(appId, message, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());
        // 6. 调用 AI 生成代码（流式）
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, appId);
        // 7. 收集 AI 响应内容并在完成后记录到对话历史
        return streamHandlerExecutor.doExecute(codeStream, chatHistoryService, appId, loginUser, codeGenTypeEnum);

    }

    @Override
    public Flux<WorkflowEvent> chatToGenCodeByExpert(Long appId, String message, LoginUserVO loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证用户是否有权限访问该应用，仅本人可以生成代码
        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        // 4. 添加用户消息到对话历史
        chatHistoryService.addChatMessage(appId, message, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());
        // 5. 执行 LangGraph4j 工作流，输出结构化事件流
        return codeGenWorkflow.expertWorkflowStream(message, appId,
                        finalContext -> saveExpertResult(appId, loginUser.getId(), finalContext))
                .doOnError(error -> {
                    // 工作流失败时记录错误信息，与一般模式的错误观感保持一致
                    log.error("专家模式代码生成失败，appId: {}, 原因: {}", appId, error.getMessage());
                    chatHistoryService.addChatMessage(appId, "AI回复失败: " + error.getMessage(),
                            ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                });
    }

    /**
     * 工作流执行完成后的收尾：回写真实选中的代码生成类型，并记录 AI 对话历史
     *
     * @param appId        应用 ID
     * @param userId       登录用户 ID
     * @param finalContext 工作流最终上下文
     */
    private void saveExpertResult(Long appId, Long userId, WorkflowContext finalContext) {
        CodeGenTypeEnum generationType = finalContext.getGenerationType();
        // 1. 回写 RouterNode 实际选中的代码生成类型，保证前端预览路径与真实生成目录一致
        if (generationType != null) {
            String selectedType = generationType.getValue();
            App oldApp = this.getById(appId);
            if (oldApp != null && !selectedType.equals(oldApp.getCodeGenType())) {
                App updateApp = App.builder()
                        .id(appId)
                        .codeGenType(selectedType)
                        .build();
                boolean updated = this.updateById(updateApp);
                log.info("专家模式回写代码生成类型，appId: {}, codeGenType: {}, 结果: {}", appId, selectedType, updated);
            }
        }
        // 2. 记录 AI 对话历史：内容过长时只记录流程摘要，避免对话历史表被代码撑爆
        String codeOutput = finalContext.getGeneratedCodeOutput();
        String historyMessage;
        if (StrUtil.isBlank(codeOutput)) {
            historyMessage = "专家模式已执行完成，但未产出代码内容，请重试。";
        } else if (codeOutput.length() > EXPERT_HISTORY_MAX_LENGTH) {
            historyMessage = String.format("专家模式生成完成：图片收集 → 提示词增强 → 智能路由 → 代码生成 → 质检 → 构建（代码生成类型：%s），完整代码请在预览或下载中查看。",
                    generationType == null ? "未知" : generationType.getValue());
        } else {
            historyMessage = codeOutput;
        }
        chatHistoryService.addChatMessage(appId, historyMessage, ChatHistoryMessageTypeEnum.AI.getValue(), userId);
    }

    @Override
    public GenModeEnum resolveGenMode(Long appId, Long userId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        ThrowUtils.throwIf(!app.getUserId().equals(userId), ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        // 历史数据（genMode 为 NULL）与非法值统一按一般模式处理
        return GenModeEnum.of(app.getGenMode());
    }

    @Override
    public String deployApp(Long appId, Long userId) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 错误");
        ThrowUtils.throwIf(userId < 0, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 权限校验，仅本人可以部署自己的应用
        if (!app.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限部署该应用");
        }
        // 4. 检查是否已有 deployKey
        String deployKey = app.getDeployKey();
        // 如果没有，则生成 6 位 deployKey（字母 + 数字）
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }
        // 5. 获取代码生成类型，获取原始代码生成路径（应用访问目录）
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 6. 检查路径是否存在
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码路径不存在，请先生成应用");
        }
        // 7. Vue 项目特殊处理：执行构建
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT) {
            // Vue 项目需要构建
            boolean buildSuccess = vueProjectBuilder.buildProject(sourceDirPath);
            ThrowUtils.throwIf(!buildSuccess, ErrorCode.SYSTEM_ERROR, "Vue 项目构建失败，请重试");
            // 检查 dist 目录是否存在
            File distDir = new File(sourceDirPath, "dist");
            ThrowUtils.throwIf(!distDir.exists(), ErrorCode.SYSTEM_ERROR, "Vue 项目构建完成但未生成 dist 目录");
            // 构建完成后，需要将构建后的文件复制到部署目录
            sourceDir = distDir;
        }
        // 8. 复制文件到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用部署失败：" + e.getMessage());
        }
        // 9. 更新数据库
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        // 10. 得到可访问的 URL 地址
        String appDeployUrl = String.format("%s/%s", AppConstant.CODE_DEPLOY_HOST, deployKey);
        // 11. 异步生成截图并且更新应用封面
        generateAppScreenshotAsync(appId, appDeployUrl);
        return appDeployUrl;
    }

    @Resource
    private ScreenshotService screenshotService;
    /**
     * 异步生成应用截图并更新封面
     *
     * @param appId  应用ID
     * @param appUrl 应用访问URL
     */
    @Override
    @Async("screenshotAsyncExecutor")
    public void generateAppScreenshotAsync(Long appId, String appUrl) {
        // 调用截图服务生成截图并上传
        String screenshotUrl = screenshotService.generateAndUploadScreenshot(appUrl);
        // 更新应用封面字段
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setCover(screenshotUrl);
        boolean updated = this.updateById(updateApp);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "更新应用封面字段失败");
    }



    @Override
    public Long createApp(AppAddRequest appAddRequest, Long userId) {
        // 参数校验
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "初始化 prompt 不能为空");
        // 构造入库对象
        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        app.setUserId(userId);
        // 应用名称暂时为 initPrompt 前 12 位
        app.setAppName(initPrompt.substring(0, Math.min(initPrompt.length(), 12)));
        // 生成模式：前端未传或传了非法值时按一般模式处理
        GenModeEnum genModeEnum = GenModeEnum.of(appAddRequest.getGenMode());
        app.setGenMode(genModeEnum.getValue());
        // 使用 AI 智能选择代码生成类型
        // 使用 AI 智能选择代码生成类型（多例模式）
        AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService = aiCodeGenTypeRoutingServiceFactory.createAiCodeGenTypeRoutingService();
        CodeGenTypeEnum selectedCodeGenType = aiCodeGenTypeRoutingService.routeCodeGenType(initPrompt);
        app.setCodeGenType(selectedCodeGenType.getValue());
        // 插入数据库
        boolean result = this.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        log.info("应用创建成功，ID: {}, 类型: {}, 模式: {}", app.getId(), selectedCodeGenType.getValue(), genModeEnum.getValue());
        return app.getId();
    }


    @Override
    public void updateAppName(AppUpdateRequest appUpdateRequest, Long userId) {
        ThrowUtils.throwIf(appUpdateRequest == null || appUpdateRequest.getId() == null, ErrorCode.PARAMS_ERROR);
        // 判断是否存在
        Long id = appUpdateRequest.getId();
        App oldApp = this.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人可更新
        ThrowUtils.throwIf(!oldApp.getUserId().equals(userId), ErrorCode.NO_AUTH_ERROR);
        App app = App.builder()
                .id(id)
                .appName(appUpdateRequest.getAppName())
                // 设置编辑时间
                .editTime(LocalDateTime.now())
                .build();
        boolean result = this.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public Boolean deleteApp(DeleteRequest deleteRequest, Long userId) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        // 判断是否存在
        Long id = deleteRequest.getId();
        App oldApp = this.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        ThrowUtils.throwIf(!oldApp.getUserId().equals(userId), ErrorCode.NO_AUTH_ERROR);
        return this.removeById(id);
    }

    /**
     * 删除应用时关联删除对话历史
     *
     * @param id 应用ID
     * @return 是否成功
     */
    @Override
    public boolean removeById(Serializable id) {
        if (id == null) {
            return false;
        }
        // 转换为 Long 类型
        Long appId = Long.valueOf(id.toString());
        if (appId <= 0) {
            return false;
        }
        // 先删除关联的对话历史
        try {
            chatHistoryService.deleteByAppId(appId);
        } catch (Exception e) {
            // 记录日志但不阻止应用删除
            log.error("删除应用关联对话历史失败: {}", e.getMessage());
        }
        // 删除应用
        return super.removeById(id);
    }


    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        // 关联查询用户信息
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }

    @Override
    public AppVO getAppVOById(Long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        App app = this.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类（包含用户信息）
        return this.getAppVO(app);
    }

    @Override
    public QueryWrapper<App> getQueryWrapper(AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String genMode = appQueryRequest.getGenMode();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        QueryWrapper<App> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id)
                .like(appName != null, "appName", appName)
                .like(cover != null, "cover", cover)
                .like(initPrompt != null, "initPrompt", initPrompt)
                .eq(codeGenType != null, "codeGenType", codeGenType)
                .eq(genMode != null, "genMode", genMode)
                .eq(deployKey != null, "deployKey", deployKey)
                .eq(priority != null, "priority", priority)
                .eq(userId != null, "userId", userId)
                .orderBy(sortField != null, "ascend".equals(sortOrder), sortField);
        return queryWrapper;
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }

    @Override
    public Page<AppVO> listMyAppVOByPage(AppQueryRequest appQueryRequest, Long userId) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 限制每页最多 20 个
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用");
        long pageNum = appQueryRequest.getPageNum();
        // 只查询当前用户的应用
        appQueryRequest.setUserId(userId);
        QueryWrapper<App> queryWrapper = this.getQueryWrapper(appQueryRequest);
        Page<App> page = this.page(Page.of(pageNum, pageSize), queryWrapper);

        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, page.getTotal());
        // 数据封装
        List<AppVO> appVOList = this.getAppVOList(page.getRecords());
        appVOPage.setRecords(appVOList);
        return appVOPage;
    }

    @Override
    public Page<AppVO> listGoodAppVOByPage(AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 限制每页最多 20 个
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用");
        long pageNum = appQueryRequest.getPageNum();
        // 只查询精选的应用
        appQueryRequest.setPriority(AppConstant.GOOD_APP_PRIORITY);
        QueryWrapper<App> queryWrapper = this.getQueryWrapper(appQueryRequest);
        // 分页查询
        Page<App> appPage = this.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotal());
        List<AppVO> appVOList = this.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return null;
    }

    @Override
    public Boolean deleteAppByAdmin(DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        long id = deleteRequest.getId();
        // 判断是否存在
        App oldApp = this.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        return this.removeById(id);
    }

    @Override
    public void updateAppByAdmin(AppAdminUpdateRequest appAdminUpdateRequest) {
        ThrowUtils.throwIf(appAdminUpdateRequest == null || appAdminUpdateRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        long id = appAdminUpdateRequest.getId();
        // 判断是否存在
        App oldApp = this.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        App app = new App();
        BeanUtil.copyProperties(appAdminUpdateRequest, app);
        // 设置编辑时间
        app.setEditTime(LocalDateTime.now());
        boolean result = this.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public Page<AppVO> listAppVOByPageByAdmin(AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = appQueryRequest.getPageNum();
        long pageSize = appQueryRequest.getPageSize();
        QueryWrapper<App> queryWrapper = this.getQueryWrapper(appQueryRequest);
        Page<App> appPage = this.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotal());
        List<AppVO> appVOList = this.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return appVOPage;
    }

    @Override
    public AppVO getAppVOByIdByAdmin(Long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        App app = this.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        return this.getAppVO(app);
    }


}
