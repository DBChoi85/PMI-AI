package io.github.dbchoi85.pmiai.benchmark;

import io.github.dbchoi85.pmiai.assurance.*;
import io.github.dbchoi85.pmiai.authz.*;
import io.github.dbchoi85.pmiai.model.Privilege;
import org.openjdk.jmh.annotations.*;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class ContextAssuranceBenchmark {
    @Param({"1", "10", "100"})
    public int contextRules;

    private AuthorizationRequest request;
    private AuthorizationContext matchingContext;
    private ContextAssuranceAuthorizer aalSatisfied;
    private ContextAssuranceAuthorizer stepUpRequired;

    @Setup(Level.Trial)
    public void setup() {
        long now = Instant.now().getEpochSecond();
        var privilege = new Privilege(Set.of("read", "write"), "/project/A/**", now + 300);
        var provenanceAal3 = new AuthorityProvenance("human-1", IdentityAssuranceLevel.IAL2,
                AuthenticatorAssuranceLevel.AAL3, now - 60, "enterprise-idp", "nist-800-63");
        var provenanceAal1 = new AuthorityProvenance("human-1", IdentityAssuranceLevel.IAL2,
                AuthenticatorAssuranceLevel.AAL1, now - 60, "enterprise-idp", "nist-800-63");
        var policy = new ActionPolicy("write", "/project/A/src/", "task-42", AuthenticatorAssuranceLevel.AAL2);

        request = new AuthorizationRequest("write", "/project/A/src/module/file.txt", now);
        var attributes = new java.util.LinkedHashMap<String, String>();
        for (int i = 0; i < contextRules; i++) attributes.put("rule-" + i, "value-" + i);
        matchingContext = new AuthorizationContext("task-42", attributes);

        aalSatisfied = new ContextAssuranceAuthorizer(privilege, provenanceAal3, policy, attributes);
        stepUpRequired = new ContextAssuranceAuthorizer(privilege, provenanceAal1, policy, attributes);
    }

    @Benchmark
    public AuthorizationDecision aalPolicySatisfied() {
        return aalSatisfied.authorize(request, matchingContext);
    }

    @Benchmark
    public AuthorizationDecision stepUpTrigger() {
        return stepUpRequired.authorize(request, matchingContext);
    }
}
