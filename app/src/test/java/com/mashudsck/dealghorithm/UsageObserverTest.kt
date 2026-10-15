package com.mashudsck.dealghorithm

import org.junit.Assert.*
import org.junit.Test

class UsageObserverTest {
    private fun event(at: Long, change: UsageChange, app: ObservedApp = ObservedApp.SELECTED,
        activity: String = "A", pkg: String = app.name) = UsageTransition(at, change, app, pkg, activity)
    private fun switch(at: Long, from: ObservedApp, to: ObservedApp) = listOf(
        event(at, UsageChange.PAUSED, from), event(at, UsageChange.RESUMED, to))

    @Test fun samePackageActivityHandoffsAreNotReturnsOrDoubleTime() {
        val reducer = UsageReducer(0, baselineKnown = true)
        val trace = listOf(event(0, UsageChange.RESUMED), event(5_000, UsageChange.RESUMED, activity = "B"),
            event(10_000, UsageChange.PAUSED), event(10_000, UsageChange.RESUMED, activity = "C"),
            event(15_000, UsageChange.PAUSED, activity = "B"))
        val snapshot = reducer.reduce(20_000, trace)
        assertEquals(20_000L, snapshot.selectedMillis)
        assertEquals(0, snapshot.qualifyingReturns)
        assertEquals(ObservedApp.SELECTED, snapshot.focus)
    }

    @Test fun otherReturnNeedsAtLeastTenSecondsAndAnEarlierSelectedOpening() {
        for (away in listOf(9_999L, 10_000L)) {
            val trace = listOf(event(0, UsageChange.RESUMED)) + switch(5_000, ObservedApp.SELECTED, ObservedApp.OTHER) +
                switch(5_000 + away, ObservedApp.OTHER, ObservedApp.SELECTED)
            val snapshot = UsageReducer(0, true).reduce(6_000 + away, trace)
            assertEquals(if (away == 10_000L) 1 else 0, snapshot.qualifyingReturns)
            assertNull(snapshot.repeatOpenings)
        }
        val firstOpening = listOf(event(0, UsageChange.RESUMED, ObservedApp.OTHER)) +
            switch(20_000, ObservedApp.OTHER, ObservedApp.SELECTED)
        assertEquals(0, UsageReducer(0, true).reduce(21_000, firstOpening).qualifyingReturns)
    }

    @Test fun ownAppReminderAndLockedReturnPathsNeverQualify() {
        val ownPath = listOf(event(0, UsageChange.RESUMED)) + switch(5_000, ObservedApp.SELECTED, ObservedApp.DEALGORITHM) +
            switch(10_000, ObservedApp.DEALGORITHM, ObservedApp.OTHER) + switch(30_000, ObservedApp.OTHER, ObservedApp.SELECTED)
        assertEquals(0, UsageReducer(0, true).reduce(31_000, ownPath).qualifyingReturns)
        val locked = listOf(event(0, UsageChange.RESUMED), UsageTransition(5_000, UsageChange.KEYGUARD_SHOWN),
            UsageTransition(5_000, UsageChange.SCREEN_OFF), UsageTransition(25_000, UsageChange.SCREEN_ON),
            UsageTransition(25_000, UsageChange.KEYGUARD_HIDDEN), event(25_000, UsageChange.RESUMED))
        val snapshot = UsageReducer(0, true).reduce(30_000, locked)
        assertEquals(10_000L, snapshot.selectedMillis)
        assertEquals(0, snapshot.qualifyingReturns)
        assertFalse(snapshot.complete)
    }

    @Test fun ambiguousMultiResumeExcludesTimeAndCannotQualifyAReturn() {
        val trace = listOf(event(0, UsageChange.RESUMED), event(10_000, UsageChange.RESUMED, ObservedApp.OTHER),
            event(15_000, UsageChange.RESUMED, ObservedApp.OTHER, pkg = "other-two"),
            event(20_000, UsageChange.PAUSED, ObservedApp.OTHER),
            event(25_000, UsageChange.PAUSED, ObservedApp.OTHER, pkg = "other-two"))
        val reducer = UsageReducer(0, true)
        val ambiguous = reducer.reduce(19_000, trace.filter { it.at < 19_000 })
        assertEquals(ObservedApp.UNKNOWN, ambiguous.focus)
        assertEquals(ObservationIssue.MULTIPLE_RESUMED_APPS, ambiguous.issue)
        assertFalse(ambiguous.complete)
        val recovered = reducer.reduce(35_000, trace)
        assertEquals(20_000L, recovered.selectedMillis)
        assertEquals(0, recovered.qualifyingReturns)
    }

