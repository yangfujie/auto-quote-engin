package com.aqe.model.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "quote_order_snapshot")
public class QuoteOrderSnapshot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long strategyInstanceId;
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;
    @Column(columnDefinition = "TEXT")
    private String snapshot;
    private Long orderId;
}
