package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GrantMiracleToCardsInHand
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Single source of truth for "does this card have miracle, and at what cost?" — used by every
 * miracle read site (the draw flow that opens the window, the cast-from-hand enumerator, and the
 * cast handler's cost lookup).
 *
 * Miracle (CR 702.94) can be either printed on the card ([KeywordAbility.Miracle] in the card's
 * keyword abilities) or granted at runtime to a card in the controller's hand by a battlefield
 * static ability ([GrantMiracleToCardsInHand], e.g. Lorehold, the Historian: "Each instant and
 * sorcery card in your hand has miracle {2}"). Routing every call site through here keeps the two
 * sources consistent so a granted miracle behaves identically to a printed one.
 *
 * Miracle is hand-only by CR 702.94a (the ability functions only while the card is in the hand), so
 * granted miracle is resolved against the controller's hand membership.
 */
object MiracleGrants {

    /**
     * The effective miracle ability for [cardId], or null if it has none. A printed miracle on
     * [cardDef] wins; otherwise the first matching [GrantMiracleToCardsInHand] on the battlefield
     * (controlled by [playerId]) is returned as a synthetic [KeywordAbility.Miracle] carrying the
     * grant's cost.
     */
    fun effectiveMiracle(
        state: GameState,
        cardId: EntityId,
        cardDef: CardDefinition?,
        playerId: EntityId,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator
    ): KeywordAbility.Miracle? {
        cardDef?.keywordAbilities
            ?.firstOrNull { it is KeywordAbility.Miracle }
            ?.let { return it as KeywordAbility.Miracle }

        // Granted miracle applies only to cards currently in the controller's hand.
        if (cardId !in state.getZone(ZoneKey(playerId, Zone.HAND))) return null
        return findGrantedMiracle(state, cardId, playerId, cardRegistry, predicateEvaluator)
    }

    private fun findGrantedMiracle(
        state: GameState,
        cardId: EntityId,
        playerId: EntityId,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator
    ): KeywordAbility.Miracle? {
        val context = PredicateContext(controllerId = playerId)
        for (entityId in state.controlledBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val def = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (ability in def.script.staticAbilities) {
                if (ability !is GrantMiracleToCardsInHand) continue
                if (predicateEvaluator.matches(state, state.projectedState, cardId, ability.filter, context)) {
                    return KeywordAbility.Miracle(ability.cost)
                }
            }
        }
        return null
    }
}
