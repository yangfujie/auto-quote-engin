package com.aqe.config;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import lombok.extern.slf4j.Slf4j;

/**
 * 自定义拒绝策略示例（按优先级丢弃）
 */
@Slf4j
public class PriorityDiscardPolicy implements RejectedExecutionHandler {
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        // 获取队列（需为 PriorityBlockingQueue）
        BlockingQueue<Runnable> queue = executor.getQueue();
        if (queue instanceof PriorityBlockingQueue) {
            // 移除优先级最低的任务（假设任务实现了 Comparable）
            queue.poll(); // 移除队首（最低优先级）
            executor.execute(r); // 重新提交当前任务
        } else {
            // 降级方案：直接丢弃当前任务，记录日志
            log.warn("Queue full, task rejected, falling back to discard policy");
        }
    }
}