package com.mashudsck.dealghorithm

enum class ObservedApp { SELECTED, DEALGORITHM, OTHER, UNKNOWN }
enum class ObservationIssue { BASELINE_MISSING, LOCKED, NO_RESUMED_ACTIVITY, MULTIPLE_RESUMED_APPS, MISSING_EVENT, LATE_EVENT, INVALID_IDENTITY, UNMATCHED_PAUSE }
enum class UsageChange { RESUMED, PAUSED, SCREEN_ON, SCREEN_OFF, KEYGUARD_HIDDEN, KEYGUARD_SHOWN, GAP }
// Keys are ephemeral salted digests in the native adapter, never unrelated package/class names.
data class UsageTransition(val at: Long, val change: UsageChange, val app: ObservedApp = ObservedApp.UNKNOWN,
    val packageKey: String? = null, val activityKey: String? = null)
data class ObservationSnapshot(val selectedMillis: Long, val durationMillis: Long, val qualifyingReturns: Int,
    val complete: Boolean, val focus: ObservedApp, val issue: ObservationIssue? = null,
    val lateEventCount: Int = 0, val maxLatenessMillis: Long = 0) {
    val repeatOpenings: Int? get() = if (complete) qualifyingReturns else null
}

class UsageReducer(private val start: Long, baselineKnown: Boolean = false, interactive: Boolean = true, locked: Boolean = false) {
    private var cursor = start
    private var known = baselineKnown
    private var baselineIssue = ObservationIssue.BASELINE_MISSING
    private var awaitingPostLockResume = false
    private var lateEventCount = 0
    private var maxLatenessMillis = 0L
    private var screenOn = interactive
    private var keyguard = locked
    private val resumed = mutableMapOf<String, Pair<ObservedApp, MutableSet<String>>>()
    private val seen = mutableSetOf<UsageTransition>()
    private val returns = mutableListOf<Long>()
    private var coverageSince: Long? = null
    private var selectedSeen = false
    private var outsideSince: Long? = null
    private var otherMillis = 0L
    private var lastCertainApp = ObservedApp.UNKNOWN
    private var returnEligible = false
    private var selectedMillis = 0L
    private var durationMillis = 0L

    private fun focus(): ObservedApp = if (!known || !screenOn || keyguard || resumed.size != 1) ObservedApp.UNKNOWN
        else resumed.values.single().first

    private fun invalidate(issue: ObservationIssue = ObservationIssue.MISSING_EVENT) {
        baselineIssue = issue
        awaitingPostLockResume = false
        known = false; resumed.clear(); coverageSince = null
        durationMillis = 0; outsideSince = null; otherMillis = 0; returnEligible = false
        selectedSeen = false; returns.clear(); lastCertainApp = ObservedApp.UNKNOWN
    }

    // A complete lifecycle set with no resumed activities proves absence, never app focus.
    private fun idle() = known && screenOn && !keyguard && resumed.isEmpty()

    private fun outside(to: Long) {
        val absence = outsideSince?.let { to - it } ?: 0
        if (absence >= 120_000) durationMillis = 0
        // Ten minutes away ends the session; continuous selected use can still reach T=20/R=0.
        if (absence >= WINDOW) { selectedSeen = false; returns.clear(); coverageSince = null }
    }

    private fun advance(to: Long) {
        val elapsed = to - cursor
        when (focus()) {
            ObservedApp.SELECTED -> { selectedMillis += elapsed; durationMillis += elapsed }
            ObservedApp.OTHER -> { otherMillis += elapsed; outside(to) }
            ObservedApp.DEALGORITHM -> outside(to)
            ObservedApp.UNKNOWN -> if (idle()) outside(to) else if (elapsed > 0) {
                coverageSince = null; durationMillis = 0; returnEligible = false
            }
        }
        cursor = to
        returns.removeAll { it <= cursor - WINDOW }
    }

