package com.organization.service.impl;

import com.organization.pojo.Character;
import com.organization.config.FileStorageConfig;
import com.organization.repository.CharacterRepository;
import com.organization.service.CharacterService;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;

//角色管理服务实现层
@Slf4j
@Service
public class CharacterServiceImpl implements CharacterService {
    @Autowired
    private CharacterRepository characterRepository;

    @Autowired
    private FileStorageConfig fileStorageConfig;

    // 核心新增：注入JdbcTemplate，用于执行原生SQL
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 预设角色配置
    private static final Long SKADI_ID = 1L;
    private static final Long AMIYA_ID = 2L;
    private static final String SKADI_NAME = "斯卡蒂";
    private static final String AMIYA_NAME = "阿米娅";

    // 初始化预设角色（删除version字段）
    @PostConstruct
    public void initPresetCharacters() {
        try {
            // 1. 插入斯卡蒂（移除version）
            jdbcTemplate.update("DELETE FROM t_character WHERE char_id = ?", SKADI_ID);
            jdbcTemplate.update(
                    "INSERT INTO t_character (char_id, char_name, avatar_path, prompt_file_path, is_preset) VALUES (?, ?, ?, ?, ?)",
                    SKADI_ID,
                    SKADI_NAME,
                    "/uploads/character/" + SKADI_ID + ".jpg",
                    "uploads/prompt/" + SKADI_ID + ".txt",
                    true
            );
            log.info("预设角色[{}]初始化成功（原生SQL）, ID={}", SKADI_NAME, SKADI_ID);

            // 2. 插入阿米娅（移除version）
            jdbcTemplate.update("DELETE FROM t_character WHERE char_id = ?", AMIYA_ID);
            jdbcTemplate.update(
                    "INSERT INTO t_character (char_id, char_name, avatar_path, prompt_file_path, is_preset) VALUES (?, ?, ?, ?, ?)",
                    AMIYA_ID,
                    AMIYA_NAME,
                    "/uploads/character/" + AMIYA_ID + ".jpg",
                    "uploads/prompt/" + AMIYA_ID + ".txt",
                    true
            );
            log.info("预设角色[{}]初始化成功（原生SQL）, ID={}", AMIYA_NAME, AMIYA_ID);

        } catch (Exception e) {
            log.error("预设角色初始化失败", e);
        }
    }

    //获取所有角色列表，按加入顺序排序
    public List<Character> getCharacterList(){
        //按ID升序查询
        return characterRepository.findAll((root,query,cb) -> query.orderBy(cb.asc(root.get("id"))).getRestriction());
    }

    //通过ID获取角色信息
    public Character getCharacterById(Long id){
        Optional<Character> character = characterRepository.findById(id);
        return character.orElse(null);
    }

    //增加用户自定义角色
    public Character createCustomCharacter(String name, MultipartFile avatarFile, String promptContent) throws IOException {
        if(name == null || name.trim().isEmpty()){
            throw new IllegalArgumentException("角色名称不能为空");
        }
        if(avatarFile == null || avatarFile.isEmpty()){
            throw new IllegalArgumentException("角色立绘不能为空");
        }
        if(promptContent == null || promptContent.trim().isEmpty()){
            throw new IllegalArgumentException("角色提示词不能为空");
        }

        //检验头像文件类型
        String contentType = avatarFile.getContentType();
        if(!fileStorageConfig.getAllowedAvatarTypes().contains(contentType)){
            throw new IllegalArgumentException("不支持的头像类型" + contentType + "，仅支持" + fileStorageConfig.getAllowedAvatarTypes());
        }

        //构建角色基本信息
        Character newCharacter = new  Character();
        newCharacter.setName(name);
        newCharacter.setPreset(false);

        //保存角色到数据库中
        Character savedCharacter = characterRepository.save(newCharacter);
        Long charId = savedCharacter.getId();
        log.info("新增自定义角色，生成数字ID:{}，角色名称:{}",charId,name);

        //保存头像文件
        String avatarExt = getFileExtension(avatarFile.getOriginalFilename());
        String avatarFileName = charId + "." +  avatarExt;
        String avatarSavePath = fileStorageConfig.getAvatarPath() + File.separator + avatarFileName;
        Path avatarPath = Paths.get(avatarSavePath);
        Files.write(avatarPath, avatarFile.getBytes());
        //更新头像路径
        savedCharacter.setAvatarPath("/uploads/character/" + avatarFileName);

        //保存提示词文件
        String promptFileName = charId + "." + "txt";
        String promptSavePath = fileStorageConfig.getPromptPath() + File.separator + promptFileName;
        try(FileWriter writer = new FileWriter(promptSavePath)){
            writer.write(promptContent);
        }
        //更新提示词路径
        savedCharacter.setPromptFilePath(promptSavePath);

        return characterRepository.save(savedCharacter);
    }

    //删除自定义角色
    public boolean deleteCharacter(Long Id){
        //先检验角色是否存在
        Character character = getCharacterById(Id);
        if(character == null){
            log.warn("删除角色失败,ID={}不存在",Id);
            return false;
        }

        //检验角色是否是预设角色
        if(character.isPreset()){
            throw new IllegalArgumentException("预设角色(ID=" + Id + ")不可删除");
        }

        //删除本地存储文件
        //删除头像
        String avatarRealPath = fileStorageConfig.getAvatarPath() + File.separator + Id + "." + getFileExtension(character.getAvatarPath());
        File avatarFile = new File(avatarRealPath);
        if(avatarFile.exists() && avatarFile.delete()){
            log.info("删除角色头像文件：{}",avatarRealPath);
        }

        //删除角色提示词
        String promptRealPath = fileStorageConfig.getPromptPath() + File.separator + Id + "." + "txt";
        File promptFile = new File(promptRealPath);
        if(promptFile.exists() && promptFile.delete()){
            log.info("删除角色提示词文件:{}",promptRealPath);
        }

        //从数据库中删除角色
        characterRepository.deleteById(Id);
        log.info("删除自定义角色成功,ID:{}",Id);
        return true;
    }

    //读取角色提示词
    public String getCharacterPromptById(Long Id)throws IOException{
        Character character = getCharacterById(Id);
        if(character == null){
            return "";
        }

        //读取本地提示词文件
        String promptRealPath = fileStorageConfig.getPromptPath() + File.separator + Id + "." + "txt";
        File promptFile = new File(promptRealPath);
        if(!promptFile.exists()){
            log.warn("角色提示词文件不存在，ID={},路径={}",Id,promptRealPath);
            return "";
        }
        return new String(Files.readAllBytes(Paths.get(promptRealPath)));
    }

    //获取文件后缀方法
    private String getFileExtension(String fileName){
        if(fileName == null || !fileName.contains(".")){
            return ".jpg";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}
