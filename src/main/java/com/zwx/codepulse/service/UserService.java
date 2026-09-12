package com.zwx.codepulse.service;

import com.zwx.codepulse.model.entity.User;
import com.zwx.codepulse.model.vo.LoginUserVO;

/**
 * @author: 张伟旭
 * @Create: 2026-09-12 16:09
 * @description:
 **/

public interface UserService {

    String login(String userAccount, String password);
    String getEncryptPassword(String userPassword);
    LoginUserVO getLoginUserVO(Long userId);
    Long userRegister(String userAccount, String userPassword, String checkPassword);
}
