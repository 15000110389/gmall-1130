package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.cart.mapper.CartMapper;
import com.atguigu.gmall.cart.pojo.Cart;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class CartAsyncService {
    @Resource
    private CartMapper cartMapper;
    @Async
    public void update(Cart cart, String userId, String skuId) {
        this.cartMapper.update(cart, new UpdateWrapper<Cart>().eq("user_id", userId).eq("sku_id", skuId));
    }
    @Async
    public void insert(Cart cart) {
        this.cartMapper.insert(cart);
    }
    @Async
    public void delete(String userId) {
        cartMapper.delete(new UpdateWrapper<Cart>().eq("user_id",userId));
    }
    @Async
    public void deleteByUserIdAndSkuId(String userId, Long skuId) {
        cartMapper.delete(new UpdateWrapper<Cart>().eq("user_id",userId).eq("sku_id",skuId));
    }
}
