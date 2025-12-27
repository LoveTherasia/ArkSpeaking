package com.organization.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.organization.pojo.CharacterInformation;
import com.organization.service.LoadCharacterInformationService;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class LoadCharacterInformationServiceImpl implements LoadCharacterInformationService {
    //基础配置
    private static final Path ROOT_PATH = Paths.get("").toAbsolutePath();

    //python脚本路径
    private static final String PYTHON_SCRIPT_PATH = "src" + File.separator + "main" + File.separator + "resources" + File.separator + "python" + File.separator + "init.py";
    //python解释器路径
    private static final String PYTHON_INTERPRETER_PATH = ".venv" + File.separator + "Scripts" + File.separator + "python.exe";
    //角色数据路径
    private static final String CHARACTER_DATA_PATH = "information";
    //角色好感度路径
    private static final String FAVOR_PATH = "src" + File.separator + "main" + File.separator + "resources" + File.separator + "python" + File.separator + "favor";

    //AI回复分隔符
    private static final String JSON_START = "###JSON_START###";
    private static final String JSON_END = "###JSON_END###";

    private CharacterInformation characterInformation;

    //获取项目根目录地址
    private String getProjectRootPath(){
        return ROOT_PATH.toString();
    }

    //拼接路径
    private String getAbsolutePath(String relativePath){
        return getProjectRootPath()+ File.separator + relativePath;
    }

    // 从指定文件读取指定键名的信息
    private String getCharacterInfoFromFile(String filePath, String key) {
        // 空值校验
        if (filePath == null || filePath.isEmpty() || key == null || key.isEmpty()) {
            System.err.println("文件路径或键名不能为空");
            return null;
        }

        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            System.err.println("文件不存在或不是有效文件：" + filePath);
            return null;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))) {

            // 读取文件内容
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }

            // 解析JSON并获取指定键的值
            JSONObject jsonObject = JSON.parseObject(stringBuilder.toString());
            return jsonObject.getString(key);

        } catch (Exception e) {
            System.err.println("读取文件[" + filePath + "]的[" + key + "]键失败：" + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // 读取嵌套JSON对象的指定键（适配file_data）
    private String getNestedCharacterInfoFromFile(String filePath, String parentKey, String childKey) {
        String parentJsonStr = getCharacterInfoFromFile(filePath, parentKey);
        if (parentJsonStr == null) {
            return null;
        }

        try {
            JSONObject parentJson = JSON.parseObject(parentJsonStr);
            return parentJson.getString(childKey);
        } catch (Exception e) {
            System.err.println("解析嵌套JSON[" + parentKey + "." + childKey + "]失败：" + e.getMessage());
            return null;
        }
    }

    // 加载角色信息，优先尝试从本地加载，如果本地加载不到就先尝试爬取prts上的信息，然后保存到本地上
    @Override
    public CharacterInformation loadCharacterInformation(String characterId, String characterName) {
        // -----------------------------动态拼接文件路径-------------------- //
        // 角色数据存储目录
        String characterDir = getAbsolutePath(CHARACTER_DATA_PATH);
        //python脚本路径
        String pythonScriptPath = getAbsolutePath(PYTHON_SCRIPT_PATH);
        //python解释器路径
        String pythonInterpreterPath = getAbsolutePath(PYTHON_INTERPRETER_PATH);

        // 首先检查本地角色数据目录是否存在,不存在则尝试创建
        File dataDir = new File(characterDir);
        if(!dataDir.exists()){
            boolean mkdirSuccess = dataDir.mkdirs();
            if(!mkdirSuccess){
                CharacterInformation errorInfo = new CharacterInformation();
                errorInfo.setError("创建本地角色数据目录失败" + characterDir);
                return errorInfo;
            }
        }

        //构建本地角色数据文件路径
        String characterFilePath = characterDir + File.separator + characterName + ".json";
        String FavorPath = getAbsolutePath(FAVOR_PATH) +  File.separator + characterId + ".json";

        File localFile = new File(characterFilePath);

        //尝试从本地文件获取角色数据信息
        if(localFile.exists() &&  localFile.isFile()){
            return loadFromLocalFile(characterFilePath, characterName,FavorPath);
        }

        // 本地文件不存在，尝试调用python的方法获取
        CharacterInformation crawlInfo = crawlByPythonScript(characterName,pythonScriptPath,pythonInterpreterPath);

        //爬取成功，尝试将角色信息保存到本地
        boolean saveSuccess = saveToLocalFile(crawlInfo,characterFilePath);
        if(!saveSuccess){
            System.out.println("角色信息爬取成功，但保存到本地失败" + characterFilePath);
        }

        return crawlInfo;
    }

    //从本地文件加载角色信息
    private CharacterInformation loadFromLocalFile(String characterFilePath, String characterName,String FavorPath){
        CharacterInformation info = new CharacterInformation();

        try {
            // 调用抽取的函数读取基础信息
            info.setName(getCharacterInfoFromFile(characterFilePath, "name"));
            info.setProfession(getCharacterInfoFromFile(characterFilePath, "class"));
            info.setCamp(getCharacterInfoFromFile(characterFilePath, "group"));
            info.setExperience(getCharacterInfoFromFile(characterFilePath, "experience"));
            info.setLevel_up(getCharacterInfoFromFile(characterFilePath, "level_up"));
            info.setFavor(Integer.parseInt(getCharacterInfoFromFile(FavorPath, "current_favor")));

            System.out.println(info.getName());
            System.out.println(info.getProfession());
            System.out.println(info.getCamp());
            System.out.println(info.getExperience());
            System.out.println(info.getLevel_up());
            System.out.println(info.getFavor());


            // 调用扩展函数读取嵌套的file_data信息
            String[] fileData = new String[4];
            for(int i = 0; i < 4; i++){
                fileData[i] = getNestedCharacterInfoFromFile(characterFilePath, "file_data", String.valueOf(i));
            }
            info.setFile_data(fileData);

            System.out.println("成功加载本地角色" + characterName + "信息");
        } catch (Exception e) {
            info.setError("加载本地角色" + characterName + "信息失败");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return info;
    }

    //使用python加载角色信息
    private CharacterInformation crawlByPythonScript(String characterName, String pythonScriptPath, String pythonInterpreterPath) {
        System.out.println("本地未找到角色[" + characterName + "]文件，开始调用Python脚本爬取...");
        CharacterInformation info = new CharacterInformation();
        File pythonScriptFile = new File(pythonScriptPath);

        try {
            // 检查Python脚本文件是否存在
            if (!pythonScriptFile.exists()) {
                throw new IOException("Python脚本文件不存在，请检查路径：" + pythonScriptPath);
            }
            if (!pythonScriptFile.isFile()) {
                throw new IOException("指定路径不是有效文件：" + pythonScriptPath);
            }

            // 构建Python调用命令
            List<String> command = new ArrayList<>();
            command.add(pythonInterpreterPath);
            command.add(pythonScriptFile.getAbsolutePath());
            command.add(characterName);

            // 运行Python脚本
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.environment().put("PYTHONIOENCODING", "UTF-8");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // 读取Python输出
            String jsonstr = null;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("Python输出：" + line);
                    if (line.contains(JSON_START) && line.contains(JSON_END)) {
                        int startIndex = line.indexOf(JSON_START) + JSON_START.length();
                        int endIndex = line.indexOf(JSON_END);
                        jsonstr = line.substring(startIndex, endIndex);
                        break;
                    }
                }
            }

            // 检查脚本执行退出码
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Python脚本执行失败，exit code：" + exitCode);
            }

            // 解析爬取的JSON数据
            if (jsonstr != null) {
                JSONObject jsonObject = JSON.parseObject(jsonstr);

                // 处理爬取错误
                if (jsonObject.getString("error") != null) {
                    info.setError(jsonObject.getString("error"));
                    return info;
                }

                // 映射数据到实体类
                info.setName(jsonObject.getString("name"));
                info.setProfession(jsonObject.getString("class"));
                info.setCamp(jsonObject.getString("group"));
                info.setExperience(jsonObject.getString("experience"));
                info.setLevel_up(jsonObject.getString("level_up"));

                JSONObject fileDataJson = jsonObject.getJSONObject("file_data");
                if (fileDataJson != null) {
                    String[] fileData = new String[4];
                    for (int i = 0; i < 4; i++) {
                        fileData[i] = fileDataJson.getString(String.valueOf(i));
                    }
                    info.setFile_data(fileData);
                }

                System.out.println("Python脚本爬取角色信息成功：" + characterName);
            } else {
                info.setError("未获取到Python返回的JSON数据");
            }

        } catch (IOException | InterruptedException e) {
            String errorMsg = "调用Python脚本异常：" + e.getMessage();
            info.setError(errorMsg);
            System.err.println(errorMsg);
            e.printStackTrace();
        }

        return info;
    }

    //将角色信息保存到本地文件中
    private boolean saveToLocalFile(CharacterInformation info,String savePath){
        try(BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(Files.newOutputStream(Paths.get(savePath)),StandardCharsets.UTF_8))){

            // 将实体类转换为JSON对象
            JSONObject jsonObject = new  JSONObject();
            jsonObject.put("name", info.getName());
            jsonObject.put("class", info.getProfession());
            jsonObject.put("group", info.getCamp());
            jsonObject.put("experience", info.getExperience());
            jsonObject.put("level_up", info.getLevel_up());
            jsonObject.put("favor", info.getFavor());

            JSONObject fileDataJson = new  JSONObject();
            if (info.getFile_data() != null) {
                for(int i = 0; i < 4; i++){
                    fileDataJson.put(String.valueOf(i), info.getFile_data()[i]);
                }
            }
            jsonObject.put("file_data", fileDataJson); // 补充缺失的file_data写入

            writer.write(JSON.toJSONString(jsonObject,true));
            System.out.println("角色信息已保存到本地");
            return true;
        }catch (IOException e){
            System.out.println("角色信息保存失败!" + e.getMessage());
            return false;
        }
    }
}