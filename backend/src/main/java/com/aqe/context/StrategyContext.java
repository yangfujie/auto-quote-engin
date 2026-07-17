package com.aqe.context;

import com.aqe.model.entity.QuoteOrder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * LiteFlow 策略执行上下文，组件间通过此对象传递数据
 */
@Data
public class StrategyContext {

    /** 策略实例ID */
    private Long instanceId;

    /** 策略实例优先级 */
    private Integer instancePriority;

    /** 交易标的 */
    private String symbol;

    /** 最新价格（由行情组件写入） */
    private double lastPrice;

    /** 最新成交量（由行情组件写入） */
    private int marketVolume;

    /** 实例参数（如 multiplier） */
    private Map<String, Object> params = new HashMap<>();

    /**
     * 节点配置（key=节点ID，value=节点属性Map）
     * 从 flow_json 中提取，每个节点的配置（如表达式、引用等）
     */
    private Map<String, Object> nodeConfigs = new HashMap<>();

    /** 输出订单（由输出组件写入） */
    private QuoteOrder outputOrder;

    public void putParam(String key, Object value) {
        params.put(key, value);
    }

    public Object getParam(String key) {
        return params.get(key);
    }
}
