package com.mashudsck.dealghorithm

import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

// These are trial targets, not claims of verified automatic support.
val trialPackages = setOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")

enum class SessionMode { MANUAL, AUTOMATIC }
enum class IntentStatus { RUNNING, PAUSED, FINISHED, EXPIRED }
enum class Purpose { FIND_SOMETHING, REPLY, ENTERTAINMENT, OTHER }
enum class ReminderKind { BUDGET, AWARENESS, INTENT_CHECK, REFLECTION, REPLACEMENT }
enum class SuppressionKind { NONE, CONTINUE, BREAK }

data class Preferences(
    val monitoringOptIn: Boolean = false,
    val selectedPackage: String? = null,
    val remindersEnabled: Boolean = true,
    val mode: SessionMode = SessionMode.MANUAL,
) {
    init { require(selectedPackage == null || selectedPackage in trialPackages) }
}

data class Activity(val id: String, val name: String) {
    init { require(id.isNotBlank() && name == name.trim() && name.length in 1..80) }
}

data class IntentCheckpoint(
    val id: String,
    val mode: SessionMode,
    val selectedPackage: String?,
    val purpose: Purpose,
    val remainingMillis: Long,
    val status: IntentStatus,
    val generation: String,
    val revision: Long = 0,
) {
    init {
        require(id.isNotBlank() && generation.isNotBlank() && remainingMillis >= 0 && revision >= 0)
        require(if (mode == SessionMode.MANUAL) selectedPackage == null else selectedPackage in trialPackages)
    }
}

data class DailySummary(
    val date: LocalDate,
    val measuredMillis: Long = 0,
    val manualMillis: Long = 0,
    val reminderRequests: Long = 0,
    val breaksChosen: Long = 0,
    val activitySelections: Long = 0,
) {
    init { require(listOf(measuredMillis, manualMillis, reminderRequests, breaksChosen, activitySelections).all { it >= 0 }) }
}

data class ReminderCheckpoint(
    val sessionId: String? = null,
    val generation: String? = null,
    val unresolved: ReminderKind? = null,
    val lastRequestElapsed: Long? = null,
    val lastRequestWall: Long? = null,
    val bootCount: Int? = null,
    val suppression: SuppressionKind = SuppressionKind.NONE,
    val suppressionRemainingMillis: Long = 0,
    val requestRevision: Long? = null,
) {
    init {
        require(suppressionRemainingMillis >= 0)
        require(lastRequestElapsed == null || lastRequestElapsed >= 0)
        require(requestRevision == null || requestRevision >= 0)
        require(unresolved == null || (sessionId != null && generation != null))
    }
}

data class AppState(
    val schemaVersion: Int = 1,
    val generation: String = UUID.randomUUID().toString(),
    val preferences: Preferences = Preferences(),
    val activities: List<Activity> = emptyList(),
    val intent: IntentCheckpoint? = null,
    val summaries: List<DailySummary> = emptyList(),
    val reminder: ReminderCheckpoint = ReminderCheckpoint(),
) {
    init {
        require(schemaVersion == 1) { "Unsupported stored schema" }
        require(generation.isNotBlank())
        require(activities.size <= 10 && activities.map { it.id }.distinct().size == activities.size)
        require(summaries.size <= 7 && summaries.map { it.date }.distinct().size == summaries.size)
        require(intent == null || intent.generation == generation)
        require(reminder.generation == null || reminder.generation == generation)
    }

    fun retained(today: LocalDate): AppState = copy(
        summaries = summaries.filter { it.date >= today.minusDays(6) && it.date <= today }.sortedBy { it.date },
    )
}

// Purpose text, observed transitions and scores intentionally have no durable fields.
data class IntentRuntime(val elapsedAnchor: Long, val wallAnchor: Long, val otherPurpose: String? = null)
data class CapabilityState(
    val usageAccess: Boolean = false,
    val notifications: Boolean = false,
    val observationComplete: Boolean = false,
    val automaticSupported: Boolean = false,
)

// Call inside a transaction with a consumed interval, never a repeated runtime snapshot.
// Half-open intervals assign each millisecond to exactly one local calendar date.
fun AppState.addManualInterval(startWallMillis: Long, durationMillis: Long, zone: ZoneId): AppState {
    require(durationMillis >= 0)
    if (durationMillis == 0L) return this
    val end = Math.addExact(startWallMillis, durationMillis)
    val lastDate = Instant.ofEpochMilli(end).atZone(zone).toLocalDate()
    val days = summaries.associateBy { it.date }.toMutableMap()
    var cursor = startWallMillis
    while (cursor < end) {
        val date = Instant.ofEpochMilli(cursor).atZone(zone).toLocalDate()
        val midnight = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val intervalEnd = minOf(end, midnight)
        val previous = days[date] ?: DailySummary(date)
        days[date] = previous.copy(manualMillis = Math.addExact(previous.manualMillis, intervalEnd - cursor))
        cursor = intervalEnd
    }
    return copy(summaries = days.values.filter { it.date >= lastDate.minusDays(6) && it.date <= lastDate }.sortedBy { it.date })
}
