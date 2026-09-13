package com.zwx.codepulse.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zwx.codepulse.model.entity.User;
import com.zwx.codepulse.model.vo.LoginUserVO;
import com.zwx.codepulse.model.vo.UserQueryRequest;
import com.zwx.codepulse.model.vo.UserVO;

import java.util.List;

/**
 * @author: 张伟旭
 * @Create: 2026-09-12 16:09
 * @description:
 **/

public interface UserService extends IService<User> {

    String login(String userAccount, String password);
    String getEncryptPassword(String userPassword);
    LoginUserVO getLoginUserVO(Long userId);
    String userRegister(String userAccount, String userPassword, String checkPassword);
    UserVO getUserVO(User user);
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);
    List<UserVO> getUserVOList(List<User> userList);

}
