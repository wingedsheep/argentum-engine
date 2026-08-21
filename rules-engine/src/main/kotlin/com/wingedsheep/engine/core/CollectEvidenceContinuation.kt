package com.wingedsheep.engine.core

import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * Resumes a resolution-time collect evidence (CR 701.59) after the player has chosen *which*
 * graveyard cards to exile.
 *
 * Carries only the payment's own parameters, not the surrounding effect: whether anything happens
 * *because* evidence was collected is the outer construct's business — a `ReflexiveTriggerEffect`
 * puts its "when you do" on the stack (CR 603.12), a `GatedEffect` runs its "if you do" inline —
 * and both already own that continuation. This frame's whole job is to finish the exile the player
 * committed to.
 *
 * @property playerId The collecting player, whose graveyard is spent.
 * @property amount The threshold N the selection must meet (CR 701.59a).
 * @property sourceName The card/ability that caused the collection, for the emitted event.
 */
@Serializable
data class CollectEvidenceContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val amount: Int,
    val sourceName: String,
    /**
     * Pipeline variable the collected threshold is republished under once the exile finishes, or
     * null when nothing downstream reads it. Set only by the chosen-X path
     * ([com.wingedsheep.sdk.scripting.effects.CollectEvidenceChosenAmountEffect]); the fixed-N
     * effect has no X to hand on.
     */
    val storeAmountAs: String? = null,
) : ContinuationFrame

/**
 * Resumes a resolution-time "collect evidence **X**" after the player has chosen X but before they
 * have chosen *which* cards — the first of the chosen-X path's two hops (CR 701.59 +
 * Incinerator of the Guilty).
 *
 * Splitting the two questions is what keeps the bound honest: X is picked against the graveyard's
 * total mana value, and only then is the card picker raised with X as its floor. Asking for cards
 * first and deriving X from them would silently force X to the exiled total, which is not what the
 * card says.
 *
 * @property playerId The collecting player, who chose X and whose graveyard is spent.
 * @property storeAmountAs Pipeline variable the chosen X is republished under.
 * @property sourceName The card/ability that caused the collection, for the emitted event.
 */
@Serializable
data class ChooseEvidenceAmountContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val storeAmountAs: String,
    val sourceName: String,
) : ContinuationFrame
