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
        //线程前缀,方便日志排查
        executor.setThreadNamePrefix("my-async-thread-");

        //拒绝策略:队列满+最大线程也用完,由调用者线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize(); //初始化,必须调用
        return executor;
    }

    /**
     * 代码生成工作流专用线程池
     * 工作流任务耗时长(一个请求会占用一个线程数分钟:AI 流式生成 + 可选的 vue 构建),
     */
    @Bean
    public Executor workflowAsyncExecutor(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //核心线程数:同时执行的代码生成流程数
        executor.setCorePoolSize(2);
        //最大线程数
        executor.setMaxPoolSize(5);
        //队列容量:排队的待执行工作流数
        executor.setQueueCapacity(50);
        //空闲线程存活时间秒
        executor.setKeepAliveSeconds(60);
        //线程前缀,方便日志排查
        executor.setThreadNamePrefix("workflow-async-thread-");

        //拒绝策略:队列满+最大线程也用完,由调用者线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize(); //初始化,必须调用
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
        //线程前缀,方便日志排查
        executor.setThreadNamePrefix("screenshot-async-thread-");

        //拒绝策略:队列满+最大线程也用完,由调用者线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize(); //初始化,必须调用
        return executor;
    }

    @Bean
    public AsyncTaskExecutor mvcAsyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Spring MVC 用它订阅 SSE/Flux 返回值,避免默认 SimpleAsyncTaskExecutor 在并发流式请求下失控。
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