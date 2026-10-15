package com.mashudsck.dealghorithm

import android.app.AppOpsManager
import android.app.KeyguardManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import java.security.MessageDigest
import java.security.SecureRandom

// This user-triggered diagnostic adapter never runs a background loop or writes behavioral data.
data class ObservationTrialState(val usageAccess: Boolean = false, val installed: Boolean = false,
    val running: Boolean = false, val message: String = "Monitoring stopped", val snapshot: ObservationSnapshot? = null)

class UsageObserver(private val context: Context) {
    private val stats = context.getSystemService(UsageStatsManager::class.java)
    private val ops = context.getSystemService(AppOpsManager::class.java)
    private val power = context.getSystemService(PowerManager::class.java)
    private val keyguard = context.getSystemService(KeyguardManager::class.java)
    private var reducer: UsageReducer? = null
    private var selected: String? = null
    private var generation: String? = null
    private var consentWall = 0L
    private var cursorWall = 0L
    private var wallAnchor = 0L
    private var elapsedAnchor = 0L
    private var salt = ByteArray(0)
    private var snapshot: ObservationSnapshot? = null
    private var stoppedReason = "Trial stopped. Start a fresh observation window."
    internal var queryCount = 0
        private set

    @Suppress("DEPRECATION")
    fun usageAccess(): Boolean = try {
        ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName) == AppOpsManager.MODE_ALLOWED
    } catch (_: SecurityException) { false }

    @Suppress("DEPRECATION")
    fun installed(pkg: String?): Boolean = pkg in trialPackages && try {
        context.packageManager.getApplicationInfo(requireNotNull(pkg), 0).enabled
    } catch (_: PackageManager.NameNotFoundException) { false } catch (_: SecurityException) { false }

    fun stop(reason: String = "Trial stopped. Start a fresh observation window.") {
        reducer = null; selected = null; generation = null; snapshot = null; salt = ByteArray(0)
        consentWall = 0; cursorWall = 0; wallAnchor = 0; elapsedAnchor = 0; stoppedReason = reason
    }

    fun status(preferences: Preferences, currentGeneration: String): ObservationTrialState {
        val allowed = usageAccess()
        val present = installed(preferences.selectedPackage)
        if (!preferences.monitoringOptIn || !allowed || !present ||
            (reducer != null && (generation != currentGeneration || selected != preferences.selectedPackage))) stop()
        val message = when {
            !preferences.monitoringOptIn -> "Monitoring stopped"
            !allowed -> "Usage Access needed"
            !present -> "Selected trial app is not installed"
            reducer == null -> stoppedReason
            snapshot == null -> "Initializing observation"
            snapshot!!.focus == ObservedApp.UNKNOWN -> "Observation uncertain"
            !snapshot!!.complete -> "Incomplete ten-minute observation window"
            else -> "Trial window complete; automatic support unverified"
        }
        return ObservationTrialState(allowed, present, reducer != null, message, snapshot)
    }

    fun start(preferences: Preferences, currentGeneration: String): ObservationTrialState {
        stop()
        val capability = status(preferences, currentGeneration)
        if (!preferences.monitoringOptIn || !capability.usageAccess || !capability.installed) return capability
        if (!power.isInteractive || keyguard.isKeyguardLocked) {
            stop("Device locked or screen off. Start a fresh trial when unlocked.")
            return status(preferences, currentGeneration)
        }
        val now = System.currentTimeMillis()
        consentWall = now; cursorWall = now; wallAnchor = now; elapsedAnchor = SystemClock.elapsedRealtime()
        selected = preferences.selectedPackage; generation = currentGeneration
        salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
        // No pre-consent baseline is imported. A fresh lock/screen boundary must establish it.
        reducer = UsageReducer(now, interactive = true, locked = false)
        return status(preferences, currentGeneration)
    }

    fun read(preferences: Preferences, currentGeneration: String): ObservationTrialState {
        val capability = status(preferences, currentGeneration)
        val current = reducer ?: return capability
        val wall = System.currentTimeMillis()
        val elapsed = SystemClock.elapsedRealtime()
        if (!power.isInteractive || keyguard.isKeyguardLocked ||
            !SessionRules.clockStable(elapsedAnchor, wallAnchor, 0, elapsed, wall, 0)) {
            stop("Clock, lock or screen state changed. Start a fresh trial.")
            return status(preferences, currentGeneration)
        }
        // ponytail: bound diagnostic queries to 60 seconds/2,000 events; widen only after device evidence.
        // The physical probe found events 392ms beyond the old two-second cursor.
        // A five-second settling tail is a trial margin, not a delivery guarantee.
        val until = wall - 5_000
        if (until <= cursorWall) return capability
        if (until - cursorWall > 60_000) {
            stop("Observation gap exceeded one minute. Start a fresh trial.")
            return status(preferences, currentGeneration)
        }
        try {
            queryCount++
            val events = stats.queryEvents(maxOf(consentWall, cursorWall - UsageReducer.OVERLAP), until)
            if (events == null) {
                stop("Usage events unavailable. Start a fresh trial.")
                return status(preferences, currentGeneration)
            }
            val reduced = mutableListOf<UsageTransition>()
            val event = UsageEvents.Event()
            var count = 0
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (++count > 2_000) {
                    stop("Observation query exceeded the trial limit. Start a fresh trial.")
                    return status(preferences, currentGeneration)
                }
                classify(event)?.let(reduced::add)
            }
            // Recheck grant and identity before publishing even transient results.
            if (!usageAccess() || !preferences.monitoringOptIn || generation != currentGeneration) {
                stop(); return status(preferences, currentGeneration)
            }
            snapshot = current.reduce(until, reduced)
            cursorWall = until; wallAnchor = wall; elapsedAnchor = elapsed
        } catch (_: Exception) { stop("Usage events could not be read. Start a fresh trial.") }
        return status(preferences, currentGeneration)
    }

    // Internal bench access reuses the exact classifier and post-start bounds; nothing is persisted.
    internal val trialStart: Long get() = consentWall
    internal val observedUntil: Long get() = cursorWall

    internal fun classify(event: UsageEvents.Event): UsageTransition? {
        val change = when (event.eventType) {
            UsageEvents.Event.ACTIVITY_RESUMED -> UsageChange.RESUMED
            UsageEvents.Event.ACTIVITY_PAUSED -> UsageChange.PAUSED
            UsageEvents.Event.SCREEN_INTERACTIVE -> UsageChange.SCREEN_ON
            UsageEvents.Event.SCREEN_NON_INTERACTIVE -> UsageChange.SCREEN_OFF
            UsageEvents.Event.KEYGUARD_SHOWN -> UsageChange.KEYGUARD_SHOWN
            UsageEvents.Event.KEYGUARD_HIDDEN -> UsageChange.KEYGUARD_HIDDEN
            UsageEvents.Event.DEVICE_STARTUP, UsageEvents.Event.DEVICE_SHUTDOWN -> UsageChange.GAP
            else -> return null
        }
        if (change != UsageChange.RESUMED && change != UsageChange.PAUSED) return UsageTransition(event.timeStamp, change)
        val pkg = event.packageName ?: return UsageTransition(event.timeStamp, UsageChange.GAP)
        val activity = event.className ?: return UsageTransition(event.timeStamp, UsageChange.GAP)
        val app = when (pkg) { selected -> ObservedApp.SELECTED; context.packageName -> ObservedApp.DEALGORITHM; else -> ObservedApp.OTHER }
        return UsageTransition(event.timeStamp, change, app, token(pkg), token("$pkg\u0000$activity"))
    }

    private fun token(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        return digest.digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}
