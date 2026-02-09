package com.organization.pojo;

import lombok.Data;
import java.time.LocalDateTime;

//用户实体类
@Data
public class User  {
    private Long id;
    private String nickname;
    private String avatar;
    private String signature;
}
