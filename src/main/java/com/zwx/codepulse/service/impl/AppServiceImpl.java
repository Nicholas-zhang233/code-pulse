package com.zwx.codepulse.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zwx.codepulse.exception.ErrorCode;
import com.zwx.codepulse.exception.ThrowUtils;
import com.zwx.codepulse.mapper.AppMapper;
import com.zwx.codepulse.mapper.UserMapper;
import com.zwx.codepulse.model.dto.AppAddRequest;
import com.zwx.codepulse.model.entity.App;
import com.zwx.codepulse.model.entity.User;
import com.zwx.codepulse.model.enums.CodeGenTypeEnum;
import com.zwx.codepulse.service.AppService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

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
}
