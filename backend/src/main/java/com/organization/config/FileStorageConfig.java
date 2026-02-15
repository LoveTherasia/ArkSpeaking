package com.organization.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;

//文件存储配置类
@Configuration
@Data
public class FileStorageConfig {

    //头像存储路径
    @Value("${file.storage.avatar-path}")
    private String avatarPath;

    //提示词存储路径
    @Value("${file.storage.prompt-path}")
    private String promptPath;

    //允许的头像文件类型
    @Value("${file.storage.allowed-avatar-types}")
    private String allowedAvatarTypes;

    @PostConstruct
    public void initStorageDir(){
        File avatarDir = new File(avatarPath);
        File promptDir = new File(promptPath);

        if(!avatarDir.exists()){
            avatarDir.mkdirs();
        }

        if(!promptDir.exists()){
            promptDir.mkdirs();
        }
    }
}
