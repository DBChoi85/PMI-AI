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

E1 includes NoAuth, JWT, static-PMI authorization, and the proposed Base AC + EPG path. E4 and E5 separately evaluate the incremental context/action-policy and human-assurance-policy decision paths.

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

- AC-per-Agent: each timed population creates N AA-issued ACs; timed population `AAcalls = N`.
- Proposed: one Base AC is established during untimed setup; timed child-population `AAcalls = 0`. The architecture-level setup cost is separately reported as one Base-AC AA interaction.
- `PopulationResult.authorityInteractions` denotes interactions created by that measured population only; it is a delta from the provider's pre-run interaction count.
- AA/provider construction, including RSA-2048 AA key generation, occurs in JMH `@Setup(Level.Invocation)` and is outside the timed benchmark method.
- AA interaction count denotes issuance calls in the local prototype. AC/EPG verification does not increment this counter.
- Network/remote-AA round-trip latency is intentionally excluded from E2. The experiment measures local cryptographic and credential-processing cost.
- Base-AC issuance/validation and root-agent key establishment for Proposed occur outside child-population timing.
- AC-per-Agent key-generation time is zero in the current prototype because an AC is issued to an agent identifier and does not generate a new subject key; no artificial key-generation cost is added.

### E3: delegation depth

Depths: 1, 2, 3, 5, 10.

E3 separates chain construction from chain verification with independent JMH states. The construction state contains only the service, depth, and root privilege; it does not pre-build a chain before the measured method. Chain construction therefore measures generation of the per-hop Ed25519 key pairs and signing of each EPG without an immediately preceding unmeasured construction at the same depth.

The verification state builds a fresh valid chain in `@Setup(Level.Invocation)`, outside the measured method. Verification therefore operates on a pre-built chain and validates each hop against the privilege established by the preceding hop. Encoded-chain-size measurement uses the same pre-built verification state and is not interpreted as construction latency.

Per-hop validation includes:

- signature validity
- operation/resource/lifetime attenuation against the immediate parent
- expected parent reference
- exact delegation-depth progression
- issuer/subject agent continuity

The generated benchmark chain reduces lifetime at every hop so that attenuation is exercised rather than merely reusing an identical privilege object.

Metrics:

- chain construction latency
- chain verification latency
- encoded credential/chain size
- per-hop marginal construction and verification cost (derived during analysis)

Negative tests cover privilege escalation, lifetime extension, broken parent linkage, invalid depth, and signed-field/signature tampering.

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

Generated JMH measurements belong under `build/results/jmh/`. Paper datasets use the following stable layout:

```text
results/
  raw/
    e1_authorization.csv
    e2_lifecycle.csv
    e3_delegation.csv
  summary/
    summary.csv
  environment.json
```

The canonical raw schema stores timing values in nanoseconds:

`experiment, implementation, run, iteration, agent_count, depth, keygen_ns, issuance_ns, verification_ns, authorization_ns, total_ns, credential_bytes, aa_interactions, success`

Summary generation reports mean, median, p95, p99, standard deviation, and throughput. Percentiles are calculated from raw per-sample values rather than inferred from JMH AverageTime aggregates.

Environment metadata records timestamp, OS/version/architecture, available processors, JVM maximum memory, Java version/vendor, JVM arguments, Git commit, and crypto dependency information. JVM maximum memory is not presented as physical system RAM; physical RAM/CPU model should additionally be recorded in the publication environment description when the benchmark host is finalized.

Two execution paths are intentionally kept separate.

JMH remains the microbenchmark validation path:

```bash
./gradlew clean test jmhClasses
./gradlew jmh
```

JMH output under `build/results/jmh/` is not treated as publication raw samples.

The publication-data path executes E1-E5 directly and records every measured repetition in the canonical schema:

```bash
GIT_COMMIT=$(git rev-parse HEAD) ./gradlew publicationBenchmark
```

