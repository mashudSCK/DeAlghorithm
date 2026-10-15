package com.mashudsck.dealghorithm

import android.app.KeyguardManager
import android.app.NotificationManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

// Opt-in bench only: never deletes records, grants permissions or runs in the default suite.
class PhysicalObservationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun boundedPhysicalWindow(): Unit = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val arguments = InstrumentationRegistry.getArguments()
        assumeTrue("Requires an explicitly selected physical bench run", arguments.getString("physicalObservation") == "true")
        val seconds = arguments.getString("windowSeconds", "90")!!.toInt()
        require(seconds in 30..900)
        val reminderBench = arguments.getString("physicalReminder") == "true"
        require(!reminderBench || seconds >= 360)
        val runtime = (compose.activity.application as DeAlghorithmApp).runtime
        compose.waitUntil(10_000) { runtime.ui.value.ready }
        val original = runtime.ui.value.state!!
        assertTrue("Consent must already be recorded in Settings", original.preferences.monitoringOptIn)
        assertTrue("Grant Usage Access through native Settings first", runtime.observer.usageAccess())
        assertTrue("Selected TikTok package must be installed", runtime.observer.installed(original.preferences.selectedPackage))
        val context = compose.activity
        val power = context.getSystemService(PowerManager::class.java)
        val keyguard = context.getSystemService(KeyguardManager::class.java)
        val notifications = context.getSystemService(NotificationManager::class.java)
        val elapsedStart = SystemClock.elapsedRealtime()
        runtime.startObservationTrial(original.generation).join()
        assertTrue(runtime.ui.value.observation.running)
        val wallStart = runtime.observer.trialStart
        fun report(value: String) {
            instrumentation.sendStatus(0, Bundle().apply { putString("physicalProbe", value) })
        }
        val issues = mutableMapOf<ObservationIssue, Int>()
        var selectedSamples = 0
        var unknownSamples = 0
        var maxSelected = 0L
        var completeSeen = false
        var lateEvents = 0
        var maxLateness = 0L
        var maxReturns: Int? = null
        var candidateReturns = 0
        var maxCandidateReturns = 0
        var currentComplete = false
        var reminderSession: String? = null
        var reminderStarted = 0L
        var notificationSeen = false
        var reminderTapped = false
        var reminderDelay = -1L
        try {
            if (reminderBench) {
                assertTrue("Preserve the user's disabled reminder preference", original.preferences.remindersEnabled)
                assertTrue("Enable the native intention channel before this bench", notifications.areNotificationsEnabled() &&
                    notifications.getNotificationChannel("intentions")?.importance?.let { it != NotificationManager.IMPORTANCE_NONE } == true)
                assertFalse("Do not replace an existing active manual intention", original.intent?.status in listOf(IntentStatus.RUNNING, IntentStatus.PAUSED))
                reminderStarted = SystemClock.elapsedRealtime()
                runtime.start(5, Purpose.OTHER).join()
                reminderSession = runtime.ui.value.state!!.intent!!.id
            }
            report("READY: fresh trial; lock/unlock once, then perform timed selected-app transitions")
            while (SystemClock.elapsedRealtime() - elapsedStart < seconds * 1_000L) {
                Thread.sleep(5_000)
                // Do not invoke the production read while locked: returned unlock events establish the baseline.
                if (!power.isInteractive || keyguard.isKeyguardLocked) continue
                runtime.checkObservation(original.generation).join()
                val trial = runtime.ui.value.observation
                if (!trial.running) { report("STOPPED: ${trial.message}"); break }
                trial.snapshot?.let { snapshot ->
                    snapshot.issue?.let { issues[it] = (issues[it] ?: 0) + 1 }
                    if (snapshot.focus == ObservedApp.SELECTED) selectedSamples++
                    if (snapshot.focus == ObservedApp.UNKNOWN) unknownSamples++
                    maxSelected = maxOf(maxSelected, snapshot.selectedMillis)
                    candidateReturns = snapshot.qualifyingReturns
                    maxCandidateReturns = maxOf(maxCandidateReturns, candidateReturns)
                    currentComplete = snapshot.complete
                    lateEvents = snapshot.lateEventCount
                    maxLateness = snapshot.maxLatenessMillis
                    completeSeen = completeSeen || snapshot.complete
                    snapshot.repeatOpenings?.let { maxReturns = maxOf(maxReturns ?: 0, it) }
                }
                if (arguments.getString("stopOnUnmatched") == "true" && trial.snapshot?.issue == ObservationIssue.UNMATCHED_PAUSE) {
                    report("EARLY_STOP: unmatched pause reproduced; collecting bounded replay before cleanup")
                    break
                }
                if (reminderBench && !notificationSeen) {
                    notifications.activeNotifications.firstOrNull { it.id == 1 && it.notification.channelId == "intentions" }?.let {
                        notificationSeen = true
                        reminderDelay = SystemClock.elapsedRealtime() - reminderStarted - 300_000
                        // Explicit bench action simulates tapping this app's own notice; no notification content is read.
                        it.notification.contentIntent?.send()
                        reminderTapped = it.notification.contentIntent != null
                        report("OWN_NOTICE: seen=$notificationSeen; tapSent=$reminderTapped; observedDelayMillis=$reminderDelay")
                    }
                }
                if ((SystemClock.elapsedRealtime() - elapsedStart) / 5_000 % 3 == 0L)
                    report("queries=${runtime.observer.queryCount}; selectedSamples=$selectedSamples; unknownSamples=$unknownSamples; selectedMillis=$maxSelected; complete=$completeSeen; candidateReturns=$candidateReturns; maxCandidateReturns=$maxCandidateReturns; returns=${maxReturns ?: "unavailable"}; issue=${trial.snapshot?.issue}; focus=${trial.snapshot?.focus}; lateEvents=$lateEvents; maxLatenessMillis=$maxLateness")
            }
            // One bounded post-start query audits classified events; only aggregates are reported.
            val counts = mutableMapOf<Int, Int>()
            var missingLifecycleClass = 0
            val until = runtime.observer.observedUntil
            val currentState = runtime.ui.value.state
            val canInspect = currentState?.generation == original.generation &&
                currentState.preferences.monitoringOptIn &&
                currentState.preferences.selectedPackage == original.preferences.selectedPackage &&
                runtime.observer.usageAccess()
            val events = if (canInspect && until > wallStart) context.getSystemService(UsageStatsManager::class.java).queryEvents(wallStart, until) else null
            val event = UsageEvents.Event()
            var inspected = 0
            val replayEvents = mutableListOf<UsageTransition>()
            while (events != null && events.hasNextEvent() && inspected++ < 20_000) {
                events.getNextEvent(event)
                runtime.observer.classify(event)?.let(replayEvents::add)
                if (event.eventType in listOf(UsageEvents.Event.ACTIVITY_RESUMED, UsageEvents.Event.ACTIVITY_PAUSED,
                        UsageEvents.Event.SCREEN_INTERACTIVE, UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                        UsageEvents.Event.KEYGUARD_SHOWN, UsageEvents.Event.KEYGUARD_HIDDEN)) {
                    counts[event.eventType] = (counts[event.eventType] ?: 0) + 1
                    if (event.eventType in listOf(UsageEvents.Event.ACTIVITY_RESUMED, UsageEvents.Event.ACTIVITY_PAUSED) && event.className == null)
                        missingLifecycleClass++
                }
            }
            if (canInspect && inspected < 20_000 && until >= wallStart) {
                val replay = UsageReducer(wallStart)
                val resumedHistory = mutableMapOf<String, MutableSet<String>>()
                var previousIssue: ObservationIssue? = null
                var unmatchedOnsets = 0
                for ((at, batch) in replayEvents.filter { it.at >= wallStart && it.at < until }
                        .distinct().groupBy { it.at }.toSortedMap()) {
                    val result = replay.reduce(at + 1, batch)
                    if (result.issue == ObservationIssue.UNMATCHED_PAUSE && previousIssue != result.issue) {
                        unmatchedOnsets++
                        val pauses = batch.filter { it.change == UsageChange.PAUSED }
                        report("AUDIT_UNMATCHED: pauseApps=${pauses.map { it.app }}; matchingResumeSeen=${pauses.map { resumedHistory[it.packageKey]?.contains(it.activityKey) == true }}; resumedClassCounts=${pauses.map { resumedHistory[it.packageKey]?.size ?: 0 }}")
                    }
                    for (transition in batch) when (transition.change) {
                        UsageChange.SCREEN_OFF, UsageChange.KEYGUARD_SHOWN, UsageChange.GAP -> resumedHistory.clear()
                        UsageChange.RESUMED -> if (transition.packageKey != null && transition.activityKey != null)
                            resumedHistory.getOrPut(transition.packageKey) { mutableSetOf() }.add(transition.activityKey)
                        else -> Unit
                    }
                    previousIssue = result.issue
                }
                val replayFinal = replay.reduce(until, emptyList())
                report("AUDIT_REPLAY: selectedMillis=${replayFinal.selectedMillis}; candidateReturns=${replayFinal.qualifyingReturns}; complete=${replayFinal.complete}; issue=${replayFinal.issue}; unmatchedOnsets=$unmatchedOnsets; incrementalSelectedMillis=$maxSelected; incrementalCandidateReturns=$candidateReturns")
            }
            report("FINAL: queries=${runtime.observer.queryCount}; selectedSamples=$selectedSamples; unknownSamples=$unknownSamples; selectedMillis=$maxSelected; complete=$completeSeen; candidateReturns=$candidateReturns; maxCandidateReturns=$maxCandidateReturns; returns=${maxReturns ?: "unavailable"}; currentComplete=$currentComplete; notificationSeen=$notificationSeen; reminderTapped=$reminderTapped; reminderDelayMillis=$reminderDelay; lateEvents=$lateEvents; maxLatenessMillis=$maxLateness; issues=$issues; eventKinds=$counts; missingLifecycleClass=$missingLifecycleClass; histogramCapped=${inspected >= 20_000}")
        } finally {
            runtime.observer.stop()
            if (reminderSession != null && runtime.ui.value.state?.intent?.id == reminderSession)
                runtime.finish(takeBreak = false).join()
            runtime.refresh().join()
        }
        assertEquals("Probe must not change activities", original.activities, runtime.ui.value.state!!.activities)
        assertEquals("Probe must not create measured summaries", original.summaries.sumOf { it.measuredMillis }, runtime.ui.value.state!!.summaries.sumOf { it.measuredMillis })
        assertTrue("Actual selected-app intervals were not established; physical observation remains unsupported", selectedSamples > 0 && maxSelected > 0)
        arguments.getString("expectedReturns")?.let {
            val expected = it.toInt()
            require(expected in 0..6)
            assertEquals("Verified return count differs from the timed script", expected, candidateReturns)
        }
        arguments.getString("expectedMaxReturns")?.let {
            val expected = it.toInt()
            require(expected in 0..6)
            assertEquals("Unexpected extra return during same-app/own-app/short-return paths", expected, maxCandidateReturns)
        }
        if (reminderBench) assertTrue("Physical native reminder and explicit tap were not observed", notificationSeen && reminderTapped)
        if (seconds >= 600) assertTrue("Current physical ten-minute window is incomplete", currentComplete)
    }
}