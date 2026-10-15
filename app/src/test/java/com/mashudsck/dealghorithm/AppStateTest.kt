package com.mashudsck.dealghorithm

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId

class AppStateTest {
    @Test fun manualIntervalsPartitionMidnightWithoutDoubleCounting() {
        val result = AppState().addManualInterval(Instant.parse("2026-10-08T23:59:50Z").toEpochMilli(), 30_000, ZoneId.of("UTC"))
        assertEquals(listOf(10_000L, 20_000L), result.summaries.map { it.manualMillis })
        assertEquals(30_000L, result.summaries.sumOf { it.manualMillis })
        assertEquals(0L, result.summaries.sumOf { it.measuredMillis })
    }
    @Test fun defaultsAndRetentionArePrivateAndBounded() {
        val today = LocalDate.of(2026, 10, 9)
        val state = AppState(summaries = listOf(DailySummary(today.minusDays(7)), DailySummary(today), DailySummary(today.plusDays(1))))
        assertFalse(state.preferences.monitoringOptIn)
        assertNull(state.preferences.selectedPackage)
        assertEquals(listOf(today), state.retained(today).summaries.map { it.date })
        assertEquals(0L, state.retained(today).summaries.single().measuredMillis)
    }

    @Test fun invalidDurableStateIsRejected() {
        val generation = "current"
        val invalid = listOf<() -> Unit>(
            { Activity("id", " ") }, { Activity("id", " name ") }, { Activity("id", "x".repeat(81)) },
            { DailySummary(LocalDate.now(), manualMillis = -1) },
            { AppState(schemaVersion = 2) },
            { AppState(activities = (0..10).map { Activity(it.toString(), "Name") }) },
            { AppState(activities = listOf(Activity("same", "One"), Activity("same", "Two"))) },
            { IntentCheckpoint("id", SessionMode.MANUAL, "com.zhiliaoapp.musically", Purpose.OTHER, 1, IntentStatus.RUNNING, generation) },
            { AppState(generation = generation, intent = IntentCheckpoint("id", SessionMode.MANUAL, null, Purpose.OTHER, 1, IntentStatus.PAUSED, "old")) },
        )
        invalid.forEach { operation ->
            try { operation(); fail("Invalid durable state accepted") } catch (_: IllegalArgumentException) { }
        }
    }
}
