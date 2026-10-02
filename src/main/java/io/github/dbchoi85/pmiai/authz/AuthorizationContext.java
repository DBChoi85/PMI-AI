package io.github.dbchoi85.pmiai.authz;

import java.util.Map;

public record AuthorizationContext(String taskId, Map<String, String> attributes) {
    public AuthorizationContext {
        attributes = Map.copyOf(attributes);
    }
}
