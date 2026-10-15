package com.mashudsck.dealghorithm

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class SessionRulesTest {
    private val wall = Instant.parse("2026-10-09T12:00:00Z").toEpochMilli()
    private val zone = ZoneId.of("UTC")

    @Test fun budgetsPauseAndExpiryCountOnlyManualTime() {
        for (minutes in listOf(5, 15, 30)) {
            val started = SessionRules.start(AppState(), minutes, Purpose.ENTERTAINMENT)
            assertEquals(minutes * 60_000L, started.intent!!.remainingMillis)
            val consumed = SessionRules.advance(started, 1_000, wall, zone)
            val paused = SessionRules.pause(consumed)
            assertEquals(paused, SessionRules.advance(paused, 10_000, wall + 1_000, zone))
            val expired = SessionRules.advance(SessionRules.resume(paused), minutes * 60_000L, wall + 1_000, zone)
            assertEquals(IntentStatus.EXPIRED, expired.intent!!.status)
            assertEquals(0L, expired.intent!!.remainingMillis)
            assertEquals(minutes * 60_000L, expired.summaries.sumOf { it.manualMillis })
            assertEquals(0L, expired.summaries.sumOf { it.measuredMillis })
        }
    }

    @Test fun extensionsAndBreaksUseTheirSpecifiedClocks() {
        val expired = SessionRules.advance(SessionRules.start(AppState(), 5, Purpose.OTHER), 300_000, wall, zone)
        val extended = SessionRules.extend(expired)
        assertEquals(300_000L, extended.intent!!.remainingMillis)
        assertEquals(300_000L, extended.reminder.suppressionRemainingMillis)
        val paused = SessionRules.pause(extended)
        assertEquals(300_000L, SessionRules.advance(paused, 60_000, wall, zone).reminder.suppressionRemainingMillis)
        val breakState = SessionRules.finish(extended, true, wall, zone)
        assertEquals(IntentStatus.FINISHED, breakState.intent!!.status)
        assertEquals(1L, breakState.summaries.sumOf { it.breaksChosen })
        assertEquals(240_000L, SessionRules.advance(breakState, 60_000, wall, zone).reminder.suppressionRemainingMillis)
    }

    @Test fun cooldownUnresolvedAndRecoveryAreConservative() {
        val expired = SessionRules.advance(SessionRules.start(AppState(), 5, Purpose.REPLY), 300_000, wall, zone)
        assertTrue(SessionRules.canRequestBudget(expired, 500_000, true))
        assertFalse(SessionRules.canRequestBudget(expired, 500_000, false))
        val cooling = expired.copy(reminder = ReminderCheckpoint(lastRequestElapsed = 500_000))
        assertFalse(SessionRules.canRequestBudget(cooling, 619_999, true))
        assertTrue(SessionRules.canRequestBudget(cooling, 620_000, true))
        val unresolved = cooling.copy(reminder = cooling.reminder.copy(sessionId = expired.intent!!.id,
            generation = expired.generation, unresolved = ReminderKind.BUDGET))
        assertFalse(SessionRules.canRequestBudget(unresolved, 700_000, true))
        val restored = SessionRules.restore(SessionRules.start(cooling, 15, Purpose.OTHER), 800_000, wall, 4)
        assertEquals(IntentStatus.PAUSED, restored.intent!!.status)
        assertEquals(900_000L, restored.intent!!.remainingMillis)
        assertEquals(800_000L, restored.reminder.lastRequestElapsed)
        assertFalse(SessionRules.clockStable(1_000, wall, 3, 2_000, wall + 1_000, 4))
        assertFalse(SessionRules.clockStable(1_000, wall, 3, 2_000, wall + 61_000, 3))
        assertFalse(SessionRules.clockStable(1_000, wall, 3, 500, wall + 1_000, 3))
        assertTrue(SessionRules.clockStable(1_000, wall, 3, 2_000, wall + 1_000, 3))
    }
}
