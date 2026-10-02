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

E1 includes NoAuth, JWT, static-PMI authorization, and the proposed Base AC + EPG path. Context/action policy and NIST AAL provenance remain later ablations.

## Experiments

### E1: authorization overhead

All implementations evaluate the same protected action: `read /project/A/src/module/file.txt` under the same child privilege. The common decision path is exposed through `RequestAuthorizer`.

Implementations:

- NoAuth: unconditional allow; lower-bound request-processing baseline.
- JWT: compact JWT signed and verified with Ed25519 plus the common privilege policy.
- Static PMI: reused X.509 Attribute Certificate verification plus the common privilege policy.
- Proposed: verified base PMI authority, attenuated child EPG verification, plus the common privilege policy.

JWT uses Ed25519 so its signature verification primitive matches EPG as closely as practical. Static PMI retains RSA/SHA-256 because the current Mini-PMI AC implementation represents the X.509 PMI baseline; cryptographic differences must therefore be disclosed when interpreting absolute latency.

JMH reports both AverageTime and Throughput for E1. Credential sizes are exposed by each authorizer for later result export.

Credential lifecycle benchmarks separately measure key generation, issuance/delegation, verification, and total lifecycle cost. EPG issuance receives a pre-generated child public key so that key-generation time is not silently included in delegation latency.

### E2: ephemeral-agent lifecycle scalability

Target populations: 1, 10, 100, 1,000, and 10,000 agents.

E2 uses `SingleShotTime` because each benchmark invocation represents creation and validation of an entire agent population rather than a steady-state single credential operation. Three warm-up invocations, ten measured invocations, and three forks are used for each population size.

Metrics:

- total lifecycle time
- issuance/delegation throughput
- credential bytes (aggregate population bytes)
- AA interaction count
- key-generation time (reported separately)
- issuance/delegation time
- verification time
- successful verification count
- component-level lifecycle latency (key generation, issuance/delegation, verification)
- total proposed lifecycle latency (key generation + EPG issuance + verification)

Expected structural difference, not a performance assumption:

- AC-per-Agent: AA issuance interactions grow with N; `AAcalls = N`.
- Proposed: one base-AC issuance establishes the authority state; EPG derivation does not require a new AA issuance for each child; `AAcalls = 1`.
- AA interaction count denotes issuance calls in the local prototype. AC/EPG verification does not increment this counter.
- Network/remote-AA round-trip latency is intentionally excluded from E2. The experiment measures local cryptographic and credential-processing cost.
- Base-AC setup for Proposed occurs outside the child-population timing. Its one-time authority interaction is still reported separately.
- AC-per-Agent key-generation time is zero in the current prototype because an AC is issued to an agent identifier and does not generate a new subject key; no artificial key-generation cost is added.

### E3: delegation depth

Depths: 1, 2, 3, 5, 10.

Metrics:

- chain verification latency
- encoded credential/chain size
- per-hop marginal verification cost

This experiment is the preferred location for a later reproduced AIP/capability-delegation baseline because it exercises comparable multi-hop semantics.

## Methodology

Lifecycle measurement boundaries:

- `agentKeyGeneration`: Ed25519 child key generation only.
- `acPerAgentIssuance`: AA-side AC construction/signing only.
- `epgIssuanceWithPreGeneratedKey`: EPG construction/signing using a child public key prepared outside the measured operation.
- `acVerification` and `epgVerification`: verification only; credentials are prepared outside the measured operation.
- `epgTotalLifecycle`: child key generation + EPG issuance + EPG verification.
- Base-AC establishment remains trial setup and is not charged to every ephemeral child.

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
