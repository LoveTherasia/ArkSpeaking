package com.organization.pojo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

//朋友圈生成请求类
@Data
public class MomentGenerateRequest {
    //角色ID
    @NotBlank(message = "角色ID不能为空")
    private String characterId;

    //角色名称
    @NotBlank(message = "角色名称不能为空")
    private String characterName;

    //角色简介
    private String characterBrief;
}
