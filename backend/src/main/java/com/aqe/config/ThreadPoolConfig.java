package com.aqe.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置类
 */
@Configuration
public class ThreadPoolConfig {

    // CPU 核心数（4核）
    private static final int CPU_CORES = Runtime.getRuntime().availableProcessors();

    /**
     * CPU密集型线程池：用于执行策略DAG、表达式计算
     * 核心线程 = CPU核心数 + 1，最大 = CPU核心数 * 2
     * 有界队列容量 200，防止OOM
     * 拒绝策略：丢弃队列中最老的任务（DiscardOldestPolicy）
     */
    @Bean("cpuExecutor")
    public ThreadPoolTaskExecutor cpuExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CPU_CORES + 1);            // 5
        executor.setMaxPoolSize(CPU_CORES * 2 + 1);        // 9
        executor.setQueueCapacity(200);                    // 有界队列
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        executor.setThreadNamePrefix("cpu-exec-");
        executor.initialize();
        return executor;
    }

    /**
     * IO密集型线程池：用于订单持久化、Redis/DB操作、MQ发送
     * 核心线程 = CPU核心数 * 2，最大 = CPU核心数 * 4
     * 队列容量 500
     * 拒绝策略：CallerRunsPolicy（将任务回退给调用者线程，避免丢失）
     */
    @Bean("ioExecutor")
    public ThreadPoolTaskExecutor ioExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CPU_CORES * 2);            // 8
        executor.setMaxPoolSize(CPU_CORES * 4);            // 16
        executor.setQueueCapacity(500);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadNamePrefix("io-exec-");
        executor.initialize();
        return executor;
    }

    /**
     * 自定义拒绝策略：丢弃队列中优先级最低的任务（可选）
     * 如需使用，可替换上面的 DiscardOldestPolicy
     */
    @Bean("priorityDiscardPolicy")
    public RejectedExecutionHandler priorityDiscardPolicy() {
        return (r, executor) -> {
            // 从队列中移除优先级最低的任务（假设任务实现了优先级接口）
            // 由于 ThreadPoolExecutor 的队列是 BlockingQueue，无法直接移除指定元素，
            // 需自定义队列（如 PriorityBlockingQueue + 比较器），此处仅作示意
            System.out.println("Queue is full, discarding lowest priority task");
            // 实际实现可参考：将队列转为 ArrayBlockingQueue，遍历移除最小优先级
        };
    }
}