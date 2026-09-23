package com.zwx.codepulse.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zwx.codepulse.common.DeleteRequest;
import com.zwx.codepulse.model.dto.AppAddRequest;
import com.zwx.codepulse.model.dto.AppUpdateRequest;
import com.zwx.codepulse.model.entity.App;
import com.zwx.codepulse.model.entity.User;
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
    Flux<String> chatToGenCode(Long appId, String message, LoginUserVO loginUser);
    String deployApp(Long appId, Long userId);
}
