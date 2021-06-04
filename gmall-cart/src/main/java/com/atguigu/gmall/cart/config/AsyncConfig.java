package com.atguigu.gmall.cart.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;

import javax.annotation.Resource;
import java.util.concurrent.Executor;
@Configuration
public class AsyncConfig implements AsyncConfigurer {
    @Resource
    private AsyncUncaughtExceptionHandler asyncUncaughtExceptionHandler;
    /**
     * 功能描述: 配置线程池
     * @author fjh
     * @date 2021/6/4
     * @return
     */
    @Override
    public Executor getAsyncExecutor() {
        return null;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return asyncUncaughtExceptionHandler;
    }
}
