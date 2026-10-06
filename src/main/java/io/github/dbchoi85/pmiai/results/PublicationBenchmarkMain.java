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

public final class PublicationBenchmarkMain {
    private PublicationBenchmarkMain() {}

    public static void main(String[] args) throws Exception {
        int runs = Integer.getInteger("pmiai.runs", 3);
        int iterations = Integer.getInteger("pmiai.iterations", 10);
        int warmups = Integer.getInteger("pmiai.warmups", 3);
        var samples = new ArrayList<BenchmarkSample>();

        for (int run = 1; run <= runs; run++) {
            runE1(samples, run, iterations, warmups);
            runE2(samples, run, iterations, warmups);
            runE3(samples, run, iterations, warmups);
            runE4(samples, run, iterations, warmups);
            runE5(samples, run, iterations, warmups);
        }

        Path raw = Path.of("results/raw/publication.csv");
        var exporter = new ResultExporter();
        exporter.writeRaw(raw, samples);
        writeSummary(exporter, samples);
        exporter.writeEnvironment(Path.of("results/environment.json"),
                EnvironmentManifest.capture(System.getenv().getOrDefault("GIT_COMMIT", "unknown")));
        System.out.printf("Wrote %d canonical samples to %s%n", samples.size(), raw);
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

            // Untimed authority setup: RSA AA key generation and Proposed Base-AC establishment
            // are completed before this parameter cell's warm-up and measured populations.
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
        for (int depth : new int[]{1, 2, 3, 5, 10}) {
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
                if (i >= 0) {
                    out.add(sample("E4", "CONTEXT_POLICY_" + rules, run, i + 1, 0, 0,
                            0, 0, 0, elapsed, elapsed, 0, 0, decision == AuthorizationDecision.ALLOW));
                }
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
                        0, 0, 0, allowNs, allowNs, 0, 0,
                        allowDecision == AuthorizationDecision.ALLOW));
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

    private static void writeSummary(ResultExporter exporter, List<BenchmarkSample> samples) throws Exception {
        var groups = samples.stream().collect(java.util.stream.Collectors.groupingBy(
                s -> s.experiment() + "\u0000" + s.implementation() + "\u0000" + s.agentCount() + "\u0000" + s.depth()));
        var summaries = groups.values().stream().map(g -> ResultStatistics.summarize(
                g.get(0).experiment(), g.get(0).implementation(), g))
                .sorted(Comparator.comparing(BenchmarkSummary::experiment)
                        .thenComparing(BenchmarkSummary::implementation)
                        .thenComparingInt(BenchmarkSummary::agentCount)
                        .thenComparingInt(BenchmarkSummary::depth))
                .toList();
        exporter.writeSummary(Path.of("results/summary/summary.csv"), summaries);
    }
}
