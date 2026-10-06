package io.github.dbchoi85.pmiai;

import io.github.dbchoi85.pmiai.security.*;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

class AdversarialAuthorizationSuiteTest {
    @Test
    void safelyHandlesRepeatedAdversarialCasesAndLegitimateControls() {
        int attempts = 3;
        var result = new AdversarialAuthorizationSuite().run(attempts);

        assertEquals(9 * attempts, result.total());
        assertEquals(8 * attempts, result.maliciousTotal());
        assertEquals(8 * attempts, result.maliciousBlocked());
        assertEquals(attempts, result.legitimateTotal());
        assertEquals(attempts, result.legitimateAccepted());
        assertTrue(result.allSafe());
        assertEquals(EnumSet.allOf(AttackCategory.class),
                result.cases().stream().map(AdversarialCaseResult::category)
                        .collect(java.util.stream.Collectors.toCollection(() -> EnumSet.noneOf(AttackCategory.class))));

        var aal = result.cases().stream()
                .filter(c -> c.category() == AttackCategory.INSUFFICIENT_AAL)
                .findFirst().orElseThrow();
        assertEquals("STEP_UP_REQUIRED", aal.expectedDecision());
        assertEquals("STEP_UP_REQUIRED", aal.actualDecision());

        var legitimate = result.cases().stream()
                .filter(c -> c.category() == AttackCategory.LEGITIMATE_CONTROL)
                .findFirst().orElseThrow();
        assertFalse(legitimate.malicious());
        assertEquals("ALLOW", legitimate.expectedDecision());
        assertEquals("ALLOW", legitimate.actualDecision());
    }
}
