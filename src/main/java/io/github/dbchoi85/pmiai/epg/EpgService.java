package io.github.dbchoi85.pmiai.epg;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dbchoi85.pmiai.model.Privilege;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

public final class EpgService {
    private final ObjectMapper mapper = new ObjectMapper();

    public KeyPair newAgentKeyPair() {
        try {
            return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public EphemeralPrivilegeGrant issue(String issuer, String subject, String parentId, String taskId,
                                         Privilege privilege, String authorityId, int depth,
                                         PublicKey subjectKey, PrivateKey issuerKey) {
        long now = System.currentTimeMillis() / 1000;
        var unsigned = new EphemeralPrivilegeGrant(issuer, subject, parentId, taskId, privilege, now,
                authorityId, depth, Base64.getEncoder().encodeToString(subjectKey.getEncoded()), "");
        try {
            Signature signer = Signature.getInstance("Ed25519");
            signer.initSign(issuerKey);
            signer.update(canonical(unsigned));
            return unsigned.withSignature(Base64.getEncoder().encodeToString(signer.sign()));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean verify(EphemeralPrivilegeGrant grant, PublicKey issuerKey, Privilege parentPrivilege,
                          long nowEpochSecond) {
        if (grant.privilege().expiresAtEpochSecond() < nowEpochSecond
                || !grant.privilege().attenuates(parentPrivilege)) return false;
        try {
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(issuerKey);
            verifier.update(canonical(grant.withSignature("")));
            return verifier.verify(Base64.getDecoder().decode(grant.signature()));
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    public int encodedSize(EphemeralPrivilegeGrant grant) {
        try { return mapper.writeValueAsBytes(grant).length; }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    private byte[] canonical(EphemeralPrivilegeGrant grant) {
        try { return mapper.writeValueAsString(grant).getBytes(StandardCharsets.UTF_8); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
}
