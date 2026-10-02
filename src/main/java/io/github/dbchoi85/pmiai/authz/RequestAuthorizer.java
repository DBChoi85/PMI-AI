package io.github.dbchoi85.pmiai.authz;

public interface RequestAuthorizer {
    boolean authorize(AuthorizationRequest request);
    int credentialSize();
}
