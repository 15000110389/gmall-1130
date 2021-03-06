package com.atguigu.gmall.ums.service.impl;

import com.atguigu.gmall.ums.entity.UserVerificationCode;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.javassist.expr.NewArray;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.ums.mapper.UserMapper;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.ums.service.UserService;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;


@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {
    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<UserEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<UserEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public Boolean checkData(String data, Integer type) {
        QueryWrapper<UserEntity> wrapper = new QueryWrapper<>();
        switch (type) {
            case 1 :
                wrapper.eq("username",data);
                break;
            case 2 :
                wrapper.eq("phone",data);
                break;
            case 3 :
                wrapper.eq("email",data);
                break;
            default :
                return null;
        }
        return this.count()==0;
    }

    @Override
    public void sendMessages(String phone) {
        System.out.println("????????????ing");
        rabbitTemplate.convertAndSend("UMS_USER_EXCHANGE","user.VerificationCode", phone.toString());
        System.out.println("??????????????????");
    }

    @Override
    public void register(UserEntity user,String code) {
        String phone = user.getPhone();
        String PhoneVerificationCode= stringRedisTemplate.opsForValue().get(phone);
        if (!StringUtils.equals(PhoneVerificationCode,code)){
            throw new RuntimeException("?????????????????????");
        }
        // TODO???1.??????????????? ??????redis????????????code??????

        // 2. ?????????
        String uuid = UUID.randomUUID().toString();
        String salt = StringUtils.substring(uuid, 0, 6);
        user.setSalt(salt);

        // 3. ????????????
        user.setPassword(DigestUtils.md5Hex(user.getPassword() + salt));

        // 4. ??????????????????
        user.setLevelId(1l);
        user.setSourceType(1);
        user.setIntegration(1000);
        user.setGrowth(1000);
        user.setStatus(1);
        user.setCreateTime(new Date());
        user.setNickname(user.getUsername());
        this.save(user);

        // TODO: ??????redis????????????
    }

    @Override
    public UserEntity queryUser(String loginName, String password) {
        // ??????????????????????????????
        List<UserEntity> userEntities = this.list(new QueryWrapper<UserEntity>()
                .eq("username", loginName)
                .or().eq("phone", loginName)
                .or().eq("email", loginName));

        // ????????????????????????
        if (CollectionUtils.isEmpty(userEntities)){
            return null;
        }

        for (UserEntity userEntity : userEntities) {
            String pwd = password;
            // ?????????????????????
            String salt = userEntity.getSalt();

            // ????????????????????????????????????
            pwd = DigestUtils.md5Hex(pwd + salt);

            // ???????????????????????????????????????????????????
            if (StringUtils.equals(pwd, userEntity.getPassword())){
                return userEntity;
            }
        }

        return null;
    }
}