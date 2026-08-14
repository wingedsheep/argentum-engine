package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.GrantsMadnessToOwnedCardsComponent
import com.wingedsheep.engine.state.components.identity.MadnessComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.EntityId

/**
 * Single source of truth for "does this card have madness, and at what cost?" — the mirror of
 * [MiracleGrants] for CR 702.35.
 *
 * Madness is either printed on the card ([MadnessComponent], stamped from
 * [com.wingedsheep.sdk.scripting.KeywordAbility.Madness]) or granted at runtime by a battlefield
 * static ([com.wingedsheep.sdk.scripting.GrantMadnessToOwnedCards], i.e. Falkenrath Gorger: "Each
 * Vampire creature card you own that isn't on the battlefield has madness. The madness cost is
 * equal to its mana cost."). Routing the discard replacement through here makes a granted madness
 * behave exactly like a printed one — same exile redirect, same CR 702.35a cast offer, same fixed
 * alternative cost on the exiled card.
 *
 * **Known simplification.** CR 616.1 lets the affected object's controller pick which applicable
 * replacement effect applies, and the Falkenrath Gorger rulings spell that out: a discarded Vampire
 * card that *also* has printed madness offers a choice of which madness ability exiles it. We take
 * the printed one without asking. Every printed madness cost on a Vampire creature card is strictly
 * cheaper than that card's mana cost, so the un-asked choice is the one a player would make; a
 * prompt mid-replacement is the only faithful fix and no card in the pool needs it yet.
 */
object MadnessGrants {

    private val predicateEvaluator = PredicateEvaluator()

    /**
     * The madness cost [cardId] effectively has, or null if it has none. Printed madness on the
     * card wins; otherwise the first matching battlefield grant applies at the card's own mana
     * cost.
     *
     * [container] is the card's own component container, passed in because every caller already
     * holds it while moving the card.
     */
    fun effectiveMadnessCost(
        state: GameState,
        cardId: EntityId,
        container: ComponentContainer
    ): ManaCost? {
        container.get<MadnessComponent>()?.let { return it.cost }
        return grantedMadnessCost(state, cardId, container)
    }

    /**
     * The granted madness cost for [cardId] — its own mana cost, if some battlefield permanent
     * grants madness to cards its controller owns matching the card.
     *
     * "That isn't on the battlefield" is the grant's own scope; a permanent is never discarded, so
     * the check is cheap insurance rather than a live case.
     */
    private fun grantedMadnessCost(
        state: GameState,
        cardId: EntityId,
        container: ComponentContainer
    ): ManaCost? {
        val card = container.get<CardComponent>() ?: return null
        val ownerId = card.ownerId ?: return null
        if (cardId in state.getBattlefield()) return null

        // "You own" — only a grant controlled by the card's owner reaches it. Walking the owner's
        // battlefield rather than the whole one is both the rule and the cheaper scan.
        val context = PredicateContext(controllerId = ownerId)
        for (permanentId in state.controlledBattlefield(ownerId)) {
            val grant = state.getEntity(permanentId)
                ?.get<GrantsMadnessToOwnedCardsComponent>() ?: continue
            for (filter in grant.filters) {
                if (predicateEvaluator.matches(state, state.projectedState, cardId, filter, context)) {
                    return card.manaCost
                }
            }
        }
        return null
    }
}
