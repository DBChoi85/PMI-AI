package io.github.dbchoi85.pmiai.auth;

import io.github.dbchoi85.pmiai.model.Privilege;
import io.github.dbchoi85.pmiai.pmi.MiniPmi;
import org.bouncycastle.cert.X509AttributeCertificateHolder;

public final class AcPerAgentProvider implements AuthorizationProvider<X509AttributeCertificateHolder> {
    private final MiniPmi pmi;
    private final Privilege privilege;
    private long sequence;

    public AcPerAgentProvider(MiniPmi pmi, Privilege privilege) {
        this.pmi = pmi; this.privilege = privilege;
    }

    public X509AttributeCertificateHolder issue() {
        return pmi.issue("agent-" + (++sequence), privilege);
    }

    public boolean verify(X509AttributeCertificateHolder credential) { return pmi.verify(credential); }
    public int credentialSize(X509AttributeCertificateHolder credential) {
        try { return credential.getEncoded().length; } catch (Exception e) { throw new IllegalStateException(e); }
    }
    public long authorityInteractions() { return pmi.interactions(); }
}
