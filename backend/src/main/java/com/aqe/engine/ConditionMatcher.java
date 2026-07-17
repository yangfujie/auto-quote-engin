// ConditionMatcher.java
package com.aqe.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 条件匹配器
 * 支持预解析：启动时将 triggerConditions JSON 解析为 ParsedCondition，
 * 主链路上只做纯内存比较，不再解析 JSON
 */
@Slf4j
public class ConditionMatcher {
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * 预解析后的触发条件
     */
    public static class ParsedCondition {
        final String logicType; // "AND" / "OR" / null(单条件)
        final List<SingleCondition> conditions;

        ParsedCondition(String logicType, List<SingleCondition> conditions) {
            this.logicType = logicType;
            this.conditions = conditions;
        }

        public boolean match(double price, int volume) {
            if (conditions.isEmpty()) return true;
            if ("AND".equalsIgnoreCase(logicType)) {
                for (SingleCondition c : conditions) {
                    if (!c.match(price, volume)) return false;
                }
                return true;
            } else if ("OR".equalsIgnoreCase(logicType)) {
                for (SingleCondition c : conditions) {
                    if (c.match(price, volume)) return true;
                }
                return false;
            } else {
                // 单条件
                return conditions.get(0).match(price, volume);
            }
        }
    }

    /**
     * 单个条件（field + operator + value），纯内存比较
     */
    public static class SingleCondition {
        final boolean isPrice; // true=price, false=volume
        final String operator;
        final double value;

        SingleCondition(String field, String operator, double value) {
            this.isPrice = "price".equalsIgnoreCase(field);
            this.operator = operator;
            this.value = value;
        }

        boolean match(double price, int volume) {
            double actual = isPrice ? price : volume;
            switch (operator) {
                case ">":  return actual > value;
                case "<":  return actual < value;
                case ">=": return actual >= value;
                case "<=": return actual <= value;
                case "==": return Math.abs(actual - value) < 1e-9;
                default:   return false;
            }
        }
    }

    /**
     * 预解析 triggerConditions JSON 为 ParsedCondition
     * 启动时调用一次，主链路不再解析 JSON
     */
    public static ParsedCondition parse(String conditionJson) {
        if (conditionJson == null || conditionJson.isEmpty()) {
            return new ParsedCondition(null, Collections.emptyList());
        }
        try {
            JsonNode root = mapper.readTree(conditionJson);
            String type = root.path("type").asText();
            List<SingleCondition> list = new ArrayList<>();

            if ("AND".equalsIgnoreCase(type) || "OR".equalsIgnoreCase(type)) {
                JsonNode conds = root.path("conditions");
                for (JsonNode cond : conds) {
                    list.add(parseSingle(cond));
                }
                return new ParsedCondition(type, list);
            } else {
                list.add(parseSingle(root));
                return new ParsedCondition(null, list);
            }
        } catch (Exception e) {
            log.error("Parse condition error", e);
            return new ParsedCondition(null, Collections.emptyList());
        }
    }

    private static SingleCondition parseSingle(JsonNode cond) {
        String field = cond.path("field").asText();
        String operator = cond.path("operator").asText();
        double value = cond.path("value").asDouble();
        return new SingleCondition(field, operator, value);
    }

    /**
     * 兼容旧接口：直接用 JSON 字符串匹配（不推荐，主链路已改用预解析）
     */
    public static boolean match(String conditionJson, double price, int volume) {
        return parse(conditionJson).match(price, volume);
    }
}