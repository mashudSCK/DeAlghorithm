package com.mashudsck.dealghorithm

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ObservationFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val runtime get() = (compose.activity.application as DeAlghorithmApp).runtime
    private fun awaitText(text: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun click(text: String) { compose.onNodeWithText(text).performScrollTo().performClick() }

    @Test fun trialConsentIsExplicitAndManualFeaturesRemainUsable() {
        runBlocking { runtime.deleteAllData().join(); runtime.reminders(false).join() }
        runtime.navigate("settings")
        awaitText("Set up observation trial")
        assertFalse(runtime.ui.value.state!!.preferences.monitoringOptIn)
        click("Set up observation trial")
        compose.onNodeWithText("Cancel").performClick()
        assertFalse(runtime.ui.value.state!!.preferences.monitoringOptIn)
        click("Set up observation trial")
        compose.onNodeWithText("Agree to trial").performClick()
        compose.waitUntil(10_000) { runtime.ui.value.state!!.preferences.monitoringOptIn }
        assertEquals(trialPackages.first(), runtime.ui.value.state!!.preferences.selectedPackage)
        assertEquals(SessionMode.MANUAL, runtime.ui.value.state!!.preferences.mode)
        if (!runtime.observer.usageAccess()) {
            awaitText("Open Usage Access settings")
            val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            val monitor = instrumentation.addMonitor(android.content.IntentFilter(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS), null, true)
            try { click("Open Usage Access settings"); assertEquals(1, monitor.hits) }
            finally { instrumentation.removeMonitor(monitor) }
        }
        compose.onNodeWithText("Automatic monitoring unavailable").assertExists()
        click("Timer")
        awaitText("Start manual timer")
        click("Start manual timer")
        awaitText("Running")
        click("Finish")
        awaitText("Finished")
        compose.onNodeWithText("Score unavailable").assertExists()
        assertEquals(0L, runtime.ui.value.state!!.summaries.sumOf { it.measuredMillis })
    }

    @Test fun deniedAccessMissingAppsAndStaleGenerationNeverQueryOrRestoreRecords(): Unit = runBlocking {
        runtime.deleteAllData().join()
        runtime.refresh().join()
        org.junit.Assume.assumeFalse(runtime.observer.usageAccess())
        runtime.checkObservation().join()
        assertEquals(0, runtime.observer.queryCount)
        assertNull(runtime.ui.value.observation.snapshot)
        runtime.enableObservationTrial(trialPackages.first()).join()
        assertEquals("Usage Access needed", runtime.ui.value.observation.message)
        runtime.startObservationTrial().join()
        runtime.checkObservation().join()
        assertFalse(runtime.ui.value.observation.running)
        assertEquals(0, runtime.observer.queryCount)
        val oldGeneration = runtime.ui.value.state!!.generation
        runtime.deleteAllData().join()
        runtime.enableObservationTrial(trialPackages.first(), oldGeneration).join()
        runtime.startObservationTrial(oldGeneration).join()
        runtime.checkObservation(oldGeneration).join()
        assertFalse(runtime.store.read().preferences.monitoringOptIn)
        assertNull(runtime.store.read().preferences.selectedPackage)
        assertNull(runtime.ui.value.observation.snapshot)
        assertEquals(0, runtime.observer.queryCount)
    }

    @Test fun accessAloneNeverImportsHistoryAndDisablePreservesManualSummaries(): Unit = runBlocking {
        runtime.deleteAllData().join()
        runtime.refresh().join()
        runtime.checkObservation().join()
        assertEquals(0, runtime.observer.queryCount)
        assertFalse(runtime.ui.value.observation.running)
        assertNull(runtime.ui.value.observation.snapshot)
        runtime.addActivity("Read two pages").join()
        runtime.selectActivity(runtime.ui.value.state!!.activities.single().id).join()
        val retained = runtime.ui.value.state!!.summaries
        runtime.enableObservationTrial(trialPackages.first()).join()
        if (!runtime.observer.installed(trialPackages.first())) {
            runtime.checkObservation().join()
            assertEquals(0, runtime.observer.queryCount)
            assertFalse(runtime.ui.value.observation.running)
        }
        runtime.disableMonitoring().join()
        assertEquals(retained, runtime.store.read().summaries)
        assertEquals(1, runtime.store.read().activities.size)
        assertFalse(runtime.store.read().preferences.monitoringOptIn)
        assertNull(runtime.ui.value.observation.snapshot)
        val bytes = runtime.store.file.readBytes().toString(Charsets.UTF_8)
        for (field in listOf("qualifyingReturns", "packageKey", "coverageSince", "observationSnapshot")) assertFalse(bytes.contains(field))
    }
}
