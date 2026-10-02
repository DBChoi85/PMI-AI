package io.github.dbchoi85.pmiai.results;

import java.util.List;

public final class ResultStatistics {
    private ResultStatistics() {}

    public static BenchmarkSummary summarize(String experiment, String implementation, List<BenchmarkSample> samples) {
        if (samples.isEmpty()) throw new IllegalArgumentException("samples must not be empty");
        double[] values = samples.stream().mapToDouble(BenchmarkSample::totalNs).sorted().toArray();
        double mean = java.util.Arrays.stream(values).average().orElseThrow();
        double variance = java.util.Arrays.stream(values).map(v -> (v - mean) * (v - mean)).average().orElse(0);
        double throughput = samples.stream().mapToDouble(s ->
                s.totalNs() == 0 ? 0 : Math.max(1, s.agentCount()) * 1_000_000_000.0 / s.totalNs()).average().orElse(0);
        return new BenchmarkSummary(experiment, implementation, values.length, mean,
                percentile(values, 0.50), percentile(values, 0.95), percentile(values, 0.99),
                Math.sqrt(variance), throughput);
    }

    static double percentile(double[] sorted, double q) {
        if (sorted.length == 1) return sorted[0];
        double pos = q * (sorted.length - 1);
        int lo = (int) Math.floor(pos), hi = (int) Math.ceil(pos);
        if (lo == hi) return sorted[lo];
        return sorted[lo] + (sorted[hi] - sorted[lo]) * (pos - lo);
    }
}
