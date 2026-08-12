package com.futureape.kanleme.ui.screens

internal data class CleaningSessionProgress(
    val target: Int,
    val completed: Int,
    val remaining: Int,
    val percent: Int,
)

/**
 * Calculates progress for the user-facing session target, not for the in-memory deck window.
 *
 * When the currently processable queue is smaller than the requested target, the observed
 * queue size becomes the effective target. This keeps a short queue from displaying an
 * impossible remaining count and lets the final available item complete the session cleanly.
 */
internal fun cleaningSessionProgress(
    completedActions: Int,
    requestedTarget: Int,
    availableRemaining: Int,
): CleaningSessionProgress {
    val completed = completedActions.coerceAtLeast(0)
    val requested = requestedTarget.coerceIn(1, 100)
    val available = availableRemaining.coerceAtLeast(0)
    val observedSessionSize = (available + completed).coerceAtLeast(completed)
    val target = if (observedSessionSize > 0) {
        minOf(requested, observedSessionSize)
    } else {
        requested
    }
    val boundedCompleted = completed.coerceAtMost(target)
    val percent = ((boundedCompleted.toLong() * 100L) / target.toLong())
        .toInt()
        .coerceIn(0, 100)
    return CleaningSessionProgress(
        target = target,
        completed = boundedCompleted,
        remaining = (target - boundedCompleted).coerceAtLeast(0),
        percent = percent,
    )
}
