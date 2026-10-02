package io.github.dbchoi85.pmiai.authz;

import io.github.dbchoi85.pmiai.model.Privilege;

public final class PrivilegePolicy {
    private PrivilegePolicy() {}

    public static boolean allows(Privilege privilege, AuthorizationRequest request) {
        if (privilege.expiresAtEpochSecond() < request.nowEpochSecond()) return false;
        if (!privilege.operations().contains(request.operation())) return false;
        return resourceMatches(request.resource(), privilege.resource());
    }

    static boolean resourceMatches(String requested, String scope) {
        String requestPath = normalize(requested);
        String scopePath = normalize(scope);
        if (scope.endsWith("/**")) {
            return requestPath.equals(scopePath) || requestPath.startsWith(scopePath + "/");
        }
        return requestPath.equals(scopePath);
    }

    private static String normalize(String resource) {
        String value = resource;
        if (value.endsWith("/**")) value = value.substring(0, value.length() - 3);
        while (value.length() > 1 && value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value;
    }
}
