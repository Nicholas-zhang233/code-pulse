package com.zwx.codepulse.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author: 张伟旭
 * @Create: 2026-08-19 11:21
 * @description:
 **/
@Configuration
public class AsyncThreadPoolConfig {
    @Bean
    public Executor myAsyncExecutor(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //核心线程数
        executor.setCorePoolSize(5);
        //最大线程数
        executor.setMaxPoolSize(20);
        //队列容量
        executor.setQueueCapacity(100);
        //空闲线程存活时间秒
        executor.setKeepAliveSeconds(60);
        //线程前缀，方便日志排查
        executor.setThreadNamePrefix("my-async-thread-");

        //拒绝策略：队列满+最大线程也用完，由调用者线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize(); //初始化，必须调用
        return executor;
    }

    @Bean
    public Executor screenshotAsyncExecutor(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //核心线程数
        executor.setCorePoolSize(5);
        //最大线程数
        executor.setMaxPoolSize(20);
        //队列容量
        executor.setQueueCapacity(100);
        //空闲线程存活时间秒
        executor.setKeepAliveSeconds(60);
        //线程前缀，方便日志排查
        executor.setThreadNamePrefix("screenshot-async-thread-");

        //拒绝策略：队列满+最大线程也用完，由调用者线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize(); //初始化，必须调用
        return executor;
    }

    @Bean
    public AsyncTaskExecutor mvcAsyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Spring MVC 用它订阅 SSE/Flux 返回值，避免默认 SimpleAsyncTaskExecutor 在并发流式请求下失控。
        executor.setThreadNamePrefix("mvc-async-thread-");
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
