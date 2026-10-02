package io.github.dbchoi85.pmiai.authz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dbchoi85.pmiai.model.Privilege;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

public final class JwtAuthorizer implements RequestAuthorizer {
    private final ObjectMapper mapper = new ObjectMapper();
    private final PublicKey verificationKey;
    private final String token;

    public JwtAuthorizer(Privilege privilege) {
        try {
            KeyPair keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
            this.verificationKey = keyPair.getPublic();
            this.token = createToken(privilege, keyPair.getPrivate());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    JwtAuthorizer(PublicKey verificationKey, String token) {
        this.verificationKey = Objects.requireNonNull(verificationKey);
        this.token = Objects.requireNonNull(token);
    }

    @Override
    public boolean authorize(AuthorizationRequest request) {
        Privilege signedPrivilege = verifiedPrivilege();
        return signedPrivilege != null && PrivilegePolicy.allows(signedPrivilege, request);
    }

    @Override public int credentialSize() { return token.getBytes(StandardCharsets.UTF_8).length; }

    private String createToken(Privilege p, PrivateKey privateKey) throws GeneralSecurityException {
        try {
            String header = base64Url(mapper.writeValueAsBytes(Map.of("alg", "EdDSA", "typ", "JWT")));
            var payloadMap = new TreeMap<String, Object>();
            payloadMap.put("exp", p.expiresAtEpochSecond());
            payloadMap.put("ops", p.operations().stream().sorted().toList());
            payloadMap.put("res", p.resource());
            String payload = base64Url(mapper.writeValueAsBytes(payloadMap));
            String signingInput = header + "." + payload;
            Signature signer = Signature.getInstance("Ed25519");
            signer.initSign(privateKey);
            signer.update(signingInput.getBytes(StandardCharsets.US_ASCII));
            return signingInput + "." + base64Url(signer.sign());
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private Privilege verifiedPrivilege() {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) return null;
            String signingInput = parts[0] + "." + parts[1];

            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(verificationKey);
            verifier.update(signingInput.getBytes(StandardCharsets.US_ASCII));
            if (!verifier.verify(Base64.getUrlDecoder().decode(parts[2]))) return null;

            JsonNode header = mapper.readTree(Base64.getUrlDecoder().decode(parts[0]));
            if (!"EdDSA".equals(header.path("alg").asText()) || !"JWT".equals(header.path("typ").asText())) return null;

            JsonNode payload = mapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
            if (!payload.path("exp").canConvertToLong() || !payload.path("res").isTextual() || !payload.path("ops").isArray()) return null;
            var operations = new HashSet<String>();
            for (JsonNode op : payload.path("ops")) {
                if (!op.isTextual() || op.asText().isBlank()) return null;
                operations.add(op.asText());
            }
            if (operations.isEmpty()) return null;
            return new Privilege(operations, payload.path("res").asText(), payload.path("exp").longValue());
        } catch (Exception e) {
            return null;
        }
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
