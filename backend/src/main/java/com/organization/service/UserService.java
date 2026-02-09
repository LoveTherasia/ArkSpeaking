package com.organization.service;


import com.organization.pojo.User;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface UserService {
    public String uploadAndSaveAvatar(MultipartFile file) throws IOException;

    public boolean saveUserInfo(User user);

    public User getCurrentInfo();
}
