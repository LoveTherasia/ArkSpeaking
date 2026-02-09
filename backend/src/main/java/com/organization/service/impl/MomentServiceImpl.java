package com.organization.service.impl;

import com.organization.pojo.MomentGenerateRequest;
import com.organization.pojo.MomentGenerateResponse;
import com.organization.service.MomentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

//朋友圈服务实现类
@Slf4j
@Service
public class MomentServiceImpl implements MomentService {
    //模拟不同风格的朋友圈模板
    private static final List<String> MOMENT_TEMPLATES = new ArrayList<>();

    // 初始化模板
    static {
        MOMENT_TEMPLATES.add("%s：今天的天气真好，适合出门走走～");
        MOMENT_TEMPLATES.add("%s：刚完成了一个小目标，开心！✨");
        MOMENT_TEMPLATES.add("%s：偶尔摆烂也是一种生活态度😜");
        MOMENT_TEMPLATES.add("%s：最近在研究%s，收获满满！");
        MOMENT_TEMPLATES.add("%s：有没有小伙伴一起打卡%s？");
        MOMENT_TEMPLATES.add("%s：人生就像一场旅行，重要的不是目的地，而是沿途的风景～");
        MOMENT_TEMPLATES.add("%s：今日份小确幸：%s");
    }

    @Override
    public MomentGenerateResponse generateMoment(MomentGenerateRequest request) {
        log.info("开始生成朋友圈内容,角色信息:{}",request);

        //获取角色信息
        String characterName = request.getCharacterName();
        String characterBrief = request.getCharacterBrief() == null ? "日常小事" :  request.getCharacterBrief();

        //随机选择模板并填充内容
        Random random = new Random();
        int templateIndex = random.nextInt(MOMENT_TEMPLATES.size());
        String template = MOMENT_TEMPLATES.get(templateIndex);

        String content;
        //根据模板参数数量填充
        if(template.contains("%s") && template.indexOf("%s") != template.lastIndexOf("%s")){
            content = String.format(template,characterName,characterBrief);
        }else{
            content = String.format(template,characterName);
        }

        log.info("朋友圈生成内容成功,{}",content);

        return new MomentGenerateResponse(content);
    }
}
