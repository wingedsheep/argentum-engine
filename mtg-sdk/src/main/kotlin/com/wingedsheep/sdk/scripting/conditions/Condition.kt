package com.wingedsheep.sdk.scripting.conditions

import kotlinx.serialization.Serializable

/**
 * Conditions that can be checked against the game state.
 * Used for conditional effects like "If you control...", "If your life total is...".
 *
 * Conditions are data objects - evaluation is handled by ConditionEvaluator
 * which checks these conditions against GameState.
 *
 * Condition implementations are organized in the conditions/ subdirectory:
 * - LifeConditions.kt - Life total comparisons
 * - BattlefieldConditions.kt - Permanents you/opponents control
 * - ZoneConditions.kt - Hand, library, graveyard conditions
 * - SourceConditions.kt - State of the source permanent (attacking, tapped, etc.)
 * - TurnConditions.kt - Turn, phase, combat, and stack conditions
 * - CompositeConditions.kt - AND, OR, NOT combinations
 *
 * Note: ConditionalEffect (an Effect that checks a Condition) is in effects/ConditionalEffect.kt
 */
@Serializable
sealed interface Condition {
    /** Human-readable description of this condition */
    val description: String
}