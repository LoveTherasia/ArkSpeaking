package com.organization.pojo;

import lombok.Data;

import java.util.Optional;

//干员信息实体类,优先在本地里寻找信息，如果没有就尝试在prts里面爬取
@Data
public class CharacterInformation {
//    name: str  # 干员名称
//    profession: str  # 职业
//    camp: str  # 阵营
//    archive: Optional[str]  # 档案文本
//    error: Optional[str] = None  # 错误信息

    private String name;
    private String profession;
    private String camp;
    private String[] file_data;
    private String experience;
    private String level_up;
    private String error;
}
