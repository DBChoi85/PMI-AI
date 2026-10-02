package io.github.dbchoi85.pmiai;

import io.github.dbchoi85.pmiai.auth.EpgProvider;
import io.github.dbchoi85.pmiai.model.Privilege;
import io.github.dbchoi85.pmiai.pmi.MiniPmi;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EpgProviderTest {
    @Test
    void issuesGrantUsingPreGeneratedChildKey() {
        var privilege = new Privilege(Set.of("read", "write"), "/project/A/",
                Instant.now().plusSeconds(600).getEpochSecond());
        var provider = new EpgProvider(new MiniPmi(), privilege);
        var childKey = provider.newAgentKeyPair();

        var grant = provider.issue(childKey.getPublic());

        assertTrue(provider.verify(grant));
        assertEquals(
                java.util.Base64.getEncoder().encodeToString(childKey.getPublic().getEncoded()),
                grant.subjectPublicKey());
        assertEquals(1, provider.authorityInteractions());
    }
}
