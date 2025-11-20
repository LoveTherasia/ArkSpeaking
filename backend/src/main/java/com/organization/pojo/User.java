package com.organization.pojo;

import lombok.Data;
import java.time.LocalDateTime;

//用户实体类
@Data
public class User  {
    private Integer id;
    private String username;
    private String password;
    private String nickname;
    private String email;
    private String userPic;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;


}