    @Test fun queryOverlapIsDeduplicatedAndLateOrMissingDataInvalidatesCoverage() {
        val reducer = UsageReducer(0, true)
        val opening = event(0, UsageChange.RESUMED)
        reducer.reduce(10_000, listOf(opening, opening))
        assertEquals(20_000L, reducer.reduce(20_000, listOf(opening)).selectedMillis)
        val late = reducer.reduce(30_000, listOf(event(15_000, UsageChange.RESUMED, ObservedApp.OTHER)))
        assertEquals(20_000L, late.selectedMillis)
        assertFalse(late.complete)
        assertNull(late.repeatOpenings)
        assertEquals(ObservationIssue.LATE_EVENT, late.issue)
        val missing = reducer.reduce(40_000, emptyList(), queryAvailable = false)
        assertEquals(20_000L, missing.selectedMillis)
        assertEquals(ObservedApp.UNKNOWN, missing.focus)
        assertEquals(ObservationIssue.MISSING_EVENT, missing.issue)
    }

    @Test fun fullTenMinuteWindowIsRequiredAndNativeStartNeedsABaselineBoundary() {
        val reducer = UsageReducer(0, true)
        val opening = event(0, UsageChange.RESUMED)
        assertFalse(reducer.reduce(599_999, listOf(opening)).complete)
        val full = reducer.reduce(600_000, emptyList())
        assertTrue(full.complete)
        assertEquals(0, full.repeatOpenings)
        val nativeStart = UsageReducer(0)
        assertEquals(ObservationIssue.BASELINE_MISSING, nativeStart.reduce(0, emptyList()).issue)
        val trace = listOf(opening, UsageTransition(100, UsageChange.SCREEN_OFF),
            UsageTransition(200, UsageChange.SCREEN_ON), UsageTransition(200, UsageChange.KEYGUARD_HIDDEN),
            event(300, UsageChange.RESUMED))
        val fresh = nativeStart.reduce(1_300, trace)
        assertEquals(1_000L, fresh.selectedMillis)
        assertNull(fresh.issue)
        assertFalse(fresh.complete)
    }

    @Test fun twoMinuteAbsenceResetsDurationAndTenMinuteAbsenceEndsReturnSession() {
        val reducer = UsageReducer(0, true)
        val trace = listOf(event(0, UsageChange.RESUMED)) + switch(20_000, ObservedApp.SELECTED, ObservedApp.OTHER)
        assertEquals(20_000L, reducer.reduce(139_999, trace).durationMillis)
        assertEquals(0L, reducer.reduce(140_000, emptyList()).durationMillis)
        val returned = reducer.reduce(151_000, switch(150_000, ObservedApp.OTHER, ObservedApp.SELECTED))
        assertEquals(1_000L, returned.durationMillis)
        assertEquals(1, returned.qualifyingReturns)
        reducer.reduce(751_000, switch(151_000, ObservedApp.SELECTED, ObservedApp.OTHER))
        val nextSession = reducer.reduce(762_000, switch(761_000, ObservedApp.OTHER, ObservedApp.SELECTED))
        assertEquals(0, nextSession.qualifyingReturns)
        assertEquals(1_000L, nextSession.durationMillis)
    }

    @Test fun postLockCleanupPauseDoesNotDiscardTheFirstFreshResume() {
        val reducer = UsageReducer(0)
        val boundary = listOf(UsageTransition(100, UsageChange.SCREEN_OFF),
            UsageTransition(200, UsageChange.SCREEN_ON), UsageTransition(200, UsageChange.KEYGUARD_HIDDEN))
        // An activity already resumed before consent may stay resumed across lock, then pause later.
        val trace = boundary + event(210, UsageChange.PAUSED, ObservedApp.DEALGORITHM) +
            event(300, UsageChange.RESUMED)
        val fresh = reducer.reduce(1_300, trace)
        assertEquals(ObservedApp.SELECTED, fresh.focus)
        assertEquals(1_000L, fresh.selectedMillis)
        assertFalse(fresh.complete)
        assertNull(fresh.issue)
        // Missing pauses outside the empty post-lock baseline still invalidate, never guess focus.
        val missing = reducer.reduce(1_500, listOf(event(1_400, UsageChange.PAUSED, ObservedApp.OTHER)))
        assertEquals(ObservationIssue.UNMATCHED_PAUSE, missing.issue)
        assertEquals(ObservedApp.UNKNOWN, missing.focus)
        val noBoundary = UsageReducer(0, true).reduce(100, listOf(event(0, UsageChange.PAUSED)))
        assertEquals(ObservationIssue.UNMATCHED_PAUSE, noBoundary.issue)
    }

