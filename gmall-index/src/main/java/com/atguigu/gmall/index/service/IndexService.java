package com.atguigu.gmall.index.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.config.BloomFilterConfig;
import com.atguigu.gmall.index.config.GmallCache;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.index.utils.DistributedLock;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class IndexService {
    @Resource
    private GmallPmsClient pmsClient;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;
    private static final String KEY_PREFIX="index:cates";
    private static final String LOCK_PREFIX="index:lock:cates";

    public List<CategoryEntity> queryLv1() {
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoryList(0l);
        return listResponseVo.getData();
    }

    public List<CategoryEntity> queryLv2(Long pid) {
        String json = this.stringRedisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if (StringUtils.isNoneBlank(json)){
            return JSON.parseArray(json, CategoryEntity.class);
        }
        RLock lock = redissonClient.getFairLock(LOCK_PREFIX+pid);
        lock.lock();

        try {
            String json2 = this.stringRedisTemplate.opsForValue().get(KEY_PREFIX + pid);
            if (StringUtils.isNoneBlank(json2)){
                return JSON.parseArray(json2, CategoryEntity.class);
            }
            ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategory(pid);
            List<CategoryEntity> data = listResponseVo.getData();
            if (CollectionUtils.isEmpty(data)){
                this.stringRedisTemplate.opsForValue().set(KEY_PREFIX + pid,JSON.toJSONString(data),5, TimeUnit.MINUTES);

            }else {
                this.stringRedisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(data), 90+new Random().nextInt(15), TimeUnit.DAYS);
            }
            return data;
        } finally {
            lock.unlock();
        }
    }

    @GmallCache(prefix = KEY_PREFIX,timeout = 129600,random =14400,lock = LOCK_PREFIX)
    public List<CategoryEntity> queryLv3(Long pid) {
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategory(pid);
        List<CategoryEntity> data = listResponseVo.getData();
        return data;
    }

    public void test() {
        String uuid = UUID.randomUUID().toString();
        Boolean lock = this.stringRedisTemplate.opsForValue().setIfAbsent("lock", uuid,3,TimeUnit.SECONDS);
        if (!lock) {
            try {
                Thread.sleep(100);
                test();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }else {
            String num = this.stringRedisTemplate.opsForValue().get("num");
            if (StringUtils.isEmpty(num)){
                this.stringRedisTemplate.opsForValue().set("num","1");
            }
            int i = Integer.parseInt(num);
            this.stringRedisTemplate.opsForValue().set("num",String.valueOf(++i));
            String script="if(redis.call('get', KEYS[1]) == ARGV[1]) then return redis.call('del', KEYS[1]) else return 0 end";
            this.stringRedisTemplate.execute(new DefaultRedisScript<>(script,Boolean.class), Arrays.asList("lock"),uuid);
//            if (StringUtils.equals(this.stringRedisTemplate.opsForValue().get("lock"),uuid)){
//            this.stringRedisTemplate.delete("lock");
//            }
        }

    }
    @Resource
    DistributedLock distributedLock;
    public void test1() {
        String uuid = UUID.randomUUID().toString();
        boolean flag = distributedLock.lock("lock", uuid, 30);
        if (flag) {

            String num = this.stringRedisTemplate.opsForValue().get("num");
            if (StringUtils.isEmpty(num)){
                this.stringRedisTemplate.opsForValue().set("num","1");
            }
            int i = Integer.parseInt(num);
            this.stringRedisTemplate.opsForValue().set("num",String.valueOf(++i));
            try {
                TimeUnit.SECONDS.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            distributedLock.unlock("lock",uuid);

        }
    }
    public void test3() {
        RLock lock = this.redissonClient.getLock("lock");
        lock.lock();
        String num = this.stringRedisTemplate.opsForValue().get("num");
            if (StringUtils.isEmpty(num)){
                this.stringRedisTemplate.opsForValue().set("num","1");
            }
            int i = Integer.parseInt(num);
            this.stringRedisTemplate.opsForValue().set("num",String.valueOf(++i));
        lock.unlock();
        }


    }

