package io.github.dbchoi85.pmiai.epg;

import io.github.dbchoi85.pmiai.model.Privilege;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

public final class DelegationChainService {
    private final EpgService epg = new EpgService();

    public DelegationChain build(int depth, Privilege rootPrivilege) {
        if (depth < 0) throw new IllegalArgumentException("depth must be >= 0");

        var keys = new ArrayList<KeyPair>(depth + 1);
        for (int i = 0; i <= depth; i++) keys.add(epg.newAgentKeyPair());

        var grants = new ArrayList<EphemeralPrivilegeGrant>(depth);
        Privilege parent = rootPrivilege;
        for (int i = 0; i < depth; i++) {
            Privilege child = attenuate(parent, i, depth);
            String parentId = i == 0 ? "base-ac" : grantId(i - 1);
            grants.add(epg.issue("agent-" + i, "agent-" + (i + 1), parentId,
                    "task-" + i, child, "pmi-aa", i + 1,
                    keys.get(i + 1).getPublic(), keys.get(i).getPrivate()));
            parent = child;
        }
        return new DelegationChain(rootPrivilege, List.copyOf(keys), List.copyOf(grants));
    }

    public boolean verify(DelegationChain chain, long nowEpochSecond) {
        Privilege parent = chain.rootPrivilege();
        for (int i = 0; i < chain.grants().size(); i++) {
            EphemeralPrivilegeGrant grant = chain.grants().get(i);
            String expectedParent = i == 0 ? "base-ac" : grantId(i - 1);
            if (grant.delegationDepth() != i + 1) return false;
            if (!expectedParent.equals(grant.parentId())) return false;
            if (!("agent-" + i).equals(grant.issuerAgent())) return false;
            if (!("agent-" + (i + 1)).equals(grant.subjectAgent())) return false;
            if (!epg.verify(grant, chain.keys().get(i).getPublic(), parent, nowEpochSecond)) return false;
            parent = grant.privilege();
        }
        return true;
    }

    public int encodedSize(DelegationChain chain) {
        return chain.grants().stream().mapToInt(epg::encodedSize).sum();
    }

    static String grantId(int zeroBasedIndex) {
        return "epg-" + (zeroBasedIndex + 1);
    }

    private static Privilege attenuate(Privilege parent, int index, int depth) {
        long remaining = Math.max(1, depth - index);
        long reduction = Math.max(1, Math.min(30, remaining));
        long expiry = Math.max(1, parent.expiresAtEpochSecond() - reduction);
        return new Privilege(parent.operations(), parent.resource(), expiry);
    }

    public record DelegationChain(Privilege rootPrivilege, List<KeyPair> keys,
                                  List<EphemeralPrivilegeGrant> grants) {}
}
