package io.github.dbchoi85.pmiai.epg;

import io.github.dbchoi85.pmiai.model.Privilege;

public record EphemeralPrivilegeGrant(
        String issuerAgent,
        String subjectAgent,
        String parentId,
        String taskId,
        Privilege privilege,
        long issuedAtEpochSecond,
        String authorityId,
        int delegationDepth,
        String subjectPublicKey,
        String signature) {

    public EphemeralPrivilegeGrant withSignature(String value) {
        return new EphemeralPrivilegeGrant(issuerAgent, subjectAgent, parentId, taskId, privilege,
                issuedAtEpochSecond, authorityId, delegationDepth, subjectPublicKey, value);
    }
}
