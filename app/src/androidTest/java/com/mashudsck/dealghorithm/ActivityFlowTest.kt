package com.mashudsck.dealghorithm

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class ActivityFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private val runtime get() = (compose.activity.application as DeAlghorithmApp).runtime
    private fun awaitText(text: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun click(text: String) { compose.onNodeWithText(text).performScrollTo().performClick() }
    private fun reset() = runBlocking { runtime.deleteAllData().join(); runtime.reminders(false).join() }

    @Test fun emptyListAndActivityCrudWorkWithoutObservationPermission() {
        reset()
        runtime.navigate("activities")
        awaitText("No saved activities")
        compose.onNodeWithText("Take a break").assertExists()
        click("Add an activity")
        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose.onNodeWithText("Activity name").performTextInput("x".repeat(81))
        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose.onNodeWithText("Activity name").performTextReplacement("  Read two pages  ")
        compose.onNodeWithText("Save").assertIsEnabled()
        compose.onNodeWithText("Save").performClick()
        awaitText("Read two pages")
        val stableId = runtime.ui.value.state!!.activities.single().id
        click("Add an activity")
        compose.onNodeWithText("Activity name").performTextInput("Stretch")
        compose.onNodeWithText("Save").performClick()
        awaitText("Stretch")
        click("Move Stretch up")
        compose.waitUntil(10_000) { runtime.ui.value.state!!.activities.first().name == "Stretch" }
        click("Rename Read two pages")
        compose.onNodeWithText("Activity name").performTextReplacement("Read one page")
        compose.onNodeWithText("Save").performClick()
        awaitText("Read one page")
        assertEquals(stableId, runtime.ui.value.state!!.activities.last().id)
        click("Choose Read one page")
        awaitText("Selected: Read one page")
        assertEquals("activities", runtime.ui.value.page)
        assertEquals(1L, runtime.ui.value.state!!.summaries.sumOf { it.activitySelections })
        compose.onNodeWithText("Choices are recorded; completion is not tracked.").assertExists()
        compose.onAllNodesWithText("Completed", substring = true).assertCountEquals(0)
        click("Delete Stretch")
        compose.waitUntil(10_000) { runtime.ui.value.state!!.activities.size == 1 }
        click("Timer")
        awaitText("Start manual timer")
        click("Start manual timer")
        awaitText("Running")
        click("Finish")
        awaitText("Finished")
        compose.onNodeWithText("Measured app use unavailable").assertExists()
        compose.onNodeWithText("Score unavailable").assertExists()
        assertEquals(0L, runtime.ui.value.state!!.summaries.sumOf { it.measuredMillis })
    }

    @Test fun sevenDaySummarySeparatesRecordedTotalsAndPrunesOldDates(): Unit = runBlocking {
        reset()
        val today = LocalDate.now()
        runtime.store.update { it.copy(summaries = listOf(
            DailySummary(today.minusDays(7), manualMillis = 999_000),
            DailySummary(today, measuredMillis = 120_000, manualMillis = 60_000,
                reminderRequests = 2, breaksChosen = 3, activitySelections = 4))) }
        runtime.refresh().join()
        runtime.navigate("summary")
        awaitText("Seven-day summary")
        compose.onNodeWithText("Measured app use: 2 min 0 sec").assertExists()
        compose.onAllNodesWithText("Measured app use: unavailable").assertCountEquals(6)
        compose.onNodeWithText("Manual timer: 1 min 0 sec").assertExists()
        compose.onNodeWithText("Reminder requests: 2").assertExists()
        compose.onNodeWithText("Breaks chosen: 3").assertExists()
        compose.onNodeWithText("Activity selections: 4").assertExists()
        compose.onNodeWithText(today.minusDays(6).toString()).performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText(today.minusDays(7).toString()).assertCountEquals(0)
        assertEquals(listOf(today), runtime.ui.value.state!!.summaries.map { it.date })
    }

    @Test fun activityEditsEnforceLimitsAndIgnoreRemovedOrDeletedIdentities(): Unit = runBlocking {
        reset()
        val generation = runtime.ui.value.state!!.generation
        for (name in listOf("", "   ", "x".repeat(81))) runtime.addActivity(name).join()
        assertTrue(runtime.ui.value.state!!.activities.isEmpty())
        assertNull(runtime.ui.value.error)
        runtime.addActivity("  Read two pages  ").join()
        val first = runtime.ui.value.state!!.activities.single()
        assertEquals("Read two pages", first.name)
        for (n in 2..10) runtime.addActivity(if (n == 10) "x".repeat(80) else "Activity $n").join()
        runtime.addActivity("Eleventh").join()
        assertEquals(10, runtime.ui.value.state!!.activities.size)
        runtime.renameActivity(first.id, " ").join()
        runtime.renameActivity(first.id, "x".repeat(81)).join()
        assertEquals(first, runtime.ui.value.state!!.activities.first())
        runtime.moveActivity(first.id, -1).join()
        runtime.moveActivity(first.id, 1).join()
        assertEquals(first.id, runtime.ui.value.state!!.activities[1].id)
        runtime.deleteActivity(first.id).join()
        runtime.selectActivity(first.id).join()
        assertEquals(0L, runtime.ui.value.state!!.summaries.sumOf { it.activitySelections })
        runtime.deleteAllData().join()
        runtime.addActivity("Stale editor", generation).join()
        runtime.renameActivity(first.id, "Stale rename", generation).join()
        runtime.selectActivity(first.id, generation).join()
        assertTrue(runtime.store.read().activities.isEmpty())
        assertTrue(runtime.store.read().summaries.isEmpty())
        assertNull(runtime.ui.value.activityMessage)
        assertNull(runtime.ui.value.error)
    }

    @Test fun confirmedDeletionCancelsWorkAndRejectsStaleActivityAndReminderCallbacks(): Unit = runBlocking {
        reset()
        runtime.reminders(true).join()
        runtime.addActivity("Read two pages").join()
        val activity = runtime.ui.value.state!!.activities.single()
        runtime.start(5, Purpose.OTHER).join()
        runtime.purposeText("Temporary purpose")
        val old = runtime.ui.value.state!!.intent!!
        runtime.selectActivity(activity.id).join()
        runtime.navigate("settings")
        awaitText("Delete all data")
        click("Delete all data")
        compose.onNodeWithText("Cancel").performClick()
        assertTrue(runtime.ui.value.state!!.activities.isNotEmpty())
        click("Delete all data")
        compose.onNodeWithText("Delete").performClick()
        compose.waitUntil(10_000) { runtime.ui.value.state?.generation != null && runtime.ui.value.state!!.generation != old.generation }
        runtime.callback(old.generation, old.id, old.revision, "ALARM").join()
        runtime.callback(old.generation, old.id, old.revision, "ADD").join()
        runtime.addActivity("Stale add", old.generation).join()
        runtime.selectActivity(activity.id, old.generation).join()
        runtime.purposeText("Stale purpose", old.generation)
        val cleared = runtime.store.read()
        assertNull(cleared.intent)
        assertTrue(cleared.activities.isEmpty())
        assertTrue(cleared.summaries.isEmpty())
        assertEquals(Preferences(), cleared.preferences)
        assertEquals(ReminderCheckpoint(), cleared.reminder)
        assertEquals("", runtime.ui.value.otherPurpose)
        assertNull(runtime.ui.value.activityMessage)
        val notifications = compose.activity.getSystemService(android.app.NotificationManager::class.java)
        assertTrue(notifications.activeNotifications.isEmpty())
        for (kind in listOf("ALARM", "ADD", "FINISH", "DISMISS")) {
            val pending = android.app.PendingIntent.getBroadcast(compose.activity, 0,
                android.content.Intent(compose.activity, ReminderReceiver::class.java).setAction(ReminderScheduler.actionName(kind))
                    .setData(ReminderScheduler.identity(old, kind)),
                android.app.PendingIntent.FLAG_NO_CREATE or android.app.PendingIntent.FLAG_IMMUTABLE)
            assertNull("Obsolete $kind callback still exists", pending)
        }
    }

    @Test fun disablingMonitoringPreservesSummariesAndManualTimer(): Unit = runBlocking {
        reset()
        runtime.addActivity("Read two pages").join()
        val activity = runtime.ui.value.state!!.activities.single()
        runtime.selectActivity(activity.id).join()
        runtime.start(5, Purpose.REPLY).join()
        runtime.pause().join()
        val before = runtime.ui.value.state!!
        runtime.store.update { it.copy(preferences = it.preferences.copy(monitoringOptIn = true)) }
        runtime.refresh().join()
        runtime.navigate("settings")
        awaitText("Disable monitoring")
        click("Disable monitoring")
        compose.waitUntil(10_000) { runtime.ui.value.state?.preferences?.monitoringOptIn == false }
        val after = runtime.store.read()
        assertEquals(before.summaries, after.summaries)
        assertEquals(before.activities, after.activities)
        assertEquals(before.intent, after.intent)
        assertFalse(after.preferences.monitoringOptIn)
        assertEquals(before.generation, after.generation)
    }
}
