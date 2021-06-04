package com.atguigu.gmall.ums.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.ums.entity.UserEntity;

import java.util.Map;

/**
 * 用户表
 *
 * @author fengge
 * @email fengge@atguigu.com
 * @date 2021-05-31 14:06:52
 */
public interface UserService extends IService<UserEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    Boolean checkData(String data, Integer type);

    void sendMessages(String phone);

    void register(UserEntity user,String code);

    UserEntity queryUser(String loginName, String password);
}

