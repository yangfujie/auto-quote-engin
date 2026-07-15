package com.aqe.messaging;

import com.aqe.config.RabbitMQConfig;
import com.aqe.engine.StrategyEngine;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class MarketConsumer {

    @Autowired
    private StrategyEngine strategyEngine;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_MARKET)
    public void handleMarket(Channel channel, Message message, MarketData data) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            log.debug("Received market event: {}", data);
            // 调用策略引擎（现有逻辑）
            strategyEngine.onMarketEvent(data.getSymbol(), data.getPrice(), data.getVolume());
            // 手动 ACK
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Error processing market message", e);
            // 拒绝并不重新入队（进入死信或丢弃）
            channel.basicNack(deliveryTag, false, false);
        }
    }
}