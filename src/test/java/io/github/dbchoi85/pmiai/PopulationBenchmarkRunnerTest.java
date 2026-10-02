package io.github.dbchoi85.pmiai;

import io.github.dbchoi85.pmiai.benchmark.PopulationBenchmarkRunner;
import io.github.dbchoi85.pmiai.model.Privilege;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PopulationBenchmarkRunnerTest {
    @Test
    void authorityInteractionsScaleDifferentlyAcrossImplementations() {
        long now = Instant.now().getEpochSecond();
        var base = new Privilege(Set.of("read", "write", "execute"), "/project/A/**", now + 3600);
        var child = new Privilege(Set.of("read", "write"), "/project/A/src/**", now + 300);
        int n = 10;

        var ac = PopulationBenchmarkRunner.runAcPerAgent(n, child);
        var proposed = PopulationBenchmarkRunner.runProposed(n, base, child);

        assertEquals(n, ac.authorityInteractions());
        assertEquals(1, proposed.authorityInteractions());
        assertEquals(n, ac.successfulVerifications());
        assertEquals(n, proposed.successfulVerifications());
        assertEquals(0, ac.keyGenerationNs());
        assertTrue(proposed.keyGenerationNs() > 0);
        assertTrue(ac.issuanceNs() > 0);
        assertTrue(proposed.issuanceNs() > 0);
        assertTrue(ac.verificationNs() > 0);
        assertTrue(proposed.verificationNs() > 0);
        assertTrue(ac.credentialBytes() > 0);
        assertTrue(proposed.credentialBytes() > 0);
    }

    @Test
    void resultReportsPositivePopulationThroughput() {
        long now = Instant.now().getEpochSecond();
        var child = new Privilege(Set.of("read"), "/project/A/src/**", now + 300);
        var result = PopulationBenchmarkRunner.runAcPerAgent(1, child);
        assertTrue(result.throughputPerSecond() > 0.0);
    }
}