Defaults are 3 runs, 3 discarded warm-ups per parameter cell, and 10 recorded iterations. They can be overridden explicitly:

```bash
GIT_COMMIT=$(git rev-parse HEAD) ./gradlew publicationBenchmark \
  -Pruns=10 -Piterations=10 -Pwarmups=5
```

This command writes:

- `results/raw/publication.csv`
- `results/summary/summary.csv`
- `results/environment.json`

Summary cells are grouped by experiment, implementation, agent count, and delegation depth so E2 population sizes and E3 depths are never pooled into the same percentile distribution. E4/E5 context-rule counts are encoded in their implementation labels because they are neither agent counts nor delegation depths.

The existing `exportBenchmarkMetadata -PrawFile=...` task remains available for re-summarizing a canonical raw CSV without rerunning the experiment.

## Implemented policy and correctness experiments

### E4: context-policy evaluation overhead

Context-policy overhead is measured with 1, 10, and 100 required key/value rules. The benchmark request must satisfy the privilege, action policy, task identifier, and every configured context rule. This isolates deterministic policy-evaluation overhead; it does not invoke an AI risk classifier. EPG signature/credential verification is not part of E4 and is measured separately in E1. E4 must therefore not be reported as end-to-end authorization latency.

### E5: assurance-policy evaluation / step-up-trigger overhead

`AuthorityProvenance` records the human/root principal reference, IAL, AAL, authentication time, issuer, and policy reference. IAL and AAL describe the human authority source; an AI agent is not itself labeled AAL1/AAL2/AAL3.

For each protected action, the relying-party `ActionPolicy` declares a `RequiredAAL`. Authorization produces one of three decisions:

- `ALLOW`: privilege/context are valid and human AAL provenance satisfies the action policy.
- `STEP_UP_REQUIRED`: privilege/context are valid but the preserved human AAL provenance is below `RequiredAAL`.
- `DENY`: privilege, action, resource, task, or context policy fails.

E5 measures the local AAL comparison/policy-decision path and the cost of producing a step-up trigger. EPG signature/credential verification is not part of E5 and is measured separately in E1. E5 must therefore not be reported as end-to-end authorization latency. It explicitly excludes the time required for a human to complete MFA or another authentication ceremony.


### E6: adversarial authorization correctness

E6 is a deterministic correctness suite rather than a latency benchmark. It executes one representative case for each defined attack category and records whether the authorization mechanism produces the expected safe decision.

Attack matrix:

- signature/signed-field tampering -> reject
- operation privilege escalation -> reject
- sibling resource-scope escalation -> reject
- expired EPG -> reject
- context mismatch -> deny
- broken parent linkage -> reject
- invalid delegation depth -> reject
- insufficient human AAL provenance -> `STEP_UP_REQUIRED`

The AAL case is intentionally not classified as a hard denial: when privilege and context remain valid, the architecture requires a human step-up rather than silently granting the action or permanently denying it.

Run:

```bash
./gradlew test
./gradlew adversarialSuite
```

The standalone suite prints a CSV-like case report and exits non-zero if any defined adversarial case is not blocked with the expected semantics.


## Interpretation boundaries

E1, E4, and E5 intentionally measure different layers:

```text
E1: credential verification + common privilege policy
E4: incremental context/action-policy evaluation
E5: incremental human-assurance policy / step-up decision
```

E4/E5 values must not be added to or compared with E1 as though all three were identical end-to-end request paths unless a later integrated benchmark explicitly composes those layers.

Current prototype limitations relevant to result interpretation are documented in `ARCHITECTURE.md`: replay prevention, a configured maximum delegation-depth policy, cryptographic parent-grant identifiers, and independently EPG-signed assurance-provenance fields are outside the evaluated implementation. E6 therefore reports deterministic correctness for implemented invariants rather than a complete production-security claim.

A reproduced public agent-native delegation implementation remains a future comparative baseline; published measurements from other hardware remain contextual reference points only.
