package io.github.dbchoi85.pmiai.security;

import java.util.List;

public record AdversarialSuiteResult(List<AdversarialCaseResult> cases) {
    public AdversarialSuiteResult { cases = List.copyOf(cases); }
    public int total() { return cases.size(); }
    public long blocked() { return cases.stream().filter(AdversarialCaseResult::attackBlocked).count(); }
    public boolean allBlocked() { return blocked() == total(); }
}
