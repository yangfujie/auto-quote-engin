// QuoteOrder.java
package com.aqe.model.entity;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Entity
@Table(name = "quote_order")
public class QuoteOrder implements Comparable<QuoteOrder> {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long instanceId;
    private Integer side;  //1买 2卖
    private BigDecimal price;
    private Integer volume;
    private Integer priority;
    private Integer status; //0排队 1已推送 2已成交
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;

    @Override
    public int compareTo(QuoteOrder o) {
        // 优先级高的在前（降序）
        return Integer.compare(o.getPriority(), this.priority);
    }
}