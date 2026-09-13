package com.zwx.codepulse.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @author: 张伟旭
 * @Create: 2026-06-13 19:49
 * @description:
 **/
@Data
public class UserAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 用户昵称
     */
    private String userName;
    /**
     * 账号
     */
    private String userAccount;
    /**
     * 用户头像
     */
    private String userAvatar;
    /**
     * 用户简介
     */
    private String userProfile;
    /**
     * 用户角色: user, admin
     */
    private String userRole;
}

