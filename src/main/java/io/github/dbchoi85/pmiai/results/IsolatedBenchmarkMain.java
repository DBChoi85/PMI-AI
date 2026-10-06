package io.github.dbchoi85.pmiai.results;

import io.github.dbchoi85.pmiai.assurance.*;
import io.github.dbchoi85.pmiai.auth.*;
import io.github.dbchoi85.pmiai.authz.*;
import io.github.dbchoi85.pmiai.benchmark.PopulationBenchmarkRunner;
import io.github.dbchoi85.pmiai.epg.DelegationChainService;
import io.github.dbchoi85.pmiai.model.Privilege;
import io.github.dbchoi85.pmiai.pmi.MiniPmi;

import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/**
 * Standalone publication benchmark worker. Run one experiment per JVM:
 * java -cp <runtime-classpath> io.github.dbchoi85.pmiai.results.IsolatedBenchmarkMain E1 1 100 100
 *
 * args: experiment, run/fork id, measured iterations, warm-up iterations
 */
public final class IsolatedBenchmarkMain {
    private IsolatedBenchmarkMain() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException("usage: <E1|E2|E3|E4|E5> <run> <iterations> <warmups>");
        }
        String experiment = args[0].toUpperCase(Locale.ROOT);
        int run = Integer.parseInt(args[1]);
        int iterations = Integer.parseInt(args[2]);
        int warmups = Integer.parseInt(args[3]);
        if (run < 1 || iterations < 1 || warmups < 0) throw new IllegalArgumentException("invalid benchmark arguments");

        var samples = new ArrayList<BenchmarkSample>();
        switch (experiment) {
            case "E1" -> runE1(samples, run, iterations, warmups);
            case "E2" -> runE2(samples, run, iterations, warmups);
            case "E3" -> runE3(samples, run, iterations, warmups);
            case "E4" -> runE4(samples, run, iterations, warmups);
            case "E5" -> runE5(samples, run, iterations, warmups);
            default -> throw new IllegalArgumentException("unknown experiment: " + experiment);
        }

        Path raw = Path.of("results/raw/forks", experiment.toLowerCase(Locale.ROOT) + "-run-" + run + ".csv");
        new ResultExporter().writeRaw(raw, samples);
        System.out.printf("Wrote %d measured samples to %s (warmups discarded: %d)%n",
                samples.size(), raw, warmups);
    }

    private static void runE1(List<BenchmarkSample> out, int run, int iterations, int warmups) {
        long now = Instant.now().getEpochSecond();
        var base = new Privilege(Set.of("read", "write", "execute"), "/project/A/**", now + 86400);
        var child = new Privilege(Set.of("read", "write"), "/project/A/src/**", now + 86400);
        var request = new AuthorizationRequest("read", "/project/A/src/module/file.txt", now);
        var authorizers = new LinkedHashMap<String, RequestAuthorizer>();
        authorizers.put("NO_AUTH", new NoAuthAuthorizer());
        authorizers.put("JWT_ED25519", new JwtAuthorizer(child));
        authorizers.put("STATIC_PMI", new StaticPmiAuthorizer(child));
        authorizers.put("BASE_AC_EPG", new EpgAuthorizer(base, child));

        for (var entry : authorizers.entrySet()) {
            for (int i = -warmups; i < iterations; i++) {
                long start = System.nanoTime();
                boolean success = entry.getValue().authorize(request);
                long elapsed = System.nanoTime() - start;
                if (i >= 0) out.add(sample("E1", entry.getKey(), run, i + 1, 0, 0,
                        0, 0, 0, elapsed, elapsed, entry.getValue().credentialSize(), 0, success));
            }
        }
    }

    private static void runE2(List<BenchmarkSample> out, int run, int iterations, int warmups) {
        for (int n : new int[]{1, 10, 100, 1000, 10000}) {
            long now = Instant.now().getEpochSecond();
            var base = new Privilege(Set.of("read", "write", "execute"), "/project/A/**", now + 86400);
            var child = new Privilege(Set.of("read", "write"), "/project/A/src/**", now + 86400);
            var acProvider = new AcPerAgentProvider(new MiniPmi(), child);
            var proposedProvider = new EpgProvider(new MiniPmi(), base);
            for (int i = -warmups; i < iterations; i++) {
                var ac = PopulationBenchmarkRunner.runAcPerAgent(n, acProvider);
                var proposed = PopulationBenchmarkRunner.runProposed(n, proposedProvider, child);
                if (i >= 0) {
                    out.add(sample("E2", ac.implementation(), run, i + 1, n, 0, ac.keyGenerationNs(),
                            ac.issuanceNs(), ac.verificationNs(), 0, ac.totalNs(), ac.credentialBytes(),
                            ac.authorityInteractions(), ac.successfulVerifications() == n));
                    out.add(sample("E2", proposed.implementation(), run, i + 1, n, 0, proposed.keyGenerationNs(),
                            proposed.issuanceNs(), proposed.verificationNs(), 0, proposed.totalNs(),
                            proposed.credentialBytes(), proposed.authorityInteractions(),
                            proposed.successfulVerifications() == n));
                }
            }
        }
    }

    private static void runE3(List<BenchmarkSample> out, int run, int iterations, int warmups) {
        var service = new DelegationChainService();
        for (int depth : new int[]{0, 1, 2, 3, 4, 5, 10}) {
            for (int i = -warmups; i < iterations; i++) {
                long now = Instant.now().getEpochSecond();
                var root = new Privilege(Set.of("read", "write"), "/project/A/**", now + 86400);
                long buildStart = System.nanoTime();
                var chain = service.build(depth, root);
                long buildNs = System.nanoTime() - buildStart;
                long verifyStart = System.nanoTime();
                boolean valid = service.verify(chain, now);
                long verifyNs = System.nanoTime() - verifyStart;
                if (i >= 0) {
                    int bytes = service.encodedSize(chain);
                    out.add(sample("E3", "EPG_BUILD", run, i + 1, 0, depth,
                            0, buildNs, 0, 0, buildNs, bytes, 0, true));
                    out.add(sample("E3", "EPG_VERIFY", run, i + 1, 0, depth,
                            0, 0, verifyNs, 0, verifyNs, bytes, 0, valid));
                }
            }
        }
    }

    private static void runE4(List<BenchmarkSample> out, int run, int iterations, int warmups) {
        for (int rules : new int[]{1, 10, 100}) {
            long now = Instant.now().getEpochSecond();
            var privilege = new Privilege(Set.of("read", "write"), "/project/A/**", now + 86400);
            var request = new AuthorizationRequest("write", "/project/A/src/module/file.txt", now);
            var attrs = new LinkedHashMap<String, String>();
            for (int r = 0; r < rules; r++) attrs.put("rule-" + r, "value-" + r);
            var context = new AuthorizationContext("task-42", attrs);
            var policy = new ActionPolicy("write", "/project/A/src/", "task-42", AuthenticatorAssuranceLevel.AAL2);
            var aal3 = new AuthorityProvenance("human-1", IdentityAssuranceLevel.IAL2,
                    AuthenticatorAssuranceLevel.AAL3, now - 60, "enterprise-idp", "nist-800-63");
            var allowed = new ContextAssuranceAuthorizer(privilege, aal3, policy, attrs);
            for (int i = -warmups; i < iterations; i++) {
                long start = System.nanoTime();
                var decision = allowed.authorize(request, context);
                long elapsed = System.nanoTime() - start;
                if (i >= 0) out.add(sample("E4", "CONTEXT_POLICY_" + rules, run, i + 1, 0, 0,
                        0, 0, 0, elapsed, elapsed, 0, 0, decision == AuthorizationDecision.ALLOW));
            }
        }
    }

    private static void runE5(List<BenchmarkSample> out, int run, int iterations, int warmups) {
        long now = Instant.now().getEpochSecond();
        var privilege = new Privilege(Set.of("read", "write"), "/project/A/**", now + 86400);
        var request = new AuthorizationRequest("write", "/project/A/src/module/file.txt", now);
        var context = new AuthorizationContext("task-42", Map.of("env", "prod"));
        var policy = new ActionPolicy("write", "/project/A/src/", "task-42", AuthenticatorAssuranceLevel.AAL2);
        var requiredContext = Map.of("env", "prod");
        var aal3 = new AuthorityProvenance("human-1", IdentityAssuranceLevel.IAL2,
                AuthenticatorAssuranceLevel.AAL3, now - 60, "enterprise-idp", "nist-800-63");
        var aal1 = new AuthorityProvenance("human-1", IdentityAssuranceLevel.IAL2,
                AuthenticatorAssuranceLevel.AAL1, now - 60, "enterprise-idp", "nist-800-63");
        var satisfied = new ContextAssuranceAuthorizer(privilege, aal3, policy, requiredContext);
        var stepUp = new ContextAssuranceAuthorizer(privilege, aal1, policy, requiredContext);
        for (int i = -warmups; i < iterations; i++) {
            long start = System.nanoTime();
            var allowDecision = satisfied.authorize(request, context);
            long allowNs = System.nanoTime() - start;
            start = System.nanoTime();
            var stepUpDecision = stepUp.authorize(request, context);
            long stepUpNs = System.nanoTime() - start;
            if (i >= 0) {
                out.add(sample("E5", "AAL_SATISFIED", run, i + 1, 0, 0,
                        0, 0, 0, allowNs, allowNs, 0, 0, allowDecision == AuthorizationDecision.ALLOW));
                out.add(sample("E5", "AAL_STEP_UP", run, i + 1, 0, 0,
                        0, 0, 0, stepUpNs, stepUpNs, 0, 0,
                        stepUpDecision == AuthorizationDecision.STEP_UP_REQUIRED));
            }
        }
    }

    private static BenchmarkSample sample(String experiment, String implementation, int run, int iteration,
                                          int agentCount, int depth, long keygen, long issuance,
                                          long verification, long authorization, long total, long bytes,
                                          long interactions, boolean success) {
        return new BenchmarkSample(experiment, implementation, run, iteration, agentCount, depth,
                keygen, issuance, verification, authorization, total, bytes, interactions, success);
    }
}
