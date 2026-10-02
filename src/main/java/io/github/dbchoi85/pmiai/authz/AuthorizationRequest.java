package io.github.dbchoi85.pmiai.authz;

public record AuthorizationRequest(String operation, String resource, long nowEpochSecond) {}
