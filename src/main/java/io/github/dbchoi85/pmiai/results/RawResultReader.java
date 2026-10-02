package io.github.dbchoi85.pmiai.results;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class RawResultReader {
    private RawResultReader() {}

    public static List<BenchmarkSample> read(Path path) throws IOException {
        var lines = Files.readAllLines(path);
        if (lines.isEmpty()) return List.of();
        var result = new ArrayList<BenchmarkSample>();
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) continue;
            String[] p = lines.get(i).split(",", -1);
            if (p.length != 14) throw new IllegalArgumentException("Expected 14 CSV columns at line " + (i + 1));
            result.add(new BenchmarkSample(p[0], p[1], Integer.parseInt(p[2]), Integer.parseInt(p[3]),
                    Integer.parseInt(p[4]), Integer.parseInt(p[5]), Long.parseLong(p[6]), Long.parseLong(p[7]),
                    Long.parseLong(p[8]), Long.parseLong(p[9]), Long.parseLong(p[10]), Long.parseLong(p[11]),
                    Long.parseLong(p[12]), Boolean.parseBoolean(p[13])));
        }
        return result;
    }
}
