package io.github.dbchoi85.pmiai;

import io.github.dbchoi85.pmiai.authz.*;
import io.github.dbchoi85.pmiai.model.Privilege;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizationBaselineTest {
    @Test
    void allBaselinesAuthorizeTheSameValidReadRequest() {
        long now = Instant.now().getEpochSecond();
        var base = new Privilege(Set.of("read", "write", "execute"), "/project/A/**", now + 3600);
        var child = new Privilege(Set.of("read", "write"), "/project/A/src/**", now + 300);
        var request = new AuthorizationRequest("read", "/project/A/src/module/file.txt", now);

        assertTrue(new NoAuthAuthorizer().authorize(request));
        assertTrue(new JwtAuthorizer(child).authorize(request));
        assertTrue(new StaticPmiAuthorizer(child).authorize(request));
        assertTrue(new EpgAuthorizer(base, child).authorize(request));
    }

    @Test
    void credentialBasedBaselinesRejectOutOfScopeOrUnauthorizedRequests() {
        long now = Instant.now().getEpochSecond();
        var base = new Privilege(Set.of("read", "write", "execute"), "/project/A/**", now + 3600);
        var child = new Privilege(Set.of("read", "write"), "/project/A/src/**", now + 300);
        RequestAuthorizer[] authorizers = {
                new JwtAuthorizer(child),
                new StaticPmiAuthorizer(child),
                new EpgAuthorizer(base, child)
        };

        for (RequestAuthorizer authorizer : authorizers) {
            assertFalse(authorizer.authorize(new AuthorizationRequest("execute", "/project/A/src/x", now)));
            assertFalse(authorizer.authorize(new AuthorizationRequest("read", "/project/AB/src/x", now)));
        }
    }

    @Test
    void proposedProviderRejectsPrivilegeExpansionAtIssuance() {
        long now = Instant.now().getEpochSecond();
        var base = new Privilege(Set.of("read"), "/project/A/**", now + 300);
        var expanded = new Privilege(Set.of("read", "write"), "/project/A/**", now + 300);

        assertThrows(IllegalArgumentException.class, () -> new EpgAuthorizer(base, expanded));
    }
}
