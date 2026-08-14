package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GraveyardCardsHaveMayhem
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Single source of truth for "does this card have mayhem, and at what cost?" — used by every mayhem
 * read site (the cast-from-graveyard enumerator and the cast handler / cast-permission check).
 *
 * Mayhem (CR 702.187) can be printed on the card ([KeywordAbility.Mayhem] in the card's keyword
 * abilities) or granted at runtime to a specific card entity (Green Goblin's "Goblin Formula":
 * "Each nonland card in your graveyard has mayhem"). Routing all call sites through here keeps the
 * two sources consistent so a granted mayhem behaves identically to a printed one.
 *
 * Unlike [HarmonizeGrants], mayhem does NOT exile the spell on resolution — see [StackResolver].
 */
object MayhemGrants {

    /**
     * The effective mayhem ability for [cardId], or null if it has none. A printed mayhem on
     * [cardDef] wins; otherwise the most recently granted runtime mayhem for this entity is
     * returned (a later grant overrides an earlier one for the same card).
     */
    fun effectiveMayhem(
        state: GameState,
        cardId: EntityId,
        cardDef: CardDefinition?,
        controllerId: EntityId? = null,
        cardRegistry: CardRegistry? = null,
        predicateEvaluator: PredicateEvaluator? = null,
    ): KeywordAbility.Mayhem? {
        cardDef?.keywordAbilities
            ?.firstOrNull { it is KeywordAbility.Mayhem }
            ?.let { return it as KeywordAbility.Mayhem }

        state.grantedKeywordAbilities
            .lastOrNull { it.entityId == cardId && it.ability is KeywordAbility.Mayhem }
            ?.let { return it.ability as KeywordAbility.Mayhem }

        if (controllerId != null && cardRegistry != null && predicateEvaluator != null) {
            groupGrantMayhem(state, cardId, controllerId, cardRegistry, predicateEvaluator)
                ?.let { return it }
        }
        return null
    }

    /**
     * Scan [controllerId]'s battlefield for a [GraveyardCardsHaveMayhem] static whose filter matches
     * [cardId] (during their turn, if the grant requires it), synthesizing a [KeywordAbility.Mayhem]
     * carrying the granted cost — the grant's fixed cost, or the card's own mana cost when the grant
     * leaves it null ("equal to that card's mana cost"). Mirrors `FlashbackGrants.groupGrantFlashback`.
     */
    private fun groupGrantMayhem(
        state: GameState,
        cardId: EntityId,
        controllerId: EntityId,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator,
    ): KeywordAbility.Mayhem? {
        val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: return null
        val context = PredicateContext(controllerId = controllerId)
        for (granterId in state.controlledBattlefield(controllerId)) {
            val def = state.getEntity(granterId)?.get<CardComponent>()
                ?.let { cardRegistry.getCard(it.cardDefinitionId) } ?: continue
            for (ability in def.script.staticAbilities) {
                if (ability !is GraveyardCardsHaveMayhem) continue
                if (ability.duringYourTurnOnly && !state.isActiveTurnFor(controllerId)) continue
                if (predicateEvaluator.matches(state, state.projectedState, cardId, ability.filter, context)) {
                    return KeywordAbility.Mayhem(ability.cost ?: cardComponent.manaCost)
                }
            }
        }
        return null
    }
}
