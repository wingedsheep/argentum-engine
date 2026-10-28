package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.faceUpCardDefinition
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GrantCyclingToCardsInHand
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Single source of truth for "which plain cycling abilities does this card in hand have?" — read by
 * the cycling enumerator and by `CycleCardHandler`'s validate and execute.
 *
 * Cycling (CR 702.29) is either printed ([KeywordAbility.Cycling] without a search filter) or granted
 * to a card in its controller's hand by a battlefield static ([GrantCyclingToCardsInHand], Rhet-Tomb
 * Mystic: "Each creature card in your hand has cycling {1}{U}"). A card with both has two cycling
 * abilities and its owner may activate either, so this returns every distinct cost, printed first.
 * Two grants with the same cost are the same choice and collapse into one option.
 */
object CyclingGrants {

    /**
     * One activatable plain cycling ability. [grantedBy] is null for the card's printed cycling,
     * otherwise the permanent whose [GrantCyclingToCardsInHand] grants it — the identity
     * `CycleCard.grantedBy` names.
     */
    data class CyclingOption(val cost: ManaCost, val grantedBy: EntityId?)

    fun plainCyclingOptions(
        state: GameState,
        cardId: EntityId,
        cardDef: CardDefinition,
        playerId: EntityId,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator
    ): List<CyclingOption> {
        val options = mutableListOf<CyclingOption>()
        cardDef.keywordAbilities
            .filterIsInstance<KeywordAbility.Cycling>()
            .firstOrNull { it.searchFilter == null }
            ?.let { options += CyclingOption(it.cost, grantedBy = null) }

        for ((sourceId, grant) in grants(state, cardId, playerId, cardRegistry, predicateEvaluator)) {
            if (options.none { it.cost == grant.cost }) options += CyclingOption(grant.cost, grantedBy = sourceId)
        }
        return options
    }

    /**
     * Every [GrantCyclingToCardsInHand] reaching [cardId] in [playerId]'s hand, paired with its
     * source — one cycling ability each, duplicates included (`ActivatedAbilityPresence` counts them;
     * [plainCyclingOptions] collapses equal costs).
     */
    fun grants(
        state: GameState,
        cardId: EntityId,
        playerId: EntityId,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator
    ): List<Pair<EntityId, GrantCyclingToCardsInHand>> {
        // Granted cycling reaches only the granter's controller's hand.
        if (cardId !in state.getZone(ZoneKey(playerId, Zone.HAND))) return emptyList()
        val projected = state.projectedState
        val context = PredicateContext(controllerId = playerId)
        val result = mutableListOf<Pair<EntityId, GrantCyclingToCardsInHand>>()
        for (sourceId in state.controlledBattlefield(playerId)) {
            if (projected.hasLostAllAbilities(sourceId)) continue
            val def = state.getEntity(sourceId)?.faceUpCardDefinition(cardRegistry) ?: continue
            for (ability in def.script.staticAbilities) {
                if (ability !is GrantCyclingToCardsInHand) continue
                if (predicateEvaluator.matches(state, projected, cardId, ability.filter, context)) {
                    result += sourceId to ability
                }
            }
        }
        return result
    }

    /** The option a submitted `CycleCard` names: its printed cycling when [grantedBy] is null. */
    fun chosenOption(
        state: GameState,
        cardId: EntityId,
        cardDef: CardDefinition,
        playerId: EntityId,
        grantedBy: EntityId?,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator
    ): CyclingOption? =
        plainCyclingOptions(state, cardId, cardDef, playerId, cardRegistry, predicateEvaluator)
            .firstOrNull { it.grantedBy == grantedBy }
}
