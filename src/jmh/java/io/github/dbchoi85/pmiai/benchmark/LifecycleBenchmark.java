package io.github.dbchoi85.pmiai.benchmark;

import io.github.dbchoi85.pmiai.auth.*;
import io.github.dbchoi85.pmiai.epg.EphemeralPrivilegeGrant;
import io.github.dbchoi85.pmiai.model.Privilege;
import io.github.dbchoi85.pmiai.pmi.MiniPmi;
import org.bouncycastle.cert.X509AttributeCertificateHolder;
import org.openjdk.jmh.annotations.*;

import java.security.KeyPair;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class LifecycleBenchmark {
    private AcPerAgentProvider ac;
    private EpgProvider epg;
    private KeyPair childKey;
    private X509AttributeCertificateHolder acCredential;
    private EphemeralPrivilegeGrant epgCredential;

    @Setup(Level.Trial)
    public void setupTrial() {
        long expiry = Instant.now().plusSeconds(3600).getEpochSecond();
        var privilege = new Privilege(Set.of("read", "write", "execute"), "/project/A/", expiry);
        ac = new AcPerAgentProvider(new MiniPmi(), privilege);
        epg = new EpgProvider(new MiniPmi(), privilege);
    }

    @Setup(Level.Invocation)
    public void setupInvocation() {
        childKey = epg.newAgentKeyPair();
        acCredential = ac.issue();
        epgCredential = epg.issue(childKey.getPublic());
    }

    @Benchmark
    public KeyPair agentKeyGeneration() {
        return epg.newAgentKeyPair();
    }

    @Benchmark
    public X509AttributeCertificateHolder acPerAgentIssuance() {
        return ac.issue();
    }

    @Benchmark
    public EphemeralPrivilegeGrant epgIssuanceWithPreGeneratedKey() {
        return epg.issue(childKey.getPublic());
    }

    @Benchmark
    public boolean acVerification() {
        return ac.verify(acCredential);
    }

    @Benchmark
    public boolean epgVerification() {
        return epg.verify(epgCredential);
    }

    @Benchmark
    public EphemeralPrivilegeGrant epgTotalLifecycle() {
        KeyPair key = epg.newAgentKeyPair();
        EphemeralPrivilegeGrant grant = epg.issue(key.getPublic());
        epg.verify(grant);
        return grant;
    }
}
