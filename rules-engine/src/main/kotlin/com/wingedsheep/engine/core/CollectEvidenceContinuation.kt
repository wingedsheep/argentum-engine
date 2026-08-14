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
) : ContinuationFrame
