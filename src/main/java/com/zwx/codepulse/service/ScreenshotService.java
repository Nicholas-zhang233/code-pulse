package com.zwx.codepulse.service;

/**
 * @author: 张伟旭
 * @Create: 2026-10-01 15:13
 * @description:
 **/
public interface ScreenshotService {
    /**
     * 通用的截图服务，可以得到访问地址
     *
     * @param webUrl 网址
     * @return
     */
    String generateAndUploadScreenshot(String webUrl);
}
