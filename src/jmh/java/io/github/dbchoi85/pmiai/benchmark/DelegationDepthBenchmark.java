package io.github.dbchoi85.pmiai.benchmark;

import io.github.dbchoi85.pmiai.epg.*;
import io.github.dbchoi85.pmiai.model.Privilege;
import org.openjdk.jmh.annotations.*;

import java.security.KeyPair;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class DelegationDepthBenchmark {
    @Param({"1", "2", "3", "5", "10"})
    public int depth;

    private final EpgService service = new EpgService();
    private Privilege privilege;
    private List<KeyPair> keys;
    private List<EphemeralPrivilegeGrant> chain;

    @Setup(Level.Invocation)
    public void setup() {
        privilege = new Privilege(Set.of("read"), "/project/A/src/", Instant.now().plusSeconds(300).getEpochSecond());
        keys = new ArrayList<>();
        chain = new ArrayList<>();
        for (int i = 0; i <= depth; i++) keys.add(service.newAgentKeyPair());
        for (int i = 0; i < depth; i++) {
            chain.add(service.issue("agent-" + i, "agent-" + (i + 1), i == 0 ? "base-ac" : "epg-" + i,
                    "task-" + i, privilege, "pmi-aa", i + 1, keys.get(i + 1).getPublic(), keys.get(i).getPrivate()));
        }
    }

    @Benchmark
    public boolean verifyChain() {
        long now = System.currentTimeMillis() / 1000;
        for (int i = 0; i < chain.size(); i++) {
            if (!service.verify(chain.get(i), keys.get(i).getPublic(), privilege, now)) return false;
        }
        return true;
    }
}
