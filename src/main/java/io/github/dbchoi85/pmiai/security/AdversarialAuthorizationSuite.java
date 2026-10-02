package io.github.dbchoi85.pmiai.security;

import io.github.dbchoi85.pmiai.assurance.*;
import io.github.dbchoi85.pmiai.authz.*;
import io.github.dbchoi85.pmiai.epg.*;
import io.github.dbchoi85.pmiai.model.Privilege;

import java.time.Instant;
import java.util.*;

public final class AdversarialAuthorizationSuite {
    public AdversarialSuiteResult run() {
        long now = Instant.now().getEpochSecond();
        var results = new ArrayList<AdversarialCaseResult>();

        runGrantCases(now, results);
        runChainCases(now, results);
        runContextAssuranceCases(now, results);

        return new AdversarialSuiteResult(results);
    }

    private static void runGrantCases(long now, List<AdversarialCaseResult> results) {
        var epg = new EpgService();
        var root = epg.newAgentKeyPair();
        var child = epg.newAgentKeyPair();
        var parent = new Privilege(Set.of("read", "write"), "/project/A/**", now + 3600);
        var valid = new Privilege(Set.of("read"), "/project/A/src/**", now + 300);
        var grant = epg.issue("root", "child", "base-ac", "task-42", valid, "pmi-aa", 1,
                child.getPublic(), root.getPrivate());

        var tampered = new EphemeralPrivilegeGrant(grant.issuerAgent(), grant.subjectAgent(), grant.parentId(),
                "tampered-task", grant.privilege(), grant.issuedAtEpochSecond(), grant.authorityId(),
                grant.delegationDepth(), grant.subjectPublicKey(), grant.signature());
        add(results, AttackCategory.SIGNATURE_TAMPERING,
                !epg.verify(tampered, root.getPublic(), parent, now), "REJECT", "signature verification");

        var operation = new Privilege(Set.of("read", "admin"), "/project/A/src/**", now + 300);
        var operationGrant = epg.issue("root", "child", "base-ac", "task-42", operation, "pmi-aa", 1,
                child.getPublic(), root.getPrivate());
        add(results, AttackCategory.OPERATION_ESCALATION,
                !epg.verify(operationGrant, root.getPublic(), parent, now), "REJECT", "privilege attenuation");

        var resource = new Privilege(Set.of("read"), "/project/AB/**", now + 300);
        var resourceGrant = epg.issue("root", "child", "base-ac", "task-42", resource, "pmi-aa", 1,
                child.getPublic(), root.getPrivate());
        add(results, AttackCategory.RESOURCE_ESCALATION,
                !epg.verify(resourceGrant, root.getPublic(), parent, now), "REJECT", "resource attenuation");

        var expired = new Privilege(Set.of("read"), "/project/A/src/**", now - 1);
        var expiredGrant = epg.issue("root", "child", "base-ac", "task-42", expired, "pmi-aa", 1,
                child.getPublic(), root.getPrivate());
        add(results, AttackCategory.EXPIRED_GRANT,
                !epg.verify(expiredGrant, root.getPublic(), parent, now), "REJECT", "expiry validation");
    }

    private static void runChainCases(long now, List<AdversarialCaseResult> results) {
        var service = new DelegationChainService();
        var root = new Privilege(Set.of("read"), "/project/A/**", now + 3600);
        var chain = service.build(3, root);

        var brokenParent = replace(chain, 1, copy(chain.grants().get(1), "wrong-parent",
                chain.grants().get(1).delegationDepth()));
        add(results, AttackCategory.BROKEN_PARENT, !service.verify(brokenParent, now),
                "REJECT", "parent linkage");

        var invalidDepth = replace(chain, 1, copy(chain.grants().get(1),
                chain.grants().get(1).parentId(), 9));
        add(results, AttackCategory.INVALID_DEPTH, !service.verify(invalidDepth, now),
                "REJECT", "delegation depth");
    }

    private static void runContextAssuranceCases(long now, List<AdversarialCaseResult> results) {
        var privilege = new Privilege(Set.of("write"), "/project/A/**", now + 300);
        var policy = new ActionPolicy("write", "/project/A/src/", "task-42", AuthenticatorAssuranceLevel.AAL2);
        var request = new AuthorizationRequest("write", "/project/A/src/file.txt", now);

        var aal3 = new AuthorityProvenance("human-1", IdentityAssuranceLevel.IAL2,
                AuthenticatorAssuranceLevel.AAL3, now - 60, "enterprise-idp", "nist-800-63");
        var contextAuthorizer = new ContextAssuranceAuthorizer(privilege, aal3, policy, Map.of("env", "prod"));
        var contextDecision = contextAuthorizer.authorize(request,
                new AuthorizationContext("task-42", Map.of("env", "dev")));
        add(results, AttackCategory.CONTEXT_MISMATCH, contextDecision == AuthorizationDecision.DENY,
                "DENY", contextDecision.name());

        var aal1 = new AuthorityProvenance("human-1", IdentityAssuranceLevel.IAL2,
                AuthenticatorAssuranceLevel.AAL1, now - 60, "enterprise-idp", "nist-800-63");
        var aalAuthorizer = new ContextAssuranceAuthorizer(privilege, aal1, policy);
        var aalDecision = aalAuthorizer.authorize(request, new AuthorizationContext("task-42", Map.of()));
        add(results, AttackCategory.INSUFFICIENT_AAL, aalDecision == AuthorizationDecision.STEP_UP_REQUIRED,
                "STEP_UP_REQUIRED", aalDecision.name());
    }

    private static void add(List<AdversarialCaseResult> results, AttackCategory category,
                            boolean blocked, String expected, String actual) {
        results.add(new AdversarialCaseResult(category, blocked, expected, actual));
    }

    private static DelegationChainService.DelegationChain replace(
            DelegationChainService.DelegationChain chain, int index, EphemeralPrivilegeGrant replacement) {
        var grants = new ArrayList<>(chain.grants());
        grants.set(index, replacement);
        return new DelegationChainService.DelegationChain(chain.rootPrivilege(), chain.keys(), grants);
    }

    private static EphemeralPrivilegeGrant copy(EphemeralPrivilegeGrant source, String parentId, int depth) {
        return new EphemeralPrivilegeGrant(source.issuerAgent(), source.subjectAgent(), parentId, source.taskId(),
                source.privilege(), source.issuedAtEpochSecond(), source.authorityId(), depth,
                source.subjectPublicKey(), source.signature());
    }
}
