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
    @NotBlank
    @Size(min = 4, max = 16, message = "账号长度在4到16之间")
    private String userAccount;

    /**
     * 密码
     */
    @NotBlank
    @Size(min = 8, max = 16, message = "密码长度在8到16之间")
    private String userPassword;

    /**
     * 确认密码
     */
    @NotBlank
    @Size(min = 8, max = 16, message = "密码长度在8到16之间")
    private String checkPassword;
}