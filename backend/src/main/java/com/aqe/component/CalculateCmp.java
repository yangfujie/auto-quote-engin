package com.aqe.component;

import com.aqe.context.StrategyContext;
import com.googlecode.aviator.Expression;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.extern.slf4j.Slf4j;
import com.yomahub.liteflow.annotation.LiteflowComponent;

import java.util.HashMap;
import java.util.Map;

/**
 * LiteFlow 计算组件
 * tag = 节点ID，用于从 nodeConfigs 中获取预编译表达式
 */
@Slf4j
@LiteflowComponent("calculateCmp")
public class CalculateCmp extends NodeComponent {

    @Override
    public void process() throws Exception {
        StrategyContext ctx = (StrategyContext) this.getRequestData();
        if (ctx == null) {
            throw new IllegalStateException("StrategyContext is null, requestData not set");
        }

        String nodeId = this.getTag();
        if (nodeId == null || nodeId.isEmpty()) {
            throw new IllegalStateException("Node tag is null or empty, please set tag for calculate node");
        }

        Map<String, Object> nodeConfigs = ctx.getNodeConfigs();
        if (nodeConfigs == null) {
            throw new IllegalStateException("nodeConfigs is null in context");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> config = (Map<String, Object>) nodeConfigs.get(nodeId);
        if (config == null) {
            throw new IllegalStateException("Node config not found for calculate node: " + nodeId);
        }

        Expression compiled = (Expression) config.get("compiledExpression");
        if (compiled == null) {
            throw new IllegalStateException("compiledExpression not found for node: " + nodeId);
        }

        // 构建变量环境：行情数据 + 实例参数
        Map<String, Object> env = buildEnv(ctx);
        Object result = compiled.execute(env);

        // 将计算结果存入 params，供后续节点引用
        ctx.putParam(nodeId, result);

        log.debug("CalculateCmp [{}]: expression='{}', result={}",
                nodeId, config.get("expression"), result);
    }

    /**
     * 构建 Aviator 变量环境
     */
    private Map<String, Object> buildEnv(StrategyContext ctx) {
        Map<String, Object> env = new HashMap<>();
        // 行情数据
        env.put("lastPrice", ctx.getLastPrice());
        env.put("volume", ctx.getMarketVolume());
        // 实例参数 + 前置节点计算结果
        env.putAll(ctx.getParams());
        return env;
    }
}
