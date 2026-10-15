package com.mashudsck.dealghorithm

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

class StateStore(context: Context, scope: CoroutineScope, private val today: () -> LocalDate = { LocalDate.now() }) {
    private val key = stringPreferencesKey("snapshot")
    val file = File(context.noBackupFilesDir, "state.preferences_pb")
    private val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })

    suspend fun read(): AppState = update { it }

    // Only the confirmed deletion path may bypass decoding a corrupt snapshot.
    suspend fun resetAfterCancellation(): AppState {
        val initial = AppState()
        store.edit { it.clear(); it[key] = encode(initial) }
        return initial
    }

    // DataStore serializes transformations against its current snapshot, never a caller's stale copy.
    suspend fun update(transform: (AppState) -> AppState): AppState {
        var result: AppState? = null
        store.edit { values ->
            val current = values[key]?.let(::decode) ?: AppState()
            val next = transform(current.retained(today())).retained(today())
            values[key] = encode(next)
            result = next
        }
        return checkNotNull(result)
    }

    private fun encode(state: AppState): String = JSONObject().apply {
        put("schemaVersion", state.schemaVersion)
        put("generation", state.generation)
        put("preferences", JSONObject().apply {
            put("monitoringOptIn", state.preferences.monitoringOptIn)
            put("selectedPackage", state.preferences.selectedPackage ?: JSONObject.NULL)
            put("remindersEnabled", state.preferences.remindersEnabled)
            put("mode", state.preferences.mode.name)
        })
        put("activities", JSONArray().apply { state.activities.forEach { activity ->
            put(JSONObject().put("id", activity.id).put("name", activity.name))
        } })
        put("intent", state.intent?.let { intent -> JSONObject().apply {
            put("id", intent.id); put("mode", intent.mode.name)
            put("selectedPackage", intent.selectedPackage ?: JSONObject.NULL)
            put("purpose", intent.purpose.name); put("remainingMillis", intent.remainingMillis)
            put("status", intent.status.name); put("generation", intent.generation); put("revision", intent.revision)
        } } ?: JSONObject.NULL)
        put("summaries", JSONArray().apply { state.summaries.forEach { day -> put(JSONObject().apply {
            put("date", day.date.toString()); put("measuredMillis", day.measuredMillis)
            put("manualMillis", day.manualMillis); put("reminderRequests", day.reminderRequests)
            put("breaksChosen", day.breaksChosen); put("activitySelections", day.activitySelections)
        }) } })
        put("reminder", JSONObject().apply {
            val reminder = state.reminder
            put("sessionId", reminder.sessionId ?: JSONObject.NULL)
            put("generation", reminder.generation ?: JSONObject.NULL)
            put("unresolved", reminder.unresolved?.name ?: JSONObject.NULL)
            put("lastRequestElapsed", reminder.lastRequestElapsed ?: JSONObject.NULL)
            put("lastRequestWall", reminder.lastRequestWall ?: JSONObject.NULL)
            put("bootCount", reminder.bootCount ?: JSONObject.NULL)
            put("suppression", reminder.suppression.name)
            put("suppressionRemainingMillis", reminder.suppressionRemainingMillis)
            put("requestRevision", reminder.requestRevision ?: JSONObject.NULL)
        })
    }.toString()

    private fun decode(text: String): AppState {
        // Do not install a corruption handler that silently replaces user records.
        val root = JSONObject(text)
        require(root.get("schemaVersion") == 1) { "Unsupported stored schema" }
        val preferences = root.getJSONObject("preferences")
        val reminder = root.getJSONObject("reminder")
        val intent = if (root.isNull("intent")) null else root.getJSONObject("intent")
        return AppState(
            schemaVersion = root.getInt("schemaVersion"), generation = root.getString("generation"),
            preferences = Preferences(preferences.getBoolean("monitoringOptIn"), preferences.optionalString("selectedPackage"),
                preferences.getBoolean("remindersEnabled"), SessionMode.valueOf(preferences.getString("mode"))),
            activities = root.getJSONArray("activities").objects().map { Activity(it.getString("id"), it.getString("name")) },
            intent = intent?.let { IntentCheckpoint(it.getString("id"), SessionMode.valueOf(it.getString("mode")),
                it.optionalString("selectedPackage"), Purpose.valueOf(it.getString("purpose")), it.getLong("remainingMillis"),
                IntentStatus.valueOf(it.getString("status")), it.getString("generation"), it.getLong("revision")) },
            summaries = root.getJSONArray("summaries").objects().map { DailySummary(LocalDate.parse(it.getString("date")),
                it.getLong("measuredMillis"), it.getLong("manualMillis"), it.getLong("reminderRequests"),
                it.getLong("breaksChosen"), it.getLong("activitySelections")) },
            reminder = ReminderCheckpoint(reminder.optionalString("sessionId"), reminder.optionalString("generation"),
                reminder.optionalString("unresolved")?.let(ReminderKind::valueOf), reminder.optionalLong("lastRequestElapsed"),
                reminder.optionalLong("lastRequestWall"), reminder.optionalLong("bootCount")?.toInt(),
                SuppressionKind.valueOf(reminder.getString("suppression")), reminder.getLong("suppressionRemainingMillis"),
                reminder.optionalLong("requestRevision")),
        )
    }

    private fun JSONObject.optionalString(name: String): String? = if (isNull(name)) null else getString(name)
    private fun JSONObject.optionalLong(name: String): Long? = if (isNull(name)) null else getLong(name)
    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map(::getJSONObject)
}
