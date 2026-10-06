package io.github.dbchoi85.pmiai.security;

import java.util.List;

public record AdversarialSuiteResult(List<AdversarialCaseResult> cases) {
    public AdversarialSuiteResult { cases = List.copyOf(cases); }
    public int total() { return cases.size(); }
    public long safeOutcomes() { return cases.stream().filter(AdversarialCaseResult::safeOutcome).count(); }
    public long maliciousTotal() { return cases.stream().filter(AdversarialCaseResult::malicious).count(); }
    public long maliciousBlocked() {
        return cases.stream().filter(AdversarialCaseResult::malicious)
                .filter(AdversarialCaseResult::safeOutcome).count();
    }
    public long legitimateTotal() { return cases.stream().filter(c -> !c.malicious()).count(); }
    public long legitimateAccepted() {
        return cases.stream().filter(c -> !c.malicious())
                .filter(AdversarialCaseResult::safeOutcome).count();
    }
    public boolean allSafe() { return safeOutcomes() == total(); }
}
