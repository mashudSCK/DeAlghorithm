package com.mashudsck.dealghorithm

import android.app.Application
import android.os.SystemClock
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class DeAlghorithmApp : Application() {
    val runtime by lazy { AppRuntime(this) }
}

data class UiState(
    val state: AppState? = null,
    val ready: Boolean = false,
    val error: String? = null,
    val notificationsAllowed: Boolean = false,
    val otherPurpose: String = "",
    val page: String = "timer",
    val activityMessage: String? = null,
    val observation: ObservationTrialState = ObservationTrialState(),
)

class AppRuntime(private val app: Application) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    internal val store = StateStore(app, scope)
    private val scheduler = ReminderScheduler(app)
    internal val observer = UsageObserver(app)
    private val mutex = Mutex()
    private val initialized = CompletableDeferred<Unit>()
    private val mutableUi = MutableStateFlow(UiState())
    val ui = mutableUi.asStateFlow()
    private var elapsedAnchor = SystemClock.elapsedRealtime()
    private var wallAnchor = System.currentTimeMillis()
    private var bootAnchor = -1
    private var validGeneration: String? = null

    init {
        scope.launch {
            mutex.withLock {
                try {
                    val previous = store.read()
                    scheduler.cancelAll(previous.intent)
                    val restored = store.update { SessionRules.restore(it, SystemClock.elapsedRealtime(), System.currentTimeMillis(), bootCount()) }
                    anchors()
                    publish(restored)
                    publishObservation(observer.status(restored.preferences, restored.generation))
                } catch (_: Exception) { failed() }
                finally { initialized.complete(Unit) }
            }
        }
        scope.launch {
            initialized.await()
            while (isActive) {
                delay(1_000)
                mutex.withLock {
                    val state = ui.value.state
                    if (state?.intent?.status == IntentStatus.RUNNING || state?.reminder?.unresolved != null ||
                        (state?.reminder?.suppressionRemainingMillis ?: 0) > 0) {
                        try { tickLocked() } catch (_: Exception) { failed() }
                    }
                }
            }
        }
    }

    private fun bootCount() = Settings.Global.getInt(app.contentResolver, "boot_count", -1)
    private fun anchors() { elapsedAnchor = SystemClock.elapsedRealtime(); wallAnchor = System.currentTimeMillis(); bootAnchor = bootCount() }
    private fun publish(state: AppState) {
        validGeneration = state.generation
        mutableUi.update { it.copy(state = state, ready = true, error = null, notificationsAllowed = scheduler.allowed()) }
    }
    private fun failed() {
        observer.stop()
        scheduler.cancelAll(ui.value.state?.intent)
        mutableUi.update { it.copy(ready = true, error = "Local data could not be read. Monitoring is stopped.", otherPurpose = "", observation = ObservationTrialState()) }
    }
    private suspend fun save(transform: (AppState) -> AppState): AppState = store.update(transform).also(::publish)

    private suspend fun tickLocked() {
        if (ui.value.error != null) return
        val current = store.read()
        val elapsed = SystemClock.elapsedRealtime()
        val wall = System.currentTimeMillis()
        val boot = bootCount()
        if (!SessionRules.clockStable(elapsedAnchor, wallAnchor, bootAnchor, elapsed, wall, boot)) {
            observer.stop("Clock changed. Start a fresh observation trial.")
            publishObservation(ObservationTrialState(message = "Clock changed. Start a fresh observation trial."))
            scheduler.cancelAll(current.intent)
            save { SessionRules.restore(it, elapsed, wall, boot) }
            anchors()
            return
        }
        val previousWall = wallAnchor
        val duration = elapsed - elapsedAnchor
        elapsedAnchor = elapsed; wallAnchor = wall; bootAnchor = boot
        val next = save { SessionRules.advance(it, duration, previousWall, ZoneId.systemDefault()) }
        if (next.intent?.status == IntentStatus.EXPIRED) scheduler.cancelTimer(next.intent)
        if (!scheduler.allowed() && next.reminder.unresolved != null) {
            scheduler.cancelAll(next.intent)
            save { it.copy(reminder = it.reminder.copy(unresolved = null)) }
            return
        }
        if (SessionRules.canRequestBudget(next, elapsed, scheduler.allowed()) && scheduler.postBudget(next.intent!!)) {
            save { state ->
                state.copy(reminder = state.reminder.copy(sessionId = next.intent.id, generation = state.generation,
                    unresolved = ReminderKind.BUDGET, lastRequestElapsed = elapsed, lastRequestWall = wall,
                    bootCount = boot, requestRevision = next.intent.revision))
                    .changeDay(LocalDate.now()) { it.copy(reminderRequests = Math.addExact(it.reminderRequests, 1)) }
            }
        }
    }

    private fun action(generation: String? = null, block: suspend () -> Unit): Job = scope.launch {
        initialized.await()
        mutex.withLock {
            if (ui.value.error != null) return@withLock
            if (generation != null && generation != ui.value.state?.generation) return@withLock
            try { tickLocked(); block() } catch (_: Exception) { failed() }
        }
    }

    private fun publishObservation(trial: ObservationTrialState) { mutableUi.update { it.copy(observation = trial) } }
    fun refresh(): Job = action {
        mutableUi.update { it.copy(notificationsAllowed = scheduler.allowed()) }
        val state = ui.value.state!!
        publishObservation(observer.status(state.preferences, state.generation))
    }
    fun enableObservationTrial(pkg: String, generation: String? = ui.value.state?.generation): Job = action(generation) {
        if (generation == null || pkg !in trialPackages) return@action
        observer.stop()
        val state = save { it.copy(preferences = it.preferences.copy(monitoringOptIn = true, selectedPackage = pkg)) }
        publishObservation(observer.start(state.preferences, state.generation))
    }
    fun startObservationTrial(generation: String? = ui.value.state?.generation): Job = action(generation) {
        if (generation == null) return@action
        val state = ui.value.state!!
        publishObservation(observer.start(state.preferences, state.generation))
    }
    fun checkObservation(generation: String? = ui.value.state?.generation): Job = action(generation) {
        if (generation == null) return@action
        val state = ui.value.state!!
        publishObservation(observer.read(state.preferences, state.generation))
    }
    fun navigate(page: String) { mutableUi.update { it.copy(page = page) }; refresh() }
    fun purposeText(text: String, generation: String? = ui.value.state?.generation) {
        mutableUi.update { if (!it.ready || it.error != null || it.state?.generation != generation) it else it.copy(otherPurpose = text) }
    }

    fun start(minutes: Int, purpose: Purpose): Job = action {
        scheduler.cancelAll(ui.value.state?.intent)
        val state = save { SessionRules.start(it, minutes, purpose) }
        mutableUi.update { it.copy(otherPurpose = if (purpose == Purpose.OTHER) it.otherPurpose else "") }
        anchors()
        if (state.preferences.remindersEnabled) scheduler.schedule(state.intent)
    }
    fun pause(): Job = action {
        scheduler.cancelAll(ui.value.state?.intent)
        save { SessionRules.pause(it) }
        anchors()
    }
    fun resume(): Job = action {
        val state = save { SessionRules.resume(it) }
        anchors()
        if (state.preferences.remindersEnabled) scheduler.schedule(state.intent)
    }
    fun extend(): Job = action { extendLocked() }
    private suspend fun extendLocked() {
        scheduler.cancelAll(ui.value.state?.intent)
        val state = save { SessionRules.extend(it) }
        anchors()
        if (state.preferences.remindersEnabled) scheduler.schedule(state.intent)
    }
    fun finish(takeBreak: Boolean): Job = action { finishLocked(takeBreak) }
    private suspend fun finishLocked(takeBreak: Boolean) {
        scheduler.cancelAll(ui.value.state?.intent)
        save { SessionRules.finish(it, takeBreak, System.currentTimeMillis(), ZoneId.systemDefault()) }
        mutableUi.update { it.copy(otherPurpose = "") }
        anchors()
    }
    fun reminders(enabled: Boolean): Job = action {
        scheduler.cancelAll(ui.value.state?.intent)
        val state = save { it.copy(preferences = it.preferences.copy(remindersEnabled = enabled),
            reminder = it.reminder.copy(unresolved = null)) }
        if (enabled) scheduler.schedule(state.intent)
    }

    fun addActivity(name: String, generation: String? = ui.value.state?.generation): Job = action(generation) {
        if (generation == null) return@action
        val trimmed = name.trim()
        when {
            trimmed.length !in 1..80 -> activityMessage("Use a name with 1–80 characters.")
            ui.value.state!!.activities.size >= 10 -> activityMessage("You can save up to 10 activities.")
            else -> {
                save { it.copy(activities = it.activities + Activity(UUID.randomUUID().toString(), trimmed)) }
                activityMessage(null)
            }
        }
    }
    fun renameActivity(id: String, name: String, generation: String? = ui.value.state?.generation): Job = action(generation) {
        if (generation == null) return@action
        val trimmed = name.trim()
        if (trimmed.length !in 1..80) { activityMessage("Use a name with 1–80 characters."); return@action }
        save { it.copy(activities = it.activities.map { activity -> if (activity.id == id) activity.copy(name = trimmed) else activity }) }
        activityMessage(null)
    }
    fun deleteActivity(id: String, generation: String? = ui.value.state?.generation): Job = action(generation) {
        if (generation == null) return@action
        save { it.copy(activities = it.activities.filterNot { activity -> activity.id == id }) }
        activityMessage(null)
    }
    fun moveActivity(id: String, direction: Int, generation: String? = ui.value.state?.generation): Job = action(generation) {
        if (generation == null || direction !in listOf(-1, 1)) return@action
        save { state ->
            val order = state.activities.toMutableList()
            val index = order.indexOfFirst { it.id == id }
            val destination = index + direction
            if (index >= 0 && destination in order.indices) order.add(destination, order.removeAt(index))
            state.copy(activities = order)
        }
        activityMessage(null)
    }
    fun selectActivity(id: String, generation: String? = ui.value.state?.generation): Job = action(generation) {
        if (generation == null) return@action
        val activity = ui.value.state!!.activities.firstOrNull { it.id == id } ?: return@action
        save { it.changeDay(LocalDate.now()) { day -> day.copy(activitySelections = Math.addExact(day.activitySelections, 1)) } }
        activityMessage("Selected: ${activity.name}")
    }
    private fun activityMessage(message: String?) { mutableUi.update { it.copy(activityMessage = message) } }

    fun disableMonitoring(): Job = action {
        val current = ui.value.state!!
        val automatic = current.intent?.mode == SessionMode.AUTOMATIC
        val automaticReminder = current.reminder.unresolved != null && current.reminder.unresolved != ReminderKind.BUDGET
        if (automatic || automaticReminder) scheduler.cancelAll(current.intent)
        observer.stop()
        val next = save { state -> state.copy(preferences = state.preferences.copy(monitoringOptIn = false, mode = SessionMode.MANUAL),
            intent = if (automatic) state.intent?.let { it.copy(status = IntentStatus.PAUSED, revision = it.revision + 1) } else state.intent,
            reminder = if (automatic || automaticReminder) state.reminder.copy(unresolved = null) else state.reminder) }
        publishObservation(observer.status(next.preferences, next.generation))
        anchors()
    }

    fun callback(generation: String, sessionId: String, revision: Long, kind: String): Job = scope.launch {
        initialized.await()
        mutex.withLock {
            if (ui.value.error != null) return@withLock
            try {
                val current = store.read()
                val intent = current.intent ?: return@withLock
                if (generation != validGeneration || current.generation != generation || intent.id != sessionId || intent.revision != revision) return@withLock
                if (kind == "ALARM") {
                    if (intent.status == IntentStatus.RUNNING) tickLocked()
                    return@withLock
                }
                val reminder = current.reminder
                if (reminder.unresolved != ReminderKind.BUDGET || reminder.sessionId != sessionId || reminder.generation != generation ||
                    reminder.requestRevision != revision) return@withLock
                tickLocked()
                val activeRequest = ui.value.state?.reminder
                if (activeRequest?.unresolved != ReminderKind.BUDGET || activeRequest.requestRevision != revision ||
                    activeRequest.generation != generation || activeRequest.sessionId != sessionId) return@withLock
                when (kind) {
                    "ADD" -> extendLocked()
                    "FINISH" -> finishLocked(false)
                    "DISMISS" -> { scheduler.cancelAll(current.intent); save { it.copy(reminder = it.reminder.copy(unresolved = null)) } }
                }
            } catch (_: Exception) { failed() }
        }
    }

    fun deleteAllData(): Job = scope.launch {
        initialized.await()
        mutex.withLock {
            val oldIntent = ui.value.state?.intent
            try {
                val invalidation = UUID.randomUUID().toString()
                validGeneration = invalidation
                observer.stop()
                mutableUi.update { it.copy(state = null, ready = false, otherPurpose = "", activityMessage = null, observation = ObservationTrialState()) }
                if (ui.value.error == null) store.update { it.copy(generation = invalidation, intent = null,
                    reminder = ReminderCheckpoint(), preferences = it.preferences.copy(monitoringOptIn = false)) }
                scheduler.cancelAll(oldIntent)
                mutableUi.update { it.copy(otherPurpose = "") }
                publish(store.resetAfterCancellation())
                anchors()
            } catch (_: Exception) { scheduler.cancelAll(oldIntent); failed() }
        }
    }
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    val runtime = (application as DeAlghorithmApp).runtime
    val ui = runtime.ui
}
