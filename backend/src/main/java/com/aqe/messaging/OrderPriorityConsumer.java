package com.aqe.messaging;

import com.aqe.config.RabbitMQConfig;
import com.aqe.model.entity.QuoteOrder;
import com.aqe.trading.TradingClient;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 订单优先级队列消费者
 * 监听 queue.order.priority，获取订单并调用交易接口发送
 * 发送失败时根据重试次数决定：进入延迟队列（5s/30s）或最终死信队列
 */
@Slf4j
@Component
public class OrderPriorityConsumer {

    @Autowired
    private TradingClient tradingClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 最大重试次数（包含本次尝试）
     * 例如设置为3，表示第1次失败进延迟队列，第2次失败再进延迟队列，第3次失败进入死信
     */
    private static final int MAX_RETRY_COUNT = 3;

    /**
     * 从订单优先级队列消费订单并发送到交易接口
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_PRIORITY)
    public void dispatch(Channel channel, Message message, QuoteOrder order) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageProperties().getMessageId();

        try {
            log.debug("Consuming order from priority queue: orderId={}, price={}, volume={}, priority={}",
                    order.getId(), order.getPrice(), order.getVolume(), order.getPriority());

            // 调用交易接口发送订单
            boolean success = tradingClient.sendOrder(order);

            if (success) {
                // 发送成功，确认消息
                channel.basicAck(deliveryTag, false);
                log.info("Order dispatched successfully: orderId={}", order.getId());
            } else {
                // 发送失败，进入重试逻辑
                handleSendFailure(channel, message, order, "Trading client returned failure");
            }

        } catch (Exception e) {
            log.error("Order dispatch error: orderId={}, error={}", order.getId(), e.getMessage(), e);
            handleSendFailure(channel, message, order, "Exception: " + e.getMessage());
        }
    }

    /**
     * 处理发送失败的情况
     * 根据重试次数决定：进入延迟队列 或 进入最终死信队列
     */
    private void handleSendFailure(Channel channel, Message message, QuoteOrder order, String reason) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        // 获取当前重试次数（从消息头中读取）
        Integer retryCount = (Integer) message.getMessageProperties().getHeaders().getOrDefault("retry-count", 0);
        retryCount = retryCount + 1;

        if (retryCount >= MAX_RETRY_COUNT) {
            // 超过最大重试次数，进入最终死信队列
            log.warn("Order dispatch failed after {} retries, sending to DLQ: orderId={}, reason={}",
                    retryCount, order.getId(), reason);

            // 拒绝消息并不重新入队（进入死信队列，由 DLX 转发）
            channel.basicNack(deliveryTag, false, false);

        } else {
            // 未达到最大重试次数，进入延迟队列（5秒后重试）
            log.info("Order dispatch failed, retry {}/{}: orderId={}, will retry after delay",
                    retryCount, MAX_RETRY_COUNT, order.getId());

            // 将消息重新发布到死信交换机（DLX），由 TTL 队列实现延迟
            Integer finalRetryCount = retryCount;
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DLX_EXCHANGE,
                    "order.retry",
                    order,
                    msg -> {
                        // 保留业务优先级，便于重新进入主队列时恢复
                        msg.getMessageProperties().setPriority(order.getPriority());
                        // 累加重试次数
                        msg.getMessageProperties().setHeader("retry-count", finalRetryCount);
                        // 保留原消息ID便于追踪
                        msg.getMessageProperties().setMessageId(message.getMessageProperties().getMessageId());
                        return msg;
                    }
            );

            // 确认原消息（已转发到重试队列，原消息可以删除）
            channel.basicAck(deliveryTag, false);
        }
    }
}