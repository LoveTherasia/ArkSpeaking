package com.organization.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.organization.pojo.Result;
import com.organization.pojo.User;
import com.organization.service.UserService;
import com.organization.utils.JwtUtil;
import com.organization.utils.Md5Util;

import jakarta.validation.constraints.Pattern;

@RestController
@RequestMapping("/user")
@Validated
public class UserController {

    @Autowired
    private UserService userService;

    //注册接口
    @PostMapping("/register")
    public Result<?> Register(@Pattern(regexp = "^\\S{3,16}$")String username,@Pattern(regexp = "^\\S{5,16}$") String password) {
        //查询用户名是否被占用
        User u = userService.findByUserName(username);
        if (u != null) {
            //用户名已经被占用了
            return Result.error("用户名已被占用");
        }
        else
        {
            //用户名没有被占用
            userService.register(username,password);
            return Result.success();
        }
    }

    @PostMapping("/login")
    public Result<?> Login(@Pattern(regexp = "^\\S{3,16}$")String username,@Pattern(regexp = "^\\S{5,16}$")String password)
    {
        User loginuser =  userService.findByUserName(username);
        if(loginuser==null){
            return Result.error("用户名不存在！");
        }

        else if(!Md5Util.code(password).equals(loginuser.getPassword())){
            return Result.error("密码错误!");
        }

        else {
            Map<String,Object> map = new HashMap<>();
            map.put("id",loginuser.getId());
            map.put("username",username);
            String token = JwtUtil.onCreate(map);
            return Result.success(token);
        }
    }
}
