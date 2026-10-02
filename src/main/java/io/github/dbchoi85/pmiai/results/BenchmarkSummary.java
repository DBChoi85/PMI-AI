package io.github.dbchoi85.pmiai.results;

public record BenchmarkSummary(
        String experiment, String implementation, int agentCount, int depth, int samples,
        double meanNs, double medianNs, double p95Ns, double p99Ns,
        double stddevNs, double throughputPerSecond) {}
