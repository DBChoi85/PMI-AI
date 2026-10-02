package io.github.dbchoi85.pmiai.authz;

public final class NoAuthAuthorizer implements RequestAuthorizer {
    @Override public boolean authorize(AuthorizationRequest request) { return true; }
    @Override public int credentialSize() { return 0; }
}
