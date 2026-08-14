package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.mechanics.EmergeCasts
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.EntityId

/**
 * Enumerates the "cast for its emerge cost" legal action (CR 702.119).
 *
 * Emerge is an alternative cost bundling a sacrifice whose *identity* changes the mana actually
 * charged: the emerge cost is reduced by an amount of generic mana equal to the sacrificed
 * creature's mana value (CR 702.119a). That coupling is why emerge gets a dedicated enumerator
 * rather than another branch in [CastSpellEnumerator] — affordability has to be evaluated once per
 * candidate creature, and only the creatures that leave the reduced cost payable may be offered.
 * Handing the client (or the AI, which takes the first candidate) a creature that can't actually
 * pay would surface an action that errors on submission.
 *
 * Emerge grants no timing permission of its own, so the spell is offered at its normal timing —
 * sorcery speed unless it's an instant or has flash (Elder Deep-Fiend).
 *
 * The action emitted is a `CastWithAlternativeCost` carrying [AlternativeCostType.EMERGE]. The
 * sacrifice is surfaced as a `SacrificePermanent` additional cost the player resolves on the
 * battlefield, then submits as `CastSpell.additionalCostPayment.sacrificedPermanents`.
 */
class EmergeCastEnumerator : ActionEnumerator {

    override fun enumerate(context: EnumerationContext): List<LegalAction> {
        val state = context.state
        val playerId = context.playerId

        if (context.cantPlayCardsFromHand) return emptyList()

        // The candidate pool is the same for every emerge card in hand; bail before touching the
        // hand at all when the player controls no creature to sacrifice.
        val candidates = EmergeCasts.sacrificeCandidates(state, playerId)
        if (candidates.isEmpty()) return emptyList()

        val result = mutableListOf<LegalAction>()
        val cachedSources = context.availableManaSources

        for (cardId in state.getHand(playerId)) {
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
            if (context.cantCastSpell(cardId)) continue

            val cardDef = context.cardRegistry.getCard(cardComponent.name) ?: continue
            val emerge = EmergeCasts.printedEmerge(cardDef) ?: continue

            // Normal timing (CR 702.119 adds no permission of its own).
            val isInstant = cardComponent.typeLine.isInstant
            val hasFlash = cardDef.keywords.contains(Keyword.FLASH) ||
                context.castPermissionUtils.hasGrantedFlash(state, cardId)
            if (!isInstant && !hasFlash && !context.canPlaySorcerySpeed) continue

            // Honor cast restrictions exactly like the normal cast path (CR 601.3).
            if (!context.castPermissionUtils.checkCastRestrictions(
                    state, playerId, cardDef.script.castRestrictions
                )
            ) continue

            // Base emerge cost through the battlefield cost-modifier pipeline (a Thalia-style tax
            // applies on top of an alternative cost — CR 601.2f).
            val baseCost = context.costCalculator.calculateEffectiveCostWithAlternativeBase(
                state, cardDef, emerge.cost, playerId
            )

            // Only creatures whose mana value leaves the reduced cost payable may be offered.
            // Cheapest-first so the AI's "take the first candidate" pick spends the least valuable
            // body rather than an arbitrary one. The surviving cost is kept per candidate and sent
            // along: it is the *only* way the client can tell the player what a given sacrifice
            // will actually cost, since the generic-only reduction is a rule, not client math.
            val costByCandidate = LinkedHashMap<EntityId, ManaCost>()
            for (creatureId in candidates.sortedBy { EmergeCasts.manaValueOf(state, it) }) {
                val reduced = EmergeCasts.reduceForSacrifice(baseCost, state, creatureId)
                if (context.manaSolver.canPay(state, playerId, reduced, precomputedSources = cachedSources)) {
                    costByCandidate[creatureId] = reduced
                }
            }
            if (costByCandidate.isEmpty()) continue
            val payableCandidates = costByCandidate.keys.toList()

            val targetReqs = buildList {
                addAll(cardDef.script.targetRequirements)
                cardDef.script.auraTarget?.let { add(it) }
            }
            val targetReqInfos = if (targetReqs.isEmpty()) {
                emptyList()
            } else {
                context.targetUtils.buildTargetInfos(state, playerId, targetReqs, cardId)
            }
            // A targeted emerge spell is only castable if every requirement has a legal target
            // right now (CR 601.2c).
            if (targetReqInfos.isNotEmpty() && !context.targetUtils.allRequirementsSatisfied(targetReqInfos)) {
                continue
            }

            val firstReq = targetReqs.firstOrNull()
            val firstReqInfo = targetReqInfos.firstOrNull()

            // Preview the line for the first candidate — the one the client pre-selects and the AI
            // takes. The real solve happens at execute() against the creature actually chosen, and
            // the client re-prices off `costAfterSacrifice` as soon as the player picks.
            val autoTapPreview = if (context.skipAutoTapPreview) null else {
                context.manaSolver
                    .solve(state, playerId, costByCandidate.values.first(), precomputedSources = cachedSources)
                    ?.sources?.map { it.entityId }
            }

            result.add(
                LegalAction(
                    actionType = "CastWithAlternativeCost",
                    // The reduction is not spelled out here on purpose: `manaCostString` +
                    // `additionalCostInfo.costAfterSacrifice` let the client render it as live
                    // arithmetic ("{5}{U} → as low as {2}{U}"), which a sentence inside the button
                    // label can only restate less clearly.
                    description = "Emerge ${cardComponent.name}",
                    action = CastSpell(
                        playerId = playerId,
                        cardId = cardId,
                        useAlternativeCost = true,
                        alternativeCostType = AlternativeCostType.EMERGE
                    ),
                    validTargets = firstReqInfo?.validTargets,
                    requiresTargets = firstReq != null,
                    targetCount = firstReqInfo?.maxTargets ?: 1,
                    minTargets = firstReq?.effectiveMinCount ?: (firstReq?.count ?: 1),
                    targetDescription = firstReq?.description,
                    targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                    manaCostString = baseCost.toString(),
                    additionalCostInfo = AdditionalCostData(
                        description = "a creature to sacrifice (its mana value reduces the emerge cost)",
                        costType = "SacrificePermanent",
                        validSacrificeTargets = payableCandidates,
                        sacrificeCount = 1,
                        costAfterSacrifice = costByCandidate.mapValues { (_, cost) -> cost.toString() }
                    ),
                    autoTapPreview = autoTapPreview
                )
            )
        }

        return result
    }
}
