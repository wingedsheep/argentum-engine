package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.legalactions.surfacedRequirements
import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.mechanics.SneakWindow
import com.wingedsheep.engine.state.components.identity.CardComponent

/**
 * Enumerates the "cast for its sneak cost" legal action (CR 702.190).
 *
 * Sneak is an alternative cost with an instant-speed casting permission that only opens during
 * the active player's declare blockers step (CR 702.190a). The normal [CastSpellEnumerator]
 * never surfaces these creature spells then — they're sorcery-speed — so this dedicated
 * enumerator handles the window without complicating the main cast path. For a sneak card that
 * is *also* an instant / has flash, [CastSpellEnumerator] still offers the regular cast and this
 * one adds the sneak option alongside it (CR 118.9a — the player chooses which cost applies).
 *
 * The action it emits is a `CastWithAlternativeCost` carrying [AlternativeCostType.SNEAK]. The
 * mana portion is the sneak [cost]; the non-mana portion — returning an unblocked attacker you
 * control to its owner's hand — is surfaced as a `BouncePermanent` additional cost the player
 * resolves, then submits as `CastSpell.additionalCostPayment.bouncedPermanents`.
 */
class SneakCastEnumerator : ActionEnumerator {

    override fun enumerate(context: EnumerationContext): List<LegalAction> {
        val state = context.state
        val playerId = context.playerId

        // The window is the same for every sneak card in hand; bail early if it's shut.
        if (!SneakWindow.isWindowOpen(state, playerId)) return emptyList()

        val result = mutableListOf<LegalAction>()
        val cachedSources = context.availableManaSources
        val bounceTargets = SneakWindow.unblockedAttackers(state, playerId)

        for (cardId in state.getHand(playerId)) {
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
            if (cardComponent.typeLine.isLand) continue
            if (context.cantCastSpell(cardId)) continue

            val cardDef = context.cardRegistry.getCard(cardComponent.name) ?: continue
            // Printed Sneak. (Ninjutsu is a hand-activated ability, not a cast — CR 702.49a.)
            val sneakAbility = cardDef.keywordAbilities
                .filterIsInstance<com.wingedsheep.sdk.scripting.KeywordAbility.Sneak>().firstOrNull() ?: continue
            val sneakKeywordName = sneakAbility.keyword.displayName

            // Honor cast restrictions exactly like the normal cast path (CR 601.3).
            if (!context.legality.castRestrictionsMet(
                    state, playerId, cardDef.script.castRestrictions
                )
            ) continue

            // Mana portion of the sneak cost (the bounce is paid separately).
            val sneakPricing = context.costCalculator.priceByTargets(
                state, cardDef, playerId, cardId, baseCost = sneakAbility.cost, withTable = !context.skipAutoTapPreview,
            )
            val sneakMana = sneakPricing.min
            val sneakManaByTargetCount = sneakPricing.byCountStrings()
            if (!context.manaSolver.canPay(state, playerId, sneakMana, precomputedSources = cachedSources)) {
                continue
            }

            val targetReqs = buildList {
                addAll(cardDef.script.targetRequirements)
                cardDef.script.castAuraTarget?.let { add(it) }
            }
            val targetReqInfos = if (targetReqs.isEmpty()) {
                emptyList()
            } else {
                context.targetUtils.buildTargetInfos(state, playerId, targetReqs, cardId, TargetingSourceType.SPELL)
            }
            // A targeted sneak spell (e.g. a "Technique") is only castable if every requirement
            // has a legal target right now (CR 601.2c).
            if (targetReqInfos.isNotEmpty() && !context.targetUtils.allRequirementsSatisfied(targetReqInfos)) {
                continue
            }

            val firstReq = targetReqs.firstOrNull()
            val firstReqInfo = targetReqInfos.firstOrNull()

            val autoTapPreview = if (context.skipAutoTapPreview) null else {
                context.manaSolver.solve(state, playerId, sneakMana, precomputedSources = cachedSources)
                    ?.sources?.map { it.entityId }
            }

            val bounceCostInfo = AdditionalCostData(
                description = "an unblocked attacker you control to return to its owner's hand",
                costType = "BouncePermanent",
                validBounceTargets = bounceTargets,
                bounceCount = 1
            )

            result.add(
                LegalAction(
                    actionType = "CastWithAlternativeCost",
                    description = "$sneakKeywordName ${cardComponent.name} ($sneakMana)",
                    action = CastSpell(
                        playerId = playerId,
                        cardId = cardId,
                        useAlternativeCost = true,
                        alternativeCostType = AlternativeCostType.SNEAK
                    ),
                    validTargets = firstReqInfo?.validTargets,
                    requiresTargets = firstReq != null,
                    targetCount = firstReqInfo?.maxTargets ?: 1,
                    minTargets = firstReq?.effectiveMinCount ?: (firstReq?.count ?: 1),
                    targetDescription = firstReq?.description,
                    targetRequirements = targetReqInfos.surfacedRequirements(),
                    manaCostString = sneakMana.toString(),
                    manaCostByTargetCount = sneakManaByTargetCount,
                    additionalCostInfo = bounceCostInfo,
                    autoTapPreview = autoTapPreview
                )
            )
        }

        // Granted graveyard sneak (Ninja Teen level 3): while the player controls an active
        // "creature cards in your graveyard have sneak {cost}" grant, their graveyard creature
        // cards are castable for that granted cost. Purely additive — the hand loop above (printed
        // Sneak) is unchanged.
        val graveyardSneakCost = SneakWindow.graveyardSneakGrantCost(state, playerId, context.cardRegistry)
        if (graveyardSneakCost != null) {
            for (cardId in state.getGraveyard(playerId)) {
                val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
                if (!cardComponent.typeLine.isCreature) continue
                if (context.cantCastSpell(cardId)) continue

                val cardDef = context.cardRegistry.getCard(cardComponent.name) ?: continue
                if (!context.legality.castRestrictionsMet(
                        state, playerId, cardDef.script.castRestrictions
                    )
                ) continue

                val sneakPricing = context.costCalculator.priceByTargets(
                    state, cardDef, playerId, cardId, baseCost = graveyardSneakCost,
                    withTable = !context.skipAutoTapPreview, fromZone = com.wingedsheep.sdk.core.Zone.GRAVEYARD,
                )
                val sneakMana = sneakPricing.min
                val sneakManaByTargetCount = sneakPricing.byCountStrings()
                if (!context.manaSolver.canPay(state, playerId, sneakMana, precomputedSources = cachedSources)) continue

                val targetReqs = buildList {
                    addAll(cardDef.script.targetRequirements)
                    cardDef.script.castAuraTarget?.let { add(it) }
                }
                val targetReqInfos = if (targetReqs.isEmpty()) {
                    emptyList()
                } else {
                    context.targetUtils.buildTargetInfos(state, playerId, targetReqs, cardId, TargetingSourceType.SPELL)
                }
                if (targetReqInfos.isNotEmpty() && !context.targetUtils.allRequirementsSatisfied(targetReqInfos)) continue

                val firstReq = targetReqs.firstOrNull()
                val firstReqInfo = targetReqInfos.firstOrNull()
                val autoTapPreview = if (context.skipAutoTapPreview) null else {
                    context.manaSolver.solve(state, playerId, sneakMana, precomputedSources = cachedSources)
                        ?.sources?.map { it.entityId }
                }
                val bounceCostInfo = AdditionalCostData(
                    description = "an unblocked attacker you control to return to its owner's hand",
                    costType = "BouncePermanent",
                    validBounceTargets = bounceTargets,
                    bounceCount = 1
                )

                result.add(
                    LegalAction(
                        actionType = "CastWithAlternativeCost",
                        description = "Sneak ${cardComponent.name} from your graveyard ($sneakMana)",
                        action = CastSpell(
                            playerId = playerId,
                            cardId = cardId,
                            useAlternativeCost = true,
                            alternativeCostType = AlternativeCostType.SNEAK
                        ),
                        validTargets = firstReqInfo?.validTargets,
                        requiresTargets = firstReq != null,
                        targetCount = firstReqInfo?.maxTargets ?: 1,
                        minTargets = firstReq?.effectiveMinCount ?: (firstReq?.count ?: 1),
                        targetDescription = firstReq?.description,
                        targetRequirements = targetReqInfos.surfacedRequirements(),
                        manaCostString = sneakMana.toString(),
                        manaCostByTargetCount = sneakManaByTargetCount,
                        additionalCostInfo = bounceCostInfo,
                        autoTapPreview = autoTapPreview
                    )
                )
            }
        }

        return result
    }
}
