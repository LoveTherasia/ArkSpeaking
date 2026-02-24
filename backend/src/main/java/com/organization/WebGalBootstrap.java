package com.organization;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class WebGalBootstrap implements org.springframework.boot.CommandLineRunner {

    private Process webGalProcess;

    @Override
    public void run(String... args) throws Exception {
        // 1. 获取SpringBoot工作目录（ArkSpeaking/backend）
        String backendDir = System.getProperty("user.dir");
        log.info("SpringBoot工作目录：{}", backendDir);

        // 2. 拼接WebGAL相对路径（ArkSpeaking/Galgame/WebGAL）
        File webGalRootDir = new File(new File(backendDir).getParent(), "Galgame/WebGAL");
        String webGalPath = webGalRootDir.getCanonicalPath();
        log.info("WebGAL根目录：{}", webGalPath);

        // 3. 校验目录是否存在
        if (!webGalRootDir.exists() || !webGalRootDir.isDirectory()) {
            log.error("❌ WebGAL目录不存在！路径：{}", webGalPath);
            return;
        }

        // 4. 构建启动命令（关键：取消后台运行，显示窗口）
        List<String> cmd = new ArrayList<>();
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            // Windows：显式弹出CMD窗口，执行yarn dev
            cmd.add("cmd.exe");
            cmd.add("/c");
            // start cmd /k 强制弹出新窗口并保持窗口不关闭
            cmd.add("start cmd /k yarn dev");
        } else {
            // Linux/Mac：在终端执行，不后台
            cmd.add("/bin/bash");
            cmd.add("-c");
            cmd.add("cd " + webGalPath + " && yarn dev");
        }

        // 5. 启动进程
        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        processBuilder.directory(webGalRootDir);
        processBuilder.redirectErrorStream(true); // 合并错误输出到标准输出

        try {
            log.info("🚀 开始启动WebGAL，执行命令：{}", String.join(" ", cmd));
            webGalProcess = processBuilder.start();

            // 6. 实时读取并打印WebGAL输出（关键：看启动日志）
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(webGalProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("📢 WebGAL输出：{}", line);
                        // 识别启动成功的关键词（根据WebGAL实际日志调整）
                        if (line.contains("VITE v") || line.contains("Local: http://") || line.contains("ready in")) {
                            log.info("✅ WebGAL启动成功！访问地址：http://localhost:3000");
                        }
                    }
                } catch (IOException e) {
                    log.error("❌ 读取WebGAL输出失败", e);
                }
            }, "WebGAL-Output-Thread").start();

            // 7. 等待进程启动（可选，避免SpringBoot先启动完）
            new Thread(() -> {
                try {
                    int exitCode = webGalProcess.waitFor();
                    log.info("WebGAL进程退出，退出码：{}", exitCode);
                    if (exitCode != 0) {
                        log.error("❌ WebGAL启动失败，退出码非0：{}", exitCode);
                    }
                } catch (InterruptedException e) {
                    log.error("❌ WebGAL进程等待被中断", e);
                    Thread.currentThread().interrupt();
                }
            }, "WebGAL-Wait-Thread").start();

        } catch (IOException e) {
            log.error("❌ 启动WebGAL进程失败", e);
        }
    }

    // Spring关闭时停止WebGAL进程
    @PreDestroy
    public void stopWebGal() {
        if (webGalProcess != null && webGalProcess.isAlive()) {
            log.info("🛑 停止WebGAL进程...");
            webGalProcess.destroy();
            // Windows额外杀死cmd窗口（可选）
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                try {
                    Runtime.getRuntime().exec("taskkill /f /im cmd.exe /fi \"WINDOWTITLE eq yarn dev*\"");
                } catch (IOException e) {
                    log.warn("🛑 关闭WebGAL的CMD窗口失败", e);
                }
            }
            log.info("🛑 WebGAL进程已停止");
        }
    }
}