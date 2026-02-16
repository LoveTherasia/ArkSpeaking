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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 角色管理服务实现层（修复主键冲突、语法错误、路径保存等问题）
 */
@Slf4j
@Service
public class CharacterServiceImpl implements CharacterService {

    @Autowired
    private CharacterRepository characterRepository;

    @Autowired
    private FileStorageConfig fileStorageConfig;

    // 核心新增：注入JdbcTemplate，用于执行原生SQL（更新自增计数器）
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 预设角色配置
    private static final Long SKADI_ID = 1L;
    private static final String SKADI_NAME = "斯卡蒂";

    @PostConstruct
    public void initPresetCharacters() {
        try {
            // 1. 确保目录存在（自动创建uploads/character和uploads/prompt）
            File avatarDir = new File(fileStorageConfig.getAvatarPath());
            File promptDir = new File(fileStorageConfig.getPromptPath());
            if (!avatarDir.exists()) {
                avatarDir.mkdirs(); // 递归创建：uploads/character
            }
            if (!promptDir.exists()) {
                promptDir.mkdirs(); // 递归创建：uploads/prompt
            }

            // 2. 删除已存在的预设角色
            jdbcTemplate.update("DELETE FROM t_character WHERE char_id = ?", SKADI_ID);

            // 3. 插入预设角色（核心修改：访问路径增加uploads）
            jdbcTemplate.update(
                    "INSERT INTO t_character (char_id, char_name, avatar_path, prompt_file_path, is_preset) VALUES (?, ?, ?, ?, ?)",
                    SKADI_ID, SKADI_NAME, "/uploads/character/1.jpg", "/uploads/prompt/1.txt", true // 关键：/uploads/xxx
            );

            // 4. 更新数据库自增计数器（不变）
            jdbcTemplate.update("ALTER TABLE t_character AUTO_INCREMENT = 2;");

            log.info("预设角色初始化成功，目录：头像={}, 提示词={}",
                    avatarDir.getAbsolutePath(), promptDir.getAbsolutePath());
        } catch (Exception e) {
            log.error("预设角色初始化失败", e);
        }
    }

    /**
     * 获取所有角色列表，按ID升序排序
     */
    @Override
    public List<Character> getCharacterList() {
        // 按ID升序查询，保证顺序一致
        return characterRepository.findAll((root, query, cb) -> query.orderBy(cb.asc(root.get("id"))).getRestriction());
    }

    /**
     * 通过ID获取角色信息
     */
    @Override
    public Character getCharacterById(Long id) {
        if (id == null || id <= 0) {
            log.warn("获取角色失败：ID为空或无效");
            return null;
        }
        Optional<Character> character = characterRepository.findById(id);
        return character.orElse(null);
    }

    /**
     * 新增用户自定义角色（修复：语法错误、提示词路径保存为相对路径）
     */
    @Override
    @Transactional // 事务管理：确保文件保存和数据库操作原子性
    public Character createCustomCharacter(String name, MultipartFile avatarFile, String promptContent) throws IOException {
        // 1. 参数校验（不变）
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("角色名称不能为空");
        }
        if (avatarFile == null || avatarFile.isEmpty()) {
            throw new IllegalArgumentException("角色立绘不能为空");
        }
        if (promptContent == null || promptContent.trim().isEmpty()) {
            throw new IllegalArgumentException("角色提示词不能为空");
        }

        // 2. 校验头像文件类型（不变）
        String contentType = avatarFile.getContentType();
        if (!Arrays.asList(fileStorageConfig.getAllowedAvatarTypes()).contains(contentType)) {
            throw new IllegalArgumentException("不支持的头像类型：" + contentType +
                    "，仅支持：" + Arrays.toString(fileStorageConfig.getAllowedAvatarTypes()));
        }

        // 3. 构建角色基础信息（不变）
        Character newCharacter = new Character();
        newCharacter.setName(name);
        newCharacter.setPreset(false);

        // 4. 先保存角色到数据库，获取自增生成的ID（不变）
        Character savedCharacter = characterRepository.save(newCharacter);
        Long charId = savedCharacter.getId();
        log.info("新增自定义角色，生成数字ID:{}，角色名称:{}", charId, name);

        // ====== 核心新增：打印实际的存储路径 ======
        String avatarDir = fileStorageConfig.getAvatarPath();
        String promptDir = fileStorageConfig.getPromptPath();
        log.info("文件存储目录 - 立绘：{}，提示词：{}", avatarDir, promptDir);

        // 5. 保存立绘文件（增加日志）
        String avatarExt = getFileExtension(avatarFile.getOriginalFilename());
        String avatarFileName = charId + "." + avatarExt;
        String avatarSavePath = avatarDir + File.separator + avatarFileName;
        log.info("准备保存立绘文件：{}", avatarSavePath);
        try {
            Path avatarPath = Paths.get(avatarSavePath);
            Files.write(avatarPath, avatarFile.getBytes());
            log.info("立绘文件保存成功：{}", avatarSavePath);
            savedCharacter.setAvatarPath("/uploads/character/" + avatarFileName);
        } catch (Exception e) {
            log.error("立绘文件保存失败：{}", avatarSavePath, e);
            throw new RuntimeException("立绘文件保存失败：" + e.getMessage());
        }

