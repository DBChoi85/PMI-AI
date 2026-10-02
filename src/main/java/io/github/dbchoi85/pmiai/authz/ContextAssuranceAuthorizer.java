package io.github.dbchoi85.pmiai.authz;

import io.github.dbchoi85.pmiai.assurance.AuthorityProvenance;
import io.github.dbchoi85.pmiai.model.Privilege;

import java.util.Map;

public final class ContextAssuranceAuthorizer {
    private final Privilege privilege;
    private final AuthorityProvenance provenance;
    private final ActionPolicy policy;
    private final Map<String, String> requiredContext;

    public ContextAssuranceAuthorizer(Privilege privilege, AuthorityProvenance provenance, ActionPolicy policy) {
        this(privilege, provenance, policy, Map.of());
    }

    public ContextAssuranceAuthorizer(Privilege privilege, AuthorityProvenance provenance, ActionPolicy policy,
                                      Map<String, String> requiredContext) {
        this.privilege = privilege;
        this.provenance = provenance;
        this.policy = policy;
        this.requiredContext = Map.copyOf(requiredContext);
    }

    public AuthorizationDecision authorize(AuthorizationRequest request, AuthorizationContext context) {
        if (!PrivilegePolicy.allows(privilege, request)) return AuthorizationDecision.DENY;
        if (!policy.operation().equals(request.operation())) return AuthorizationDecision.DENY;
        if (!resourceMatchesPolicy(request.resource(), policy.resourcePrefix())) return AuthorizationDecision.DENY;
        if (policy.requiredTaskId() != null && !policy.requiredTaskId().equals(context.taskId())) {
            return AuthorizationDecision.DENY;
        }
        for (var requirement : requiredContext.entrySet()) {
            if (!requirement.getValue().equals(context.attributes().get(requirement.getKey()))) {
                return AuthorizationDecision.DENY;
            }
        }
        if (!provenance.aal().satisfies(policy.requiredAal())) {
            return AuthorizationDecision.STEP_UP_REQUIRED;
        }
        return AuthorizationDecision.ALLOW;
    }

    private static boolean resourceMatchesPolicy(String resource, String prefix) {
        String normalized = prefix.endsWith("/") ? prefix : prefix + "/";
        return resource.equals(prefix) || resource.startsWith(normalized);
    }
}
