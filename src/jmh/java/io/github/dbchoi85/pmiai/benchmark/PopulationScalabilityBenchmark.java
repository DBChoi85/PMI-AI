package io.github.dbchoi85.pmiai.benchmark;

import io.github.dbchoi85.pmiai.model.Privilege;
import org.openjdk.jmh.annotations.*;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 10)
@Fork(3)
@State(Scope.Thread)
public class PopulationScalabilityBenchmark {
    @Param({"1", "10", "100", "1000", "10000"})
    public int agentCount;

    private Privilege basePrivilege;
    private Privilege childPrivilege;

    @Setup(Level.Invocation)
    public void setup() {
        long now = Instant.now().getEpochSecond();
        basePrivilege = new Privilege(Set.of("read", "write", "execute"), "/project/A/**", now + 3600);
        childPrivilege = new Privilege(Set.of("read", "write"), "/project/A/src/**", now + 300);
    }

    @Benchmark
    public PopulationResult acPerAgentPopulation() {
        return PopulationBenchmarkRunner.runAcPerAgent(agentCount, childPrivilege);
    }

    @Benchmark
    public PopulationResult proposedPopulation() {
        return PopulationBenchmarkRunner.runProposed(agentCount, basePrivilege, childPrivilege);
    }
}
