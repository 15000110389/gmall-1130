package com.atguigu.gmall.cart.interceptor;

import com.atguigu.gmall.cart.config.JwtProperties;
import com.atguigu.gmall.cart.pojo.UserInfo;
import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;

@Component
@Data
//@Scope("prototype")
@EnableConfigurationProperties(JwtProperties.class)
public class LoginInterceptor implements HandlerInterceptor{
    @Resource
    private JwtProperties jwtProperties;
    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        THREAD_LOCAL.set(new UserInfo(1l,"45656161"));
        System.out.println("前置方法");
        String token = CookieUtils.getCookieValue(request, jwtProperties.getCookieName());
        String userKey = CookieUtils.getCookieValue(request, jwtProperties.getUserKey());
        if (StringUtils.isEmpty(userKey)){
            userKey= UUID.randomUUID().toString();
            CookieUtils.setCookie(request, response,jwtProperties.getUserKey(),userKey,jwtProperties.getExpire());
        }
        UserInfo userInfo = new UserInfo(null, userKey);

        try {
            if (StringUtils.isNotEmpty(token)){
            Map<String, Object> map = JwtUtils.getInfoFromToken(token, jwtProperties.getPublicKey());
            userInfo.setUserId(Long.valueOf(map.get("userId").toString()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        THREAD_LOCAL.set(userInfo);
        return true;
    }

    public static UserInfo getUserInfo(){
        return THREAD_LOCAL.get();
    }
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        System.out.println("后置方法");
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        THREAD_LOCAL.remove();
        System.out.println("结束方法");
    }
}
