package com.zwx.codepulse.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zwx.codepulse.exception.BusinessException;
import com.zwx.codepulse.exception.ErrorCode;
import com.zwx.codepulse.exception.ThrowUtils;
import com.zwx.codepulse.mapper.UserMapper;
import com.zwx.codepulse.model.entity.User;
import com.zwx.codepulse.model.enums.UserRoleEnum;
import com.zwx.codepulse.model.vo.LoginUserVO;
import com.zwx.codepulse.model.vo.UserQueryRequest;
import com.zwx.codepulse.model.vo.UserVO;
import com.zwx.codepulse.service.UserService;
import com.zwx.codepulse.tools.UsernameGeneratorTool;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: 张伟旭
 * @Create: 2026-09-12 16:10
 * @description:
 **/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Resource
    private UserMapper userMapper;

    @Override
    public String login(String userAccount, String userPassword) {
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        String encryptPassword = getEncryptPassword(userPassword);
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");

        StpUtil.login(user.getId());
        return StpUtil.getTokenValue();
    }

    @Override
    public String getEncryptPassword(String userPassword) {
        // 盐值，混淆密码
        final String SALT = "CodePulse";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

    @Override
    public LoginUserVO getLoginUserVO(Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR);
        User user = userMapper.selectById(userId);
        LoginUserVO loginUser = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUser);
        return loginUser;
    }

    @Override
    public String userRegister(String userAccount, String userPassword, String checkPassword) {
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, userPassword, checkPassword), ErrorCode.PARAMS_ERROR, "参数为空");
        ThrowUtils.throwIf(!userPassword.equals(checkPassword), ErrorCode.PARAMS_ERROR, "密码不一致");

        Long count = userMapper.selectCount(new QueryWrapper<User>().eq("userAccount", userAccount));
        ThrowUtils.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "账号重复");
        String encryptPassword = getEncryptPassword(userPassword);
        User user = User.builder()
                .userAccount(userAccount)
                .userPassword(encryptPassword)
                .userAvatar("https://picsum.photos")
                .userName(UsernameGeneratorTool.generateNicknameStyle())
                .userRole(UserRoleEnum.USER.getValue()).build();
        int saveResult = userMapper.insert(user);
        if (saveResult < 1) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        }
        System.out.println(user.getId());
        StpUtil.login(user.getId());
        return StpUtil.getTokenValueByLoginId(user.getId());
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper
                .eq(id != null, "id", id)
                .eq(userRole != null, "userRole", userRole)
                .like(userAccount != null && !userAccount.isEmpty(), "userAccount", userAccount)
                .like(userName != null && !userName.isEmpty(), "userName", userName)
                .like(userProfile != null && !userProfile.isEmpty(), "userProfile", userProfile)
                .orderBy(true, "ascend".equals(sortOrder), sortField);
        return queryWrapper;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }
}
