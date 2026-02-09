package com.organization.service;

import com.organization.pojo.MomentGenerateRequest;
import com.organization.pojo.MomentGenerateResponse;

//朋友圈相关服务接口
public interface MomentService {
    MomentGenerateResponse generateMoment(MomentGenerateRequest momentGenerateRequest);
}
