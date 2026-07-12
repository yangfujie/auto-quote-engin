// PriorityOrderQueue.java - 包装PriorityBlockingQueue
package com.aqe.queue;

import com.aqe.model.entity.QuoteOrder;
import org.springframework.stereotype.Component;
import java.util.concurrent.PriorityBlockingQueue;

@Component
public class PriorityOrderQueue {
    private final PriorityBlockingQueue<QuoteOrder> queue = new PriorityBlockingQueue<>(1000);

    public void offer(QuoteOrder order) {
        queue.offer(order);
    }

    public QuoteOrder poll() {
        return queue.poll();
    }

    public int size() {
        return queue.size();
    }

    public void drainTo(java.util.Collection<? super QuoteOrder> c, int max) {
        queue.drainTo(c, max);
    }
}