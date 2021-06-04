package com.atguigu.gmall.auth;

import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.common.utils.RsaUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {

    // 别忘了创建D:\\project\rsa目录
	private static final String pubKeyPath = "D:\\日志\\密钥\\rsa.pub";
    private static final String priKeyPath = "D:\\日志\\密钥\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "234");
    }

    @BeforeEach
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "11");
        map.put("username", "liuyan");
        // 生成token
        String token = JwtUtils.generateToken(map, privateKey, 3);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjExIiwidXNlcm5hbWUiOiJsaXV5YW4iLCJleHAiOjE2MjI1MTcwNjh9.BWgmZfptF8r5lvY38byRm6KV1wV8bhNPsAFOHAs1L68G038YeshuTIpthJ3B8La7ghwXnfFHf_Dpv2eYUB-qVHCfjPlXBcCUy7pQXwxZD9iDBi7sYBG3l94RstHMP2KqbobwR_dtd6YVE1aUL16ef5m8Ck5CAuDCxjq2KJPcYtdxPwkzBKwj7iVGyO1cDH9qK-0260yXkG6wBG6DRyqkadrv1c8hqvF35JJuxL6wahcfRst33FM2ypOSLUbYnutz-6ogbJ7Ia37k0msPoBi-z2JXIQXt90lMSRIYODgALMokUWnFspVppZR2MFGZ3oh2yjWTdZgEkRyLBUAIAkaLyQ";
        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }
}