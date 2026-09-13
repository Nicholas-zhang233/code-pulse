package com.zwx.codepulse.config;

import cn.dev33.satoken.stp.StpInterface;
import com.zwx.codepulse.exception.ErrorCode;
import com.zwx.codepulse.exception.ThrowUtils;
import com.zwx.codepulse.mapper.UserMapper;
import com.zwx.codepulse.model.entity.User;
import com.zwx.codepulse.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义权限加载接口实现类
 */
@Component
public class StpInterfaceImpl implements StpInterface {

	@Resource
	private UserMapper userMapper;
	/**
	 * 返回一个账号所拥有的权限码集合
	 */
	@Override
	public List<String> getPermissionList(Object loginId, String loginType) {
		List<String> list = new ArrayList<String>();
		return list;
	}

	/**
	 * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
	 */
	@Override
	public List<String> getRoleList(Object loginId, String loginType) {
		List<String> list = new ArrayList<String>();
		User user = userMapper.selectById(loginId.toString());
		ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);
		list.add(user.getUserRole());
		return list;
	}

}