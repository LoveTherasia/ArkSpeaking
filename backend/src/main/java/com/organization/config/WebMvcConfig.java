package com.organization.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. 配置/uploads/** 映射到项目根目录下的uploads文件夹
        // 核心：file: 前缀必须加（表示本地文件系统），路径用绝对路径/正确的相对路径
        String localUploadPath = new File("./uploads").getAbsolutePath() + File.separator;
        registry.addResourceHandler("/uploads/**") // 前端访问的URL前缀
                .addResourceLocations("file:" + localUploadPath) // 本地文件存储路径
                .setCachePeriod(3600); // 可选：设置缓存，提升性能

        // 2. 处理favicon.ico（可选，解决第二个报错）
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/");
    }
}