// /backend/src/main/java/com/organization/config/FileStorageConfig.java
package com.organization.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Paths;
import java.io.File;

@Data
@Configuration
public class FileStorageConfig {
    // 前端public根目录（基于backend项目根目录的相对路径）
    @Value("${frontend.public.path:../frontend/public}")
    private String frontendPublicPath;

    // 允许的头像类型
    private final String[] allowedAvatarTypes = {"image/jpg", "image/jpeg", "image/png"};

    // 核心修复：获取绝对路径（基于后端项目根目录）
    private String getAbsoluteFrontendPublicPath() {
        // 获取后端项目根目录（无论运行目录是哪里，都能定位到backend根目录）
        String backendRootPath = System.getProperty("user.dir");
        // 拼接前端public目录的绝对路径
        File frontendPublicDir = new File(backendRootPath, frontendPublicPath);
        // 转换为标准化的绝对路径（解决../解析问题）
        return frontendPublicDir.getAbsolutePath();
    }

    // 核心修改：使用绝对路径拼接uploads/character
    public String getAvatarPath() {
        String absoluteFrontendPath = getAbsoluteFrontendPublicPath();
        return Paths.get(absoluteFrontendPath, "uploads", "character").toString();
    }

    // 核心修改：使用绝对路径拼接uploads/prompt
    public String getPromptPath() {
        String absoluteFrontendPath = getAbsoluteFrontendPublicPath();
        return Paths.get(absoluteFrontendPath, "uploads", "prompt").toString();
    }

    public String[] getAllowedAvatarTypes() {
        return allowedAvatarTypes;
    }
}