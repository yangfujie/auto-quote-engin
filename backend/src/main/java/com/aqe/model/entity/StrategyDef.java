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
    private String flowJson;   // 存储流程图JSON
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;
}