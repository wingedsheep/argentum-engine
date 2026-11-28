package com.wingedsheep.engine.mechanics.cost

import com.wingedsheep.engine.handlers.ownManaCostOrNull
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.mana.ManaSource
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.costs.CostAtom

/**
 * Pricing for [AbilityCost.ExiledCardManaCost] before the card is chosen — "Exile a creature card
 * from your graveyard and pay its mana cost" (Back from the Brink).
 *
 * The price depends on the card the payer picks, so the activation is legal only while some
 * candidate's cost (plus any mana the rest of the cost names) is affordable, and the picker offers
 * only those candidates. The legal-action listing and the activation's choice pause both ask here,
 * so what is offered and what may be picked never disagree.
 */
internal object ExiledCardManaCostPricing {

    /** True when [cost] still carries an un-lowered [AbilityCost.ExiledCardManaCost]. */
    fun pricesByExiledCard(cost: AbilityCost): Boolean = when (cost) {
        is AbilityCost.ExiledCardManaCost -> true
        is AbilityCost.Composite -> cost.costs.any { it is AbilityCost.ExiledCardManaCost }
        else -> false
    }

    /** The candidates among [candidates] whose exile the payer can afford alongside the rest of [cost]. */
    fun affordable(
        state: GameState,
        playerId: EntityId,
        cost: AbilityCost,
        candidates: List<EntityId>,
        manaSolver: ManaSolver,
        excludeSources: Set<EntityId> = emptySet(),
        spellContext: SpellPaymentContext? = null,
        precomputedSources: List<ManaSource>? = null,
    ): List<EntityId> {
        val otherMana = otherMana(cost)
        return candidates.filter { cardId ->
            val price = ownManaCostOrNull(state, cardId) ?: return@filter false
            manaSolver.canPay(
                state, playerId, otherMana?.let { price + it } ?: price,
                excludeSources = excludeSources, spellContext = spellContext,
                precomputedSources = precomputedSources,
            )
        }
    }

    private fun otherMana(cost: AbilityCost): ManaCost? = when (cost) {
        is AbilityCost.Atom -> (cost.atom as? CostAtom.Mana)?.cost
        is AbilityCost.Composite -> cost.costs
            .mapNotNull { ((it as? AbilityCost.Atom)?.atom as? CostAtom.Mana)?.cost }
            .reduceOrNull { a, b -> a + b }
        else -> null
    }
}
