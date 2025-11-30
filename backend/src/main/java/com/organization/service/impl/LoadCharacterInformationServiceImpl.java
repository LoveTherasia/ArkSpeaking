package com.organization.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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
import java.util.concurrent.TimeUnit;

@Service
public class LoadCharacterInformationServiceImpl implements LoadCharacterInformationService {
    private static final String PYTHON_SCRIPT_ABSOLUTE_PATH = "E:\\ArkSpeaking\\backend\\src\\main\\resources\\python\\init.py";
    private static final String PYTHON_INTERPRETER_PATH = "E:\\ArkSpeaking\\backend\\.venv\\Scripts\\python.exe";

    private static final String JSON_START = "###JSON_START###";
    private static final String JSON_END = "###JSON_END###";

    private CharacterInformation characterInformation;

    @Override
    public CharacterInformation loadCharacterInformation(String characterName) {
        System.out.println("characterName:" + characterName);

        File pythonScriptFile = new File(PYTHON_SCRIPT_ABSOLUTE_PATH);
        CharacterInformation info = new CharacterInformation();

        try {
            if (!pythonScriptFile.exists()) {
                throw new IOException("Python脚本文件不存在，请检查路径：" + PYTHON_SCRIPT_ABSOLUTE_PATH);
            }
            if (!pythonScriptFile.isFile()) {
                throw new IOException("指定路径不是有效文件：" + PYTHON_SCRIPT_ABSOLUTE_PATH);
            }

            // 构建Python调用命令
            List<String> command = new ArrayList<>();
            command.add(PYTHON_INTERPRETER_PATH);       // Python解释器绝对路径
            command.add(pythonScriptFile.getAbsolutePath()); // 脚本绝对路径
            command.add(characterName);                 // 脚本参数

            //运行python脚本
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.environment().put("PYTHONIOENCODING", "UTF-8");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // 读取Python脚本输出
            String jsonstr = null;

            try(BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while((line = reader.readLine()) != null){
                    System.out.println(line);

                    if(line.contains(JSON_START) && line.contains(JSON_END)){
                        int startIndex = line.indexOf(JSON_START) +  JSON_START.length();
                        int endIndex = line.indexOf(JSON_END);
                        jsonstr = line.substring(startIndex, endIndex);
                        break;
                    }
                }
            }

            // 等待执行完成并检查退出码
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Python脚本执行失败，exit code：" + exitCode);
            }

            if(jsonstr != null){
                JSONObject jsonObject = JSON.parseObject(jsonstr);

                if(jsonObject.getString("error") != null){
                    info.setError(jsonObject.getString("error"));
                }

                info.setName(jsonObject.getString("name"));
                info.setProfession(jsonObject.getString("class"));
                info.setCamp(jsonObject.getString("group"));
                info.setExperience(jsonObject.getString("experience"));
                info.setLevel_up(jsonObject.getString("level_up"));

                JSONObject fileDataJson = jsonObject.getJSONObject("file_data");
                String[] fileData = new String[4];
                for(int i=0;i<4;i++)
                {
                    fileData[i] = fileDataJson.getString(String.valueOf(i));
                }
                info.setFile_data(fileData);
            }else {
                info.setError("未获取到python返回的json数据");
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("调用Python脚本异常：" + e.getMessage());
            e.printStackTrace(); // 可选：打印完整堆栈，方便定位问题
        }

        return info;
    }
}