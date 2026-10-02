package io.github.dbchoi85.pmiai.results;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.List;

public record EnvironmentManifest(
        String timestamp, String os, String osVersion, String architecture,
        int availableProcessors, long maxJvmMemoryBytes, String javaVersion,
        String javaVendor, List<String> jvmArguments, String gitCommit,
        String cryptoProvider) {

    public static EnvironmentManifest capture(String gitCommit) {
        var runtime = Runtime.getRuntime();
        var bean = ManagementFactory.getRuntimeMXBean();
        return new EnvironmentManifest(
                Instant.now().toString(),
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("os.arch"),
                runtime.availableProcessors(),
                runtime.maxMemory(),
                System.getProperty("java.version"),
                System.getProperty("java.vendor"),
                List.copyOf(bean.getInputArguments()),
                gitCommit == null || gitCommit.isBlank() ? "unknown" : gitCommit,
                "JCA default providers; Bouncy Castle 1.82 dependencies");
    }
}
