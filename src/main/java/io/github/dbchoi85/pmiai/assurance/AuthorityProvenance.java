package io.github.dbchoi85.pmiai.assurance;

public record AuthorityProvenance(
        String humanPrincipalRef,
        IdentityAssuranceLevel ial,
        AuthenticatorAssuranceLevel aal,
        long authenticationTimeEpochSecond,
        String issuer,
        String policyRef) {}
