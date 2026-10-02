package io.github.dbchoi85.pmiai.authz;

import io.github.dbchoi85.pmiai.assurance.AuthenticatorAssuranceLevel;

public record ActionPolicy(String operation, String resourcePrefix, String requiredTaskId,
                           AuthenticatorAssuranceLevel requiredAal) {}
