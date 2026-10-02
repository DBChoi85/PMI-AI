package io.github.dbchoi85.pmiai;

import io.github.dbchoi85.pmiai.results.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResultInfrastructureTest {
    @TempDir Path temp;

    @Test
    void computesSummaryStatisticsFromRawNanoseconds() {
        var samples = List.of(
                sample(10), sample(20), sample(30), sample(40), sample(50)
        );
        var summary = ResultStatistics.summarize("E2", "TEST", samples);

        assertEquals(30.0, summary.meanNs());
        assertEquals(30.0, summary.medianNs());
        assertEquals(48.0, summary.p95Ns());
        assertEquals(49.6, summary.p99Ns(), 0.0001);
        assertTrue(summary.stddevNs() > 0);
        assertTrue(summary.throughputPerSecond() > 0);
    }

    @Test
    void exportsRawSummaryAndEnvironmentFiles() throws Exception {
        var exporter = new ResultExporter();
        var samples = List.of(sample(100), sample(200));
        var summary = ResultStatistics.summarize("E2", "TEST", samples);

        Path raw = temp.resolve("raw/e2.csv");
        Path summaries = temp.resolve("summary/e2_summary.csv");
        Path env = temp.resolve("environment.json");

        exporter.writeRaw(raw, samples);
        exporter.writeSummary(summaries, List.of(summary));
        exporter.writeEnvironment(env, EnvironmentManifest.capture("abc123"));

        assertTrue(Files.readString(raw).startsWith("experiment,implementation,run,iteration"));
        assertTrue(Files.readString(summaries).contains("median_ns"));
        assertTrue(Files.readString(env).contains("abc123"));
    }

    private static BenchmarkSample sample(long totalNs) {
        return new BenchmarkSample("E2", "TEST", 1, 1, 1, 0,
                0, 0, 0, 0, totalNs, 100, 1, true);
    }
}
