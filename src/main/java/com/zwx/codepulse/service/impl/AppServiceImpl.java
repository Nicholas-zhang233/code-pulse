package com.zwx.codepulse.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zwx.codepulse.common.DeleteRequest;
import com.zwx.codepulse.exception.ErrorCode;
import com.zwx.codepulse.exception.ThrowUtils;
import com.zwx.codepulse.mapper.AppMapper;
import com.zwx.codepulse.mapper.UserMapper;
import com.zwx.codepulse.model.dto.AppAddRequest;
import com.zwx.codepulse.model.dto.AppUpdateRequest;
import com.zwx.codepulse.model.entity.App;
import com.zwx.codepulse.model.entity.User;
import com.zwx.codepulse.model.enums.CodeGenTypeEnum;
import com.zwx.codepulse.service.AppService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

/**
 * @author: 张伟旭
 * @Create: 2026-09-15 16:42
 * @description:
 **/
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {


    @Override
    public Long createApp(@Validated AppAddRequest appAddRequest, Long userId) {
        String initPrompt = appAddRequest.getInitPrompt();
        App app = App.builder()
                .userId(userId)
                .initPrompt(initPrompt)
                // 暂时设置为多文件生成
                .codeGenType(CodeGenTypeEnum.MULTI_FILE.getValue())
                // 应用名称暂时为 initPrompt 前 12 位
                .appName(initPrompt.substring(0, Math.min(initPrompt.length(), 12)))
                .build();
        // 插入数据库
        boolean result = this.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
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
}
