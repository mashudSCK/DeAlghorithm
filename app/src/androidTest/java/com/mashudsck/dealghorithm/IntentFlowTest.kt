package com.mashudsck.dealghorithm

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class IntentFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Before fun openTimerPage() {
        val runtime = (compose.activity.application as DeAlghorithmApp).runtime
        runtime.navigate("timer")
        runBlocking { runtime.refresh().join() }
    }

    private fun awaitText(text: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun manualControlsWorkWithoutUsageAccess() {
        val runtime = (compose.activity.application as DeAlghorithmApp).runtime
        runBlocking { runtime.deleteAllData().join(); runtime.reminders(false).join() }
        awaitText("Start manual timer")
        compose.onNodeWithText("Score unavailable").assertExists()
        compose.onNodeWithText("Start manual timer").performScrollTo().performClick()
        awaitText("Pause")
        compose.onNodeWithText("Pause").performClick()
        awaitText("Paused")
        compose.onNodeWithText("Paused").assertExists()
        compose.onNodeWithText("Resume").performClick()
        awaitText("Running")
        runBlocking {
            runtime.store.update { it.copy(intent = it.intent!!.copy(remainingMillis = 0)) }
            runtime.refresh().join()
        }
        awaitText("Overdue")
        compose.onNodeWithText("Choose an activity").assertExists()
        compose.onNodeWithText("Add 5 minutes").performClick()
        awaitText("Running")
        compose.onNodeWithText("Finish").performClick()
        awaitText("Finished")
        compose.onNodeWithText("Finished").assertExists()
        compose.onNodeWithText("Score unavailable").assertExists()
        assertEquals(0L, runtime.ui.value.state!!.summaries.sumOf { it.measuredMillis })
        assertEquals(0L, runtime.ui.value.state!!.summaries.sumOf { it.reminderRequests })
    }

    @Test fun notificationActionsExtendOnceAndRejectStaleIdentity(): Unit = runBlocking {
        val runtime = (compose.activity.application as DeAlghorithmApp).runtime
        try {
            runtime.deleteAllData().join()
            runtime.start(5, Purpose.ENTERTAINMENT).join()
            runtime.store.update { it.copy(intent = it.intent!!.copy(remainingMillis = 0)) }
            runtime.refresh().join()
            compose.waitUntil(10_000) { runtime.ui.value.state?.reminder?.unresolved == ReminderKind.BUDGET }
            val expired = runtime.ui.value.state!!.intent!!
            awaitText("Overdue")
            val action = Intent(compose.activity, ReminderReceiver::class.java)
                .setAction(ReminderScheduler.actionName("ADD"))
                .setData(ReminderScheduler.identity(expired, "ADD"))
            compose.activity.sendBroadcast(action)
            compose.waitUntil(10_000) { runtime.ui.value.state?.intent?.revision != expired.revision }
            val extended = runtime.ui.value.state!!.intent!!
            assertEquals(IntentStatus.RUNNING, extended.status)
            assertTrue(extended.remainingMillis in 290_000L..300_000L)
            compose.activity.sendBroadcast(action)
            delay(300)
            runtime.refresh().join()
            assertEquals(extended.revision, runtime.ui.value.state!!.intent!!.revision)
            assertEquals(1L, runtime.ui.value.state!!.summaries.sumOf { it.reminderRequests })
            for (kind in listOf("DISMISS", "FINISH", "CHOOSE")) {
                runtime.deleteAllData().join()
                runtime.start(5, Purpose.OTHER).join()
                runtime.store.update { it.copy(intent = it.intent!!.copy(remainingMillis = 0)) }
                runtime.refresh().join()
                compose.waitUntil(10_000) { runtime.ui.value.state?.reminder?.unresolved == ReminderKind.BUDGET }
                val notice = runtime.ui.value.state!!.intent!!
                if (kind == "CHOOSE") {
                    val launch = Intent(compose.activity, MainActivity::class.java).setData(ReminderScheduler.identity(notice, kind))
                    val pending = android.app.PendingIntent.getActivity(compose.activity, 0, launch,
                        android.app.PendingIntent.FLAG_NO_CREATE or android.app.PendingIntent.FLAG_IMMUTABLE)
                    assertNotNull(pending)
                    pending.send()
                    compose.waitUntil(10_000) { runtime.ui.value.page == "activities" }
                    awaitText("No saved activities")
                } else {
                    compose.activity.sendBroadcast(Intent(compose.activity, ReminderReceiver::class.java)
                        .setAction(ReminderScheduler.actionName(kind)).setData(ReminderScheduler.identity(notice, kind)))
                    compose.waitUntil(10_000) { runtime.ui.value.state?.reminder?.unresolved == null }
                    assertEquals(if (kind == "FINISH") IntentStatus.FINISHED else IntentStatus.EXPIRED,
                        runtime.ui.value.state!!.intent!!.status)
                }
                assertEquals(1L, runtime.ui.value.state!!.summaries.sumOf { it.reminderRequests })
            }
        } finally {
            runtime.finish(false).join()
        }
    }

    @Test fun purposeTextNeverReachesDurableState(): Unit = runBlocking {
        val runtime = (compose.activity.application as DeAlghorithmApp).runtime
        runtime.deleteAllData().join()
        val marker = "transient-purpose-test-marker"
        runtime.purposeText(marker)
        runtime.start(5, Purpose.OTHER).join()
        assertFalse(String(runtime.store.file.readBytes(), Charsets.UTF_8).contains(marker))
        runtime.finish(false).join()
        assertEquals("", runtime.ui.value.otherPurpose)
    }

    @Test fun prepareRunningRecoveryCheckpoint(): Unit = runBlocking {
        val runtime = (compose.activity.application as DeAlghorithmApp).runtime
        runtime.deleteAllData().join()
        runtime.start(15, Purpose.OTHER).join()
        assertEquals(IntentStatus.RUNNING, runtime.ui.value.state!!.intent!!.status)
    }

    @Test fun rotationKeepsTheLiveTimerRunning(): Unit = runBlocking {
        val runtime = (compose.activity.application as DeAlghorithmApp).runtime
        runtime.deleteAllData().join()
        runtime.start(5, Purpose.OTHER).join()
        val revision = runtime.ui.value.state!!.intent!!.revision
        compose.activityRule.scenario.recreate()
        awaitText("Running")
        assertEquals(revision, runtime.ui.value.state!!.intent!!.revision)
        runtime.finish(false).join()
    }

    @Test fun deletedGenerationCannotRecreateTimerOrPurpose(): Unit = runBlocking {
        val runtime = (compose.activity.application as DeAlghorithmApp).runtime
        runtime.deleteAllData().join()
        runtime.start(5, Purpose.OTHER).join()
        val old = runtime.ui.value.state!!.intent!!
        runtime.deleteAllData().join()
        runtime.purposeText("stale-purpose", old.generation)
        runtime.callback(old.generation, old.id, old.revision, "ALARM").join()
        runtime.callback(old.generation, old.id, old.revision, "ADD").join()
        assertNull(runtime.ui.value.state!!.intent)
        assertTrue(runtime.ui.value.state!!.summaries.isEmpty())
        assertEquals("", runtime.ui.value.otherPurpose)
        assertFalse(runtime.ui.value.state!!.preferences.monitoringOptIn)
    }

    @Test fun deniedNotificationsLeaveOverdueControlsUsable(): Unit = runBlocking {
        val runtime = (compose.activity.application as DeAlghorithmApp).runtime
        runtime.deleteAllData().join()
        org.junit.Assume.assumeFalse(runtime.ui.value.notificationsAllowed)
        runtime.start(5, Purpose.ENTERTAINMENT).join()
        runtime.store.update { it.copy(intent = it.intent!!.copy(remainingMillis = 0)) }
        runtime.refresh().join()
        awaitText("Overdue")
        assertEquals(0L, runtime.ui.value.state!!.summaries.sumOf { it.reminderRequests })
        compose.onNodeWithText("Add 5 minutes").performClick()
        awaitText("Running")
        runtime.finish(false).join()
    }
}
