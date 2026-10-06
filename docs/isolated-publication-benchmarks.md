# Isolated JVM publication benchmarks

Issue #38 replaces the single-process publication measurement path with experiment-isolated Java VM execution.

## Build only

Gradle prepares compiled classes and runtime dependencies; it does not launch publication measurements:

```bash
./gradlew clean preparePublicationBenchmarks
```

## Full E1-E5 run

```bash
bash scripts/run_publication_benchmarks.sh
```

The script launches each fork using `java -cp ... IsolatedBenchmarkMain`. Warm-up observations are discarded inside each JVM before canonical samples are written.

Publication configuration:

| Experiment | JVM forks | Warm-up/fork | Measured/fork |
|---|---:|---:|---:|
| E1 | 10 | 100 | 100 |
| E2 | 10 | 5 | 10 |
| E3 | 10 | 50 | 100 per depth |
| E4 | 10 | 100 | 100 |
| E5 | 10 | 100 | 100 |

E2 intentionally preserves the previous practical repetition scale because the 10,000-agent cell dominates runtime.

E3 retains all 1,000 measured EPG observations per depth for its statistics. The official AIP Rust baseline has 100 observations per depth; the publication must disclose this methodology/sample-count difference rather than down-sampling EPG. Depth 0 means established base authority with no EPG delegation and therefore is not directly equivalent to AIP's signed authority token at depth 0. Direct AIP/EPG comparisons should use depths 1-5.

Outputs remain:

- `results/raw/publication.csv`
- `results/summary/summary.csv`
- `results/environment.json`

E6 remains a deterministic correctness suite and should be run separately with 100 attempts/category.
