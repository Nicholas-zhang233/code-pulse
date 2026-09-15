package com.zwx.codepulse.service;

import com.zwx.codepulse.common.DeleteRequest;
import com.zwx.codepulse.model.dto.AppAddRequest;
import com.zwx.codepulse.model.dto.AppUpdateRequest;

/**
 * @author: 张伟旭
 * @Create: 2026-09-15 16:41
 * @description:
 **/
public interface AppService {

    Long createApp(AppAddRequest appAddRequest, Long userId);
    void updateAppName(AppUpdateRequest appUpdateRequest, Long userId);
    Boolean deleteApp(DeleteRequest deleteRequest, Long userId);
}
