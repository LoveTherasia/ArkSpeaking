package com.organization.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//朋友圈生成响应
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MomentGenerateResponse {
    //生成的朋友圈内容
    private String content;
}
