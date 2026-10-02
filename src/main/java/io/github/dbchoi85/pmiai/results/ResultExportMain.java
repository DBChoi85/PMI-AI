package io.github.dbchoi85.pmiai.results;

import java.nio.file.Path;
import java.util.List;

public final class ResultExportMain {
    private ResultExportMain() {}

    public static void main(String[] args) throws Exception {
        String gitCommit = System.getenv().getOrDefault("GIT_COMMIT", "unknown");
        Path root = Path.of("results");
        var exporter = new ResultExporter();
        exporter.writeEnvironment(root.resolve("environment.json"), EnvironmentManifest.capture(gitCommit));

        if (args.length == 1) {
            Path raw = Path.of(args[0]);
            List<BenchmarkSample> samples = RawResultReader.read(raw);
            var grouped = samples.stream().collect(java.util.stream.Collectors.groupingBy(
                    s -> s.experiment() + "\u0000" + s.implementation()));
            var summaries = grouped.values().stream().map(group -> ResultStatistics.summarize(
                    group.get(0).experiment(), group.get(0).implementation(), group)).toList();
            exporter.writeSummary(root.resolve("summary/summary.csv"), summaries);
        }
    }
}
