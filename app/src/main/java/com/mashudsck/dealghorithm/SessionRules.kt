package com.mashudsck.dealghorithm

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.math.abs

object SessionRules {
    const val FIVE_MINUTES = 300_000L
    const val COOLDOWN = 120_000L

    fun start(state: AppState, minutes: Int, purpose: Purpose): AppState {
        require(minutes in listOf(5, 15, 30))
        return state.copy(intent = IntentCheckpoint(UUID.randomUUID().toString(), SessionMode.MANUAL,
            null, purpose, minutes * 60_000L, IntentStatus.RUNNING, state.generation),
            reminder = state.reminder.copy(sessionId = null, generation = null, unresolved = null))
    }

    fun pause(state: AppState): AppState {
        val intent = state.intent ?: return state
        if (intent.status != IntentStatus.RUNNING) return state
        return state.copy(intent = intent.copy(status = IntentStatus.PAUSED, revision = intent.revision + 1))
    }

    fun resume(state: AppState): AppState {
        val intent = state.intent ?: return state
        if (intent.status != IntentStatus.PAUSED || intent.remainingMillis == 0L || intent.mode != SessionMode.MANUAL) return state
        return state.copy(intent = intent.copy(status = IntentStatus.RUNNING, revision = intent.revision + 1))
    }

    fun extend(state: AppState): AppState {
        val intent = state.intent ?: return state
        if (intent.status == IntentStatus.FINISHED || intent.mode != SessionMode.MANUAL) return state
        return state.copy(intent = intent.copy(remainingMillis = Math.addExact(intent.remainingMillis, FIVE_MINUTES),
            status = IntentStatus.RUNNING, revision = intent.revision + 1),
            reminder = state.reminder.copy(unresolved = null, suppression = SuppressionKind.CONTINUE,
                suppressionRemainingMillis = FIVE_MINUTES))
    }

    fun finish(state: AppState, takeBreak: Boolean, wallMillis: Long, zone: ZoneId): AppState {
        val intent = state.intent
        val next = state.copy(intent = intent?.copy(status = IntentStatus.FINISHED, revision = intent.revision + 1),
            reminder = state.reminder.copy(unresolved = null,
                suppression = if (takeBreak) SuppressionKind.BREAK else state.reminder.suppression,
                suppressionRemainingMillis = if (takeBreak) FIVE_MINUTES else state.reminder.suppressionRemainingMillis))
        return if (takeBreak) next.changeDay(Instant.ofEpochMilli(wallMillis).atZone(zone).toLocalDate()) {
            it.copy(breaksChosen = Math.addExact(it.breaksChosen, 1))
        } else next
    }

    fun advance(state: AppState, elapsedMillis: Long, startWallMillis: Long, zone: ZoneId): AppState {
        require(elapsedMillis >= 0)
        val intent = state.intent
        val consumed = if (intent?.status == IntentStatus.RUNNING && intent.mode == SessionMode.MANUAL)
            minOf(elapsedMillis, intent.remainingMillis) else 0L
        val suppressionConsumed = when (state.reminder.suppression) {
            SuppressionKind.BREAK -> elapsedMillis
            SuppressionKind.CONTINUE -> consumed
            SuppressionKind.NONE -> 0L
        }
        val suppressionRemaining = (state.reminder.suppressionRemainingMillis - suppressionConsumed).coerceAtLeast(0)
        val remaining = intent?.remainingMillis?.minus(consumed)
        val next = state.copy(intent = intent?.copy(remainingMillis = remaining!!,
            status = if (intent.status == IntentStatus.RUNNING && remaining == 0L) IntentStatus.EXPIRED else intent.status),
            reminder = state.reminder.copy(suppressionRemainingMillis = suppressionRemaining,
                suppression = if (suppressionRemaining == 0L) SuppressionKind.NONE else state.reminder.suppression))
        return next.addManualInterval(startWallMillis, consumed, zone)
    }

    fun canRequestBudget(state: AppState, elapsedMillis: Long, notificationAllowed: Boolean): Boolean {
        val intent = state.intent ?: return false
        val reminder = state.reminder
        return intent.mode == SessionMode.MANUAL && intent.status == IntentStatus.EXPIRED && state.preferences.remindersEnabled && notificationAllowed &&
            reminder.unresolved == null && reminder.suppressionRemainingMillis == 0L &&
            !(reminder.sessionId == intent.id && reminder.requestRevision == intent.revision) &&
            (reminder.lastRequestElapsed == null || elapsedMillis - reminder.lastRequestElapsed >= COOLDOWN)
    }

    fun restore(state: AppState, elapsedMillis: Long, wallMillis: Long, bootCount: Int): AppState = state.copy(
        intent = state.intent?.let { if (it.status == IntentStatus.RUNNING) it.copy(status = IntentStatus.PAUSED,
            revision = it.revision + 1) else it },
        reminder = state.reminder.copy(unresolved = null,
            lastRequestElapsed = state.reminder.lastRequestElapsed?.let { elapsedMillis },
            lastRequestWall = state.reminder.lastRequestWall?.let { wallMillis }, bootCount = bootCount),
    )

    fun clockStable(beforeElapsed: Long, beforeWall: Long, beforeBoot: Int,
                    nowElapsed: Long, nowWall: Long, nowBoot: Int): Boolean =
        nowElapsed >= beforeElapsed && beforeBoot == nowBoot &&
            abs((nowWall - beforeWall) - (nowElapsed - beforeElapsed)) <= 5_000
}

fun AppState.changeDay(today: LocalDate, transform: (DailySummary) -> DailySummary): AppState {
    val retained = retained(today)
    val current = retained.summaries.find { it.date == today } ?: DailySummary(today)
    return retained.copy(summaries = (retained.summaries.filterNot { it.date == today } + transform(current)).sortedBy { it.date })
}
