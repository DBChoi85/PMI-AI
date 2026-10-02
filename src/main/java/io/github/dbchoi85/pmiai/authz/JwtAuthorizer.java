package io.github.dbchoi85.pmiai.authz;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dbchoi85.pmiai.model.Privilege;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

public final class JwtAuthorizer implements RequestAuthorizer {
    private final ObjectMapper mapper = new ObjectMapper();
    private final PublicKey verificationKey;
    private final String token;
    private final Privilege privilege;

    public JwtAuthorizer(Privilege privilege) {
        try {
            KeyPair keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
            this.verificationKey = keyPair.getPublic();
            this.privilege = privilege;
            this.token = createToken(privilege, keyPair.getPrivate());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean authorize(AuthorizationRequest request) {
        return verifySignature() && PrivilegePolicy.allows(privilege, request);
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

    private boolean verifySignature() {
        try {
            int lastDot = token.lastIndexOf('.');
            String signingInput = token.substring(0, lastDot);
            byte[] signature = Base64.getUrlDecoder().decode(token.substring(lastDot + 1));
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(verificationKey);
            verifier.update(signingInput.getBytes(StandardCharsets.US_ASCII));
            return verifier.verify(signature);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;
        }
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
