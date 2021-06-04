package com.atguigu.gmall.index.aspect;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.index.config.BloomFilterConfig;
import com.atguigu.gmall.index.config.GmallCache;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.google.common.hash.BloomFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.binding.MapperMethod;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;


import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class GmallCacheAspect {
    @Pointcut("@annotation(com.atguigu.gmall.index.config.GmallCache)")
    public void pointcut() {

    }
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RBloomFilter bloomFilter;
    @Around(value = "pointcut()")
    public Object around(ProceedingJoinPoint joinPoint)throws Throwable{
        MethodSignature signature = (MethodSignature)joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> returnType = method.getReturnType();
        GmallCache gmallCache = method.getAnnotation(GmallCache.class);
        Object[] args = joinPoint.getArgs();
        String join = StringUtils.join(args, ",");
        String s = gmallCache.prefix() +join;
        if (!bloomFilter.contains(s)){
            return null;
        }
        String json = redisTemplate.opsForValue().get(s);
        if (StringUtils.isNotEmpty(json)){
           return JSON.parseObject(json, returnType);
        }
        String lock = gmallCache.lock();
        RLock fairLock = redissonClient.getFairLock(lock + join);
        fairLock.lock();
        try {
            String json2 = redisTemplate.opsForValue().get(s);
            if (StringUtils.isNotEmpty(json2)){
                return JSON.parseObject(json2, returnType);
            }
            Object proceed = joinPoint.proceed(joinPoint.getArgs());
            if (proceed!=null){
                int i = gmallCache.timeout() + new Random().nextInt(gmallCache.random());
                redisTemplate.opsForValue().set(s,JSON.toJSONString(proceed),i, TimeUnit.MINUTES);
            }
            return proceed;
        } finally {
            fairLock.unlock();
        }
    }
//    @Before("execution(* com.atguigu.gmall.index.service.*.*(..))")
//    public void before(JoinPoint joinPoint){
//        MethodSignature signature = (MethodSignature)joinPoint.getSignature();
//        System.out.println("前置通知"+signature.getMethod().getName());
//        System.out.println("前置通知"+joinPoint.getTarget().getClass().getName());
//        System.out.println("前置通知"+joinPoint.getArgs());
//    }
//    @AfterReturning(value = "execution(* com.atguigu.gmall.index.service.*.*(..))",returning = "ret")
//    public void afterReturning(Object ret){
//        System.out.println("返回后通知");
//    }
//    @AfterThrowing(value = "execution(* com.atguigu.gmall.index.service.*.*(..))",throwing = "e")
//    public void afterThrowing(Exception e){
//        System.out.println("异常后通知");
//    }
//    @After(value = "execution(* com.atguigu.gmall.index.service.*.*(..))")
//    public void after(){
//        System.out.println("最终通知");
//    }

}
