package io.github.dbchoi85.pmiai;

import io.github.dbchoi85.pmiai.security.*;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

class AdversarialAuthorizationSuiteTest {
    @Test
    void blocksEveryDefinedAdversarialCaseWithExpectedSemantics() {
        var result = new AdversarialAuthorizationSuite().run();

        assertEquals(8, result.total());
        assertEquals(8, result.blocked());
        assertTrue(result.allBlocked());
        assertEquals(EnumSet.allOf(AttackCategory.class),
                result.cases().stream().map(AdversarialCaseResult::category)
                        .collect(java.util.stream.Collectors.toCollection(() -> EnumSet.noneOf(AttackCategory.class))));

        var aal = result.cases().stream()
                .filter(c -> c.category() == AttackCategory.INSUFFICIENT_AAL)
                .findFirst().orElseThrow();
        assertEquals("STEP_UP_REQUIRED", aal.expectedDecision());
        assertEquals("STEP_UP_REQUIRED", aal.actualDecision());
    }
}
