package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.sdk.core.CounterType

/**
 * Resolves the string form of a counter type (as stored on effect data classes) into the
 * matching [CounterType] enum value. The naive `valueOf` + character-substitution fallback
 * breaks for counter names containing non-word characters like "+1/+1" and "-1/-1", so the
 * symbolic counters are mapped explicitly.
 *
 * Unknown strings default to [CounterType.PLUS_ONE_PLUS_ONE] to preserve the pre-existing
 * behavior of the executors.
 */
internal fun resolveCounterType(counterType: String): CounterType = when (counterType) {
    "+1/+1" -> CounterType.PLUS_ONE_PLUS_ONE
    "-1/-1" -> CounterType.MINUS_ONE_MINUS_ONE
    else -> try {
        CounterType.valueOf(
            counterType.uppercase()
                .replace(' ', '_')
                .replace('+', 'P')
                .replace('-', 'M')
                .replace("/", "_")
        )
    } catch (e: IllegalArgumentException) {
        CounterType.PLUS_ONE_PLUS_ONE
    }
}

/**
 * Convert a [CounterType] enum back to the wire-format string used on counter
 * events and effect data classes (e.g., `PLUS_ONE_PLUS_ONE` → `"+1/+1"`).
 */
internal fun counterTypeToString(counterType: CounterType): String = when (counterType) {
    CounterType.PLUS_ONE_PLUS_ONE -> "+1/+1"
    CounterType.MINUS_ONE_MINUS_ONE -> "-1/-1"
    else -> counterType.name.lowercase()
}
