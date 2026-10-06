package io.github.dbchoi85.pmiai.security;

import java.util.stream.Collectors;

public final class AdversarialSuiteMain {
    private AdversarialSuiteMain() {}

    public static void main(String[] args) {
        int attempts = Integer.getInteger("pmiai.adversarialAttempts", 100);
        var result = new AdversarialAuthorizationSuite().run(attempts);

        System.out.println("category,attempts,malicious,expected,safe_outcomes,rate");
        var groups = result.cases().stream().collect(Collectors.groupingBy(
                c -> c.category() + "\u0000" + c.malicious() + "\u0000" + c.expectedDecision()));
        groups.values().stream()
                .sorted(java.util.Comparator.comparing(g -> g.get(0).category().name()))
                .forEach(group -> {
                    var first = group.get(0);
                    long safe = group.stream().filter(AdversarialCaseResult::safeOutcome).count();
                    System.out.printf("%s,%d,%s,%s,%d,%.4f%n", first.category(), group.size(),
                            first.malicious(), first.expectedDecision(), safe,
                            safe / (double) group.size());
                });

        System.out.printf("MALICIOUS_REJECTION,%d/%d%n", result.maliciousBlocked(), result.maliciousTotal());
        System.out.printf("LEGITIMATE_ACCEPTANCE,%d/%d%n", result.legitimateAccepted(), result.legitimateTotal());
        if (!result.allSafe()) System.exit(1);
    }
}
