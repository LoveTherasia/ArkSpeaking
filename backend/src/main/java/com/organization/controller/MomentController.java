package com.organization.controller;

import com.organization.pojo.MomentGenerateRequest;
import com.organization.pojo.MomentGenerateResponse;
import com.organization.service.MomentService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//朋友圈相关接口

@Slf4j
@RestController
@RequestMapping("/moment")
public class MomentController {
    @Autowired
    private MomentService momentService;

    @PostMapping("/generate")
    public MomentGenerateResponse generateMoment(@Valid @RequestBody MomentGenerateRequest request){
        log.info("收到朋友圈生成请求:{}",request);
        return momentService.generateMoment(request);
    }
}
