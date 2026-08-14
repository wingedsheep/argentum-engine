package com.wingedsheep.engine.replacement

import com.wingedsheep.sdk.scripting.effects.Effect
import kotlinx.serialization.Serializable

/**
 * The result of applying a single replacement effect to a [PendingGameEvent].
 *
 * Three outcomes are possible per CR 614:
 *
 * - [Modified]: the event is modified (e.g., draw 2 instead of 1). The modified
 *   event must be re-checked against remaining replacement effects (CR 616.1f).
 *
 * - [Replaced]: the event is fully replaced with a new composed effect to
 *   execute instead (e.g., Rest in Peace redirecting graveyard to exile).
 *
 * - [Consumed]: the event is prevented entirely (e.g., PreventDraw stops the
 *   draw, PreventDamage stops the damage).
 */
@Serializable
sealed interface ReplacementOutcome {

    /**
     * The original event is modified — adjust values and re-check replacements.
     */
    @Serializable
    data class Modified(val modifiedEvent: PendingGameEvent) : ReplacementOutcome

    /**
     * The event is replaced entirely — execute this effect instead.
     */
    @Serializable
    data class Replaced(val newEffect: Effect) : ReplacementOutcome

    /**
     * The event is consumed — nothing happens, skip the original action.
     */
    @Serializable
    data object Consumed : ReplacementOutcome
}
