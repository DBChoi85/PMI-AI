package io.github.dbchoi85.pmiai;

import io.github.dbchoi85.pmiai.model.Privilege;
import io.github.dbchoi85.pmiai.pmi.MiniPmi;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MiniPmiTest {
    @Test
    void issuesAndVerifiesAttributeCertificate() {
        var pmi = new MiniPmi();
        var privilege = new Privilege(Set.of("read", "write"), "/project/A/", Instant.now().plusSeconds(600).getEpochSecond());
        var ac = pmi.issue("agent-1", privilege);
        assertTrue(pmi.verify(ac));
        assertEquals(privilege.resource(), pmi.readPrivilege(ac).resource());
        assertEquals(1, pmi.interactions());
    }
}
