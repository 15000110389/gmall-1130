package com.atguigu.gmall.order.interceptor;



import com.atguigu.gmall.order.pojo.UserInfo;
import lombok.Data;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@Data
//@Scope("prototype")

public class LoginInterceptor implements HandlerInterceptor{

    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String userId = request.getHeader("userId");
        UserInfo userInfo = new UserInfo(Long.valueOf(userId), null);

        THREAD_LOCAL.set(userInfo);
        return true;
    }

    public static UserInfo getUserInfo(){
        return THREAD_LOCAL.get();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        THREAD_LOCAL.remove();
    }
}
