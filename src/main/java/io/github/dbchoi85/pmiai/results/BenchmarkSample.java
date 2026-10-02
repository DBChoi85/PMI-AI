package io.github.dbchoi85.pmiai.results;

public record BenchmarkSample(
        String experiment, String implementation, int run, int iteration,
        int agentCount, int depth, long keygenNs, long issuanceNs,
        long verificationNs, long authorizationNs, long totalNs,
        long credentialBytes, long aaInteractions, boolean success) {}
