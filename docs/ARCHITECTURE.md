# PMI-AI benchmark architecture

## Scope

This repository evaluates AI-agent privilege delegation rooted in X.509 Privilege Management Infrastructure (PMI). DPKI is outside the scope.

## Authority model

```text
Human principal
(IAL/AAL-authenticated)
       |
       | authority provenance
       v
X.509 PMI Source of Authority / Attribute Authority
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
       |
       v
Protected action
       |
context/action/AAL policy
```

The AA remains the persistent organizational authority. Dynamically created child agents do not require a new AA-issued Attribute Certificate in the proposed path.

NIST identity and authenticator assurance are properties of the human/root authority source. The prototype does not label an AI agent as AAL1, AAL2, or AAL3. `AuthorityProvenance` preserves the human principal reference, IAL, AAL, authentication time, issuer, and policy reference for local action-policy evaluation.

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

A derived privilege is valid only when its operation set, resource scope, and lifetime are no broader than its parent. Resource scopes use path-boundary semantics: for example, `/project/A/**` includes `/project/A/src/**` but not the sibling prefix `/project/AB/**`. Each grant is signed by the parent agent, and task ID, parent ID, privilege, delegation depth, authority ID, subject key, and other grant fields are covered by the signature.

Delegation-chain validation additionally checks parent-reference progression, exact delegation depth, issuer/subject continuity, signature validity, and attenuation against the immediate parent.

At action time, `ContextAssuranceAuthorizer` evaluates privilege validity, operation/resource policy, task identifier, required context attributes, and the relying party's `RequiredAAL`. Insufficient human AAL provenance produces `STEP_UP_REQUIRED`; invalid privilege or context produces `DENY`.

## PMI implementation

The Mini-PMI uses Bouncy Castle X.509 Attribute Certificates. The benchmark AA signs ACs with RSA/SHA-256. EPGs use Ed25519 so that PMI certificate issuance cost and lightweight local delegation can be measured independently. Absolute latency therefore reflects both architectural and cryptographic/representation differences and must not be attributed solely to the delegation architecture.

## Prototype limitations

The current implementation intentionally leaves several controls outside the evaluated prototype:

- no replay identifier, nonce, or replay cache is enforced for EPG use
- no explicit policy maximum for delegation depth is enforced; E3 evaluates finite configured depths
- `parentId` is a logical grant reference rather than a cryptographic hash/fingerprint of the parent grant
- `AuthorityProvenance` is trusted authorization input derived from the established authority state; its fields are not independently embedded and signed inside each EPG
- authentication freshness is recorded through authentication time but no maximum-authentication-age policy is currently evaluated
- network/remote-AA latency and the time for a human to complete step-up authentication are outside the measured local prototype

These limitations constrain the security claims: E6 validates implemented authorization invariants, not complete replay-resistant production security.

## Interpretation

The proposed mechanism is not intended to claim that short-lived delegation, capability attenuation, or human-rooted authority is novel by itself. The experimental question is whether an established PMI authority can serve as a persistent organizational anchor while dynamically instantiated agents receive constrained short-lived privileges without per-agent AA issuance, and how much local authorization/policy overhead that projection introduces.
