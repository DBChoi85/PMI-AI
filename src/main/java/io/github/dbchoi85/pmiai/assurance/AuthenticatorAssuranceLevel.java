package io.github.dbchoi85.pmiai.assurance;

public enum AuthenticatorAssuranceLevel {
    AAL1(1), AAL2(2), AAL3(3);
    private final int level;
    AuthenticatorAssuranceLevel(int level) { this.level = level; }
    public boolean satisfies(AuthenticatorAssuranceLevel required) { return level >= required.level; }
}
