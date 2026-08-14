package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.mechanics.WebSlinging
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Keyword

/**
 * Enumerates the "cast for its web-slinging cost" legal action (CR 702.188).
 *
 * Web-slinging is an alternative cost bundling a non-mana portion — returning a tapped creature you
 * control to its owner's hand — with the [cost] mana. Unlike the ninjutsu family
 * ([SneakCastEnumerator]) it grants **no** timing permission, so the option is only offered at the
 * spell's *normal* timing (sorcery speed for creatures; any priority for Spider-Sense, an instant).
 * The normal [CastSpellEnumerator] still offers the ordinary cast alongside this one; the player
 * chooses which cost applies (CR 118.9a). Handled as its own enumerator rather than a pass in the
 * main cast path so the tapped-creature bounce surfacing stays out of that already-dense flow —
 * mirroring how Sneak is enumerated separately.
 *
 * The action it emits is a `CastWithAlternativeCost` carrying [AlternativeCostType.WEB_SLINGING].
 * The mana portion is the web-slinging [cost]; the non-mana portion is surfaced as a
 * `BouncePermanent` additional cost the player resolves, then submits as
 * `CastSpell.additionalCostPayment.bouncedPermanents`.
 */
class WebSlingingCastEnumerator : ActionEnumerator {

    override fun enumerate(context: EnumerationContext): List<LegalAction> {
        val state = context.state
        val playerId = context.playerId

        // The bounce pool is the same for every web-slinging card in hand; compute once. With no
        // tapped creature to return, the web-slinging cost can never be paid — bail early.
        val bounceTargets = WebSlinging.tappedCreaturesYouControl(state, playerId)
        if (bounceTargets.isEmpty()) return emptyList()

        val result = mutableListOf<LegalAction>()
        val cachedSources = context.availableManaSources

        for (cardId in state.getHand(playerId)) {
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
            if (cardComponent.typeLine.isLand) continue
            if (context.cantCastSpell(cardId)) continue

            val cardDef = context.cardRegistry.getCard(cardComponent.name) ?: continue
            val webSlinging = WebSlinging.effectiveWebSlinging(
                state, cardId, cardDef, playerId, context.cardRegistry, context.predicateEvaluator
            ) ?: continue

            // Web-slinging carries no timing permission (CR 702.188) — respect the card's normal
            // timing exactly like the ordinary cast path: non-instants without flash need sorcery
            // speed. Spider-Sense (an instant) is castable whenever the player has priority.
            val isInstant = cardComponent.typeLine.isInstant
            val grantedFlash = cardDef.keywords.contains(Keyword.FLASH) ||
                context.castPermissionUtils.hasGrantedFlash(state, cardId)
            if (!isInstant && !grantedFlash && !context.canPlaySorcerySpeed) continue

            // Honor cast restrictions exactly like the normal cast path (CR 601.3).
            if (!context.castPermissionUtils.checkCastRestrictions(
                    state, playerId, cardDef.script.castRestrictions
                )
            ) continue

            // Mana portion of the web-slinging cost (the bounce is paid alongside). Cost increases
            // and reductions still apply on top of the alternative cost (CR 118.9d).
            val webSlingMana = context.costCalculator.calculateEffectiveCostWithAlternativeBase(
                state, cardDef, webSlinging.cost, playerId
            )
            if (!context.manaSolver.canPay(state, playerId, webSlingMana, precomputedSources = cachedSources)) {
                continue
            }

            val targetReqs = buildList {
                addAll(cardDef.script.targetRequirements)
                cardDef.script.auraTarget?.let { add(it) }
            }
            val targetReqInfos = if (targetReqs.isEmpty()) {
                emptyList()
            } else {
                context.targetUtils.buildTargetInfos(state, playerId, targetReqs, cardId)
            }
            // A targeted web-slinging spell (e.g. Spider-Sense) is only castable if every
            // requirement has a legal target right now (CR 601.2c).
            if (targetReqInfos.isNotEmpty() && !context.targetUtils.allRequirementsSatisfied(targetReqInfos)) {
                continue
            }

            val firstReq = targetReqs.firstOrNull()
            val firstReqInfo = targetReqInfos.firstOrNull()

            val autoTapPreview = if (context.skipAutoTapPreview) null else {
                context.manaSolver.solve(state, playerId, webSlingMana, precomputedSources = cachedSources)
                    ?.sources?.map { it.entityId }
            }

            val bounceCostInfo = AdditionalCostData(
                description = "a tapped creature you control to return to its owner's hand",
                costType = "BouncePermanent",
                validBounceTargets = bounceTargets,
                bounceCount = 1
            )

            result.add(
                LegalAction(
                    actionType = "CastWithAlternativeCost",
                    description = "Web-slinging ${cardComponent.name} ($webSlingMana)",
                    action = CastSpell(
                        playerId = playerId,
                        cardId = cardId,
                        useAlternativeCost = true,
                        alternativeCostType = AlternativeCostType.WEB_SLINGING
                    ),
                    validTargets = firstReqInfo?.validTargets,
                    requiresTargets = firstReq != null,
                    targetCount = firstReqInfo?.maxTargets ?: 1,
                    minTargets = firstReq?.effectiveMinCount ?: (firstReq?.count ?: 1),
                    targetDescription = firstReq?.description,
                    targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                    manaCostString = webSlingMana.toString(),
                    additionalCostInfo = bounceCostInfo,
                    autoTapPreview = autoTapPreview
                )
            )
        }

        return result
    }
}
