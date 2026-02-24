package com.organization.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

//论文解析&脚本生成核心控制器
@Slf4j
@RestController
@RequestMapping("api/galgame")
public class PaperAnalysisController {
    private static final String PROJECT_ROOT = System.getProperty("user.dir");
    private static final String PARSER_DIR = Paths.get(PROJECT_ROOT).getParent().resolve("Galgame").resolve("core").resolve("parser").toString();
    private static final String SCRIPT_GENERATOR_DIR = Paths.get(PROJECT_ROOT).getParent().resolve("Galgame").resolve("core").resolve("scripts_generator").toString();
    private static final String WEBGAL_SCENE_FILE = Paths.get(PROJECT_ROOT).getParent().resolve("Galgame").resolve("WebGAL").resolve("packages").resolve("webgal").resolve("public").resolve("game").resolve("scene").resolve("zh_cn.txt").toString();
    private static final String PROMPT_GENERATOR = Paths.get(PROJECT_ROOT).getParent().resolve("Galgame").resolve("core").resolve("prompt_generator").resolve("prompt_generator.py").toString();

    //保存角色提示词
    @PostMapping("/character/prompt")
    public ResponseEntity<Map<String,Object>> saveCharacterPrompt(
            @RequestParam("promptContent") String promptContent){
        Map<String,Object> result = new HashMap<>();
        try{
            //角色提示词文件路径
            Path promptFile = Paths.get(SCRIPT_GENERATOR_DIR, "nene.txt");

            //确保目录存在
            Files.createDirectories(promptFile.getParent());
            //保存原始提示词
            Files.write(promptFile,promptContent.getBytes(StandardCharsets.UTF_8));

            //拼接python脚本路径和参数
            //构建cmd执行命令
            String[] cmd = new String[]{
                    "python",
                    "-X","utf-8",
                    PROMPT_GENERATOR,
                    promptFile.toString()
            };

            log.info("执行python命令:{}",String.join(" ",cmd));

            result.put("code",200);
            result.put("message","角色提示词保存成功");
            result.put("data",promptFile);

            return ResponseEntity.ok(result);
        }catch(Exception e){
            log.error("保存角色提示词失败",e);
            result.put("code",500);
            result.put("message","保存失败" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    //上传论文到parser目录
    @PostMapping("/paper/upload")
    public ResponseEntity<Map<String,Object>> uploadPaper(
            @RequestParam("paperFile") MultipartFile file){
        Map<String,Object> result = new HashMap<>();
        try{
            if(file.isEmpty()){
                result.put("code",400);
                result.put("message","上传文件不能为空");
                return ResponseEntity.badRequest().body(result);
            }

            //论文保存路径
            String fileName = System.currentTimeMillis()+"."+file.getOriginalFilename();
            Path paperPath = Paths.get(PARSER_DIR,fileName);

            Files.createDirectories(paperPath.getParent());
            Files.write(paperPath,file.getBytes());

            result.put("code",200);
            result.put("message","论文上传成功");
            result.put("data",paperPath.toString());
            return ResponseEntity.ok(result);
        }catch (Exception e){
            log.error("上传论文失败",e);
            result.put("code",500);
            result.put("message","上传失败:" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    //调用python脚本生成GAL脚本并写入WebGAL
    @PostMapping("/script/generate")
    public ResponseEntity<Map<String,Object>> generateScript(
            @RequestParam("paperPath") String paperPath,
            @RequestParam(value = "promptFile",defaultValue = "nene.txt")String promptFile){
        Map<String,Object> result = new HashMap<>();
        try{
            //拼接python脚本路径和参数
            String pythonScript = Paths.get(SCRIPT_GENERATOR_DIR,"gal_script_generator.py").toString();
            String promptFilePath = Paths.get(SCRIPT_GENERATOR_DIR,promptFile).toString();

            //构建cmd执行命令
            String[] cmd = new String[]{
                    "python",
                    "-X","utf-8",
                    pythonScript,
                    paperPath,
                    promptFilePath
            };

            log.info("执行python命令:{}",String.join(" ",cmd));

            Process process = Runtime.getRuntime().exec(cmd);

            //读取python输出和错误信息
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream(),StandardCharsets.UTF_8));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(),StandardCharsets.UTF_8));

            StringBuilder scriptContent =  new StringBuilder();
            String line;

            while((line = inputReader.readLine()) != null){
                scriptContent.append(line).append("\n");
            }

            StringBuilder errorMsg = new StringBuilder();
            while((line = errorReader.readLine()) != null){
                errorMsg.append(line).append("\n");
            }

            //等待进程执行完成并获取退出码
            int exitCode = process.waitFor();
            log.info("Python脚本执行退出码:{}", exitCode);

            //将生成的脚本写入WebGAL的nene.txt
            Path webGalFile = Paths.get(WEBGAL_SCENE_FILE);
            Files.createDirectories(webGalFile.getParent());
            Files.write(webGalFile,scriptContent.toString().getBytes(StandardCharsets.UTF_8));

            //返回结果 - 包含python输出、错误信息、退出码等
            result.put("code",200);
            result.put("message","GAL脚本生成成功");

            // 构建详细的返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("webGalFilePath", WEBGAL_SCENE_FILE);
            data.put("pythonCommand", String.join(" ", cmd));
            data.put("pythonOutput", scriptContent.toString());  // Python正常输出
            data.put("pythonError", errorMsg.toString());        // Python错误输出
            data.put("exitCode", exitCode);                      // 进程退出码

            result.put("data", data);

            return ResponseEntity.ok(result);
        }catch(Exception e){
            log.error("生成脚本失败",e);
            result.put("code",500);
            result.put("message","生成失败: " + e.getMessage());
            result.put("data", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
}