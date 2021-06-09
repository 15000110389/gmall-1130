package com.atguigu.gmall.wms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.baomidou.mybatisplus.annotation.TableField;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.wms.mapper.WareSkuMapper;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuMapper, WareSkuEntity> implements WareSkuService {
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private WareSkuMapper wareSkuMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    private static final String LOCK_PREFIX="stock:lock:";
    private static final String KEY_PREFIX="stock:info:";
    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<WareSkuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageResultVo(page);
    }
    @Transactional
    @Override
    public List<SkuLockVo> checkLock(List<SkuLockVo> lockVos, String orderToken) {

        if (CollectionUtils.isEmpty(lockVos)){
            throw new OrderException("您没有购买的商品");
        }

        // 遍历送货清单，验库存并锁库存
        lockVos.forEach(skuLockVo -> {
            this.checkAndLock(skuLockVo);
        });

        // 判断是否有锁定失败的商品，如果有则释放所有锁定成功的商品库存
        if (lockVos.stream().anyMatch(skuLockVo -> !skuLockVo.getLock())) {
            // 遍历所有锁定成功的库存，解锁库存
            lockVos.stream().filter(SkuLockVo::getLock).collect(Collectors.toList()).forEach(lockVo -> {
                this.wareSkuMapper.unlock(lockVo.getWareSkuId(), lockVo.getCount());
            });
            // 返回锁定信息
            return lockVos;
        }

        // 为了方便将来解锁库存 或者 减库存 。需要缓存锁定信息
        this.redisTemplate.opsForValue().set(KEY_PREFIX + orderToken, JSON.toJSONString(lockVos));

        // 发送延时消息定时解锁库存
        this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "stock.ttl", orderToken);

        // 都锁定成功的情况下，返回 null
        return null;
    }
    private void checkAndLock(SkuLockVo skuLockVo){
        RLock rLock = redissonClient.getLock(LOCK_PREFIX + skuLockVo.getSkuId());
        rLock.lock();
        try {
            List<WareSkuEntity> check = wareSkuMapper.check(skuLockVo.getSkuId(), skuLockVo.getCount());
            if (CollectionUtils.isEmpty(check)){
                skuLockVo.setLock(false);
                return;
            }
            WareSkuEntity wareSkuEntity = check.get(0);
           if (wareSkuMapper.lock(wareSkuEntity.getId(),skuLockVo.getCount())==1){
               skuLockVo.setLock(true);
               skuLockVo.setWareSkuId(wareSkuEntity.getId());
           }
        } finally {
            rLock.unlock();
        }
    }
}