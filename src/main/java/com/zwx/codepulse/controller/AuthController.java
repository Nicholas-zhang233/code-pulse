package com.zwx.codepulse.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zwx.codepulse.common.BaseResponse;
import com.zwx.codepulse.common.ResultUtils;
import com.zwx.codepulse.exception.ErrorCode;
import com.zwx.codepulse.exception.ThrowUtils;
import com.zwx.codepulse.model.vo.LoginUserVO;
import com.zwx.codepulse.model.vo.UserLoginRequest;
import com.zwx.codepulse.model.vo.UserRegisterRequest;
import com.zwx.codepulse.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * @author: 张伟旭
 * @Create: 2026-09-12 16:03
 * @description:
 **/

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Resource
    private UserService userService;

    /**
     * 用户登录
     * @param userLoginRequest
     * @return 登录结果
     */
    @PostMapping("/login")
    public BaseResponse<String> login(@RequestBody @Validated UserLoginRequest userLoginRequest) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        String token = userService.login(userAccount, userPassword);
        return ResultUtils.success("登录成功", token);
    }

    /**
     * 查询登录状态
     */
    @GetMapping("/isLogin")
    public BaseResponse<Boolean> isLogin() {
        return ResultUtils.success(StpUtil.isLogin());
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public BaseResponse<String> logout() {
        StpUtil.logout();
        return ResultUtils.success("退出成功");
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/userInfo")
    public BaseResponse<LoginUserVO> getUserInfo() {
        // 检查登录状态，未登录会抛出异常
        ThrowUtils.throwIf(!StpUtil.isLogin(), ErrorCode.NOT_LOGIN_ERROR);
        LoginUserVO loginUser = userService.getLoginUserVO(StpUtil.getLoginIdAsLong());
        return ResultUtils.success(loginUser);
    }


    @PostMapping("/register")
    public BaseResponse<String> userRegister(@RequestBody @Validated UserRegisterRequest userRegisterRequest){
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String token = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success("注册成功",token);
    }
}