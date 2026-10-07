package io.github.dbchoi85.pmiai.epg;

import java.security.PublicKey;
import java.util.Base64;

public final class EpgSubjectBinding {
    private EpgSubjectBinding() {}

    public static boolean matches(EphemeralPrivilegeGrant grant, String callerAgent, PublicKey callerKey) {
        if (grant == null || callerAgent == null || callerKey == null) return false;
        String encodedCallerKey = Base64.getEncoder().encodeToString(callerKey.getEncoded());
        return grant.subjectAgent().equals(callerAgent) && grant.subjectPublicKey().equals(encodedCallerKey);
    }
}
