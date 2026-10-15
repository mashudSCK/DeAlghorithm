package com.mashudsck.dealghorithm

import org.junit.Assert.*
import org.junit.Test

class ScoreRulesTest {
    @Test fun examplesCapsAndUpwardHalfRoundingAreExact() {
        assertEquals(50, ScoreRules.score(600_000, 3, true))
        assertEquals(100, ScoreRules.score(1_200_000, 6, true))
        assertEquals(60, ScoreRules.score(1_200_000, 0, true))
        assertEquals(100, ScoreRules.score(Long.MAX_VALUE, Int.MAX_VALUE, true))
        assertEquals(0, ScoreRules.score(9_999, 0, true))
        assertEquals(1, ScoreRules.score(10_000, 0, true))
        assertNull(ScoreRules.score(null, 0, true))
        assertNull(ScoreRules.score(600_000, null, true))
        assertNull(ScoreRules.score(600_000, 3, false))
        assertNull(ScoreRules.score(600_000, 3, true, SessionMode.MANUAL))
    }

    @Test fun boundariesAndStageAdvancementRespectFirstAwarenessAndPriorReflection() {
        assertNull(ScoreRules.candidate(0))
        assertNull(ScoreRules.candidate(30))
        for (score in listOf(31, 60, 61, 80, 81, 100)) assertEquals(ReminderKind.AWARENESS, ScoreRules.candidate(score))
        assertEquals(ReminderKind.AWARENESS, ScoreRules.candidate(60, ReminderKind.AWARENESS))
        assertEquals(ReminderKind.INTENT_CHECK, ScoreRules.candidate(61, ReminderKind.AWARENESS))
        assertEquals(ReminderKind.INTENT_CHECK, ScoreRules.candidate(100, ReminderKind.AWARENESS))
        assertEquals(ReminderKind.REFLECTION, ScoreRules.candidate(81, ReminderKind.INTENT_CHECK))
        assertEquals(ReminderKind.REPLACEMENT, ScoreRules.candidate(81, ReminderKind.REFLECTION))
        assertEquals(ReminderKind.INTENT_CHECK, ScoreRules.candidate(80, ReminderKind.REPLACEMENT))
        assertEquals(ReminderKind.AWARENESS, ScoreRules.candidate(31, ReminderKind.REPLACEMENT))
    }

    @Test fun budgetExpiryWinsAndSuppressionPreventsCandidatesWithoutAdvancingStage() {
        val previous = ReminderKind.REFLECTION
        assertEquals(ReminderKind.BUDGET, ScoreRules.candidate(100, previous, budgetExpired = true, activeBudget = true))
        assertNull(ScoreRules.candidate(100, previous, activeBudget = true))
        assertNull(ScoreRules.candidate(100, previous, budgetExpired = true, suppressed = true))
        assertNull(ScoreRules.candidate(100, previous, unresolved = true))
        assertNull(ScoreRules.candidate(100, previous, cooling = true))
        assertEquals(ReminderKind.REPLACEMENT, ScoreRules.candidate(100, previous))
    }

    @Test fun verifiedDurationResetsAtTwoMinutesWithoutResettingRollingReturnsEarly() {
        val reducer = UsageReducer(0, true)
        val selected = UsageTransition(0, UsageChange.RESUMED, ObservedApp.SELECTED, "selected", "A")
        val away = listOf(UsageTransition(600_000, UsageChange.PAUSED, ObservedApp.SELECTED, "selected", "A"),
            UsageTransition(600_000, UsageChange.RESUMED, ObservedApp.OTHER, "other", "A"))
        assertEquals(30, ScoreRules.score(reducer.reduce(600_000, listOf(selected)).durationMillis, 0, true))
        val reset = reducer.reduce(720_000, away)
        assertEquals(0L, reset.durationMillis)
        assertEquals(0, ScoreRules.score(reset.durationMillis, reset.repeatOpenings, reset.complete))
    }
}
