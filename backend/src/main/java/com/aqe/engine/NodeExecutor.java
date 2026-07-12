// NodeExecutor.java
package com.aqe.engine;

import java.util.Map;

public interface NodeExecutor {
    Object execute(Map<String, Object> context, Map<String, Object> properties);
}