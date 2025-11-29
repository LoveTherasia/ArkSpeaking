package com.organization.service.impl;

import com.organization.pojo.CharacterInformation;
import com.organization.service.LoadCharacterInformationService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Service
public class LoadCharacterInformationServiceImpl implements LoadCharacterInformationService {
    private static final String PYTHON_SCRIPT_CLASSPATH = "python/init.py";
    private static final String PYTHON_INTERPRETER_PATH = "E:\\ArkSpeaking\\backend\\.venv\\Scripts\\python.exe";

    private CharacterInformation characterInformation;

    @Override
    public void loadCharacterInformation(String characterName) {
        System.out.println("characterName:" + characterName);
        // 临时文件：用于存放从JAR中复制出来的Python脚本
        File tempScriptFile = null;
        try {
            //获取resources里的Python脚本
            ClassPathResource resource = new ClassPathResource(PYTHON_SCRIPT_CLASSPATH);
            if (resource.exists()) {
                if (resource.isFile()) {
                    // 开发阶段：直接用本地文件路径
                    tempScriptFile = resource.getFile();
                } else {
                    // 打包后：复制JAR内的脚本到临时文件
                    tempScriptFile = File.createTempFile("init", ".py");
                    tempScriptFile.deleteOnExit(); // 程序退出时删除临时文件
                    try (InputStream inputStream = resource.getInputStream();
                         OutputStream outputStream = Files.newOutputStream(tempScriptFile.toPath())) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }
                }
            } else {
                throw new FileNotFoundException("Python脚本不存在：" + PYTHON_SCRIPT_CLASSPATH);
            }

            Charset charset = Charset.defaultCharset();
            if(System.getProperty("os.name").toLowerCase().contains("win")) {
                charset = Charset.forName("GBK");
            }
            else
            {
                charset = StandardCharsets.UTF_8;
            }

            // ========== 构建调用命令（避免字符串拼接，兼容含空格的参数） ==========
            List<String> command = new ArrayList<>();
            command.add(PYTHON_INTERPRETER_PATH); // Python解释器绝对路径
            command.add(tempScriptFile.getAbsolutePath()); // 脚本绝对路径（临时文件/本地文件）
            command.add(characterName); // 脚本参数

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.environment().put("PYTHONIOENCODING","UTF-8");

            // 重定向错误流到标准输出，统一捕获
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // ========== 捕获脚本输出（含stdout+stderr，定位错误的核心） ==========
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // 打印所有输出（包括错误），方便调试
                    System.out.println("Python脚本输出：" + line);
                }
            }

            // ========== 等待执行完成，检查退出码 ==========
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Python脚本执行失败，exit code：" + exitCode);
            }

        } catch (FileNotFoundException e) {
            System.err.println("脚本文件未找到：" + e.getMessage());
        } catch (IOException | InterruptedException e) {
            System.err.println("调用Python脚本异常：" + e.getMessage());
        } finally {
            // 清理临时文件（可选，deleteOnExit已兜底）
            if (tempScriptFile != null && tempScriptFile.exists()) {
                tempScriptFile.delete();
            }
        }
    }
}