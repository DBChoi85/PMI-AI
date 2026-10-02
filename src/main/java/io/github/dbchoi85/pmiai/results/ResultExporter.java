package io.github.dbchoi85.pmiai.results;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

public final class ResultExporter {
    private static final String RAW_HEADER = "experiment,implementation,run,iteration,agent_count,depth,keygen_ns,issuance_ns,verification_ns,authorization_ns,total_ns,credential_bytes,aa_interactions,success";
    private static final String SUMMARY_HEADER = "experiment,implementation,agent_count,depth,samples,mean_ns,median_ns,p95_ns,p99_ns,stddev_ns,throughput_per_second";
    private final ObjectMapper mapper = new ObjectMapper();

    public void writeRaw(Path path, List<BenchmarkSample> samples) throws IOException {
        Files.createDirectories(path.getParent());
        var lines = new java.util.ArrayList<String>();
        lines.add(RAW_HEADER);
        for (var s : samples) lines.add(String.join(",",
                s.experiment(), s.implementation(), Integer.toString(s.run()), Integer.toString(s.iteration()),
                Integer.toString(s.agentCount()), Integer.toString(s.depth()), Long.toString(s.keygenNs()),
                Long.toString(s.issuanceNs()), Long.toString(s.verificationNs()), Long.toString(s.authorizationNs()),
                Long.toString(s.totalNs()), Long.toString(s.credentialBytes()), Long.toString(s.aaInteractions()),
                Boolean.toString(s.success())));
        Files.write(path, lines);
    }

    public void writeSummary(Path path, List<BenchmarkSummary> summaries) throws IOException {
        Files.createDirectories(path.getParent());
        var lines = new java.util.ArrayList<String>();
        lines.add(SUMMARY_HEADER);
        for (var s : summaries) lines.add(String.join(",", s.experiment(), s.implementation(),
                Integer.toString(s.agentCount()), Integer.toString(s.depth()), Integer.toString(s.samples()),
                Double.toString(s.meanNs()), Double.toString(s.medianNs()), Double.toString(s.p95Ns()),
                Double.toString(s.p99Ns()), Double.toString(s.stddevNs()), Double.toString(s.throughputPerSecond())));
        Files.write(path, lines);
    }

    public void writeEnvironment(Path path, EnvironmentManifest manifest) throws IOException {
        Files.createDirectories(path.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), manifest);
    }
}
