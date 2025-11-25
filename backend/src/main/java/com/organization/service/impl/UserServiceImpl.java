package com.organization.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.organization.mapper.UserMapper;
import com.organization.pojo.User;
import com.organization.service.UserService;
import com.organization.utils.Md5Util;
import com.organization.utils.UsernameCryptoUtil;
// import org.springframework.util.DigestUtils;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;

    @Override
    public User findByUserName(String username) {
        String encUsername = UsernameCryptoUtil.encrypt(username);
        User u = userMapper.findByUserName(encUsername);
        return u;
    }

    @Override
    public void register(String username, String password) {
        String encUsername = UsernameCryptoUtil.encrypt(username);
        String md5Password = Md5Util.code(password);
        userMapper.add(encUsername, md5Password);
    }
}
