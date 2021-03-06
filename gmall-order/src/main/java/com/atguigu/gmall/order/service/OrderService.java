package com.atguigu.gmall.order.service;


import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.common.bean.ResponseVo;

import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.order.feign.*;
import com.atguigu.gmall.order.interceptor.LoginInterceptor;

import com.atguigu.gmall.order.pojo.UserInfo;
import com.atguigu.gmall.order.vo.OrderConfirmVo;

import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private GmallCartClient cartClient;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GmallUmsClient umsClient;

    @Autowired
    private GmallOmsClient omsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final String KEY_PREFIX = "order:token:";

    public OrderConfirmVo confirm() {
        OrderConfirmVo confirmVo = new OrderConfirmVo();

        // ????????????id
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();

        // ????????????
        ResponseVo<List<Cart>> cartResponseVo = this.cartClient.queryCheckedCartsByUserId(userId);
        List<Cart> carts = cartResponseVo.getData();
        if (CollectionUtils.isEmpty(carts)){
            throw new OrderException("???????????????????????????");
        }
        List<OrderItemVo> items = carts.stream().map(cart -> {
            OrderItemVo orderItemVo = new OrderItemVo();
            orderItemVo.setSkuId(cart.getSkuId());
            orderItemVo.setCount(cart.getCount());

            // ??????sku??????
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity == null) {
                return null;
            }
            orderItemVo.setTitle(skuEntity.getTitle());
            orderItemVo.setPrice(skuEntity.getPrice());
            orderItemVo.setDefaultImage(skuEntity.getDefaultImage());
            orderItemVo.setWeight(skuEntity.getWeight());

            // ??????????????????
            ResponseVo<List<WareSkuEntity>> wareResponseVo = this.wmsClient.queryWareSkuList(skuEntity.getId());
            List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)){
                orderItemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }

            // ??????????????????
            ResponseVo<List<SkuAttrValueEntity>> saleAttrsResponseVo = this.pmsClient.querySaleAttrValuesBySkuId(cart.getSkuId());
            orderItemVo.setSaleAttrs(saleAttrsResponseVo.getData());

            // ??????????????????
            ResponseVo<List<ItemSaleVo>> salesResponseVo = this.smsClient.querySaleListBySkuId(cart.getSkuId());
            List<ItemSaleVo> itemSaleVos = salesResponseVo.getData();
            orderItemVo.setSales(itemSaleVos);

            return orderItemVo;
        }).collect(Collectors.toList());;
        confirmVo.setItems(items);

        // ??????????????????
        ResponseVo<List<UserAddressEntity>> responseVo = this.umsClient.queryUserAddressesByUserId(userId);
        List<UserAddressEntity> addressEntities = responseVo.getData();
        confirmVo.setAddresses(addressEntities);

        // ????????????
        ResponseVo<UserEntity> userEntityResponseVo = this.umsClient.queryUserById(userId);
        UserEntity userEntity = userEntityResponseVo.getData();
        if (userEntity != null) {
            confirmVo.setBounds(userEntity.getIntegration());
        }

        // ?????????????????????????????????????????? + redis
        String orderToken = IdWorker.getIdStr();
        confirmVo.setOrderToken(orderToken);
        this.redisTemplate.opsForValue().set(KEY_PREFIX + orderToken, orderToken,24, TimeUnit.HOURS);

        return confirmVo;
    }

    public void submit(OrderSubmitVo submitVo) {
        // 1.???????????????????????????
        String orderToken = submitVo.getOrderToken();
        if (StringUtils.isBlank(orderToken)){
            throw new OrderException("????????????");
        }
        String script="if(redis.call('get', KEYS[1]) == ARGV[1]) then return redis.call('del', KEYS[1]) else return 0 end";
        Boolean flag = this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(KEY_PREFIX + orderToken), orderToken);
        if (!flag){
            throw  new OrderException("?????????????????????");
        }
        // 2.?????????????????????????????????????????? ??? ????????????????????????
        BigDecimal totalPrice = submitVo.getTotalPrice();
        List<OrderItemVo> items = submitVo.getItems();
        if (CollectionUtils.isEmpty(items)){
            throw new OrderException("???????????????????????????");
        }
        BigDecimal currentPrice = items.stream().map(orderItemVo -> {
            ResponseVo<SkuEntity> skuEntityResponseVo = pmsClient.querySkuById(orderItemVo.getSkuId());
            SkuEntity data = skuEntityResponseVo.getData();
            if (data == null) {
                return new BigDecimal(0);
            }
            return data.getPrice().multiply(orderItemVo.getCount());
        }).reduce((a, b) -> a.add(b)).get();
        if (totalPrice.compareTo(currentPrice)!=0){
            throw new OrderException("???????????????");
        }
        // 3.??????????????????????????????????????????
        List<SkuLockVo> skuLockVos = items.stream().map(orderItem -> {
            SkuLockVo skuLockVo = new SkuLockVo();
            skuLockVo.setSkuId(orderItem.getSkuId());
            skuLockVo.setCount(orderItem.getCount().intValue());
            return skuLockVo;
        }).collect(Collectors.toList());
        ResponseVo<List<SkuLockVo>> skuLockResponseVo = wmsClient.checkLock(skuLockVos, orderToken);
        List<SkuLockVo> skuLockVoList = skuLockResponseVo.getData();
        if (!CollectionUtils.isEmpty(skuLockVoList)){
            throw new OrderException(JSON.toJSONString(skuLockVoList));
        }
        // 4.????????????
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();
        try {
            omsClient.saveOrder(submitVo,userId);
            //??????????????????,????????????????????????
            rabbitTemplate.convertAndSend("ORDER_EXCHANGE","order.ttl",orderToken);
        } catch (Exception e) {
            e.printStackTrace();
            rabbitTemplate.convertAndSend("ORDER_EXCHANGE","cart.disable",orderToken);
            throw new OrderException("???????????????");
        }
        // 5.?????????????????????????????????????????????
        Map<String, Object> msg=new HashMap<>();
        msg.put("userId",userId);
        List<Long> skuIds = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
        msg.put("skuIds",JSON.toJSONString(skuIds));
        rabbitTemplate.convertAndSend("ORDER_EXCHANGE","cart.delete",msg);
    }
}
