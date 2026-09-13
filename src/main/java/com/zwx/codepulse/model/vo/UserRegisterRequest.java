package com.zwx.codepulse.model.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NonNull;

import java.io.Serializable;

/**
 * @author: 张伟旭
 * @Create: 2026-06-12 21:33
 * @description:
 **/

@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 账号
     */
    @NotBlank(message = "账号不能为空")
    @Size(min = 4, max = 16, message = "账号长度必须在4到16之间")
    private String userAccount;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 16, message = "密码长度必须在6到16之间")
    private String userPassword;

    /**
     * 确认密码
     */
    @NotBlank(message = "确认密码不能为空")
    @Size(min = 6, max = 16, message = "密码长度在6到16之间")
    private String checkPassword;
}