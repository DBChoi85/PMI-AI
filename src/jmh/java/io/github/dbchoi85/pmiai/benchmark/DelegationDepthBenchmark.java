package io.github.dbchoi85.pmiai.benchmark;

import io.github.dbchoi85.pmiai.epg.DelegationChainService;
import io.github.dbchoi85.pmiai.model.Privilege;
import org.openjdk.jmh.annotations.*;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class DelegationDepthBenchmark {

    @State(Scope.Thread)
    public static class ConstructionState {
        @Param({"1", "2", "3", "5", "10"})
        public int depth;

        DelegationChainService service;
        Privilege rootPrivilege;

        @Setup(Level.Trial)
        public void setup() {
            service = new DelegationChainService();
            rootPrivilege = new Privilege(Set.of("read", "write"), "/project/A/**",
                    Instant.now().plusSeconds(86400).getEpochSecond());
        }
    }

    @State(Scope.Thread)
    public static class VerificationState {
        @Param({"1", "2", "3", "5", "10"})
        public int depth;

        DelegationChainService service;
        DelegationChainService.DelegationChain chain;

        @Setup(Level.Invocation)
        public void setup() {
            service = new DelegationChainService();
            var rootPrivilege = new Privilege(Set.of("read", "write"), "/project/A/**",
                    Instant.now().plusSeconds(3600).getEpochSecond());
            chain = service.build(depth, rootPrivilege);
        }
    }

    @Benchmark
    public DelegationChainService.DelegationChain buildChain(ConstructionState state) {
        return state.service.build(state.depth, state.rootPrivilege);
    }

    @Benchmark
    public boolean verifyChain(VerificationState state) {
        return state.service.verify(state.chain, System.currentTimeMillis() / 1000);
    }

    @Benchmark
    public int encodedChainSize(VerificationState state) {
        return state.service.encodedSize(state.chain);
    }
}
