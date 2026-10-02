package io.github.dbchoi85.pmiai;

import io.github.dbchoi85.pmiai.epg.DelegationChainService;
import io.github.dbchoi85.pmiai.epg.EphemeralPrivilegeGrant;
import io.github.dbchoi85.pmiai.model.Privilege;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DelegationChainServiceTest {
    @Test
    void verifiesSupportedDepthsAndAttenuatesLifetimeAtEveryHop() {
        var service = new DelegationChainService();
        long now = Instant.now().getEpochSecond();
        var root = new Privilege(Set.of("read", "write"), "/project/A/**", now + 3600);

        for (int depth : new int[]{1, 2, 3, 5, 10}) {
            var chain = service.build(depth, root);
            assertEquals(depth, chain.grants().size());
            assertTrue(service.verify(chain, now));

            long parentExpiry = root.expiresAtEpochSecond();
            for (var grant : chain.grants()) {
                assertTrue(grant.privilege().expiresAtEpochSecond() < parentExpiry);
                parentExpiry = grant.privilege().expiresAtEpochSecond();
            }
            assertTrue(service.encodedSize(chain) > 0);
        }
    }

    @Test
    void rejectsBrokenParentLinkAndDepth() {
        var service = new DelegationChainService();
        long now = Instant.now().getEpochSecond();
        var root = new Privilege(Set.of("read"), "/project/A/**", now + 3600);
        var original = service.build(3, root);

        var brokenParent = replaceGrant(original, 1, copy(original.grants().get(1),
                "wrong-parent", original.grants().get(1).delegationDepth(), original.grants().get(1).privilege()));
        assertFalse(service.verify(brokenParent, now));

        var brokenDepth = replaceGrant(original, 1, copy(original.grants().get(1),
                original.grants().get(1).parentId(), 9, original.grants().get(1).privilege()));
        assertFalse(service.verify(brokenDepth, now));
    }

    @Test
    void rejectsPrivilegeAndLifetimeEscalationEvenWhenGrantShapeLooksValid() {
        var service = new DelegationChainService();
        long now = Instant.now().getEpochSecond();
        var root = new Privilege(Set.of("read"), "/project/A/**", now + 3600);
        var original = service.build(2, root);
        var second = original.grants().get(1);

        var expandedPrivilege = new Privilege(Set.of("read", "write"), second.privilege().resource(),
                second.privilege().expiresAtEpochSecond());
        var privilegeEscalation = replaceGrant(original, 1, copy(second, second.parentId(),
                second.delegationDepth(), expandedPrivilege));
        assertFalse(service.verify(privilegeEscalation, now));

        var extendedLifetime = new Privilege(second.privilege().operations(), second.privilege().resource(),
                original.grants().get(0).privilege().expiresAtEpochSecond() + 1);
        var lifetimeEscalation = replaceGrant(original, 1, copy(second, second.parentId(),
                second.delegationDepth(), extendedLifetime));
        assertFalse(service.verify(lifetimeEscalation, now));
    }

    @Test
    void rejectsInvalidSignature() {
        var service = new DelegationChainService();
        long now = Instant.now().getEpochSecond();
        var root = new Privilege(Set.of("read"), "/project/A/**", now + 3600);
        var original = service.build(2, root);
        var first = original.grants().get(0);
        var tampered = new EphemeralPrivilegeGrant(first.issuerAgent(), first.subjectAgent(), first.parentId(),
                "tampered-task", first.privilege(), first.issuedAtEpochSecond(), first.authorityId(),
                first.delegationDepth(), first.subjectPublicKey(), first.signature());

        assertFalse(service.verify(replaceGrant(original, 0, tampered), now));
    }

    private static DelegationChainService.DelegationChain replaceGrant(
            DelegationChainService.DelegationChain chain, int index, EphemeralPrivilegeGrant replacement) {
        var grants = new ArrayList<>(chain.grants());
        grants.set(index, replacement);
        return new DelegationChainService.DelegationChain(chain.rootPrivilege(), chain.keys(), grants);
    }

    private static EphemeralPrivilegeGrant copy(EphemeralPrivilegeGrant source, String parentId,
                                                 int depth, Privilege privilege) {
        return new EphemeralPrivilegeGrant(source.issuerAgent(), source.subjectAgent(), parentId,
                source.taskId(), privilege, source.issuedAtEpochSecond(), source.authorityId(), depth,
                source.subjectPublicKey(), source.signature());
    }
}
