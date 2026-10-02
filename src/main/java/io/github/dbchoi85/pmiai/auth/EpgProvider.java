package io.github.dbchoi85.pmiai.auth;

import io.github.dbchoi85.pmiai.epg.*;
import io.github.dbchoi85.pmiai.model.Privilege;
import io.github.dbchoi85.pmiai.pmi.MiniPmi;
import org.bouncycastle.cert.X509AttributeCertificateHolder;

import java.security.KeyPair;
import java.security.PublicKey;

public final class EpgProvider implements AuthorizationProvider<EphemeralPrivilegeGrant> {
    private final MiniPmi pmi;
    private final EpgService epg = new EpgService();
    private final KeyPair rootKey;
    private final Privilege basePrivilege;
    private final X509AttributeCertificateHolder baseAc;
    private long sequence;

    public EpgProvider(MiniPmi pmi, Privilege basePrivilege) {
        this.pmi = pmi;
        this.basePrivilege = basePrivilege;
        this.rootKey = epg.newAgentKeyPair();
        this.baseAc = pmi.issue("root-agent", basePrivilege);
        if (!pmi.verify(baseAc)) throw new IllegalStateException("Base AC validation failed");
    }

    public KeyPair newAgentKeyPair() {
        return epg.newAgentKeyPair();
    }

    public EphemeralPrivilegeGrant issue(PublicKey childPublicKey) {
        return issue(childPublicKey, basePrivilege);
    }

    public EphemeralPrivilegeGrant issue(PublicKey childPublicKey, Privilege childPrivilege) {
        if (!childPrivilege.attenuates(basePrivilege)) {
            throw new IllegalArgumentException("Child privilege must attenuate base privilege");
        }
        return epg.issue("root-agent", "agent-" + (++sequence), "base-ac", "benchmark-task",
                childPrivilege, "pmi-aa", 1, childPublicKey, rootKey.getPrivate());
    }

    @Override
    public EphemeralPrivilegeGrant issue() {
        return issue(newAgentKeyPair().getPublic());
    }

    public boolean verify(EphemeralPrivilegeGrant credential) {
        return epg.verify(credential, rootKey.getPublic(), basePrivilege, System.currentTimeMillis() / 1000);
    }

    public int credentialSize(EphemeralPrivilegeGrant credential) { return epg.encodedSize(credential); }
    public long authorityInteractions() { return pmi.interactions(); }
    public X509AttributeCertificateHolder baseAc() { return baseAc; }
}
