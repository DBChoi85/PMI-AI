# PMI-AI benchmark architecture

## Scope

This repository evaluates AI-agent privilege delegation rooted in X.509 Privilege Management Infrastructure (PMI). DPKI is outside the scope.

## Authority model

```text
Human / organizational authority
            |
            v
       X.509 PMI AA
            |
       Base AC (root)
            |
            v
        Root Agent
            |
      signed EPG
            |
            v
       Child Agent
            |
      signed EPG
            |
            v
      Descendant Agent
```

The AA remains the persistent organizational authority. Dynamically created child agents do not require a new AA-issued Attribute Certificate in the proposed path.

## Ephemeral Privilege Grant

The prototype EPG contains:

- issuer and subject agent identifiers
- parent credential/grant reference
- task identifier
- operations/resource/expiry privilege
- authority identifier
- delegation depth
- child public key
- Ed25519 signature

The JSON representation is used for storage/size measurement only and is not a proposed wire standard. Signatures do not depend on JSON serialization. The signing input uses a versioned, length-prefixed binary representation with operations sorted lexicographically, so equivalent privilege sets produce deterministic signing bytes.

## Security invariants

A derived privilege is valid only when its operation set, resource scope, and lifetime are no broader than its parent. Resource scopes use path-boundary semantics: for example, `/project/A/**` includes `/project/A/src/**` but not the sibling prefix `/project/AB/**`. Each grant is signed by the parent agent, and task ID, parent ID, privilege, delegation depth, authority ID, subject key, and other grant fields are covered by the signature. Later increments will add explicit maximum delegation depth, replay identifiers, context constraints, action policy, and assurance provenance.

## PMI implementation

The Mini-PMI uses Bouncy Castle X.509 Attribute Certificates. The benchmark AA signs ACs with RSA/SHA-256. EPGs use Ed25519 so that PMI certificate issuance cost and lightweight local delegation can be measured independently.

## Interpretation

The proposed mechanism is not intended to claim that short-lived delegation, capability attenuation, or human-rooted authority is novel by itself. The experimental question is whether an established PMI authority can serve as a persistent organizational anchor while dynamically instantiated agents receive constrained short-lived privileges without per-agent AA issuance.
