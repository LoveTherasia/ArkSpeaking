package com.organization.service.impl;

import com.organization.pojo.CharacterInformation;
import com.organization.service.LoadCharacterInformationService;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class LoadCharacterInformationServiceImpl implements LoadCharacterInformationService {
    // ========== 核心修改：替换为Python脚本的绝对路径 ==========
    // 请根据实际环境修改为你的Python脚本绝对路径（示例为Windows路径，Linux请改为/xxx/xxx/init.py）
    private static final String PYTHON_SCRIPT_ABSOLUTE_PATH = "E:\\ArkSpeaking\\backend\\src\\main\\resources\\python\\init.py";
    private static final String PYTHON_INTERPRETER_PATH = "E:\\ArkSpeaking\\backend\\.venv\\Scripts\\python.exe";

    private CharacterInformation characterInformation;

    @Override
    public void loadCharacterInformation(String characterName) {
        System.out.println("characterName:" + characterName);
        // 直接通过绝对路径创建文件对象（不再使用临时文件）
        File pythonScriptFile = new File(PYTHON_SCRIPT_ABSOLUTE_PATH);

        try {
            // ========== 验证脚本文件是否存在 ==========
            if (!pythonScriptFile.exists()) {
                throw new IOException("Python脚本文件不存在，请检查路径：" + PYTHON_SCRIPT_ABSOLUTE_PATH);
            }
            if (!pythonScriptFile.isFile()) {
                throw new IOException("指定路径不是有效文件：" + PYTHON_SCRIPT_ABSOLUTE_PATH);
            }

            // 系统编码适配（保留原有逻辑）
            Charset charset = Charset.defaultCharset();
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                charset = Charset.forName("GBK");
            } else {
                charset = StandardCharsets.UTF_8;
            }

            // 构建Python调用命令（保留原有逻辑，仅替换脚本路径）
            List<String> command = new ArrayList<>();
            command.add(PYTHON_INTERPRETER_PATH);       // Python解释器绝对路径
            command.add(pythonScriptFile.getAbsolutePath()); // 脚本绝对路径
            command.add(characterName);                 // 脚本参数

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.environment().put("PYTHONIOENCODING", "UTF-8");
            processBuilder.redirectErrorStream(true);    // 错误流合并到标准输出
            Process process = processBuilder.start();

            // 读取Python脚本输出（保留原有逻辑）
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("Python脚本输出：" + line);
                }
            }

            // 等待执行完成并检查退出码
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Python脚本执行失败，exit code：" + exitCode);
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("调用Python脚本异常：" + e.getMessage());
            e.printStackTrace(); // 可选：打印完整堆栈，方便定位问题
        }
        // ========== 移除临时文件清理逻辑：因为是绝对路径的真实文件，不能删除 ==========
    }
}