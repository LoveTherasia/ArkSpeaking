package com.organization;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {

    //测试能否正常生成JWT令牌
    @Test
    public void generateJwt(){
        Map<String,Object> map = new HashMap<>();
        map.put("username","admin");
        map.put("password","123456");

        String token = JWT.create()
                .withClaim("user",map)//添加载荷
                .withExpiresAt(new Date(System.currentTimeMillis()+1000 * 60 * 60 * 12))//添加过期时间
                .sign(Algorithm.HMAC256("123456"));//指定算法

        System.out.println(token);
    }

    //测试能否正常检测令牌合理性
    @Test
    public void testParse(){
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" +
                ".eyJ1c2VyIjp7InBhc3N3b3JkIjoiMTIzNDU2IiwidXNlcm5hbWUiOiJhZG1pbiJ9LCJleHAiOjE3NjM1Nzg1MTZ9" +
                ".nzZ47K3dQKLcYfpv_K_veL8ZA_893OC6gdcALmM7OKM";

        JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256("123456")).build();

        DecodedJWT decodedJWT = jwtVerifier.verify(token);//验证token，生成一个解析后的对象
        Map<String, Claim> claims = decodedJWT.getClaims();
        System.out.println(claims);
    }
}
