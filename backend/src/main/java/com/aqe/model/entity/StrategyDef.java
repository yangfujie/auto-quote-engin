// StrategyDef.java
package com.aqe.model.entity;

import lombok.Data;
import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "strategy_def")
public class StrategyDef {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(columnDefinition = "json")
    private String flowJson;   // 存储流程图 JSON（Drawflow 格式）
    @Column(name = "chain_expression")
    private String chainExpression; // LiteFlow EL 表达式（由 flow_json 转换而来）
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;
}