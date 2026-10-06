package io.github.dbchoi85.pmiai.security;

public record AdversarialCaseResult(
        AttackCategory category,
        int attempt,
        boolean malicious,
        boolean safeOutcome,
        String expectedDecision,
        String actualDecision) {}
