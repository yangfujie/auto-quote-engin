package com.aqe.queue;

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

@Slf4j
@Component
public class OrderPriorityConsumer {

    @Autowired
    private TradingClient tradingClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // 记录重试次数（可通过消息头或 Redis 实现）

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_PRIORITY)
    public void dispatch(Channel channel, Message message, QuoteOrder order) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            // 发送到交易接口
            boolean success = tradingClient.sendOrder(order);
            if (success) {
                // 更新订单状态为已推送
                channel.basicAck(deliveryTag, false);
            } else {
                // 失败，重试逻辑：检查重试次数（此处简化，实际通过消息头或 Redis）
                // 重试次数超过阈值则进入 DLQ，否则进入延迟队列
                Integer retryCount = (Integer) message.getMessageProperties().getHeaders().getOrDefault("retry-count", 0);
                if (retryCount >= 3) {
                    // 进入死信队列
                    channel.basicNack(deliveryTag, false, false);
                } else {
                    // 重新发布到延迟队列（5秒后重试）
                    rabbitTemplate.convertAndSend(RabbitMQConfig.DLX_EXCHANGE, "order.retry", order,
                            m -> {
                                m.getMessageProperties().setHeader("retry-count", retryCount + 1);
                                return m;
                            });
                    channel.basicAck(deliveryTag, false); // 确认原消息
                }
            }
        } catch (Exception e) {
            log.error("Order dispatch error", e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}