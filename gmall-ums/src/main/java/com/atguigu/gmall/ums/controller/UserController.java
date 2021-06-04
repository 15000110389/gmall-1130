package com.atguigu.gmall.ums.controller;

import java.util.List;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.ums.service.UserService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.bean.PageParamVo;

/**
 * 用户表
 *
 * @author fengge
 * @email fengge@atguigu.com
 * @date 2021-05-31 14:06:52
 */
@Api(tags = "用户表 管理")
@RestController
@RequestMapping("ums/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("query")
    public ResponseVo<UserEntity> queryUser(@RequestParam("loginName")String loginName ,@RequestParam("password")String password){
            UserEntity user=userService.queryUser(loginName, password);
            return ResponseVo.ok(user);
    }


    /**
     * 用户注册
     * @param user form表单格式
     * @param code 短信验证码
     * @return
     */
    @PostMapping("register")
    public ResponseVo  register(UserEntity user,@RequestParam("code")String code){
        userService.register(user,code);
        return ResponseVo.ok();
    }


    /**
     * #  生成短信验证码
     *
     * **功能说明：**
     *
     * 根据用户输入的手机号，生成随机验证码，长度为6位，纯数字。并且调用短信服务，发送验证码到用户手机。
     * @param phone 手机号
     * @return
     */
    @PostMapping ("code")
    @ApiOperation("验证码")
    public ResponseVo sendMessages(String phone){
        userService.sendMessages(phone);
        return ResponseVo.ok();
    }

    /**
     * #  数据校验
     *
     * **功能说明：**
     *
     * 实现用户数据的校验，主要包括对：手机号、用户名的唯一性校验。
     * @param data 要校验的数据是String
     * @param type 要校验的数据类型：1，用户名；2，手机；3，邮箱
     * @return
     */
    @GetMapping("check/{param}/{type}")
    public ResponseVo<Boolean> checkData(@PathVariable("param") String data, @PathVariable("type") Integer type) {
        Boolean result=userService.checkData(data, type);
        return  ResponseVo.ok(result);
    }
    /**
     * 列表
     */
    @GetMapping
    @ApiOperation("分页查询")
    public ResponseVo<PageResultVo> queryUserByPage(PageParamVo paramVo){
        PageResultVo pageResultVo = userService.queryPage(paramVo);

        return ResponseVo.ok(pageResultVo);
    }


    /**
     * 信息
     */
    @GetMapping("{id}")
    @ApiOperation("详情查询")
    public ResponseVo<UserEntity> queryUserById(@PathVariable("id") Long id){
		UserEntity user = userService.getById(id);

        return ResponseVo.ok(user);
    }

    /**
     * 保存
     */
    @PostMapping
    @ApiOperation("保存")
    public ResponseVo<Object> save(@RequestBody UserEntity user){
		userService.save(user);

        return ResponseVo.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    @ApiOperation("修改")
    public ResponseVo update(@RequestBody UserEntity user){
		userService.updateById(user);

        return ResponseVo.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @ApiOperation("删除")
    public ResponseVo delete(@RequestBody List<Long> ids){
		userService.removeByIds(ids);

        return ResponseVo.ok();
    }

}
