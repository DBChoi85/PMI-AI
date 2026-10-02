package io.github.dbchoi85.pmiai;

import io.github.dbchoi85.pmiai.epg.EpgService;
import io.github.dbchoi85.pmiai.model.Privilege;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EpgServiceTest {
    @Test
    void acceptsAttenuatedPrivilegeAndRejectsEscalation() {
        var service = new EpgService();
        var root = service.newAgentKeyPair();
        var child = service.newAgentKeyPair();
        long now = Instant.now().getEpochSecond();
        var parent = new Privilege(Set.of("read", "write"), "/project/A/", now + 3600);
        var allowed = new Privilege(Set.of("read"), "/project/A/src/", now + 300);
        var escalation = new Privilege(Set.of("admin"), "/project/A/src/", now + 300);

        var good = service.issue("root", "child", "base", "t1", allowed, "aa", 1, child.getPublic(), root.getPrivate());
        var bad = service.issue("root", "child", "base", "t1", escalation, "aa", 1, child.getPublic(), root.getPrivate());

        assertTrue(service.verify(good, root.getPublic(), parent, now));
        assertFalse(service.verify(bad, root.getPublic(), parent, now));
    }
}
