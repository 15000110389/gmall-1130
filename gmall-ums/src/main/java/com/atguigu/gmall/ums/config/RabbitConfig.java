package com.atguigu.gmall.ums.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Configuration
@Slf4j
public class RabbitConfig {
    @Resource
    private RabbitTemplate rabbitTemplate;
    @PostConstruct
    public void init() {
        this.rabbitTemplate.setConfirmCallback((correlationData,ack,cause)->{
            if (!ack){
                log.error("消息没有到达交换机:{}",cause);
            }
        });
        this.rabbitTemplate.setReturnCallback((message,replyCode,replyText,exchange,routingKey)->{
            log.error("消息没有达到队列。交换机：{}，路由键：{}，消息内容：{}。失败原因：{}", exchange, routingKey, new String(message.getBody()), replyText);
        });
    }
}
