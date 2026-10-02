package io.github.dbchoi85.pmiai;

import io.github.dbchoi85.pmiai.epg.EphemeralPrivilegeGrant;
import io.github.dbchoi85.pmiai.epg.EpgService;
import io.github.dbchoi85.pmiai.model.Privilege;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EpgServiceTest {
    @Test
    void acceptsAttenuatedPrivilegeAndRejectsEscalation() {
        var service = new EpgService();
        var root = service.newAgentKeyPair();
        var child = service.newAgentKeyPair();
        long now = Instant.now().getEpochSecond();
        var parent = new Privilege(Set.of("read", "write"), "/project/A/**", now + 3600);
        var allowed = new Privilege(Set.of("read"), "/project/A/src/**", now + 300);
        var escalation = new Privilege(Set.of("admin"), "/project/A/src/**", now + 300);

        var good = service.issue("root", "child", "base", "t1", allowed, "aa", 1, child.getPublic(), root.getPrivate());
        var bad = service.issue("root", "child", "base", "t1", escalation, "aa", 1, child.getPublic(), root.getPrivate());

        assertTrue(service.verify(good, root.getPublic(), parent, now));
        assertFalse(service.verify(bad, root.getPublic(), parent, now));
    }

    @Test
    void rejectsSiblingPrefixAndLifetimeExpansion() {
        long now = Instant.now().getEpochSecond();
        var parent = new Privilege(Set.of("read", "write"), "/project/A/**", now + 300);

        assertTrue(new Privilege(Set.of("read"), "/project/A/src/**", now + 60).attenuates(parent));
        assertFalse(new Privilege(Set.of("read"), "/project/AB/**", now + 60).attenuates(parent));
        assertFalse(new Privilege(Set.of("read"), "/project/A/src/**", now + 301).attenuates(parent));
    }

    @Test
    void canonicalEncodingIsIndependentOfOperationInsertionOrder() {
        var service = new EpgService();
        long expiry = Instant.now().plusSeconds(300).getEpochSecond();

        var firstOps = new LinkedHashSet<String>();
        firstOps.add("write");
        firstOps.add("read");
        var secondOps = new LinkedHashSet<String>();
        secondOps.add("read");
        secondOps.add("write");

        var a = grant(new Privilege(firstOps, "/project/A/src/**", expiry));
        var b = grant(new Privilege(secondOps, "/project/A/src/**", expiry));

        assertArrayEquals(service.canonicalSigningBytes(a), service.canonicalSigningBytes(b));
    }

    @Test
    void rejectsTamperingOfSignedFields() {
        var service = new EpgService();
        var root = service.newAgentKeyPair();
        var child = service.newAgentKeyPair();
        long now = Instant.now().getEpochSecond();
        var parent = new Privilege(Set.of("read", "write"), "/project/A/**", now + 3600);
        var privilege = new Privilege(Set.of("read"), "/project/A/src/**", now + 300);

        var original = service.issue("root", "child", "base", "task-1", privilege, "aa", 1,
                child.getPublic(), root.getPrivate());
        assertTrue(service.verify(original, root.getPublic(), parent, now));

        assertFalse(service.verify(copy(original, "task-2", original.parentId(), original.privilege(),
                original.delegationDepth()), root.getPublic(), parent, now));
        assertFalse(service.verify(copy(original, original.taskId(), "other-parent", original.privilege(),
                original.delegationDepth()), root.getPublic(), parent, now));
        assertFalse(service.verify(copy(original, original.taskId(), original.parentId(),
                new Privilege(Set.of("read", "write"), "/project/A/src/**", now + 300),
                original.delegationDepth()), root.getPublic(), parent, now));
        assertFalse(service.verify(copy(original, original.taskId(), original.parentId(), original.privilege(),
                original.delegationDepth() + 1), root.getPublic(), parent, now));
    }

    private static EphemeralPrivilegeGrant grant(Privilege privilege) {
        return new EphemeralPrivilegeGrant("root", "child", "base", "task", privilege,
                123456789L, "aa", 1, "public-key", "");
    }

    private static EphemeralPrivilegeGrant copy(EphemeralPrivilegeGrant source, String taskId, String parentId,
                                                 Privilege privilege, int depth) {
        return new EphemeralPrivilegeGrant(source.issuerAgent(), source.subjectAgent(), parentId, taskId,
                privilege, source.issuedAtEpochSecond(), source.authorityId(), depth,
                source.subjectPublicKey(), source.signature());
    }
}
