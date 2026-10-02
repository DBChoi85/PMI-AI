package io.github.dbchoi85.pmiai.benchmark;

import io.github.dbchoi85.pmiai.auth.*;
import io.github.dbchoi85.pmiai.model.Privilege;
import io.github.dbchoi85.pmiai.pmi.MiniPmi;
import org.openjdk.jmh.annotations.*;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class LifecycleBenchmark {
    private AcPerAgentProvider ac;
    private EpgProvider epg;

    @Setup(Level.Trial)
    public void setup() {
        long expiry = Instant.now().plusSeconds(3600).getEpochSecond();
        var privilege = new Privilege(Set.of("read", "write", "execute"), "/project/A/", expiry);
        ac = new AcPerAgentProvider(new MiniPmi(), privilege);
        epg = new EpgProvider(new MiniPmi(), privilege);
    }

    @Benchmark public Object acPerAgentIssue() { return ac.issue(); }
    @Benchmark public Object epgIssue() { return epg.issue(); }
}
