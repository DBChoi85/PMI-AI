package io.github.dbchoi85.pmiai.benchmark;

import io.github.dbchoi85.pmiai.auth.*;
import io.github.dbchoi85.pmiai.epg.EphemeralPrivilegeGrant;
import io.github.dbchoi85.pmiai.model.Privilege;
import io.github.dbchoi85.pmiai.pmi.MiniPmi;
import org.bouncycastle.cert.X509AttributeCertificateHolder;
import org.openjdk.jmh.annotations.*;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class VerificationBenchmark {
    private AcPerAgentProvider acProvider;
    private EpgProvider epgProvider;
    private X509AttributeCertificateHolder ac;
    private EphemeralPrivilegeGrant epg;

    @Setup(Level.Trial)
    public void setup() {
        long expiry = Instant.now().plusSeconds(3600).getEpochSecond();
        var privilege = new Privilege(Set.of("read", "write", "execute"), "/project/A/", expiry);
        acProvider = new AcPerAgentProvider(new MiniPmi(), privilege);
        epgProvider = new EpgProvider(new MiniPmi(), privilege);
        ac = acProvider.issue();
        epg = epgProvider.issue();
    }

    @Benchmark public boolean acPerAgentVerify() { return acProvider.verify(ac); }
    @Benchmark public boolean epgVerify() { return epgProvider.verify(epg); }
}
