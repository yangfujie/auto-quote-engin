CREATE TABLE IF NOT EXISTS `strategy_def` (
                                              `id` bigint PRIMARY KEY AUTO_INCREMENT,
                                              `name` varchar(100) NOT NULL,
    `flow_json` json NOT NULL,
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS `strategy_instance` (
                                                   `id` bigint PRIMARY KEY AUTO_INCREMENT,
                                                   `strategy_def_id` bigint NOT NULL,
                                                   `symbol` varchar(20) NOT NULL,
    `status` tinyint DEFAULT 1,
    `params` json,
    `priority` int DEFAULT 5,
    `trigger_conditions` json,
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`strategy_def_id`) REFERENCES `strategy_def`(`id`)
    );

CREATE TABLE IF NOT EXISTS `quote_order` (
                                             `id` bigint PRIMARY KEY AUTO_INCREMENT,
                                             `instance_id` bigint NOT NULL,
                                             `side` tinyint COMMENT '1-买 2-卖',
                                             `price` decimal(16,8),
    `volume` int,
    `priority` int,
    `status` tinyint DEFAULT 0,
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`instance_id`) REFERENCES `strategy_instance`(`id`)
    );