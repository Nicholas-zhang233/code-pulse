package com.zwx.codepulse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zwx.codepulse.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}