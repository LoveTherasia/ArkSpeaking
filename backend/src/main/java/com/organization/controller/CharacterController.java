package com.organization.controller;

import com.organization.pojo.Result;
import com.organization.pojo.Character;
import com.organization.service.CharacterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 角色管理控制器（适配数字ID）
 */
@RestController
@RequestMapping("/api/character")
@Slf4j
public class CharacterController {

    @Autowired
    private CharacterService characterService;

    /**
     * 获取所有角色列表（按加入顺序）
     */
    @GetMapping("/list")
    public Result<List<Character>> getCharacterList() {
        try {
            List<Character> list = characterService.getCharacterList();
            return Result.success("获取角色列表成功", list);
        } catch (Exception e) {
            log.error("获取角色列表失败：{}", e.getMessage());
            return Result.error("获取角色列表失败：" + e.getMessage());
        }
    }

    /**
     * 通过数字ID获取角色信息
     */
    @GetMapping("/{id}")
    public Result<Character> getCharacterById(@PathVariable Long id) {
        try {
            Character character = characterService.getCharacterById(id);
            return Result.success("获取角色信息成功", character);
        } catch (Exception e) {
            log.error("获取角色信息失败：ID={}，原因={}", id, e.getMessage());
            return Result.error("获取角色信息失败：" + e.getMessage());
        }
    }

    /**
     * 通过数字ID读取角色提示词
     */
    @GetMapping("/{id}/prompt")
    public Result<String> getCharacterPromptById(@PathVariable Long id) {
        try {
            String prompt = characterService.getCharacterPromptById(id);
            return Result.success("读取提示词成功", prompt);
        } catch (Exception e) {
            log.error("读取角色提示词失败：ID={}，原因={}", id, e.getMessage());
            return Result.error("读取提示词失败：" + e.getMessage());
        }
    }

    /**
     * 新增自定义角色（无ID入参，后端自增生成）
     */
    @PostMapping("/create")
    public Result<Character> createCustomCharacter(
            @RequestParam String name, // 用户输入的角色名称
            @RequestParam MultipartFile avatarFile, // 用户上传的立绘
            @RequestParam String promptContent // 用户输入的提示词
    ) {
        try {
            Character character = characterService.createCustomCharacter(name, avatarFile, promptContent);
            return Result.success("新增角色成功（ID=" + character.getId() + "）", character);
        } catch (IllegalArgumentException e) {
            log.error("新增角色参数错误：{}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (IOException e) {
            log.error("新增角色文件存储失败：{}", e.getMessage());
            return Result.error("文件存储失败：" + e.getMessage());
        } catch (Exception e) {
            log.error("新增角色失败：{}", e.getMessage());
            return Result.error("新增角色失败：" + e.getMessage());
        }
    }

    /**
     * 删除角色（数字ID）
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteCharacter(@PathVariable Long id) {
        try {
            boolean success = characterService.deleteCharacter(id);
            if (success) {
                return Result.success("删除角色成功（ID=" + id + "）", true);
            } else {
                return Result.error(404, "角色不存在（ID=" + id + "）");
            }
        } catch (IllegalArgumentException e) {
            log.error("删除角色失败：{}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("删除角色失败：ID={}，原因={}", id, e.getMessage());
            return Result.error("删除角色失败：" + e.getMessage());
        }
    }
}