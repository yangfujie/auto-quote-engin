// StrategyInstance.java
package com.aqe.model.entity;

import lombok.Data;
import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "strategy_instance")
public class StrategyInstance {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long strategyDefId;
    private String symbol;
    private Integer status; // 1-运行 0-停止
    @Column(columnDefinition = "json")
    private String params;  // 参数覆盖
    private Integer priority;
    @Column(columnDefinition = "json")
    private String triggerConditions;
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;
}