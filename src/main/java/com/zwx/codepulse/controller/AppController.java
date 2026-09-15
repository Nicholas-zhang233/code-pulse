package com.zwx.codepulse.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zwx.codepulse.common.BaseResponse;
import com.zwx.codepulse.common.ResultUtils;
import com.zwx.codepulse.exception.ErrorCode;
import com.zwx.codepulse.exception.ThrowUtils;
import com.zwx.codepulse.model.dto.AppAddRequest;
import com.zwx.codepulse.service.AppService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: 张伟旭
 * @Create: 2026-09-15 16:37
 * @description:
 **/
@RestController
public class AppController {
    @Resource
    private AppService appService;

    /**
     * 创建应用
     *
     * @param appAddRequest 创建应用请求
     * @return 应用 id
     */
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest appAddRequest) {
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = appService.createApp(appAddRequest, StpUtil.getLoginIdAsLong());
        return ResultUtils.success(appId);
    }

}
