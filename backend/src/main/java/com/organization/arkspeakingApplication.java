package com.organization;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 应用入口类
 * 核心功能：自动检测可用端口启动应用，避免固定端口被占用导致启动失败
 */
@SpringBootApplication
public class arkspeakingApplication {

    // 程序入口方法
    public static void main(String[] args) {
        // 1. 获取首选端口（优先级：环境变量APP_PORT > 系统属性server.port > 默认8081）
        int basePort = getBasePort();
        // 2. 从首选端口开始，查找第一个可用端口（最多扫描后续50个端口）
        int selectedPort = findAvailablePort(basePort);

        // 3. 创建Spring Boot应用实例
        SpringApplication app = new SpringApplication(arkspeakingApplication.class);
        // 4. 封装动态获取的端口，设置为应用默认配置（覆盖默认8080端口）
        Map<String, Object> props = new HashMap<>();
        props.put("server.port", selectedPort);
        app.setDefaultProperties(props);

        // 5. 启动应用（传入命令行参数）
        app.run(args);
    }

    /**
     * 获取首选端口：按优先级读取配置，非法配置自动跳过
     * @return 首选端口号（兜底默认8081）
     */
    private static int getBasePort() {
        // 优先级1：读取系统环境变量APP_PORT（适合服务器/容器部署）
        String envPort = System.getenv("APP_PORT");
        if (envPort != null) {
            try {
                return Integer.parseInt(envPort);
            } catch (NumberFormatException ignored) {
                // 若配置为非数字（如"abc"），直接忽略，走下一个优先级
            }
        }

        // 优先级2：读取系统属性server.port（适合本地启动时通过命令行指定，如-Dserver.port=8082）
        String ymlPort = System.getProperty("server.port");
        if (ymlPort != null) {
            try {
                return Integer.parseInt(ymlPort);
            } catch (NumberFormatException ignored) {
                // 非法数字配置忽略，走兜底逻辑
            }
        }

        // 优先级3：兜底默认值8081
        return 8081;
    }

    /**
     * 查找可用端口：从首选端口开始扫描，最多扫描50个
     * @param preferred 首选端口（扫描起始点）
     * @return 第一个可用端口；若50个都被占用，返回0（Spring Boot会自动分配随机端口）
     */
    private static int findAvailablePort(int preferred) {
        // 循环扫描：preferred ~ preferred+49 端口
        for (int port = preferred; port < preferred + 50; port++) {
            if (isPortFree(port)) {
                return port; // 找到可用端口，直接返回
            }
        }
        return 0; // 50个端口均被占用，返回0触发随机端口分配
    }

    /**
     * 检测端口是否空闲：尝试绑定端口，绑定成功则未被占用
     * @param port 要检测的端口号
     * @return true=端口空闲，false=端口已被占用/无权限
     */
    private static boolean isPortFree(int port) {
        // try-with-resources：自动关闭ServerSocket，避免资源泄漏
        try (ServerSocket socket = new ServerSocket(
                port,  // 要检测的端口
                1,    // 连接请求队列长度（仅检测端口，设为1即可）
                InetAddress.getByName("0.0.0.0") // 绑定所有网卡（避免仅检测本地localhost）
        )) {
            socket.setReuseAddress(true); // 允许端口快速重用（避免TIME_WAIT状态导致误判）
            return true; // 绑定成功 → 端口空闲
        } catch (IOException e) {
            // 绑定失败（端口被占用/无权限）→ 端口不可用
            return false;
        }
    }
}