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
import com.wingedsheep.sdk.scripting.GrantWarpToCardsInHand
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Single source of truth for "does this card have warp, and at what cost?" — used by every warp
 * read site (the cast-from-zone enumerator, the cast handler's cost/additional-cost lookups,
 * permission resolution, and the `wasWarped` resolution flag).
 *
 * Warp (CR 702.185) can be either printed on the card ([KeywordAbility.Warp] in the card's
 * keyword abilities) or granted at runtime to a card in the controller's hand by a battlefield
 * static ability ([GrantWarpToCardsInHand], e.g. "Artifact cards and red creature cards in your
 * hand have warp {2}{R}"). Routing every call site through here keeps the two sources consistent
 * so a granted warp behaves identically to a printed one — same alternative-cast legality, same
 * delayed exile trigger, same recast-from-exile path.
 *
 * Granted warp is hand-only by CR 702.185a and the granters' oracle text. Once the warped
 * permanent has been exiled at end of turn, the card stops matching the granter's filter
 * (it's no longer in hand); the recast-from-exile step uses the regular mana cost via the
 * [com.wingedsheep.engine.state.permissions.MayPlayPermission] added by
 * [com.wingedsheep.engine.handlers.effects.zones.WarpExileExecutor], so a granted-warp card
 * with no printed warp is still recastable from exile.
 */
object WarpGrants {

    /**
     * The effective warp ability for [cardId], or null if it has none. A printed warp on
     * [cardDef] wins; otherwise the first matching [GrantWarpToCardsInHand] on the
     * battlefield (controlled by [playerId]) is returned as a synthetic
     * [KeywordAbility.Warp] carrying the grant's cost.
     */
    fun effectiveWarp(
        state: GameState,
        cardId: EntityId,
        cardDef: CardDefinition?,
        playerId: EntityId,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator
    ): KeywordAbility.Warp? {
        cardDef?.keywordAbilities
            ?.firstOrNull { it is KeywordAbility.Warp }
            ?.let { return it as KeywordAbility.Warp }

        // Granted warp applies only to cards currently in the controller's hand.
        if (cardId !in state.getZone(ZoneKey(playerId, Zone.HAND))) return null
        return findGrantedWarp(state, cardId, playerId, cardRegistry, predicateEvaluator)
    }

    /**
     * Returns true if [cardId] is in [playerId]'s hand and a battlefield static ability
     * controlled by [playerId] grants warp to it. Hand-only by design (CR 702.185a).
     */
    fun hasGrantedWarpInHand(
        state: GameState,
        cardId: EntityId,
        playerId: EntityId,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator
    ): Boolean {
        if (cardId !in state.getZone(ZoneKey(playerId, Zone.HAND))) return false
        return findGrantedWarp(state, cardId, playerId, cardRegistry, predicateEvaluator) != null
    }

    private fun findGrantedWarp(
        state: GameState,
        cardId: EntityId,
        playerId: EntityId,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator
    ): KeywordAbility.Warp? {
        val context = PredicateContext(controllerId = playerId)
        // Controlled view — not the ownership-keyed zone map — so the grant follows whoever
        // currently controls the source permanent (Mind Control, Act of Treason, etc.) rather
        // than its owner. CR 109.4: "you" in an ability refers to the object's controller.
        for (entityId in state.controlledBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val def = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (ability in def.script.staticAbilities) {
                if (ability !is GrantWarpToCardsInHand) continue
                if (predicateEvaluator.matches(state, state.projectedState, cardId, ability.filter, context)) {
                    return KeywordAbility.Warp(ability.cost)
                }
            }
        }
        return null
    }
}
