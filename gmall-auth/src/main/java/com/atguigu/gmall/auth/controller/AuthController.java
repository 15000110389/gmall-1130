package com.atguigu.gmall.auth.controller;

import com.atguigu.gmall.auth.service.AuthServcie;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class AuthController {
    @Resource
    private AuthServcie authServcie;
    @GetMapping("toLogin.html")
    public String toLogin(@RequestParam(value="returnUrl",defaultValue = "http://gmall.com")String returnUrl, Model model){
        model.addAttribute("returnUrl",returnUrl);
        return "login";
    }

    /**
     * 登录方法
     * @return
     */
    @PostMapping("login")
    public String login(
            @RequestParam("returnUrl")String returnUrl,
            @RequestParam("loginName")String loginName,
            @RequestParam("password")String password,
            HttpServletRequest request,
            HttpServletResponse response){
        authServcie.login(loginName, password,request, response);
        return "redirect:"+returnUrl;
    }
}
