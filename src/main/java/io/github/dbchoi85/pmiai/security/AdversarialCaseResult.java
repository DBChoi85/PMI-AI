package io.github.dbchoi85.pmiai.security;

public record AdversarialCaseResult(
        AttackCategory category,
        boolean attackBlocked,
        String expectedDecision,
        String actualDecision) {}