        // 6. 保存提示词文件（增加日志）
        String promptFileName = charId + ".txt";
        String promptSavePath = promptDir + File.separator + promptFileName;
        log.info("准备保存提示词文件：{}", promptSavePath);
        try (FileWriter writer = new FileWriter(promptSavePath)) {
            writer.write(promptContent);
            log.info("提示词文件保存成功：{}", promptSavePath);
            savedCharacter.setPromptFilePath("/uploads/prompt/" + promptFileName);
        } catch (Exception e) {
            log.error("提示词文件保存失败：{}", promptSavePath, e);
            throw new RuntimeException("提示词文件保存失败：" + e.getMessage());
        }
        // 核心修改：提示词访问路径改为 /prompt/xxx.txt
        savedCharacter.setPromptFilePath("/uploads/prompt/" + promptFileName);

        // 7. 再次保存更新后的角色信息（不变）
        return characterRepository.save(savedCharacter);
    }

    /**
     * 删除自定义角色（预设角色不可删除）
     */
    @Override
    @Transactional
    public boolean deleteCharacter(Long id) {
        // 1. 参数校验（不变）
        if (id == null || id <= 0) {
            log.warn("删除角色失败：ID为空或无效");
            return false;
        }

        // 2. 检查角色是否存在（不变）
        Character character = getCharacterById(id);
        if (character == null) {
            log.warn("删除角色失败：ID={} 不存在", id);
            return false;
        }

        // 3. 检查是否为预设角色（预设角色不可删除）（不变）
        if (character.isPreset()) {
            throw new IllegalArgumentException("预设角色(ID=" + id + ")不可删除");
        }

        // 4. 删除前端public/character目录下的头像文件
        String avatarExt = getFileExtension(character.getAvatarPath());
        String avatarRealPath = fileStorageConfig.getAvatarPath() + File.separator + id + "." + avatarExt;
        File avatarFile = new File(avatarRealPath);
        if (avatarFile.exists() && avatarFile.delete()) {
            log.info("删除角色头像文件成功：{}", avatarRealPath);
        } else {
            log.warn("删除角色头像文件失败：{}（文件不存在或删除失败）", avatarRealPath);
        }

        // 5. 删除前端public/prompt目录下的提示词文件
        String promptRealPath = fileStorageConfig.getPromptPath() + File.separator + id + ".txt";
        File promptFile = new File(promptRealPath);
        if (promptFile.exists() && promptFile.delete()) {
            log.info("删除角色提示词文件成功：{}", promptRealPath);
        } else {
            log.warn("删除角色提示词文件失败：{}（文件不存在或删除失败）", promptRealPath);
        }

        // 6. 从数据库删除角色（不变）
        characterRepository.deleteById(id);
        log.info("删除自定义角色成功：ID={}", id);
        return true;
    }


    /**
     * 读取角色提示词内容
     */
    @Override
    public String getCharacterPromptById(Long id) throws IOException {
        // 1. 参数校验（不变）
        if (id == null || id <= 0) {
            log.warn("读取提示词失败：ID为空或无效");
            return "";
        }

        // 2. 检查角色是否存在（不变）
        Character character = getCharacterById(id);
        if (character == null) {
            log.warn("读取提示词失败：ID={} 角色不存在", id);
            return "";
        }

        // 3. 读取前端public/prompt目录下的提示词文件
        String promptRelativePath = character.getPromptFilePath();
        String promptFileName = promptRelativePath.substring(promptRelativePath.lastIndexOf("/") + 1);
        String promptRealPath = fileStorageConfig.getPromptPath() + File.separator + promptFileName;

        File promptFile = new File(promptRealPath);
        if (!promptFile.exists()) {
            log.warn("角色提示词文件不存在：ID={}, 路径={}", id, promptRealPath);
            return "";
        }

        // 4. 读取文件内容并返回（不变）
        return new String(Files.readAllBytes(Paths.get(promptRealPath)));
    }

    /**
     * 修复：获取文件后缀（优化逻辑，避免多余的点、空后缀问题）
     * @param fileName 文件名
     * @return 纯后缀（如 jpg、png、txt）
     */
    private String getFileExtension(String fileName) {
        // 处理空文件名/无后缀情况，默认返回jpg
        if (fileName == null || fileName.trim().isEmpty() || !fileName.contains(".")) {
            return "jpg";
        }
        // 截取后缀并转小写
        String ext = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        // 兜底：如果后缀为空（比如文件名是"test."），返回jpg
        return ext.isEmpty() ? "jpg" : ext;
    }
}