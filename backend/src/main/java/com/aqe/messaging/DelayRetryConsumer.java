package com.aqe.messaging;

import com.aqe.config.RabbitMQConfig;
import com.aqe.model.entity.QuoteOrder;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class DelayRetryConsumer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = {RabbitMQConfig.QUEUE_ORDER_DELAY_5S, RabbitMQConfig.QUEUE_ORDER_DELAY_30S})
    public void handleRetry(Channel channel, Message message, QuoteOrder order) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            log.debug("Retry order from delay queue: orderId={}, retryCount={}",
                    order.getId(),
                    message.getMessageProperties().getHeaders().get("retry-count"));

            // 重新发送到主优先级队列，继续尝试分发
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.TOPIC_EXCHANGE,
                    "order.priority.retry",
                    order,
                    msg -> {
                        // 恢复优先级
                        msg.getMessageProperties().setPriority(order.getPriority());
                        // 保留重试次数
                        msg.getMessageProperties().setHeader("retry-count",
                                message.getMessageProperties().getHeaders().get("retry-count"));
                        // 保留消息ID
                        msg.getMessageProperties().setMessageId(message.getMessageProperties().getMessageId());
                        return msg;
                    }
            );

            // 确认延迟消息（已转发）
            channel.basicAck(deliveryTag, false);
            log.debug("Order re-queued to priority queue: orderId={}", order.getId());

        } catch (Exception e) {
            log.error("Failed to retry order: orderId={}", order.getId(), e);
            // 失败则进入死信队列
            channel.basicNack(deliveryTag, false, false);
        }
    }
}