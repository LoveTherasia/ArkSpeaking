package com.organization.service;

import com.organization.pojo.User;

public interface UserService {
    //根据用户名查询用户
    User findByUserName(String username);
    //注册功能
    void register(String username, String password);
}
