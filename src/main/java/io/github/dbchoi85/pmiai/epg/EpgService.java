package io.github.dbchoi85.pmiai.epg;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dbchoi85.pmiai.model.Privilege;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.*;
import java.util.Base64;

public final class EpgService {
    private static final int CANONICAL_VERSION = 1;
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
            signer.update(canonicalSigningBytes(unsigned));
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
            verifier.update(canonicalSigningBytes(grant));
            return verifier.verify(Base64.getDecoder().decode(grant.signature()));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;
        }
    }

    public int encodedSize(EphemeralPrivilegeGrant grant) {
        try { return mapper.writeValueAsBytes(grant).length; }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    public byte[] canonicalSigningBytes(EphemeralPrivilegeGrant grant) {
        try {
            var bytes = new ByteArrayOutputStream();
            var out = new DataOutputStream(bytes);
            out.writeInt(CANONICAL_VERSION);
            writeString(out, grant.issuerAgent());
            writeString(out, grant.subjectAgent());
            writeString(out, grant.parentId());
            writeString(out, grant.taskId());

            var operations = grant.privilege().operations().stream().sorted().toList();
            out.writeInt(operations.size());
            for (String operation : operations) writeString(out, operation);
            writeString(out, grant.privilege().resource());
            out.writeLong(grant.privilege().expiresAtEpochSecond());

            out.writeLong(grant.issuedAtEpochSecond());
            writeString(out, grant.authorityId());
            out.writeInt(grant.delegationDepth());
            writeString(out, grant.subjectPublicKey());
            out.flush();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        if (value == null) throw new IllegalArgumentException("canonical field must not be null");
        byte[] encoded = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        out.writeInt(encoded.length);
        out.write(encoded);
    }
}
