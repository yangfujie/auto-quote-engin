package com.aqe.messaging;

import com.aqe.config.RabbitMQConfig;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MessagePublisher {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发布行情消息
     */
    public void publishMarket(String symbol, double price, int volume) {
        MarketData data = new MarketData(symbol, price, volume, System.currentTimeMillis());
        String routingKey = "market." + symbol;
        CorrelationData correlation = new CorrelationData(UUID.randomUUID().toString());
        rabbitTemplate.convertAndSend(RabbitMQConfig.TOPIC_EXCHANGE, routingKey, data, correlation);
    }

    /**
     * 发布订单状态变更
     */
    public void publishOrderStatus(Long orderId, String status) {
        // 略
    }

    /**
     * 发布账户变动
     */
    public void publishAccountUpdate(String accountId, double balance) {
        // 略
    }

    /**
     * 发布策略控制指令（启停、参数变更）
     */
    public void publishControlCommand(Long instanceId, String command, Object payload) {
        // 略
    }
}