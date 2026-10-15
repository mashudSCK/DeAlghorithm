package com.mashudsck.dealghorithm

// Pure draft rules only. Production scoring remains gated by physical observation/background evidence.
object ScoreRules {
    fun score(durationMillis: Long?, returns: Int?, complete: Boolean, mode: SessionMode = SessionMode.AUTOMATIC): Int? {
        require(durationMillis == null || durationMillis >= 0)
        require(returns == null || returns >= 0)
        if (!complete || mode != SessionMode.AUTOMATIC || durationMillis == null || returns == null) return null
        // Common integer denominator avoids floating-point half-rounding errors.
        val numerator = minOf(durationMillis, 1_200_000L) * 3 + minOf(returns, 6) * 400_000L
        return ((numerator + 30_000) / 60_000).toInt()
    }

    fun candidate(score: Int?, lastStage: ReminderKind? = null, budgetExpired: Boolean = false,
        activeBudget: Boolean = false, suppressed: Boolean = false, unresolved: Boolean = false, cooling: Boolean = false): ReminderKind? {
        require(score == null || score in 0..100)
        if (suppressed || unresolved || cooling) return null
        if (budgetExpired) return ReminderKind.BUDGET
        if (activeBudget || score == null || score <= 30) return null
        val stages = listOf(ReminderKind.AWARENESS, ReminderKind.INTENT_CHECK, ReminderKind.REFLECTION, ReminderKind.REPLACEMENT)
        val ceiling = when { score <= 60 -> 0; score <= 80 -> 1; else -> 3 }
        return stages[minOf(ceiling, stages.indexOf(lastStage) + 1)]
    }
}
