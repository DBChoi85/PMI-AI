package io.github.dbchoi85.pmiai.authz;

import io.github.dbchoi85.pmiai.model.Privilege;
import io.github.dbchoi85.pmiai.pmi.MiniPmi;
import org.bouncycastle.cert.X509AttributeCertificateHolder;

public final class StaticPmiAuthorizer implements RequestAuthorizer {
    private final MiniPmi pmi;
    private final X509AttributeCertificateHolder credential;
    private final Privilege privilege;

    public StaticPmiAuthorizer(Privilege privilege) {
        this.pmi = new MiniPmi();
        this.credential = pmi.issue("root-agent", privilege);
        this.privilege = pmi.readPrivilege(credential);
    }

    @Override
    public boolean authorize(AuthorizationRequest request) {
        return pmi.verify(credential) && PrivilegePolicy.allows(privilege, request);
    }

    @Override
    public int credentialSize() {
        try { return credential.getEncoded().length; }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
}
