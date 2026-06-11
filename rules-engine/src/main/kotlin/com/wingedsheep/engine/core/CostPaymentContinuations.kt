package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.Effect
import kotlinx.serialization.Serializable

/**
 * Resume after a player responds to a cost-payment prompt created by
 * `com.wingedsheep.engine.mechanics.cost.CostPaymentService.pay`.
 *
 * A *single* frame serves all ten [PayCost] variants: the resumer dispatches on [cost]'s shape to
 * interpret the response — yes/no for mana / life / random-discard, card-selection for
 * discard / exile / reveal / sacrifice / return / tap, option-pick for [PayCost.Choice]. On a paid
 * outcome it performs the payment and runs [onPaid]; on a declined or short selection it runs
 * [onDeclined]. Either way it then calls `checkForMore`, so a caller-pushed frame beneath this one
 * resumes for non-effect follow-ups.
 *
 * [PayCost.OwnManaCost] is resolved to a concrete [PayCost.Atom] (CostAtom.Mana) (against the source's printed cost)
 * before the frame is created, so it never appears here. A [PayCost.Choice] is stored already reduced
 * to the *affordable* options, so the chosen option index maps positionally onto [PayCost.Choice.options]
 * and an index past the end means "decline".
 *
 * @property payerId the player paying the cost.
 * @property sourceId the spell/permanent the cost is attached to (needed for self-exclusion and
 *   own-mana-cost resolution).
 * @property sourceName name of the source for event/prompt text.
 * @property cost the (already-resolved) cost being paid.
 * @property onPaid effect to run after the cost is paid (null = nothing).
 * @property onDeclined effect to run if the cost is declined or unpayable (null = nothing).
 * @property targets / [namedTargets] / [storedCollections] context threaded into the follow-up so it
 *   can resolve `ContextTarget`s and reference pipeline collections.
 */
@Serializable
data class CostPaymentContinuation(
    override val decisionId: String,
    val payerId: EntityId,
    val sourceId: EntityId,
    val sourceName: String,
    val cost: PayCost,
    val onPaid: Effect? = null,
    val onDeclined: Effect? = null,
    val targets: List<ChosenTarget> = emptyList(),
    val namedTargets: Map<String, ChosenTarget> = emptyMap(),
    val storedCollections: Map<String, List<EntityId>> = emptyMap()
) : ContinuationFrame
