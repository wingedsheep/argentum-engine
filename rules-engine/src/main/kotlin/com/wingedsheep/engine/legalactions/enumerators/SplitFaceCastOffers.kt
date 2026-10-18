package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.legalactions.surfacedRequirements
import com.wingedsheep.engine.legalactions.utils.TargetEnumerationUtils
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardFace
import com.wingedsheep.sdk.model.EntityId

/**
 * The cast offer for one half of a split-layout card (CR 709.3 — only the chosen half goes on the
 * stack and is evaluated for legality). Shared by the hand enumerator ([CastSpellEnumerator]) and
 * the graveyard's aftermath offer ([CastFromZoneEnumerator]), which differ only in where the card
 * is and so in the [sourceZone] label the client groups the offer under.
 *
 * Each face has its own mana cost, type line (so timing is per-face), and — crucially — its own
 * `targetRequirements`. Reading the face's targets here is what makes targeted split halves
 * (Pain // Suffering, Stand // Deliver, Wax // Wane) actually offer their targets to the client;
 * without it the cast surfaces with no valid targets even though the validation layer
 * (`CastSpellHandler`) reads from the face script and expects them.
 *
 * Modal effects, alternative costs, blight, behold, kicker, and convoke on a split half are not yet
 * wired up — cards that need them can extend this later.
 */
internal object SplitFaceCastOffers {

    fun offer(
        context: EnumerationContext,
        cardId: EntityId,
        cardDef: CardDefinition,
        faceIndex: Int?,
        face: CardFace,
        result: MutableList<LegalAction>,
        sourceZone: String? = null,
        descriptionSuffix: String = "",
        castFused: Boolean = false,
        useWithoutPayingManaCost: Boolean = false,
    ) {
        val state = context.state
        val playerId = context.playerId
        val description = "Cast ${face.name}$descriptionSuffix"

        // Per-face timing: the chosen half's own type line governs sorcery-speed restriction.
        if (!face.typeLine.isInstant && !context.canPlaySorcerySpeed) return

        val cachedSources = context.availableManaSources
        val free = useWithoutPayingManaCost || state.getEntity(cardId)?.has<com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent>() == true
        val faceDef = context.costCalculator.faceCharacteristics(cardDef, face)
        // A paid half is priced by its own target count; a free cast pays no mana cost to price.
        val pricing = if (free) null else context.costCalculator.priceByTargets(
            state, faceDef, playerId, cardId, withTable = !context.skipAutoTapPreview,
            fromZone = if (sourceZone == "GRAVEYARD") com.wingedsheep.sdk.core.Zone.GRAVEYARD else null, card = cardDef,
        )
        val effectiveCost = pricing?.min ?: context.costCalculator.calculateFreeCastCost(state,
            faceDef, playerId, fromZone = com.wingedsheep.sdk.core.Zone.HAND, card = cardDef)
        val costByTargetCount = pricing?.byCountStrings()
        if (!context.manaSolver.canPay(state, playerId, effectiveCost, precomputedSources = cachedSources)) return

        val autoTapPreview = if (context.skipAutoTapPreview) null else {
            context.manaSolver
                .solve(state, playerId, effectiveCost, precomputedSources = cachedSources)
                ?.sources?.map { it.entityId }
        }
        val manaCostString = effectiveCost.toString()
        val hasXCost = effectiveCost.hasX
        val maxAffordableX = if (hasXCost) {
            val available = context.manaSolver.getAvailableManaCount(state, playerId, precomputedSources = cachedSources)
            ((available - effectiveCost.cmc) / effectiveCost.xCount.coerceAtLeast(1)).coerceAtLeast(0)
        } else null


        val targetReqs = face.script.targetRequirements
        if (targetReqs.isEmpty()) {
            result.add(
                LegalAction(
                    actionType = "CastSpell",
                    description = description,
                    action = CastSpell(playerId, cardId, faceIndex = faceIndex, castFused = castFused, useWithoutPayingManaCost = useWithoutPayingManaCost),
                    manaCostString = manaCostString,
                    manaCostByTargetCount = costByTargetCount,
                    autoTapPreview = autoTapPreview,
                    hasXCost = hasXCost,
                    maxAffordableX = maxAffordableX,
                    sourceZone = sourceZone,
                )
            )
            return
        }

        val targetInfos = context.targetUtils.buildSpellTargetInfos(state, playerId, targetReqs, cardId)
        if (!context.targetUtils.allRequirementsSatisfied(targetInfos)) return
        val firstReq = targetReqs.first()
        val firstInfo = targetInfos.first()

        // Auto-select a sole legal player target (e.g. "target player" in a 2-player game where
        // only one player is legal), matching the normal cast path's UX.
        val canAutoSelect = targetReqs.size == 1 &&
            TargetEnumerationUtils.shouldAutoSelectPlayerTarget(firstReq, firstInfo.validTargets)
        if (canAutoSelect) {
            val autoTarget = ChosenTarget.Player(firstInfo.validTargets.first())
            result.add(
                LegalAction(
                    actionType = "CastSpell",
                    description = description,
                    action = CastSpell(playerId, cardId, faceIndex = faceIndex, targets = listOf(autoTarget), castFused = castFused, useWithoutPayingManaCost = useWithoutPayingManaCost),
                    manaCostString = manaCostString,
                    manaCostByTargetCount = costByTargetCount,
                    autoTapPreview = autoTapPreview,
                    hasXCost = hasXCost,
                    maxAffordableX = maxAffordableX,
                    sourceZone = sourceZone,
                )
            )
            return
        }

        result.add(
            LegalAction(
                actionType = "CastSpell",
                description = description,
                action = CastSpell(playerId, cardId, faceIndex = faceIndex, castFused = castFused, useWithoutPayingManaCost = useWithoutPayingManaCost),
                validTargets = firstInfo.validTargets,
                requiresTargets = true,
                targetCount = firstInfo.maxTargets,
                minTargets = firstReq.effectiveMinCount,
                targetDescription = firstReq.description,
                targetRequirements = targetInfos.surfacedRequirements(),
                manaCostString = manaCostString,
                manaCostByTargetCount = costByTargetCount,
                autoTapPreview = autoTapPreview,
                hasXCost = hasXCost,
                maxAffordableX = maxAffordableX,
                sourceZone = sourceZone,
            )
        )
    }
}
