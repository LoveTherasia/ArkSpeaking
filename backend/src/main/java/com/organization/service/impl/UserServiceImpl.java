package com.organization.service.impl;

import com.organization.pojo.User;
import com.organization.mapper.UserMapper;
import com.organization.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Slf4j//日志注解
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;

    @Value("${avatar.upload.path}")//从yml中获取头像存储路径
    private String avatarUploadPath;

    @Value("${avatar.access.prefix}")//从yml中获取头像访问前缀
    private String avatarAccessPrefix;

    //保存用户头像
    public String uploadAndSaveAvatar(MultipartFile file) throws IOException{
        //检验文件是否为空
        if(file.isEmpty()){
            throw new IllegalArgumentException("上传的头像文件不能为空");
        }

        //检验文件类型
        String originalFilename = file.getOriginalFilename();
        if(originalFilename == null || !originalFilename.matches("^.+\\.(jpg|png|jpeg)$")){
            throw new IllegalArgumentException("仅支持jpg|png|jpeg类型的头像文件");
        }

        //生成唯一的文件名
        String fileExt = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFileName = UUID.randomUUID().toString() + fileExt;

        //自动创建存储目录
        File uploadDir = new File(avatarUploadPath);
        if(!uploadDir.exists()){
            boolean mkdirsSuccess = uploadDir.mkdirs();
            if(!mkdirsSuccess){
                throw new IOException("无法创建头像文件夹，请检查运行权限");
            }
            log.info("头像存储目录创建成功:{}",uploadDir.getAbsolutePath());
        }

        //保存头像文件
        File destFile = new File(uploadDir,uniqueFileName);
        file.transferTo(destFile);
        log.info("头像文件存储成功:{}",destFile.getAbsolutePath());

        //生成头像访问URL
        String avatarUrl = avatarAccessPrefix + uniqueFileName;
        log.info("头像访问URL生成成功:{}",avatarUrl);
        return avatarUrl;
    }

    public boolean saveUserInfo(User user){
        //因为当前是单用户模式，所以让ID=1就可以了
        if(user.getId() == null){
            user.setId(1L);
        }

        //调用Mapper存储到数据库中
        int affectRows = userMapper.updateUserInfo(user);
        boolean isSuccess = affectRows > 0;
        log.info("用户信息存储:{},受影响行数:{}",isSuccess ? "成功" : "失败",affectRows);

        return isSuccess;
    }

    //从数据库中获取用户信息
    public User getCurrentInfo(){
        User user = userMapper.getCurrentUser();
        log.info("从数据库中查询用户信息:{}",user);
        return user;
    }
}