    @Test fun lateEventsInvalidateOldCoverageButDoNotDiscardAFreshBoundaryOrReplayTwice() {
        val reducer = UsageReducer(0, true)
        reducer.reduce(20_000, listOf(event(0, UsageChange.RESUMED)))
        val batch = listOf(event(15_000, UsageChange.RESUMED, ObservedApp.OTHER),
            UsageTransition(21_000, UsageChange.SCREEN_OFF), UsageTransition(22_000, UsageChange.SCREEN_ON),
            UsageTransition(22_000, UsageChange.KEYGUARD_HIDDEN), event(23_000, UsageChange.RESUMED))
        val recovered = reducer.reduce(30_000, batch)
        assertEquals(ObservedApp.SELECTED, recovered.focus)
        assertEquals(27_000L, recovered.selectedMillis)
        assertEquals(7_000L, recovered.durationMillis)
        assertEquals(1, recovered.lateEventCount)
        assertEquals(5_000L, recovered.maxLatenessMillis)
        assertFalse(recovered.complete)
        assertNull(recovered.repeatOpenings)
        val overlap = reducer.reduce(31_000, batch)
        assertEquals(ObservedApp.SELECTED, overlap.focus)
        assertEquals(28_000L, overlap.selectedMillis)
        assertEquals(1, overlap.lateEventCount)
        assertEquals(5_000L, overlap.maxLatenessMillis)
        assertNull(overlap.issue)
    }

    @Test fun matchedPauseResumeHandoffsKeepVerifiedInputsWithoutCountingEmptyIntervals() {
        val trace = listOf(event(0, UsageChange.RESUMED), event(10_000, UsageChange.PAUSED),
            event(10_050, UsageChange.RESUMED, ObservedApp.OTHER),
            event(25_050, UsageChange.PAUSED, ObservedApp.OTHER), event(25_100, UsageChange.RESUMED))
        val snapshot = UsageReducer(0, true).reduce(600_000, trace)
        assertEquals(584_900L, snapshot.selectedMillis)
        assertEquals(584_900L, snapshot.durationMillis)
        assertTrue(snapshot.complete)
        assertEquals(1, snapshot.repeatOpenings)
        // An empty handoff cannot turn 9,999 verified OTHER milliseconds into ten seconds.
        val shortOther = trace.take(3) + event(20_049, UsageChange.PAUSED, ObservedApp.OTHER) +
            event(20_100, UsageChange.RESUMED)
        assertEquals(0, UsageReducer(0, true).reduce(600_000, shortOther).repeatOpenings)
        val idle = UsageReducer(0, true).reduce(20_000, trace.take(2))
        assertEquals(10_000L, idle.selectedMillis)
        assertEquals(ObservedApp.UNKNOWN, idle.focus)
        assertNull(idle.repeatOpenings)
    }

    @Test fun missingIdentityOrOwnAppDuringHandoffsNeverQualifiesAReturn() {
        val initial = listOf(event(0, UsageChange.RESUMED), event(10_000, UsageChange.PAUSED))
        val missing = initial + UsageTransition(10_050, UsageChange.RESUMED, ObservedApp.OTHER) +
            event(25_100, UsageChange.RESUMED)
        val invalid = UsageReducer(0, true).reduce(600_000, missing)
        assertFalse(invalid.complete)
        assertEquals(0, invalid.qualifyingReturns)
        val own = initial + event(10_050, UsageChange.RESUMED, ObservedApp.DEALGORITHM) +
            event(15_000, UsageChange.PAUSED, ObservedApp.DEALGORITHM) +
            event(15_050, UsageChange.RESUMED, ObservedApp.OTHER) +
            event(30_050, UsageChange.PAUSED, ObservedApp.OTHER) + event(30_100, UsageChange.RESUMED)
        assertEquals(0, UsageReducer(0, true).reduce(600_000, own).repeatOpenings)
    }
}
