package com.organization.controller;

import com.organization.pojo.User;
import com.organization.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.MultipartStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
public class UserController {
    @Autowired
    private UserService userService;

    //上传头像接口
    @PostMapping("/user/upload/avatar")
    public ResponseEntity<Map<String,Object>> uploadAvatar(@RequestParam("avatar") MultipartFile file){
        Map<String,Object> result = new HashMap<>();

        try{
            //调用service层存储头像
            String avatarUrl = userService.uploadAndSaveAvatar(file);
            result.put("success",true);
            result.put("avatarUrl",avatarUrl);
            return ResponseEntity.ok(result);
        }catch(IllegalArgumentException e){
            //参数错误
            result.put("success",false);
            result.put("message",e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }catch(IOException e){
            //文件存储失败
            result.put("success",false);
            result.put("message",e.getMessage());
            log.error("头像上传异常",e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @PostMapping("/user/update")
    public ResponseEntity<Map<String,Object>> updateUserInfo(@RequestBody User user){
        Map<String,Object> result = new HashMap<>();

        try{
            //调用Service存储用户信息到数据库
            boolean isSuccess = userService.saveUserInfo(user);
            result.put("success",isSuccess);
            result.put("message",isSuccess ? "用户信息存储成功" : "用户信息存储失败");
            return ResponseEntity.ok(result);
        }catch(Exception e){
            result.put("success",false);
            result.put("message",e.getMessage());
            log.error("保存用户信息异常",e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @PostMapping("user/info")
    public ResponseEntity<User> getCurrentUserInfo(){
        User user = userService.getCurrentInfo();
        return ResponseEntity.ok(user);
    }
}
