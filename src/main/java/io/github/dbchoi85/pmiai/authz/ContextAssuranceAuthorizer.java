package io.github.dbchoi85.pmiai.authz;

import io.github.dbchoi85.pmiai.assurance.AuthorityProvenance;
import io.github.dbchoi85.pmiai.model.Privilege;

public final class ContextAssuranceAuthorizer {
    private final Privilege privilege;
    private final AuthorityProvenance provenance;
    private final ActionPolicy policy;

    public ContextAssuranceAuthorizer(Privilege privilege, AuthorityProvenance provenance, ActionPolicy policy) {
        this.privilege = privilege;
        this.provenance = provenance;
        this.policy = policy;
    }

    public AuthorizationDecision authorize(AuthorizationRequest request, AuthorizationContext context) {
        if (!PrivilegePolicy.allows(privilege, request)) return AuthorizationDecision.DENY;
        if (!policy.operation().equals(request.operation())) return AuthorizationDecision.DENY;
        if (!request.resource().startsWith(policy.resourcePrefix())) return AuthorizationDecision.DENY;
        if (policy.requiredTaskId() != null && !policy.requiredTaskId().equals(context.taskId())) {
            return AuthorizationDecision.DENY;
        }
        if (!provenance.aal().satisfies(policy.requiredAal())) {
            return AuthorizationDecision.STEP_UP_REQUIRED;
        }
        return AuthorizationDecision.ALLOW;
    }
}
