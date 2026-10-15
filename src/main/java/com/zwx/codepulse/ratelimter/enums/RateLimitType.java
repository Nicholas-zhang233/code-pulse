package com.zwx.codepulse.ratelimter.enums;

/**
 * @author: 张伟旭
 * @Create: 2026-10-15 17:37
 * @description:
 **/

public enum RateLimitType {

    /**
     * 接口级别限流
     */
    API,

    /**
     * 用户级别限流
     */
    USER,

    /**
     * IP级别限流
     */
    IP
}
