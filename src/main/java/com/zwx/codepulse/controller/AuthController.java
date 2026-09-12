package com.zwx.codepulse.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zwx.codepulse.common.BaseResponse;
import com.zwx.codepulse.common.ResultUtils;
import com.zwx.codepulse.exception.ErrorCode;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: 张伟旭
 * @Create: 2026-09-12 16:03
 * @description:
 **/

@RestController
@RequestMapping("/auth")
public class AuthController {

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return 登录结果
     */
    @PostMapping("/login")
    public BaseResponse login(@RequestParam String username, @RequestParam String password) {
        // 1. 验证用户名密码（此处仅示例，实际应查询数据库）
        if ("admin".equals(username) && "123456".equals(password)) {
            // 2. 登录成功，为用户创建 Session 并生成 Token
            StpUtil.login(10001);  // 10001 是用户 id

            // 3. 获取 Token 返回给前端
            String token = StpUtil.getTokenValue();

            return ResultUtils.success("登录成功", token);
        }

        return ResultUtils.error(ErrorCode.PARAMS_ERROR,"用户名或密码错误");
    }

    /**
     * 查询登录状态
     */
    @GetMapping("/isLogin")
    public BaseResponse isLogin() {
        boolean isLogin = StpUtil.isLogin();
        return ResultUtils.success("是否登录：" + isLogin);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public BaseResponse logout() {
        StpUtil.logout();
        return ResultUtils.success("退出成功");
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/userInfo")
    public BaseResponse getUserInfo() {
        // 检查登录状态，未登录会抛出异常
        StpUtil.checkLogin();

        // 获取当前登录用户 id
        long userId = StpUtil.getLoginIdAsLong();

        // 根据 userId 查询用户信息（此处省略）
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", userId);
        userInfo.put("username", "admin");

        return ResultUtils.success(userInfo);
    }
}