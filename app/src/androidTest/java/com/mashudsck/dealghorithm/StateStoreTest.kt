package com.mashudsck.dealghorithm

import androidx.test.platform.app.InstrumentationRegistry
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.io.File
import android.content.ContextWrapper
import java.util.UUID

class StateStoreTest {
    private fun fixtureContext(): ContextWrapper {
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(target.noBackupFilesDir, "storage-test-${UUID.randomUUID()}")
        return object : ContextWrapper(target) {
            override fun getNoBackupFilesDir(): File = directory.apply { mkdirs() }
        }
    }
    @Test fun transactionsRetainSevenDatesAndSeparateTotals(): Unit = runBlocking {
        val context = fixtureContext()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val today = LocalDate.of(2026, 10, 9)
        val store = StateStore(context, scope) { today }
        try {
            store.update { AppState() }
            assertEquals(context.noBackupFilesDir.canonicalFile, requireNotNull(store.file.parentFile).canonicalFile)
            store.update { it.copy(activities = listOf(Activity("stable", "Read two pages")),
                summaries = listOf(DailySummary(today.minusDays(7), manualMillis = 900),
                    DailySummary(today.minusDays(6), measuredMillis = 50), DailySummary(today, manualMillis = 100))) }
            val restored = store.read()
            assertEquals(listOf(today.minusDays(6), today), restored.summaries.map { it.date })
            assertEquals(0L, restored.summaries.last().measuredMillis)
            assertEquals(100L, restored.summaries.last().manualMillis)
            assertEquals("stable", restored.activities.single().id)
            try { store.update { it.copy(activities = (0..10).map { n -> Activity(n.toString(), "Name") }) }; fail("Limit accepted") }
            catch (_: IllegalArgumentException) { }
            assertEquals(1, store.read().activities.size)
            store.update { AppState() }
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin(); context.noBackupFilesDir.deleteRecursively() }
    }

    @Test fun unsupportedAndCorruptSnapshotsArePreserved(): Unit = runBlocking {
        val context = fixtureContext()
        val file = File(context.noBackupFilesDir, "state.preferences_pb")
        val key = stringPreferencesKey("snapshot")
        suspend fun writeRaw(text: String?) {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val rawStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
            try { rawStore.edit { if (text == null) it.remove(key) else it[key] = text } }
            finally { scope.coroutineContext[Job]!!.cancelAndJoin() }
        }
        try {
            for (invalid in listOf("{\"schemaVersion\":2}", "not valid JSON")) {
                writeRaw(invalid)
                val before = file.readBytes()
                val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
                try {
                    try { StateStore(context, scope).read(); fail("Corrupt snapshot accepted") }
                    catch (error: Exception) { assertTrue(error is IllegalArgumentException || error is org.json.JSONException) }
                } finally { scope.coroutineContext[Job]!!.cancelAndJoin() }
                assertArrayEquals(before, file.readBytes())
            }
        } finally { writeRaw(null); context.noBackupFilesDir.deleteRecursively() }
    }

    @Test fun activityBoundsOrderingAndCalendarChangesAreTransactional(): Unit = runBlocking {
        val context = fixtureContext()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        var today = LocalDate.of(2026, 10, 9)
        val store = StateStore(context, scope) { today }
        try {
            val activities = (1..10).map { Activity("id-$it", if (it == 10) "x".repeat(80) else "Activity $it") }
            store.update { it.copy(activities = activities, summaries = (0..6).map { offset ->
                DailySummary(today.minusDays(offset.toLong()), measuredMillis = 120_000, manualMillis = 60_000,
                    reminderRequests = 2, breaksChosen = 3, activitySelections = 4)
            }) }
            for (name in listOf("", "   ", " untrimmed ", "x".repeat(81))) {
                try { store.update { it.copy(activities = listOf(Activity("invalid", name))) }; fail("Invalid name accepted") }
                catch (_: IllegalArgumentException) { }
                assertEquals(activities, store.read().activities)
            }
            try { store.update { it.copy(activities = it.activities + Activity("eleven", "Extra")) }; fail("Eleventh activity accepted") }
            catch (_: IllegalArgumentException) { }
            store.update { it.copy(activities = it.activities.reversed().map { activity ->
                if (activity.id == "id-1") activity.copy(name = "Read two pages") else activity
            }) }
            assertEquals(activities.map { it.id }.reversed(), store.read().activities.map { it.id })
            assertEquals("Read two pages", store.read().activities.last().name)
            today = today.plusDays(1)
            val advanced = store.read()
            assertEquals(6, advanced.summaries.size)
            assertTrue(advanced.summaries.all { it.measuredMillis == 120_000L && it.manualMillis == 60_000L })
            today = today.minusDays(2)
            assertTrue(store.read().summaries.all { it.date <= today })
            val restored = store.read()
            assertEquals(5, restored.summaries.size)
            assertTrue(restored.summaries.all { it.reminderRequests == 2L && it.breaksChosen == 3L && it.activitySelections == 4L })
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin(); context.noBackupFilesDir.deleteRecursively() }
    }
}
