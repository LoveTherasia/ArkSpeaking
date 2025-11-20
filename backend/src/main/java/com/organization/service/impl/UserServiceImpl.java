package com.organization.service.impl;

import com.organization.mapper.UserMapper;
import com.organization.pojo.User;
import com.organization.service.UserService;
import com.organization.utils.Md5Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;

    @Override
    public User findByUserName(String username) {
        User u = userMapper.findByUserName(username);
        return u;
    }

    @Override
    public void register(String username, String password) {
        //加密
        String md5Password = Md5Util.code(password);
        //添加
        userMapper.add(username,md5Password);
    }
}
