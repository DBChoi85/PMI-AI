package io.github.dbchoi85.pmiai.benchmark;

import io.github.dbchoi85.pmiai.epg.DelegationChainService;
import io.github.dbchoi85.pmiai.model.Privilege;
import org.openjdk.jmh.annotations.*;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class DelegationDepthBenchmark {
    @Param({"1", "2", "3", "5", "10"})
    public int depth;

    private DelegationChainService service;
    private Privilege rootPrivilege;
    private DelegationChainService.DelegationChain chain;

    @Setup(Level.Trial)
    public void setupTrial() {
        service = new DelegationChainService();
    }

    @Setup(Level.Invocation)
    public void setupInvocation() {
        rootPrivilege = new Privilege(Set.of("read", "write"), "/project/A/**",
                Instant.now().plusSeconds(3600).getEpochSecond());
        chain = service.build(depth, rootPrivilege);
    }

    @Benchmark
    public DelegationChainService.DelegationChain buildChain() {
        return service.build(depth, rootPrivilege);
    }

    @Benchmark
    public boolean verifyChain() {
        return service.verify(chain, System.currentTimeMillis() / 1000);
    }

    @Benchmark
    public int encodedChainSize() {
        return service.encodedSize(chain);
    }
}
