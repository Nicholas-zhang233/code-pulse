package com.zwx.codepulse.service;

import com.zwx.codepulse.model.dto.AppAddRequest;

/**
 * @author: 张伟旭
 * @Create: 2026-09-15 16:41
 * @description:
 **/
public interface AppService {

    Long createApp(AppAddRequest appAddRequest, Long userId);
}
