package io.github.dbchoi85.pmiai.authz;

import io.github.dbchoi85.pmiai.auth.EpgProvider;
import io.github.dbchoi85.pmiai.epg.EphemeralPrivilegeGrant;
import io.github.dbchoi85.pmiai.model.Privilege;
import io.github.dbchoi85.pmiai.pmi.MiniPmi;

public final class EpgAuthorizer implements RequestAuthorizer {
    private final EpgProvider provider;
    private final EphemeralPrivilegeGrant credential;

    public EpgAuthorizer(Privilege basePrivilege, Privilege childPrivilege) {
        this.provider = new EpgProvider(new MiniPmi(), basePrivilege);
        var childKey = provider.newAgentKeyPair();
        this.credential = provider.issue(childKey.getPublic(), childPrivilege);
    }

    @Override
    public boolean authorize(AuthorizationRequest request) {
        return provider.verify(credential) && PrivilegePolicy.allows(credential.privilege(), request);
    }

    @Override
    public int credentialSize() {
        return provider.credentialSize(credential);
    }
}
