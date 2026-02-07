package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable

/**
 * Represents the duration of a temporary effect.
 *
 * Magic has many different durations for effects:
 * - "Until end of turn"
 * - "Until your next turn"
 * - "For as long as you control this permanent"
 * - "Until end of combat"
 *
 * Usage:
 * ```kotlin
 * ModifyStatsEffect(
 *     powerModifier = 3,
 *     toughnessModifier = 3,
 *     target = EffectTarget.ContextTarget(0),
 *     duration = Duration.EndOfTurn
 * )
 * ```
 */
@Serializable
sealed interface Duration {
    val description: String

    /**
     * Effect lasts until end of turn.
     * Example: Giant Growth
     */
    @Serializable
    data object EndOfTurn : Duration {
        override val description = "until end of turn"
    }

    /**
     * Effect lasts until the beginning of your next turn.
     * Example: Teferi's Protection
     */
    @Serializable
    data object UntilYourNextTurn : Duration {
        override val description = "until your next turn"
    }

    /**
     * Effect lasts until the beginning of your next upkeep.
     * Example: Various older cards
     */
    @Serializable
    data object UntilYourNextUpkeep : Duration {
        override val description = "until the beginning of your next upkeep"
    }

    /**
     * Effect lasts until end of combat.
     * Example: Fog effects, combat tricks
     */
    @Serializable
    data object EndOfCombat : Duration {
        override val description = "until end of combat"
    }

    /**
     * Effect is permanent (static abilities, auras while attached).
     * No duration text is displayed.
     */
    @Serializable
    data object Permanent : Duration {
        override val description = ""
    }

    /**
     * Effect lasts while the source permanent is on the battlefield.
     * Example: Anthem effects, equipment bonuses
     */
    @Serializable
    data class WhileSourceOnBattlefield(
        val sourceDescription: String = "this permanent"
    ) : Duration {
        override val description = "for as long as $sourceDescription remains on the battlefield"
    }

    /**
     * Effect lasts until a specific phase.
     * Example: "Until your next end step"
     */
    @Serializable
    data class UntilPhase(val phase: String) : Duration {
        override val description = "until the $phase"
    }

    /**
     * Effect lasts until a condition is met.
     * Example: "Until that creature leaves the battlefield"
     */
    @Serializable
    data class UntilCondition(val conditionDescription: String) : Duration {
        override val description = "until $conditionDescription"
    }

    /**
     * Effect lasts while the source permanent remains tapped.
     * Example: Everglove Courier "for as long as Everglove Courier remains tapped"
     */
    @Serializable
    data class WhileSourceTapped(
        val sourceDescription: String = "this creature"
    ) : Duration {
        override val description = "for as long as $sourceDescription remains tapped"
    }
}

/**
 * Convenience object for DSL-style duration creation.
 */
object Durations {
    val EndOfTurn = Duration.EndOfTurn
    val UntilYourNextTurn = Duration.UntilYourNextTurn
    val EndOfCombat = Duration.EndOfCombat
    val Permanent = Duration.Permanent

    fun whileOnBattlefield(source: String = "this permanent") =
        Duration.WhileSourceOnBattlefield(source)

    fun whileSourceTapped(source: String = "this creature") =
        Duration.WhileSourceTapped(source)

    fun untilPhase(phase: String) = Duration.UntilPhase(phase)
    fun until(condition: String) = Duration.UntilCondition(condition)
}
