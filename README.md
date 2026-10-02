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
- JMH issuance and verification benchmarks
- delegation-depth benchmark (1, 2, 3, 5, 10)
- JUnit correctness tests

See [Benchmark specification](docs/BENCHMARK.md) and [Architecture](docs/ARCHITECTURE.md).

## Requirements

- JDK 21
- Gradle 8.x

## Run

```bash
gradle test
gradle jmh
```

JMH CSV output is written under `build/results/jmh/results.csv`.

## Benchmark policy

Reported numbers from AIP and other related systems are not treated as direct performance comparisons unless their implementation is reproduced under the same benchmark environment.

## Planned increments

1. NoAuth/JWT/static-PMI authorization ablations
2. population scalability: 1, 10, 100, 1K, 10K agents
3. task/context action authorization
4. NIST authentication-assurance provenance
5. adversarial delegation/correctness suite
6. reproduced public agent-native delegation baseline
