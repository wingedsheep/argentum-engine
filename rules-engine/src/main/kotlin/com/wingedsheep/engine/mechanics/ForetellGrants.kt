package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.controlledLiveStaticAbilities
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GrantForetellToCardsInHand
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.MayForetellOnAnyTurn

/**
 * Single source of truth for "can this card in hand be foretold, for what later cost, and when?" —
 * read by both [com.wingedsheep.engine.legalactions.enumerators.ForetellEnumerator] and
 * [com.wingedsheep.engine.handlers.actions.ability.ForetellCardHandler] so what is offered is what is
 * accepted.
 *
 * Foretell (CR 702.143) is printed on the card ([KeywordAbility.Foretell]) or granted to cards in
 * hand by a battlefield [GrantForetellToCardsInHand] (Dream Devourer). A printed foretell wins — the grant
 * is to cards "without foretell". The timing is "any time you have priority during your turn"
 * (CR 116.2h) unless the player controls a [MayForetellOnAnyTurn] (Cosmos Charger).
 */
object ForetellGrants {

    /**
     * The foretell cost [cardId] would be cast for later if [playerId] foretold it now, or null if it
     * can't be foretold. A grant's cost is the card's mana cost with the grant's reduction taken off
     * its generic part (CR 118.7 — a reduction never removes coloured pips).
     */
    fun effectiveForetellCost(
        state: GameState,
        cardId: EntityId,
        cardDef: CardDefinition,
        playerId: EntityId,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator,
    ): ManaCost? {
        cardDef.keywordAbilities.firstOrNull { it is KeywordAbility.Foretell }
            ?.let { return (it as KeywordAbility.Foretell).cost }

        // "Equal to its mana cost" — a card with no mana cost has an unpayable foretell cost
        // (CR 118.6), so it gains nothing it could cast.
        if (cardDef.hasNoManaCost) return null
        val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: return null
        val context = PredicateContext(controllerId = playerId)
        val grant = state.controlledLiveStaticAbilities(playerId, cardRegistry).map { it.second }
            .filterIsInstance<GrantForetellToCardsInHand>()
            .firstOrNull { predicateEvaluator.matches(state, state.projectedState, cardId, it.filter, context) }
            ?: return null
        return cardComponent.manaCost.reduceGeneric(grant.costReduction)
    }

    /** Whether [playerId] may take the foretell special action right now (CR 116.2h). */
    fun mayForetellNow(state: GameState, playerId: EntityId, cardRegistry: CardRegistry): Boolean =
        state.hasPriority(playerId) &&
            (state.isActiveTurnFor(playerId) ||
                state.controlledLiveStaticAbilities(playerId, cardRegistry).any { it.second is MayForetellOnAnyTurn })
}
