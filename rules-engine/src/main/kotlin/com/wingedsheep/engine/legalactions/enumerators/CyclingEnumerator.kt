package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.CycleCard
import com.wingedsheep.engine.core.TypecycleCard
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.mechanics.CyclingGrants
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

            // Cycling is an activated ability of the card in hand (CR 702.29a), so an any-zone
            // "players can't activate abilities" (Yuriko, Blade of the Mighty) or a name lock on
            // "sources" (Pithing Needle) forbids it.
            if (context.castPermissionUtils.isActivationForbidden(state, cardId, playerId)) continue

            val typedCycling = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Cycling>()
                .firstOrNull { it.searchFilter != null }

            // Printed cycling plus any granted to this card in hand (Rhet-Tomb Mystic): one action
            // per distinct cost, since each is its own activated ability the player may choose.
            val plainOptions = CyclingGrants.plainCyclingOptions(
                state, cardId, cardDef, playerId, context.cardRegistry, context.predicateEvaluator
            )
            for (plainCycling in plainOptions) {
                // An `{X}` cycling cost (Webstrike Elite's "Cycling {X}{G}{G}") is affordable as
                // soon as its fixed part is payable — X = 0 is always a legal announcement (CR
                // 107.3a). Substituting X = 0 gives that baseline for both the check and the
                // auto-tap preview; the displayed cost string keeps the `{X}` so the player sees
                // the real cost, and hasXCost/maxAffordableX drive the client's X chooser.
                val hasXCost = plainCycling.cost.hasX
                // Cycling is an activated ability (CR 702.29a), so an "abilities you activate"
                // reduction (Zirda) prices it — the same reduction the handler charges.
                // A free-cycling grant (New Perspectives) swaps in {0} as an alternative cost
                // before the reductions — for an {X} cost that's the X = 0 baseline.
                val baseCost = context.castPermissionUtils.applyActivatedAbilityCostReduction(
                    context.castPermissionUtils.cyclingBaseCost(plainCycling.cost, 0, state, playerId),
                    state, cardId, playerId
                )
                val canAfford = context.manaSolver.canPay(state, playerId, baseCost, precomputedSources = context.availableManaSources)
                val autoTapPreview = if (context.skipAutoTapPreview) null else {
                    context.manaSolver.solve(state, playerId, baseCost, precomputedSources = context.availableManaSources)
                        ?.sources?.map { it.entityId }
                }
                val maxAffordableX = if (hasXCost) {
                    val available = context.manaSolver.getAvailableManaCount(state, playerId, context.availableManaSources)
                    // X > 0 pays the printed cost (the {0} alternative fixes X at 0), so the
                    // ceiling is measured against the printed cost's fixed part.
                    val printedFixed = context.castPermissionUtils.applyActivatedAbilityCostReduction(
                        plainCycling.cost.withXAs(0), state, cardId, playerId
                    )
                    ((available - printedFixed.cmc).coerceAtLeast(0)) / plainCycling.cost.xCount.coerceAtLeast(1)
                } else null
                result.add(
                    LegalAction(
                        actionType = "CycleCard",
                        description = "Cycle ${cardComponent.name}",
                        action = CycleCard(playerId, cardId, grantedBy = plainCycling.grantedBy),
                        affordable = canAfford,
                        hasXCost = hasXCost,
                        maxAffordableX = maxAffordableX,
                        // A free cycle (New Perspectives) prints as an empty cost; surface "{0}".
                        manaCostString = if (hasXCost) plainCycling.cost.toString() else baseCost.toString().ifEmpty { "{0}" },
                        autoTapPreview = autoTapPreview
                    )
                )
            }

            if (typedCycling != null) {
                val cost = context.castPermissionUtils.applyActivatedAbilityCostReduction(
                    context.castPermissionUtils.cyclingBaseCost(typedCycling.cost, null, state, playerId),
                    state, cardId, playerId
                )
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
                        manaCostString = cost.toString().ifEmpty { "{0}" },
                        autoTapPreview = autoTapPreview
                    )
                )
            }
        }

        return result
    }
}
