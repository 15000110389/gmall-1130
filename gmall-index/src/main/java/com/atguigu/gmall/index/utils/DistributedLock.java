package com.atguigu.gmall.index.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import java.sql.Time;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Component
public class DistributedLock {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    private Timer timer;
    public boolean lock(String lockName,String uuid,Integer expireTime)  {
        String script = "if(redis.call('exists', KEYS[1]) == 0 or redis.call('hexists', KEYS[1], ARGV[1]) == 1) then redis.call('hincrby', KEYS[1], ARGV[1], 1) redis.call('expire', KEYS[1], ARGV[2]) return 1 else return 0 end";
        Boolean flag=this.stringRedisTemplate.execute(new DefaultRedisScript<>(script,Boolean.class),Arrays.asList(lockName),uuid,expireTime.toString());
        if (!flag) {
            try {
                Thread.sleep(100);
                lock(lockName, uuid, expireTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }else {
            this.renewExpire(lockName, uuid, expireTime);
        }
        return true;
    }
    public void unlock(String lockName,String uuid) {
        String script ="if(redis.call('hexists', KEYS[1], ARGV[1]) == 0) then return nil elseif(redis.call('hincrby', KEYS[1], ARGV[1], -1) == 0) then return redis.call('del', KEYS[1]) else return 0 end";
        Long flag = this.stringRedisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(lockName), uuid);
        if (flag==null){
            throw new RuntimeException("尝试释放别人的锁");
        }else if (flag == 1){
            this.timer.cancel();
        }
    }

    private void renewExpire(String lockName,String uuid,Integer expireTime){
        String script = "if(redis.call('hexists', KEYS[1], ARGV[1]) == 1) then redis.call('expire', KEYS[1], ARGV[2]) return 1 else return 0 end";
        this.timer=new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                stringRedisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(lockName), uuid,expireTime.toString());
            }
        },expireTime*1000/3,expireTime*1000/3);
    }

    public static void main(String[] args) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {

            }
        },10000,10000);
//        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(3);
//        executorService.scheduleAtFixedRate(()->{
//
//        },10,10, TimeUnit.SECONDS);
    }
}
