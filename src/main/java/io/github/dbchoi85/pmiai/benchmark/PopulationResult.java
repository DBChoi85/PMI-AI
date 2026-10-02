package io.github.dbchoi85.pmiai.benchmark;

public record PopulationResult(
        String implementation,
        int agentCount,
        long keyGenerationNs,
        long issuanceNs,
        long verificationNs,
        long totalNs,
        long credentialBytes,
        long authorityInteractions,
        int successfulVerifications) {

    public double throughputPerSecond() {
        return totalNs == 0 ? 0.0 : agentCount * 1_000_000_000.0 / totalNs;
    }
}
