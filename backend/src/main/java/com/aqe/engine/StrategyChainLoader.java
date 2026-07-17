package com.aqe.engine;

import com.aqe.model.entity.StrategyDef;
import com.aqe.repository.StrategyDefRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.aviator.AviatorEvaluator;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.flow.FlowBus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 策略链加载器
 * - 应用启动时从 DB 加载所有策略，转换为 LiteFlow Chain 并注册到 FlowBus
 * - 策略更新时调用 reloadChain() 实现热加载
 */
@Slf4j
@Component
@Order(1)
public class StrategyChainLoader implements CommandLineRunner {

    @Autowired
    private StrategyDefRepository strategyDefRepository;

    @Autowired
    private FlowJsonToElConverter converter;

    @Autowired
    private List<NodeComponent> nodeComponents;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void run(String... args) {
        ensureNodesRegistered();
        loadAllChains();
    }

    private void ensureNodesRegistered() {
        for (NodeComponent cmp : nodeComponents) {
            String nodeId = getNodeId(cmp);
            if (nodeId != null && !nodeId.isEmpty()) {
                FlowBus.addManagedNode(nodeId, cmp);
                log.debug("Registered node: {} -> {}", nodeId, cmp.getClass().getSimpleName());
            }
        }
        log.info("Ensured {} nodes registered in FlowBus", nodeComponents.size());
    }

    private String getNodeId(NodeComponent cmp) {
        Class<?> clazz = cmp.getClass();
        if (clazz.getName().contains("$$")) {
            clazz = clazz.getSuperclass();
        }
        com.yomahub.liteflow.annotation.LiteflowComponent lfAnn =
                clazz.getAnnotation(com.yomahub.liteflow.annotation.LiteflowComponent.class);
        if (lfAnn != null) {
            return lfAnn.value().isEmpty() ? lfAnn.id() : lfAnn.value();
        }
        org.springframework.stereotype.Component spAnn =
                clazz.getAnnotation(org.springframework.stereotype.Component.class);
        if (spAnn != null) {
            return spAnn.value();
        }
        return null;
    }
    /**
     * 启动时加载所有策略链
     */
    public void loadAllChains() {
        int count = 0;
        for (StrategyDef def : strategyDefRepository.findAll()) {
            try {
                reloadChain(def);
                count++;
            } catch (Exception e) {
                log.error("Failed to load chain for strategy def id={}", def.getId(), e);
            }
        }
        log.info("Loaded {} strategy chains from DB", count);
    }

    /**
     * 热加载/刷新单个策略链
     * 将 flow_json 转换为 EL 表达式，注册到 LiteFlow FlowBus
     */
    public void reloadChain(StrategyDef def) {
        String el = converter.convert(def.getFlowJson());

        // 预编译所有 calculate 节点的 Aviator 表达式
        preCompileExpressions(def.getFlowJson());

        // 动态注册 chain，若已存在则替换（热更新）
        String chainId = "chain_" + def.getId();
        LiteFlowChainELBuilder.createChain()
                .setChainName(chainId)
                .setEL(el)
                .build();
        log.info("Chain loaded/reloaded: {}, EL: {}", chainId, el);
    }

    /**
     * 预编译 flow_json 中所有 calculate 节点的 Aviator 表达式
     * 编译后的 Expression 对象存入 extractNodeConfigs 返回的配置中
     */
    private void preCompileExpressions(String flowJson) {
        try {
            JsonNode root = objectMapper.readTree(flowJson);
            for (JsonNode node : root.path("nodes")) {
                if ("calculate".equals(node.path("type").asText())) {
                    String expr = node.path("expression").asText();
                    if (expr != null && !expr.isEmpty()) {
                        AviatorEvaluator.compile(expr, true);
                        log.debug("Pre-compiled expression: {}", expr);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to pre-compile expressions", e);
        }
    }

    /**
     * 从 flow_json 提取每个节点的配置信息
     * 供 StrategyEngine 在执行前注入到 StrategyContext.nodeConfigs
     *
     * @return Map<节点ID, 节点配置Map>
     */
    public Map<String, Object> extractNodeConfigs(String flowJson) {
        Map<String, Object> configs = new HashMap<>();
        try {
            JsonNode root = objectMapper.readTree(flowJson);
            for (JsonNode node : root.path("nodes")) {
                String id = node.path("id").asText();
                String type = node.path("type").asText();
                Map<String, Object> config = new HashMap<>();
                config.put("id", id);
                config.put("type", type);

                switch (type) {
                    case "market":
                        if (node.has("symbol")) {
                            config.put("symbol", node.path("symbol").asText());
                        }
                        break;
                    case "calculate":
                        String expr = node.path("expression").asText();
                        config.put("expression", expr);
                        // 使用预编译的 Expression（AviatorEvaluator.compile(expr, true) 会缓存）
                        config.put("compiledExpression", AviatorEvaluator.compile(expr, true));
                        break;
                    case "output":
                        if (node.has("priceRef"))  config.put("priceRef",  node.path("priceRef").asText());
                        if (node.has("volumeRef")) config.put("volumeRef", node.path("volumeRef").asText());
                        if (node.has("sideRef"))   config.put("sideRef",   node.path("sideRef").asText());
                        break;
                }
                configs.put(id, config);
            }
        } catch (Exception e) {
            log.error("Failed to extract node configs from flow JSON", e);
        }
        return configs;
    }
}
