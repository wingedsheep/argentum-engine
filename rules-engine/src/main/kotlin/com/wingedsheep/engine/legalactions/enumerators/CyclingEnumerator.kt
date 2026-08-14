package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.CycleCard
import com.wingedsheep.engine.core.TypecycleCard
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Enumerates cycling and typecycling actions for cards in hand.
 *
 * CastSpell actions for these cards are emitted by [CastSpellEnumerator]; when timing
 * prevents a cast, the client renders a greyed-out "Cast" entry alongside the cycle
 * option so the player always sees both choices.
 */
class CyclingEnumerator : ActionEnumerator {

    override fun enumerate(context: EnumerationContext): List<LegalAction> {
        val result = mutableListOf<LegalAction>()
        val state = context.state
        val playerId = context.playerId
        if (context.cyclingPrevented) return result

        val hand = state.getHand(playerId)
        for (cardId in hand) {
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
            val cardDef = context.cardRegistry.getCard(cardComponent.name) ?: continue

            val cyclingAbilities = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Cycling>()
            val plainCycling = cyclingAbilities.firstOrNull { it.searchFilter == null }
            val typedCycling = cyclingAbilities.firstOrNull { it.searchFilter != null }

            if (plainCycling != null) {
                // An `{X}` cycling cost (Webstrike Elite's "Cycling {X}{G}{G}") is affordable as
                // soon as its fixed part is payable — X = 0 is always a legal announcement (CR
                // 107.3a). Substituting X = 0 gives that baseline for both the check and the
                // auto-tap preview; the displayed cost string keeps the `{X}` so the player sees
                // the real cost, and hasXCost/maxAffordableX drive the client's X chooser.
                val hasXCost = plainCycling.cost.hasX
                val baseCost = plainCycling.cost.withXAs(0)
                val canAfford = context.manaSolver.canPay(state, playerId, baseCost, precomputedSources = context.availableManaSources)
                val autoTapPreview = if (context.skipAutoTapPreview) null else {
                    context.manaSolver.solve(state, playerId, baseCost, precomputedSources = context.availableManaSources)
                        ?.sources?.map { it.entityId }
                }
                val maxAffordableX = if (hasXCost) {
                    val available = context.manaSolver.getAvailableManaCount(state, playerId, context.availableManaSources)
                    ((available - baseCost.cmc).coerceAtLeast(0)) / plainCycling.cost.xCount.coerceAtLeast(1)
                } else null
                result.add(
                    LegalAction(
                        actionType = "CycleCard",
                        description = "Cycle ${cardComponent.name}",
                        action = CycleCard(playerId, cardId),
                        affordable = canAfford,
                        hasXCost = hasXCost,
                        maxAffordableX = maxAffordableX,
                        manaCostString = plainCycling.cost.toString(),
                        autoTapPreview = autoTapPreview
                    )
                )
            }

            if (typedCycling != null) {
                val cost = typedCycling.cost
                val description = "${typedCycling.displayPrefix} ${cardComponent.name}"
                val canAfford = context.manaSolver.canPay(state, playerId, cost, precomputedSources = context.availableManaSources)
                val autoTapPreview = if (context.skipAutoTapPreview) null else {
                    context.manaSolver.solve(state, playerId, cost, precomputedSources = context.availableManaSources)
                        ?.sources?.map { it.entityId }
                }
                result.add(
                    LegalAction(
                        actionType = "TypecycleCard",
                        description = description,
                        action = TypecycleCard(playerId, cardId),
                        affordable = canAfford,
                        manaCostString = cost.toString(),
                        autoTapPreview = autoTapPreview
                    )
                )
            }
        }

        return result
    }
}
