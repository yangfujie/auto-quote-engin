package com.aqe.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    // ========== Exchange ==========
    public static final String TOPIC_EXCHANGE = "aqe.topic.exchange";
    public static final String DLX_EXCHANGE = "aqe.dlx.exchange";

    // ========== Routing Keys ==========
    public static final String ROUTING_KEY_MARKET = "market.*";
    public static final String ROUTING_KEY_ORDER = "order.*";
    public static final String ROUTING_KEY_ACCOUNT = "account.*";
    public static final String ROUTING_KEY_CONTROL = "control.*";

    // ========== Queues ==========
    public static final String QUEUE_MARKET = "queue.market";
    public static final String QUEUE_ORDER_PRIORITY = "queue.order.priority";
    public static final String QUEUE_ORDER_STATUS = "queue.order.status";
    public static final String QUEUE_ACCOUNT = "queue.account";
    public static final String QUEUE_CONTROL = "queue.control";

    // ========== Dead Letter Queues ==========
    public static final String QUEUE_ORDER_DELAY_5S = "queue.order.delay.5s";
    public static final String QUEUE_ORDER_DELAY_30S = "queue.order.delay.30s";
    public static final String QUEUE_ORDER_DLQ = "queue.order.dlq";

    // ========== 定义 Topic Exchange ==========
    @Bean
    public TopicExchange topicExchange() {
        return ExchangeBuilder.topicExchange(TOPIC_EXCHANGE).durable(true).build();
    }

    // ========== 定义 Dead Letter Exchange ==========
    @Bean
    public DirectExchange dlxExchange() {
        return ExchangeBuilder.directExchange(DLX_EXCHANGE).durable(true).build();
    }

    // ========== 行情队列（高吞吐） ==========
    @Bean
    public Queue marketQueue() {
        // 使用 Lazy Queue 减少内存压力
        Map<String, Object> args = new HashMap<>();
        args.put("x-queue-mode", "lazy");
        return QueueBuilder.durable(QUEUE_MARKET)
                .withArguments(args)
                .build();
    }

    // ========== 优先级订单队列（替代内存 PriorityBlockingQueue） ==========
    @Bean
    public Queue orderPriorityQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-max-priority", 10);          // 1~10 优先级
        args.put("x-max-length", 100000);        // 防止无限积压
        // 设置死信转发到 DLX
        args.put("x-dead-letter-exchange", DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", "order.retry");
        return QueueBuilder.durable(QUEUE_ORDER_PRIORITY)
                .withArguments(args)
                .build();
    }

    // ========== 订单状态队列 ==========
    @Bean
    public Queue orderStatusQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_STATUS).build();
    }

    // ========== 账户队列 ==========
    @Bean
    public Queue accountQueue() {
        return QueueBuilder.durable(QUEUE_ACCOUNT).build();
    }

    // ========== 控制指令队列（高优先级） ==========
    @Bean
    public Queue controlQueue() {
        return QueueBuilder.durable(QUEUE_CONTROL).build();
    }

    // ========== 死信队列 - 延迟5秒重试 ==========
    @Bean
    public Queue orderDelay5sQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", 5000);          // 5秒后过期
        args.put("x-dead-letter-exchange", TOPIC_EXCHANGE);
        args.put("x-dead-letter-routing-key", "order.retry.5s"); // 最终回到主队列
        return QueueBuilder.durable(QUEUE_ORDER_DELAY_5S)
                .withArguments(args)
                .build();
    }

    // ========== 死信队列 - 延迟30秒重试 ==========
    @Bean
    public Queue orderDelay30sQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", 30000);
        args.put("x-dead-letter-exchange", TOPIC_EXCHANGE);
        args.put("x-dead-letter-routing-key", "order.retry.30s");
        return QueueBuilder.durable(QUEUE_ORDER_DELAY_30S)
                .withArguments(args)
                .build();
    }

    // ========== 最终死信队列（人工介入） ==========
    @Bean
    public Queue orderDlqQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_DLQ).build();
    }

    // ========== 绑定 ==========
    @Bean
    public Binding marketBinding() {
        return BindingBuilder.bind(marketQueue())
                .to(topicExchange())
                .with("market.*");
    }

    @Bean
    public Binding orderPriorityBinding() {
        return BindingBuilder.bind(orderPriorityQueue())
                .to(topicExchange())
                .with("order.priority.*");
    }

    @Bean
    public Binding orderStatusBinding() {
        return BindingBuilder.bind(orderStatusQueue())
                .to(topicExchange())
                .with("order.status.*");
    }

    @Bean
    public Binding accountBinding() {
        return BindingBuilder.bind(accountQueue())
                .to(topicExchange())
                .with("account.*");
    }

    @Bean
    public Binding controlBinding() {
        return BindingBuilder.bind(controlQueue())
                .to(topicExchange())
                .with("control.*");
    }

    // 绑定死信队列到 DLX
    @Bean
    public Binding dlxToDelay5s() {
        return BindingBuilder.bind(orderDelay5sQueue())
                .to(dlxExchange())
                .with("order.retry");
    }

    @Bean
    public Binding dlxToDelay30s() {
        return BindingBuilder.bind(orderDelay30sQueue())
                .to(dlxExchange())
                .with("order.retry");
    }

    @Bean
    public Binding dlxToDlq() {
        return BindingBuilder.bind(orderDlqQueue())
                .to(dlxExchange())
                .with("order.dlq");
    }
}