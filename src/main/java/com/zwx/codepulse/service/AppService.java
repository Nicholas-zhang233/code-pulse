package com.zwx.codepulse.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zwx.codepulse.common.DeleteRequest;
import com.zwx.codepulse.langgraph4j.model.WorkflowEvent;
import com.zwx.codepulse.model.dto.AppAddRequest;
import com.zwx.codepulse.model.dto.AppUpdateRequest;
import com.zwx.codepulse.model.entity.App;
import com.zwx.codepulse.model.entity.User;
import com.zwx.codepulse.model.enums.GenModeEnum;
import com.zwx.codepulse.model.vo.AppAdminUpdateRequest;
import com.zwx.codepulse.model.vo.AppQueryRequest;
import com.zwx.codepulse.model.vo.AppVO;
import com.zwx.codepulse.model.vo.LoginUserVO;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * @author: 张伟旭
 * @Create: 2026-09-15 16:41
 * @description:
 **/
public interface AppService {

    Long createApp(AppAddRequest appAddRequest, Long userId);
    void updateAppName(AppUpdateRequest appUpdateRequest, Long userId);
    Boolean deleteApp(DeleteRequest deleteRequest, Long userId);
    AppVO getAppVO(App app);
    AppVO getAppVOById(Long id);
    QueryWrapper<App> getQueryWrapper(AppQueryRequest appQueryRequest);
    List<AppVO> getAppVOList(List<App> appList);
    Page<AppVO> listMyAppVOByPage(AppQueryRequest appQueryRequest, Long userId);
    Page<AppVO> listGoodAppVOByPage(AppQueryRequest appQueryRequest);
    Boolean deleteAppByAdmin(DeleteRequest deleteRequest);
    void updateAppByAdmin(AppAdminUpdateRequest appAdminUpdateRequest);
    Page<AppVO> listAppVOByPageByAdmin(AppQueryRequest appQueryRequest);
    AppVO getAppVOByIdByAdmin(Long id);
    /**
     * 通过对话生成应用代码
     *
     * @param appId 应用 ID
     * @param message 提示词
     * @param loginUser 登录用户
     * @return
     */
    Flux<String> chatToGenCode(Long appId, String message, LoginUserVO loginUser);

    /**
     * 专家模式：通过 LangGraph4j 工作流生成应用代码
     * <p>
     * 输出结构化事件流（step：阶段进度，message：代码正文）
     *
     * @param appId     应用 ID
     * @param message   用户消息
     * @param loginUser 登录用户
     * @return 事件流
     */
    Flux<WorkflowEvent> chatToGenCodeByExpert(Long appId, String message, LoginUserVO loginUser);

    /**
     * 解析应用的生成模式，内部完成应用存在性与归属校验
     *
     * @param appId  应用 ID
     * @param userId 登录用户 ID
     * @return 生成模式，历史数据或非法值统一兜底为一般模式
     */
    GenModeEnum resolveGenMode(Long appId, Long userId);
    /**
     * 应用部署
     *
     * @param appId 应用 ID
     * @param userId 登录用户 ID
     * @return 可访问的部署地址
     */
    String deployApp(Long appId, Long userId);
    /**
     * 异步生成应用截图并更新封面
     *
     * @param appId  应用ID
     * @param appUrl 应用访问URL
     */
    void generateAppScreenshotAsync(Long appId, String appUrl);
}
