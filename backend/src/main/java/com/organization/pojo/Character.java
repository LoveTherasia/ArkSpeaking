package com.organization.pojo;

import jakarta.persistence.Entity;
import jakarta.persistence.*;
import lombok.Data;

//角色实体类
@Entity
@Table(name = "t_character")
@Data
public class Character {
    //角色ID
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "char_id", nullable = false, updatable = true)
    private Long id;

    //角色名称
    @Column(name = "char_name" , nullable = false)
    private String name;

    //头像访问路径
    @Column(name = "avatar_path")
    private String avatarPath;

    //提示词文件访问路径
    @Column(name = "prompt_file_path")
    private String promptFilePath;

    //是否为预设角色
    @Column(name = "is_preset", nullable = false)
    private boolean isPreset;
}
