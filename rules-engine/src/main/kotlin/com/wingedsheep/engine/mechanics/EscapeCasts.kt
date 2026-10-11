package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GraveyardCardsHaveEscape
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Single source of truth for "can this card escape, and at what cost?" — used by the
 * cast-from-graveyard enumerator, the cast zone resolver, the cost totaller and the additional-cost
 * collector, so every read site agrees.
 *
 * Escape (CR 702.138a) is a static ability that functions while the card is in its owner's
 * graveyard: "You may cast this card from your graveyard by paying [cost] rather than paying its
 * mana cost." It comes from two sources, checked in order:
 *  1. **Printed** on the card ([KeywordAbility.Escape] in its keyword abilities).
 *  2. **Whole-graveyard group grant** from a battlefield static ([GraveyardCardsHaveEscape],
 *     Underworld Breach: "Each nonland card in your graveyard has escape …"), controlled by the
 *     card's caster. Mirrors [FlashbackGrants]. A card that has both offers only the printed one —
 *     the rulings let you pick either, and a second escape option is a choice no card needs yet.
 */
object EscapeCasts {

    /** The printed escape keyword on [cardDef], or null when it has none. */
    fun printedEscape(cardDef: CardDefinition?): KeywordAbility.Escape? =
        cardDef?.keywordAbilities?.filterIsInstance<KeywordAbility.Escape>()?.firstOrNull()

    /**
     * The effective escape ability for [cardId]: the printed one, else a [GraveyardCardsHaveEscape]
     * grant on a permanent [controllerId] controls (CR 109.5 — "your graveyard" is the granter's
     * controller's) whose filter matches the card, synthesized with the grant's cost — or the card's
     * own mana cost when the grant leaves it null. A card with no mana cost gets nothing from an
     * "equal to the card's mana cost" grant: its escape cost is unpayable (Underworld Breach ruling).
     */
    fun effectiveEscape(
        state: GameState,
        cardId: EntityId,
        cardDef: CardDefinition?,
        controllerId: EntityId,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator,
    ): KeywordAbility.Escape? {
        printedEscape(cardDef)?.let { return it }

        val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: return null
        val context = PredicateContext(controllerId = controllerId)
        for (granterId in state.controlledBattlefield(controllerId)) {
            val def = state.getEntity(granterId)?.get<CardComponent>()
                ?.let { cardRegistry.getCard(it.cardDefinitionId) } ?: continue
            for (ability in def.script.staticAbilities) {
                if (ability !is GraveyardCardsHaveEscape) continue
                if (ability.duringYourTurnOnly && !state.isActiveTurnFor(controllerId)) continue
                val cost = ability.cost ?: cardComponent.manaCost.takeIf { !it.isEmpty() } ?: continue
                if (predicateEvaluator.matches(state, state.projectedState, cardId, ability.filter, context)) {
                    return KeywordAbility.Escape(cost, ability.additionalCost)
                }
            }
        }
        return null
    }
}
