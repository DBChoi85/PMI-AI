package io.github.dbchoi85.pmiai.security;

public final class AdversarialSuiteMain {
    private AdversarialSuiteMain() {}

    public static void main(String[] args) {
        var result = new AdversarialAuthorizationSuite().run();
        System.out.println("category,blocked,expected,actual");
        for (var c : result.cases()) {
            System.out.printf("%s,%s,%s,%s%n", c.category(), c.attackBlocked(),
                    c.expectedDecision(), c.actualDecision());
        }
        System.out.printf("TOTAL,%d/%d,,%n", result.blocked(), result.total());
        if (!result.allBlocked()) System.exit(1);
    }
}
