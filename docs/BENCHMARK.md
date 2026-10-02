# Benchmark specification

## Objective

Measure the incremental cost of projecting an established X.509 PMI authority into short-lived AI-agent privileges. The benchmark does **not** use results reported on different hardware as direct performance competitors.

## Common workload

Base privilege:

- resource: `/project/A/**`
- operations: `read, write, execute`
- validity: 3600 s

Derived child privilege:

- resource: `/project/A/src/**`
- operations: `read, write`
- validity: <= 300 s

A valid delegation must satisfy:

`P_child ⊆ P_parent`, `R_child ⊆ R_parent`, and `T_child <= T_parent`.

## Implementations

1. **AC-per-Agent** — the AA issues an X.509 Attribute Certificate for each agent.
2. **Base AC + EPG (proposed)** — the AA issues and validates one base AC for the root agent. The root then signs task-scoped Ephemeral Privilege Grants (EPGs) for children.

NoAuth, JWT, static-PMI authorization, context/action policy, and NIST AAL provenance are planned ablations and are intentionally separated from the first credential-lifecycle benchmarks.

## Experiments

### E1: credential operation overhead

JMH benchmarks issuance/delegation and verification separately. Key generation, credential construction, signing, and verification must not be silently conflated when interpreting results.

Metrics: mean latency, error, sample count; final analysis will additionally report median/p95/p99 from raw samples where applicable.

### E2: ephemeral-agent lifecycle scalability

Target populations: 1, 10, 100, 1,000, and 10,000 agents.

Metrics:

- total lifecycle time
- issuance/delegation throughput
- credential bytes
- AA interaction count
- key-generation time (reported separately)

Expected structural difference, not a performance assumption:

- AC-per-Agent: AA interactions grow with N.
- Proposed: one base-AC issuance/validation establishes the authority state; EPG derivation does not require a new AA issuance for each child.

### E3: delegation depth

Depths: 1, 2, 3, 5, 10.

Metrics:

- chain verification latency
- encoded credential/chain size
- per-hop marginal verification cost

This experiment is the preferred location for a later reproduced AIP/capability-delegation baseline because it exercises comparable multi-hop semantics.

## Methodology

Default JMH configuration:

- Java 21 toolchain
- 5 warm-up iterations
- 10 measurement iterations
- 3 forks
- 1 s per measurement iteration
- CSV result output

Do not compare absolute latency from another paper directly with these measurements unless hardware, runtime, cryptography, workload, and measurement methodology are equivalent. Published AIP or other agent-authorization measurements are reference points only until their implementation is reproduced in this harness.

## Result handling

Generated measurements belong under `build/results/jmh/`; raw research datasets intended for publication should be copied to `results/raw/` with an accompanying environment manifest.

Record at minimum: OS, CPU, cores, RAM, Java version, crypto/provider version, Git commit, benchmark parameters, and timestamp.

## Next benchmark increments

- E4 task/context policy complexity (1, 5, 10, 20, 50, 100 rules)
- E5 NIST authentication-assurance provenance ablation
- E6 adversarial/correctness cases: privilege escalation, resource expansion, TTL expansion, expiration, forged signature, parent invalidation, depth violation, context violation, and insufficient assurance
- reproduced agent-native delegation baseline (prefer a public reference implementation rather than an ad-hoc rewrite)