    private fun apply(event: UsageTransition) {
        when (event.change) {
            UsageChange.SCREEN_OFF, UsageChange.KEYGUARD_SHOWN -> {
                if (event.change == UsageChange.SCREEN_OFF) screenOn = false else keyguard = true
                resumed.clear(); known = true; awaitingPostLockResume = true; coverageSince = null; returnEligible = false; durationMillis = 0
            }
            UsageChange.SCREEN_ON -> screenOn = true
            UsageChange.KEYGUARD_HIDDEN -> keyguard = false
            UsageChange.GAP -> invalidate()
            UsageChange.RESUMED, UsageChange.PAUSED -> {
                if (!screenOn || keyguard) return
                val pkg = event.packageKey
                val activity = event.activityKey
                if (pkg == null || activity == null || event.app == ObservedApp.UNKNOWN) { invalidate(ObservationIssue.INVALID_IDENTITY); return }
                if (event.change == UsageChange.RESUMED) {
                    val current = resumed.getOrPut(pkg) { event.app to mutableSetOf() }
                    if (current.first != event.app || resumed.size > 32) { invalidate(ObservationIssue.INVALID_IDENTITY); return }
                    current.second.add(activity)
                    awaitingPostLockResume = false
                } else {
                    val current = resumed[pkg]
                    if (current == null || !current.second.remove(activity)) {
                        // A pre-consent activity can pause after unlocking without a new resume event.
                        // Only the empty, proven post-lock baseline accepts this cleanup; no time is counted.
                        if (known && !(awaitingPostLockResume && resumed.isEmpty())) invalidate(ObservationIssue.UNMATCHED_PAUSE)
                        return
                    }
                    if (current.second.isEmpty()) resumed.remove(pkg)
                }
            }
        }
    }

    fun reduce(until: Long, events: List<UsageTransition>, queryAvailable: Boolean = true): ObservationSnapshot {
        require(until >= cursor)
        val fresh = events.filter { it.at >= start && it.at < until && it !in seen }.distinct()
        if (!queryAvailable) {
            invalidate(); cursor = until
            return snapshot()
        }
        val late = fresh.filter { it.at < cursor }
        if (late.isNotEmpty()) {
            lateEventCount += late.size
            maxLatenessMillis = maxOf(maxLatenessMillis, late.maxOf { cursor - it.at })
            invalidate(ObservationIssue.LATE_EVENT)
            // Forget old coverage, but deduplicate rejected events and process new recovery boundaries.
            // Never replay a late event into previously counted time.
            seen.addAll(late)
        }
        // Batch equal timestamps so a pause/resume handoff has no fabricated zero-length OTHER interval.
        for ((at, batch) in fresh.filter { it.at >= cursor }.groupBy { it.at }.toSortedMap()) {
            advance(at)
            val previousFocus = focus()
            val previous = if (idle()) lastCertainApp else previousFocus
            batch.forEach { apply(it); seen.add(it) }
            val current = focus()
            val empty = idle()
            if (current == ObservedApp.UNKNOWN && !empty) {
                coverageSince = null; returnEligible = false; otherMillis = 0; lastCertainApp = ObservedApp.UNKNOWN
            } else if (current != ObservedApp.UNKNOWN && coverageSince == null) coverageSince = at
            if (current != previous) {
                when (current) {
                    ObservedApp.SELECTED -> {
                        if (selectedSeen && returnEligible && otherMillis >= 10_000) returns.add(at)
                    }
                    ObservedApp.OTHER -> {
                        if (previous == ObservedApp.SELECTED) {
                            if (outsideSince == null) outsideSince = at
                            returnEligible = selectedSeen; otherMillis = 0
                        }
                    }
                    ObservedApp.DEALGORITHM -> {
                        if (previous == ObservedApp.SELECTED && outsideSince == null) outsideSince = at
                        returnEligible = false; otherMillis = 0
                    }
                    ObservedApp.UNKNOWN -> Unit
                }
            }
            if (current == ObservedApp.SELECTED) {
                if (!selectedSeen) coverageSince = at
                selectedSeen = true; outsideSince = null; otherMillis = 0; returnEligible = false
            } else if (empty && previousFocus == ObservedApp.SELECTED) outsideSince = at
            if (current != ObservedApp.UNKNOWN) lastCertainApp = current

        }
        advance(until)
        seen.removeAll { it.at < cursor - OVERLAP }
        return snapshot()
    }

    private fun issue(): ObservationIssue? = when {
        !known -> baselineIssue
        !screenOn || keyguard -> ObservationIssue.LOCKED
        resumed.isEmpty() -> ObservationIssue.NO_RESUMED_ACTIVITY
        resumed.size > 1 -> ObservationIssue.MULTIPLE_RESUMED_APPS
        else -> null
    }

    private fun snapshot() = ObservationSnapshot(selectedMillis, durationMillis, returns.size,
        selectedSeen && focus() != ObservedApp.UNKNOWN && coverageSince?.let { cursor - it >= WINDOW } == true, focus(), issue(), lateEventCount, maxLatenessMillis)

    companion object { const val WINDOW = 600_000L; const val OVERLAP = 20_000L }
}
