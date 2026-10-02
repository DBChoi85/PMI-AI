package io.github.dbchoi85.pmiai;

import io.github.dbchoi85.pmiai.assurance.*;
import io.github.dbchoi85.pmiai.authz.*;
import io.github.dbchoi85.pmiai.model.Privilege;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ContextAssuranceAuthorizerTest {
    @Test
    void allowsWhenContextAndHumanAalProvenanceSatisfyPolicy() {
        var fixture = fixture(AuthenticatorAssuranceLevel.AAL3, AuthenticatorAssuranceLevel.AAL2);
        assertEquals(AuthorizationDecision.ALLOW,
                fixture.authorizer.authorize(fixture.request, new AuthorizationContext("task-42", Map.of())));
    }

    @Test
    void requestsStepUpWhenHumanAuthenticationProvenanceIsInsufficient() {
        var fixture = fixture(AuthenticatorAssuranceLevel.AAL1, AuthenticatorAssuranceLevel.AAL2);
        assertEquals(AuthorizationDecision.STEP_UP_REQUIRED,
                fixture.authorizer.authorize(fixture.request, new AuthorizationContext("task-42", Map.of())));
    }

    @Test
    void deniesWrongTaskOperationAndResource() {
        var fixture = fixture(AuthenticatorAssuranceLevel.AAL3, AuthenticatorAssuranceLevel.AAL2);
        assertEquals(AuthorizationDecision.DENY,
                fixture.authorizer.authorize(fixture.request, new AuthorizationContext("other-task", Map.of())));

        long now = fixture.request.nowEpochSecond();
        assertEquals(AuthorizationDecision.DENY, fixture.authorizer.authorize(
                new AuthorizationRequest("execute", fixture.request.resource(), now),
                new AuthorizationContext("task-42", Map.of())));
        assertEquals(AuthorizationDecision.DENY, fixture.authorizer.authorize(
                new AuthorizationRequest("write", "/project/AB/src/x", now),
                new AuthorizationContext("task-42", Map.of())));
    }

    private static Fixture fixture(AuthenticatorAssuranceLevel actual, AuthenticatorAssuranceLevel required) {
        long now = Instant.now().getEpochSecond();
        var privilege = new Privilege(Set.of("read", "write"), "/project/A/**", now + 300);
        var provenance = new AuthorityProvenance("human-1", IdentityAssuranceLevel.IAL2, actual,
                now - 30, "enterprise-idp", "nist-800-63");
        var policy = new ActionPolicy("write", "/project/A/src/", "task-42", required);
        var request = new AuthorizationRequest("write", "/project/A/src/file.txt", now);
        return new Fixture(new ContextAssuranceAuthorizer(privilege, provenance, policy), request);
    }

    private record Fixture(ContextAssuranceAuthorizer authorizer, AuthorizationRequest request) {}
}
