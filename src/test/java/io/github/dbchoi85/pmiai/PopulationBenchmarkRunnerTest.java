package io.github.dbchoi85.pmiai;

import io.github.dbchoi85.pmiai.auth.AcPerAgentProvider;
import io.github.dbchoi85.pmiai.auth.EpgProvider;
import io.github.dbchoi85.pmiai.benchmark.PopulationBenchmarkRunner;
import io.github.dbchoi85.pmiai.model.Privilege;
import io.github.dbchoi85.pmiai.pmi.MiniPmi;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PopulationBenchmarkRunnerTest {
    @Test
    void timedPopulationInteractionsExcludePreexistingSetupInteractions() {
        long now = Instant.now().getEpochSecond();
        var base = new Privilege(Set.of("read", "write", "execute"), "/project/A/**", now + 3600);
        var child = new Privilege(Set.of("read", "write"), "/project/A/src/**", now + 300);
        int n = 10;

        var acProvider = new AcPerAgentProvider(new MiniPmi(), child);
        var proposedProvider = new EpgProvider(new MiniPmi(), base);
        assertEquals(1, proposedProvider.authorityInteractions(), "Base AC is established during setup");

        var ac = PopulationBenchmarkRunner.runAcPerAgent(n, acProvider);
        var proposed = PopulationBenchmarkRunner.runProposed(n, proposedProvider, child);

        assertEquals(n, ac.authorityInteractions());
        assertEquals(0, proposed.authorityInteractions(),
                "Child EPG population requires no additional AA issuance after Base AC setup");
        assertEquals(1, proposedProvider.authorityInteractions(),
                "Provider retains exactly the one setup-time Base AC interaction");
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
    void repeatedRunsReportOnlyInteractionsCreatedByThatPopulation() {
        long now = Instant.now().getEpochSecond();
        var child = new Privilege(Set.of("read"), "/project/A/src/**", now + 300);
        var provider = new AcPerAgentProvider(new MiniPmi(), child);

        assertEquals(2, PopulationBenchmarkRunner.runAcPerAgent(2, provider).authorityInteractions());
        assertEquals(3, PopulationBenchmarkRunner.runAcPerAgent(3, provider).authorityInteractions());
        assertEquals(5, provider.authorityInteractions());
    }

    @Test
    void resultReportsPositivePopulationThroughput() {
        long now = Instant.now().getEpochSecond();
        var child = new Privilege(Set.of("read"), "/project/A/src/**", now + 300);
        var provider = new AcPerAgentProvider(new MiniPmi(), child);
        var result = PopulationBenchmarkRunner.runAcPerAgent(1, provider);
        assertTrue(result.throughputPerSecond() > 0.0);
    }
}
