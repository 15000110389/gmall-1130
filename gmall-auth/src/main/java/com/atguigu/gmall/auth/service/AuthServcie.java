package com.atguigu.gmall.auth.service;

import com.atguigu.gmall.auth.config.JwtProperties;
import com.atguigu.gmall.auth.feign.GmallUmsClient;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.UserException;
import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.IPUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.ums.entity.UserEntity;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
@EnableConfigurationProperties(JwtProperties.class)
@Service
public class AuthServcie {
    @Resource
    private GmallUmsClient gmallUmsClient;
    @Resource
    private JwtProperties jwtProperties;
    public void login(String loginName, String password, HttpServletRequest request, HttpServletResponse response){
        ResponseVo<UserEntity> userEntityResponseVo = gmallUmsClient.queryUser(loginName, password);
        UserEntity userEntity = userEntityResponseVo.getData();
        if (userEntity == null) {
            throw new UserException("用户名或密码错误");
        }
        HashMap<String, Object> map = new HashMap<>();
        map.put("userId",userEntity.getId());
        map.put("userName",userEntity.getUsername());
        String ip = IPUtils.getIpAddressAtService(request);
        map.put("ip",ip);
        try {
            String token = JwtUtils.generateToken(map, jwtProperties.getPrivateKey(), jwtProperties.getExpire());
            CookieUtils.setCookie(request,response,jwtProperties.getCookieName(),token,jwtProperties.getExpire()*60);
            CookieUtils.setCookie(request,response,jwtProperties.getUnick(),userEntity.getNickname(),jwtProperties.getExpire()*60);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
