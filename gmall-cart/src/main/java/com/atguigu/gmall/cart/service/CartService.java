package com.atguigu.gmall.cart.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.feign.GmallSmsClient;
import com.atguigu.gmall.cart.feign.GmallWmsClient;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.mapper.CartMapper;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.pojo.UserInfo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.CartException;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CartMapper cartMapper;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Resource
    private CartAsyncService cartAsyncService;

    private static final String KEY_PREFIX = "cart:info";
    public void addCart(Cart cart) {
        String userId = getUserId();

        BoundHashOperations<String, Object, Object> hashOps = stringRedisTemplate.boundHashOps(KEY_PREFIX + userId);
        String skuId = cart.getSkuId().toString();
        BigDecimal count = cart.getCount();
        if (hashOps.hasKey(skuId)){
            String json = hashOps.get(skuId).toString();
            cart = JSON.parseObject(json, Cart.class);
            //累加
            cart.setCount(cart.getCount().add(count));
            this.cartAsyncService.update(cart, userId, skuId);
        }else {
            cart.setUserId(userId);
            cart.setCheck(true);
            ResponseVo<SkuEntity> skuEntityResponseVo = pmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity == null) {
                throw new CartException("没有对应的商品");
            }

            cart.setTitle(skuEntity.getTitle());
            cart.setPrice(skuEntity.getPrice());
            cart.setDefaultImage(skuEntity.getDefaultImage());
            ResponseVo<List<WareSkuEntity>> wareResponseVo = wmsClient.queryWareSkuList(cart.getSkuId());
            List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)){
                cart.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock()-wareSkuEntity.getStockLocked()>0));
            }
            ResponseVo<List<SkuAttrValueEntity>> responseVo = pmsClient.querySaleAttrValuesBySkuId(cart.getSkuId());
            List<SkuAttrValueEntity> skuAttrValueEntities = responseVo.getData();
            cart.setSaleAttrs(JSON.toJSONString(skuAttrValueEntities));
            ResponseVo<List<ItemSaleVo>> salesResponseVo = smsClient.querySaleListBySkuId(cart.getSkuId());
            List<ItemSaleVo> itemSaleVos = salesResponseVo.getData();
            cart.setSales(JSON.toJSONString(itemSaleVos));
            this.cartAsyncService.insert(userId, cart);
        }

        hashOps.put(skuId, JSON.toJSONString(cart));
    }

    /**
     * 功能描述: 登录状态验证
     * @author fjh
     * @date 2021/6/4
     * @return
     */
    private String getUserId() {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userId=null;
        if (userInfo.getUserId()==null){
            userId=userInfo.getUserKey();
        }else {
            userId=userInfo.getUserId().toString();
        }
        return userId;
    }

    public Cart queryCart(Long skuId) {
        String userId = getUserId();
        BoundHashOperations<String, Object, Object> hashOps = stringRedisTemplate.boundHashOps(KEY_PREFIX + userId);
        if (!hashOps.hasKey(skuId.toString())) {
            throw new CartException("没有记录");
        }
        String json = hashOps.get(skuId.toString()).toString();
        return JSON.parseObject(json, Cart.class);
    }

    public List<Cart> queryCarts() {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userKey = userInfo.getUserKey();
        String unLoginKey = KEY_PREFIX + userKey;
        //获取内层map
        BoundHashOperations<String, Object, Object> unLoginHashOps = stringRedisTemplate.boundHashOps(unLoginKey);
        List<Object> unLoginsCartJson = unLoginHashOps.values();
        List<Cart> unLoginCarts=null;
        if (CollectionUtils.isEmpty(unLoginsCartJson)){
            unLoginCarts = unLoginsCartJson.stream().map(json -> JSON.parseObject(json.toString(), Cart.class)).collect(Collectors.toList());

        }
        //获取userId,判断是否为空,为空则直接返回未登录的购物车
        Long userId = userInfo.getUserId();
        if (userId == null){
            return unLoginCarts;
        }
        BoundHashOperations<String, Object, Object> LoginHashOps = stringRedisTemplate.boundHashOps(KEY_PREFIX + userId);

        //.不为空,把未登录的购物车合并到已登录的购物车中
        if (!CollectionUtils.isEmpty(unLoginCarts)){
            unLoginCarts.forEach(cart -> {
                String skuId = cart.getSkuId().toString();
                BigDecimal count = cart.getCount();
                if (LoginHashOps.hasKey(skuId)){
                    String json = LoginHashOps.get(skuId).toString();
                    cart=JSON.parseObject(json,Cart.class);
                    cart.setCount(cart.getCount().add(count));
                    cartAsyncService.update(cart,userId.toString(),skuId);
                }else {
                    cart.setUserId(userId.toString());
                    cartAsyncService.insert(userId.toString(), cart);
                }
                LoginHashOps.put(skuId,JSON.toJSONString(cart));
            });
        }
        //.删除未登录的购物车
        stringRedisTemplate.delete(unLoginKey);
        cartAsyncService.delete(userKey);
        //.获取已登录的购物车,并返回
        List<Object> values = LoginHashOps.values();
        if (!CollectionUtils.isEmpty(values)){
            return values.stream().map(json->JSON.parseObject(json.toString(), Cart.class)).collect(Collectors.toList());
        }
        return null;
    }

    public void undateNum(Cart cart) {
        String userId = getUserId();
        BoundHashOperations<String, Object, Object> hashOps = stringRedisTemplate.boundHashOps(KEY_PREFIX + userId);
        String skuId = cart.getSkuId().toString();
        BigDecimal count = cart.getCount();
        if (!hashOps.hasKey(skuId)){
            throw new CartException("没有对于的购物车记录");
        }
        String json = hashOps.get(skuId).toString();
        cart = JSON.parseObject(json, Cart.class);
        cart.setCount(count);
        hashOps.put(skuId,JSON.toJSONString(cart));
        cartAsyncService.update(cart,userId,skuId);
    }

    public void undateStatus(Cart cart) {
        String userId = getUserId();
        BoundHashOperations<String, Object, Object> hashOps = stringRedisTemplate.boundHashOps(KEY_PREFIX + userId);
        String skuId = cart.getSkuId().toString();
        Boolean store = cart.getCheck();
        if (!hashOps.hasKey(skuId)){
            throw new CartException("没有对于的购物车记录");
        }
        String json = hashOps.get(skuId).toString();
        cart = JSON.parseObject(json, Cart.class);
        cart.setCheck(store);
        hashOps.put(skuId,JSON.toJSONString(cart));
        cartAsyncService.update(cart,userId,skuId);
    }

    public void deleteCart(Long skuId) {
        String userId = getUserId();
        BoundHashOperations<String, Object, Object> hashOps = stringRedisTemplate.boundHashOps(KEY_PREFIX + userId);
        hashOps.delete(skuId.toString());
        cartAsyncService.deleteByUserIdAndSkuId(userId, skuId);
    }

    public List<Cart> queryCheckedCartsByUserId(Long userId) {
        BoundHashOperations<String, Object, Object> hashOps = stringRedisTemplate.boundHashOps(KEY_PREFIX + userId);
        List<Object> cartJsons = hashOps.values();
        if (CollectionUtils.isEmpty(cartJsons)){
            return null;
        }
        return cartJsons.stream().map(cart->JSON.parseObject(cart.toString(),Cart.class)).filter(Cart::getCheck).collect(Collectors.toList());
//        return cartJsons.stream().map(cart->JSON.parseObject(cartJson.toString(), Cart.class)).filter(Cart::getCheck).collect(Collectors.toList());
    }
}
