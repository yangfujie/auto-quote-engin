// ConditionMatcher.java
package com.aqe.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConditionMatcher {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static boolean match(String conditionJson, double price, int volume) {
        if (conditionJson == null || conditionJson.isEmpty()) return true; // 无条件则触发
        try {
            JsonNode root = mapper.readTree(conditionJson);
            String type = root.path("type").asText();
            if ("AND".equalsIgnoreCase(type)) {
                JsonNode conds = root.path("conditions");
                for (JsonNode cond : conds) {
                    if (!matchSingle(cond, price, volume)) return false;
                }
                return true;
            } else if ("OR".equalsIgnoreCase(type)) {
                JsonNode conds = root.path("conditions");
                for (JsonNode cond : conds) {
                    if (matchSingle(cond, price, volume)) return true;
                }
                return false;
            } else {
                return matchSingle(root, price, volume);
            }
        } catch (Exception e) {
            log.error("Parse condition error", e);
            return false;
        }
    }

    private static boolean matchSingle(JsonNode cond, double price, int volume) {
        String field = cond.path("field").asText();
        String operator = cond.path("operator").asText();
        double value = cond.path("value").asDouble();
        double actual = "price".equalsIgnoreCase(field) ? price : volume;
        switch (operator) {
            case ">": return actual > value;
            case "<": return actual < value;
            case ">=": return actual >= value;
            case "<=": return actual <= value;
            case "==": return Math.abs(actual - value) < 1e-9;
            default: return false;
        }
    }
}