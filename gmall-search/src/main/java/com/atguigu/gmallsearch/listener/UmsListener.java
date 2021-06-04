package com.atguigu.gmallsearch.listener;

import com.atguigu.gmallsearch.pojo.UserVerificationCode;
import com.atguigu.gmallsearch.utils.SMSUtils;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class UmsListener {
    @Resource
    SMSUtils smsUtils;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("UMS_USER_QUEUE"),
            exchange = @Exchange(value = "UMS_USER_EXCHANGE",ignoreDeclarationExceptions = "true",type= ExchangeTypes.TOPIC),
            key ={"user.*"}
    ))
    public void listener(String phone, Channel channel, Message message){
        try {
            smsUtils.sendShortMessage(phone,"123456");

            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

            stringRedisTemplate.opsForValue().set(phone,"123456",60, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
