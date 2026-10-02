package io.github.dbchoi85.pmiai.benchmark;

import io.github.dbchoi85.pmiai.authz.*;
import io.github.dbchoi85.pmiai.model.Privilege;
import org.openjdk.jmh.annotations.*;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.AverageTime, Mode.Throughput})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class AuthorizationBenchmark {
    private AuthorizationRequest request;
    private RequestAuthorizer noAuth;
    private RequestAuthorizer jwt;
    private RequestAuthorizer staticPmi;
    private RequestAuthorizer proposed;

    @Setup(Level.Trial)
    public void setup() {
        long now = Instant.now().getEpochSecond();
        var base = new Privilege(Set.of("read", "write", "execute"), "/project/A/**", now + 3600);
        var child = new Privilege(Set.of("read", "write"), "/project/A/src/**", now + 300);
        request = new AuthorizationRequest("read", "/project/A/src/module/file.txt", now);

        noAuth = new NoAuthAuthorizer();
        jwt = new JwtAuthorizer(child);
        staticPmi = new StaticPmiAuthorizer(child);
        proposed = new EpgAuthorizer(base, child);
    }

    @Benchmark public boolean noAuth() { return noAuth.authorize(request); }
    @Benchmark public boolean jwtEd25519() { return jwt.authorize(request); }
    @Benchmark public boolean staticPmi() { return staticPmi.authorize(request); }
    @Benchmark public boolean proposedEpg() { return proposed.authorize(request); }
}
