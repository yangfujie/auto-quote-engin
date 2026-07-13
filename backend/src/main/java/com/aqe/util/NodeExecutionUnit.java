package com.aqe.util;

import com.aqe.engine.NodeExecutor;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class NodeExecutionUnit {
    private NodeExecutor executor;
    private Map<String, Object> properties;
}
