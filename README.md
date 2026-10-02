# PMI-AI

Experimental implementation and benchmark harness for X.509 PMI-backed authorization of dynamically instantiated AI agents.

## Research focus

The prototype separates persistent organizational authority from ephemeral operational privilege:

```text
X.509 PMI / Attribute Authority
            |
        Base AC
            |
        Root Agent
            |
   Ephemeral Privilege Grant
            |
       Child Agent
```

The proposed path reuses an established PMI authority state instead of requesting a new Attribute Certificate from the AA for every short-lived child agent.

## Current implementation

- Mini X.509 PMI Attribute Authority using Bouncy Castle
- AC-per-Agent baseline
- Base-AC + signed Ephemeral Privilege Grant (EPG) path
- privilege/resource/lifetime attenuation checks
- E1 NoAuth/JWT/static-PMI/Base-AC+EPG authorization comparison
- E2 ephemeral-agent population scalability (1, 10, 100, 1K, 10K)
- E3 delegation-depth benchmark (1, 2, 3, 5, 10)
- E4 context/action-policy overhead
- E5 human IAL/AAL provenance and step-up policy overhead
- E6 adversarial authorization correctness suite
- canonical publication raw/summary/environment result pipeline
- JUnit correctness tests

See [Benchmark specification](docs/BENCHMARK.md) and [Architecture](docs/ARCHITECTURE.md).

## Requirements

- JDK 21
- Gradle 8.x

## Run

```bash
./gradlew test
./gradlew jmh
GIT_COMMIT=$(git rev-parse HEAD) ./gradlew publicationBenchmark
./gradlew adversarialSuite
```

JMH CSV output is written under `build/results/jmh/results.csv`. Publication samples are written separately under `results/`; JMH aggregate output is not used as publication raw data.

## Benchmark policy

Reported numbers from AIP and other related systems are not treated as direct performance comparisons unless their implementation is reproduced under the same benchmark environment.

## Remaining comparative work

The implemented E1-E6 harness is complete. A reproduced public agent-native delegation implementation remains a future comparative baseline; results reported by external papers are not treated as same-environment performance measurements.
