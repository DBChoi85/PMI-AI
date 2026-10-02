package io.github.dbchoi85.pmiai;

import io.github.dbchoi85.pmiai.authz.*;
import io.github.dbchoi85.pmiai.model.Privilege;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizationBaselineTest {
    @Test
    void allBaselinesAuthorizeTheSameValidReadRequest() {
        long now = Instant.now().getEpochSecond();
        var base = new Privilege(Set.of("read", "write", "execute"), "/project/A/**", now + 3600);
        var child = new Privilege(Set.of("read", "write"), "/project/A/src/**", now + 300);
        var request = new AuthorizationRequest("read", "/project/A/src/module/file.txt", now);

        assertTrue(new NoAuthAuthorizer().authorize(request));
        assertTrue(new JwtAuthorizer(child).authorize(request));
        assertTrue(new StaticPmiAuthorizer(child).authorize(request));
        assertTrue(new EpgAuthorizer(base, child).authorize(request));
    }

    @Test
    void credentialBasedBaselinesRejectOutOfScopeOrUnauthorizedRequests() {
        long now = Instant.now().getEpochSecond();
        var base = new Privilege(Set.of("read", "write", "execute"), "/project/A/**", now + 3600);
        var child = new Privilege(Set.of("read", "write"), "/project/A/src/**", now + 300);
        RequestAuthorizer[] authorizers = {
                new JwtAuthorizer(child),
                new StaticPmiAuthorizer(child),
                new EpgAuthorizer(base, child)
        };

        for (RequestAuthorizer authorizer : authorizers) {
            assertFalse(authorizer.authorize(new AuthorizationRequest("execute", "/project/A/src/x", now)));
            assertFalse(authorizer.authorize(new AuthorizationRequest("read", "/project/AB/src/x", now)));
        }
    }

    @Test
    void jwtRejectsTamperedSignedClaims() throws Exception {
        long now = Instant.now().getEpochSecond();
        var jwt = new JwtAuthorizer(new Privilege(Set.of("read"), "/project/A/**", now + 300));

        Field tokenField = JwtAuthorizer.class.getDeclaredField("token");
        tokenField.setAccessible(true);
        String token = (String) tokenField.get(jwt);
        String[] parts = token.split("\\.", -1);
        byte[] payload = java.util.Base64.getUrlDecoder().decode(parts[1]);
        String changed = new String(payload, java.nio.charset.StandardCharsets.UTF_8)
                .replace("\\"read\\"", "\\"execute\\"");
        String tampered = parts[0] + "." + java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(changed.getBytes(java.nio.charset.StandardCharsets.UTF_8)) + "." + parts[2];

        Field verificationKeyField = JwtAuthorizer.class.getDeclaredField("verificationKey");
        verificationKeyField.setAccessible(true);
        var key = (java.security.PublicKey) verificationKeyField.get(jwt);
        var constructor = JwtAuthorizer.class.getDeclaredConstructor(java.security.PublicKey.class, String.class);
        constructor.setAccessible(true);
        var tamperedAuthorizer = constructor.newInstance(key, tampered);

        assertFalse(tamperedAuthorizer.authorize(
                new AuthorizationRequest("execute", "/project/A/x", now)));
    }

    @Test
    void jwtRejectsMalformedCompactToken() throws Exception {
        var constructor = JwtAuthorizer.class.getDeclaredConstructor(java.security.PublicKey.class, String.class);
        constructor.setAccessible(true);
        var key = java.security.KeyPairGenerator.getInstance("Ed25519").generateKeyPair().getPublic();
        var malformed = constructor.newInstance(key, "not.a.valid.jwt");
        assertFalse(malformed.authorize(new AuthorizationRequest("read", "/project/A/x",
                Instant.now().getEpochSecond())));
    }

    @Test
    void proposedProviderRejectsPrivilegeExpansionAtIssuance() {
        long now = Instant.now().getEpochSecond();
        var base = new Privilege(Set.of("read"), "/project/A/**", now + 300);
        var expanded = new Privilege(Set.of("read", "write"), "/project/A/**", now + 300);

        assertThrows(IllegalArgumentException.class, () -> new EpgAuthorizer(base, expanded));
    }
}
