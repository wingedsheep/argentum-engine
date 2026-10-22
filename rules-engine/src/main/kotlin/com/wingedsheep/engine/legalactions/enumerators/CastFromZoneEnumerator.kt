package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.state.LifePaymentPurpose

import com.wingedsheep.engine.state.components.battlefield.liveStaticAbilities
import com.wingedsheep.engine.handlers.actions.spell.chosenKickersLabel
import com.wingedsheep.engine.handlers.actions.spell.MAX_OPTIONAL_COST_REPEATS
import com.wingedsheep.engine.handlers.actions.spell.optionalCostDeclarations
import com.wingedsheep.engine.handlers.actions.spell.optionalCostsManaPaid
import com.wingedsheep.engine.legalactions.surfacedRequirements
import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.mechanics.cost.PlayerCounterPayment
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GraveyardCastRiderSelection
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.legalactions.ConvokeCreatureData
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.battlefield.MayCastFromGraveyardUsedThisTurnComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.permissions.activeMayPlayFor
import com.wingedsheep.engine.state.permissions.hasMayPlayFor
import com.wingedsheep.engine.state.components.player.MayCastCreaturesFromGraveyardWithForageComponent
import com.wingedsheep.engine.state.components.identity.PlayWithAdditionalCostComponent
import com.wingedsheep.engine.state.components.identity.PlayWithCostIncreaseComponent
import com.wingedsheep.engine.state.components.identity.PlayWithFixedAlternativeManaCostComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.legalactions.utils.TargetEnumerationUtils
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GrantMayCastFromLinkedExile
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard
import com.wingedsheep.engine.state.components.identity.emblemStaticAbilitiesOf
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import com.wingedsheep.sdk.scripting.effects.DividedDamageEffect
import com.wingedsheep.engine.handlers.actions.spell.CastZoneResolver
import com.wingedsheep.engine.mechanics.DisturbCasts
import com.wingedsheep.engine.mechanics.FlashbackGrants
import com.wingedsheep.engine.mechanics.AftermathCasts
import com.wingedsheep.engine.mechanics.HarmonizeGrants
import com.wingedsheep.engine.mechanics.cost.spell.SpellCostEnumeration
import com.wingedsheep.engine.mechanics.cost.spell.SpellCosts
import com.wingedsheep.engine.mechanics.MayhemGrants
import com.wingedsheep.engine.mechanics.EscapeCasts
import com.wingedsheep.engine.mechanics.WarpGrants
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.mechanics.mana.spellPaymentContextFor
import com.wingedsheep.engine.mechanics.mana.TapForGeneric
import com.wingedsheep.engine.state.components.stack.ChosenTarget

/**
 * Enumerates spells and lands castable/playable from non-hand zones:
 * - Top of library (PlayFromTopOfLibrary, CastSpellTypesFromTopOfLibrary)
 * - Exile (MayPlayPermission on the GameState)
 * - Linked exile (GrantMayCastFromLinkedExile)
 * - Intrinsic zone cast (MayCastSelfFromZones, e.g. Squee)
 * - Graveyard permanents (MayPlayPermanentsFromGraveyard, e.g. Muldrotha)
 */
class CastFromZoneEnumerator(
    private val predicateEvaluator: PredicateEvaluator
) : ActionEnumerator {

    override fun enumerate(context: EnumerationContext): List<LegalAction> {
        val result = mutableListOf<LegalAction>()
        val state = context.state
        val playerId = context.playerId

        // Avatar's Wrath: "your opponents can't cast spells from anywhere other than their hands."
        // Every path this enumerator offers is a non-hand cast (library top, exile, linked exile,
        // graveyard, flashback, harmonize, command zone), so suppress the whole enumerator while
        // the player is restricted to hand-only casting. Hand casts (CastSpellEnumerator) are
        // unaffected; CastSpellHandler re-checks this authoritatively.
        if (context.restrictedToHandCasting) return result

        enumerateTopOfLibrary(context, result)
        val linkedExileCardIds = enumerateExileCards(context, result)
        enumerateLinkedExile(context, result, linkedExileCardIds)
        enumerateIntrinsicZoneCast(context, result, linkedExileCardIds)
        enumerateGraveyardPermanents(context, result)
        enumerateGraveyardCreaturesWithForage(context, result)
        enumerateFlashback(context, result)
        enumerateAftermath(context, result)
        enumerateHarmonize(context, result)
        enumerateMayhem(context, result)
        enumerateEscape(context, result)
        enumerateDisturb(context, result)
        enumerateGraveyardCast(context, result)
        enumerateWarp(context, result)
        enumerateDash(context, result)
        enumerateBlitz(context, result)
        enumerateCommandZone(context, result)
        enumerateKickerForZoneCasts(context, result)

        val costs = com.wingedsheep.engine.handlers.actions.spell.CastAdditionalCosts(
            context.cardRegistry, context.costCalculator,
            com.wingedsheep.engine.handlers.actions.spell.CastZoneResolver(
                context.cardRegistry, context.conditionEvaluator, context.legality
            ), context.predicateEvaluator
        )
        // Improvise on a cast from exile, the graveyard or the command zone — printed, or granted
        // (Archway of Innovation's "the next spell you cast this turn has improvise" applies from
        // any zone) — gets the same tap-to-help metadata as a hand cast.
        return applyImproviseMetadata(context, result, creditAffordability = true).map { offer ->
            val action = offer.action as? CastSpell ?: return@map offer
            val card = state.getEntity(action.cardId)?.get<CardComponent>() ?: return@map offer
            val cardDef = context.cardRegistry.getCard(card.cardDefinitionId)
            val owed = costs.owedAdditionalCosts(state, action, cardDef)
            val counterMaxX = SpellCosts.additionalCostMaxX(state, playerId, owed, context.predicateEvaluator)
            val hasXCost = offer.hasXCost || counterMaxX != null
            val maxAffordableX = listOfNotNull(offer.maxAffordableX.takeIf { offer.hasXCost }, counterMaxX).minOrNull()
            // "If you cast this spell this way, X can't be 0" (Light Up the Night): the flashback
            // route floors X, so an offer whose ceiling is below the floor can't be cast.
            val minX = if (action.alternativeCostType == AlternativeCostType.FLASHBACK && cardDef != null) {
                FlashbackGrants.effectiveFlashback(state, action.cardId, cardDef, playerId, context.cardRegistry, context.predicateEvaluator)
                    ?.minimumXValue ?: 0
            } else offer.minX
            offer.copy(
                affordable = offer.affordable && PlayerCounterPayment.canAffordSpell(state, playerId, owed, action.cardId) &&
                    (!hasXCost || (maxAffordableX ?: 0) >= minX),
                hasXCost = hasXCost,
                maxAffordableX = maxAffordableX,
                minX = minX,
            )
        }
    }

    // =========================================================================
    // Command zone (Commander format)
    // =========================================================================

    /**
     * Enumerate casts of cards in the player's command zone tagged with [CommanderComponent].
     * Cost calculation already accounts for commander tax via `CostCalculator.priceByTargets`
     * with `fromZone = Zone.COMMAND`. Modeled on [enumerateIntrinsicZoneCast] (Squee-shaped).
     */
    private fun enumerateCommandZone(
        context: EnumerationContext,
        result: MutableList<LegalAction>,
    ) {
        val state = context.state
        val playerId = context.playerId
        val commandZone = state.getZone(ZoneKey(playerId, Zone.COMMAND))
        if (commandZone.isEmpty()) return

        for (cardId in commandZone) {
            val container = state.getEntity(cardId) ?: continue
            val commanderComponent = container.get<CommanderComponent>() ?: continue
            if (commanderComponent.ownerId != playerId) continue

            val cardComponent = container.get<CardComponent>() ?: continue
            // Lands don't normally exist in the command zone, but skip defensively.
            if (cardComponent.typeLine.isLand) continue

            val cardDef = context.cardRegistry.getCard(cardComponent.name) ?: continue

            val isInstant = cardComponent.typeLine.isInstant
            val hasFlash = cardDef.keywords.contains(com.wingedsheep.sdk.core.Keyword.FLASH) ||
                context.castPermissionUtils.hasGrantedFlash(state, cardId, context.playerId)
            if (!isInstant && !hasFlash && !context.canPlaySorcerySpeed) continue
            if (context.cantCastSpell(cardId)) continue

            val castRestrictions = cardDef.script.castRestrictions
            if (!context.legality.castRestrictionsMet(state, playerId, castRestrictions)) continue

            val pricing = context.costCalculator.priceByTargets(
                state, cardDef, playerId, cardId, withTable = !context.skipAutoTapPreview, fromZone = Zone.COMMAND,
            )
            val effectiveCost = pricing.min
            val costByTargetCount = pricing.byCountStrings()
            val cachedSources = context.availableManaSources
            val canAfford = context.manaSolver.canPay(
                state, playerId, effectiveCost, precomputedSources = cachedSources,
            )

            val targetReqs = buildList {
                addAll(cardDef.script.targetRequirements)
                cardDef.script.castAuraTarget?.let { add(it) }
            }

            val manaCostString = effectiveCost.toString()
            val hasXCost = effectiveCost.hasX
            val maxAffordableX: Int? = if (hasXCost) {
                val availableSources = context.manaSolver.getAvailableManaCount(
                    state, playerId, precomputedSources = cachedSources,
                )
                val fixedCost = effectiveCost.cmc
                val xSymbolCount = effectiveCost.xCount.coerceAtLeast(1)
                ((availableSources - fixedCost) / xSymbolCount).coerceAtLeast(0)
            } else null
            val autoTapPreview = if (context.skipAutoTapPreview) null else {
                context.manaSolver.solve(state, playerId, effectiveCost, precomputedSources = cachedSources)
                    ?.sources?.map { it.entityId }
            }

            if (targetReqs.isNotEmpty()) {
                val targetInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs, targetingSourceType = TargetingSourceType.SPELL)
                val allSatisfied = context.targetUtils.allRequirementsSatisfied(targetInfos)
                if (canAfford && allSatisfied) {
                    val firstReq = targetReqs.first()
                    val firstInfo = targetInfos.first()
                    result.add(
                        LegalAction(
                            actionType = "CastSpell",
                            description = "Cast ${cardComponent.name}",
                            action = CastSpell(playerId, cardId),
                            validTargets = firstInfo.validTargets,
                            requiresTargets = true,
                            targetCount = firstInfo.maxTargets,
                            minTargets = firstReq.effectiveMinCount,
                            targetDescription = firstReq.description,
                            targetRequirements = targetInfos.surfacedRequirements(),
                            xConstrainsTargetManaValue = firstInfo.xConstrainsManaValue,
                            xConstrainsTargetManaValueExactly = firstInfo.xConstrainsManaValueExactly,
                            xConstrainsTargetPower = firstInfo.xConstrainsPower,
                            xConstrainsTargetCount = firstInfo.xConstrainsCount,
                            xConstrainsTargetCountExactly = firstInfo.xConstrainsCountExactly,
                            hasXCost = hasXCost,
                            maxAffordableX = maxAffordableX,
                            manaCostString = manaCostString,
                            manaCostByTargetCount = costByTargetCount,
                            autoTapPreview = autoTapPreview,
                            sourceZone = "COMMAND",
                        )
                    )
                }
            } else if (canAfford) {
                result.add(
                    LegalAction(
                        actionType = "CastSpell",
                        description = "Cast ${cardComponent.name}",
                        action = CastSpell(playerId, cardId),
                        hasXCost = hasXCost,
                        maxAffordableX = maxAffordableX,
                        manaCostString = manaCostString,
                        manaCostByTargetCount = costByTargetCount,
                        autoTapPreview = autoTapPreview,
                        sourceZone = "COMMAND",
                    )
                )
            }
        }
    }

    // =========================================================================
    // Top of library
    // =========================================================================

    private fun enumerateTopOfLibrary(context: EnumerationContext, result: MutableList<LegalAction>) {
        val state = context.state
        val playerId = context.playerId

        val canPlayAllFromTop = context.castPermissionUtils.hasPlayFromTopOfLibrary(state, playerId)
        // One entry per granting permanent: a source-relative filter (Cemetery Illuminator's
        // "shares a card type with a card exiled with this creature") has to be evaluated against
        // the permanent that granted it, so the pairs can't be folded into a single filter.
        val castFromTopGrants = if (!canPlayAllFromTop) {
            context.castPermissionUtils.getCastFromTopOfLibraryGrants(state, playerId)
        } else emptyList()
        val castFilteredFromTopFilters = if (!canPlayAllFromTop) {
            context.castPermissionUtils.getCastFilteredFromTopOfLibraryFilters(state, playerId)
        } else emptyList()

        val library = state.getLibrary(playerId)
        if (library.isEmpty()) return

        val topCardId = library.first()
        val topCardComponent = state.getEntity(topCardId)?.get<CardComponent>() ?: return
        val topCardDef = context.cardRegistry.getCard(topCardComponent.name)
        // Per card, not per player: a land permission can be narrowed by a filter (Isu's snow lands).
        val canPlayLandsFromTop = canPlayAllFromTop ||
            context.castPermissionUtils.hasPlayLandsFromTopOfLibrary(state, playerId, topCardId)

        if (!canPlayAllFromTop && castFromTopGrants.isEmpty() && !canPlayLandsFromTop && castFilteredFromTopFilters.isEmpty()) return

        // Land on top of library (PlayFromTopOfLibrary or PlayLandsAndCastFilteredFromTopOfLibrary)
        if (canPlayLandsFromTop && topCardComponent.typeLine.isLand && context.canPlayLand) {
            result.add(
                LegalAction(
                    actionType = "PlayLand",
                    description = "Play ${topCardComponent.name}",
                    action = PlayLand(playerId, topCardId),
                    sourceZone = "LIBRARY"
                )
            )
        }

        // Non-land spell on top of library
        val matchingCastFromTopGrants = castFromTopGrants.filter { grant ->
            context.predicateEvaluator.matches(
                state, state.projectedState, topCardId, grant.filter,
                PredicateContext(controllerId = playerId, sourceId = grant.sourceId)
            )
        }
        val castFilteredFromTopMatches = castFilteredFromTopFilters.any { filter ->
            context.predicateEvaluator.matches(
                state, state.projectedState, topCardId, filter, PredicateContext(controllerId = playerId)
            )
        }
        val topCardMatchesFilter = canPlayAllFromTop || matchingCastFromTopGrants.isNotEmpty() || castFilteredFromTopMatches
        // Falco Spara: a costed cast-from-top permission owes its additional cost only when no
        // cost-free permission covers the card (mirrors CastZoneResolver.topOfLibraryGrantAdditionalCost).
        val costFreeTopCast = canPlayAllFromTop || castFilteredFromTopMatches ||
            matchingCastFromTopGrants.any { it.additionalCost == null } ||
            state.hasMayPlayFor(topCardId, playerId, context.conditionEvaluator, context.cardRegistry)
        val topGrantAdditionalCost = if (costFreeTopCast) null else matchingCastFromTopGrants.firstNotNullOfOrNull { it.additionalCost }

        if (!topCardComponent.typeLine.isLand && topCardMatchesFilter) {
            // Check timing
            val isInstant = topCardComponent.typeLine.isInstant
            if (isInstant || context.canPlaySorcerySpeed) {
                // Check cast restrictions
                val castRestrictions = topCardDef?.script?.castRestrictions ?: emptyList()
                if (context.legality.castRestrictionsMet(state, playerId, castRestrictions)) {
                    // Gwenom: a spell cast from the top under a PlayFromTopWithAlternativeCost
                    // permission pays no mana and instead pays life equal to its mana value.
                    val topAltCost = context.castPermissionUtils.playFromTopAlternativeCost(state, playerId)
                        ?.takeIf { grant ->
                            val grantFilter = grant.filter
                            grantFilter == null || context.predicateEvaluator.matches(
                                state, state.projectedState, topCardId, grantFilter,
                                PredicateContext(controllerId = playerId)
                            )
                        }
                    val freeCastFromTop = topAltCost?.withoutPayingManaCost == true
                    val payLifeMv = topAltCost?.additionalCost is AdditionalCost.PayLifeEqualToManaValueOfSpell
                    val lifeForThisCard = if (payLifeMv) topCardComponent.manaCost.cmc else 0
                    val lifeAffordable = !payLifeMv || context.canPayLife(lifeForThisCard)
                    val (grantCostInfo, canPayGrantCost) = if (topGrantAdditionalCost != null) {
                        presentOwedCosts(context, topCardId, listOf(topGrantAdditionalCost))
                    } else null to true
                    val topAltAdditionalCostInfo = if (payLifeMv) {
                        AdditionalCostData(description = "Pay $lifeForThisCard life", costType = "PayLife")
                    } else grantCostInfo

                    val topPricing = topCardDef?.let {
                        context.costCalculator.priceByTargets(
                            state, it, playerId, topCardId, withTable = !context.skipAutoTapPreview,
                            fromZone = Zone.LIBRARY,
                        )
                    }
                    val topEffectiveCost = topPricing?.min ?: topCardComponent.manaCost
                    val topCostByTargetCount = topPricing?.byCountStrings()
                    // "You can spend mana of any type to cast [these] spells" (Vizier of the
                    // Menagerie — whose own second ability is what makes a creature spell castable
                    // from here in the first place). Relaxed for payment only; the displayed
                    // `manaCostString` below stays the printed cost.
                    val topPayableCost = context.castPermissionUtils
                        .relaxSpellCostColorsIfAny(state, playerId, topCardId, topEffectiveCost)
                    val cachedSources = context.availableManaSources
                    // A free cast still owes the cost increases (CR 601.2f).
                    val topFreeCost by lazy { context.freeCastCost(topCardDef, Zone.LIBRARY) }
                    val canAfford = (if (freeCastFromTop) context.canPayMana(topFreeCost) else
                        context.manaSolver.canPay(state, playerId, topPayableCost, precomputedSources = cachedSources)) &&
                        lifeAffordable && canPayGrantCost
                    // A battlefield `MayCastWithoutPayingManaCost` source whose zones include the
                    // library (One with the Multiverse) frees this cast. Offered as its own variant
                    // (CR 118.9a) alongside the pay path; X is treated as 0 (CR 107.3b). Not stacked
                    // on a top-of-library alternative cost, which is already the cast's alternative.
                    val freeCastFromSource = topAltCost == null &&
                        context.freeCastPermissionFor(topCardId, Zone.LIBRARY) && context.canPayMana(topFreeCost)
                    val targetReqs = buildList {
                        addAll(topCardDef?.script?.targetRequirements ?: emptyList())
                        topCardDef?.script?.castAuraTarget?.let { add(it) }
                    }
                    val targetInfos = if (targetReqs.isNotEmpty() && (canAfford || freeCastFromSource)) {
                        context.targetUtils.buildTargetInfos(state, playerId, targetReqs, targetingSourceType = TargetingSourceType.SPELL)
                    } else emptyList()
                    val targetsSatisfied = targetReqs.isEmpty() || context.targetUtils.allRequirementsSatisfied(targetInfos)

                    fun castAction(free: Boolean, manaCostString: String, hasXCost: Boolean, maxAffordableX: Int?, autoTapPreview: List<EntityId>?): LegalAction {
                        val action = CastSpell(playerId, topCardId, useWithoutPayingManaCost = free)
                        // Gwenom's pay-life cast and a free cast pay no mana cost to price per target.
                        val costByTargetCount = if (free || freeCastFromTop) null else topCostByTargetCount
                        val description = if (free) "Cast ${topCardComponent.name} (Free)" else "Cast ${topCardComponent.name}"
                        // A permission's additional cost (Falco Spara) is still owed on a free cast;
                        // Gwenom's pay-life is the alternative cost a free cast replaces.
                        val additionalCostInfo = if (free) grantCostInfo else topAltAdditionalCostInfo
                        if (targetReqs.isEmpty()) {
                            return LegalAction(
                                actionType = "CastSpell",
                                description = description,
                                action = action,
                                hasXCost = hasXCost,
                                maxAffordableX = maxAffordableX,
                                manaCostString = manaCostString,
                                manaCostByTargetCount = costByTargetCount,
                                autoTapPreview = autoTapPreview,
                                sourceZone = "LIBRARY",
                                additionalCostInfo = additionalCostInfo
                            )
                        }
                        val firstReq = targetReqs.first()
                        val firstInfo = targetInfos.first()
                        return LegalAction(
                            actionType = "CastSpell",
                            description = description,
                            action = action,
                            validTargets = firstInfo.validTargets,
                            requiresTargets = true,
                            targetCount = firstInfo.maxTargets,
                            minTargets = firstReq.effectiveMinCount,
                            targetDescription = firstReq.description,
                            targetRequirements = targetInfos.surfacedRequirements(),
                            xConstrainsTargetManaValue = firstInfo.xConstrainsManaValue,
                            xConstrainsTargetManaValueExactly = firstInfo.xConstrainsManaValueExactly,
                            xConstrainsTargetPower = firstInfo.xConstrainsPower,
                            xConstrainsTargetCount = firstInfo.xConstrainsCount,
                            xConstrainsTargetCountExactly = firstInfo.xConstrainsCountExactly,
                            hasXCost = hasXCost,
                            maxAffordableX = maxAffordableX,
                            manaCostString = manaCostString,
                            manaCostByTargetCount = costByTargetCount,
                            autoTapPreview = autoTapPreview,
                            sourceZone = "LIBRARY",
                            additionalCostInfo = additionalCostInfo
                        )
                    }

                    if (canAfford && targetsSatisfied) {
                        val manaCostString = if (freeCastFromTop) freeCostString(topFreeCost) else topEffectiveCost.toString()
                        val hasXCost = topEffectiveCost.hasX
                        val maxAffordableX: Int? = if (hasXCost) {
                            val availableSources = context.manaSolver.getAvailableManaCount(state, playerId, precomputedSources = cachedSources)
                            val fixedCost = topEffectiveCost.cmc
                            val xSymbolCount = topEffectiveCost.xCount.coerceAtLeast(1)
                            ((availableSources - fixedCost) / xSymbolCount).coerceAtLeast(0)
                        } else null
                        val autoTapPreview = if (context.skipAutoTapPreview) null else {
                            context.manaSolver.solve(state, playerId, if (freeCastFromTop) topFreeCost else topPayableCost, precomputedSources = cachedSources)
                                ?.sources?.map { it.entityId }
                        }
                        result.add(castAction(free = false, manaCostString, hasXCost, maxAffordableX, autoTapPreview))
                    }
                    if (freeCastFromSource && canPayGrantCost && targetsSatisfied) {
                        result.add(castAction(free = true, freeCostString(topFreeCost), hasXCost = false, maxAffordableX = null,
                            autoTapPreview = freeCostPreview(context, topFreeCost)))
                    }
                }
            }

            // Check for a face-down cast on top of library (only for PlayFromTopOfLibrary) —
            // morph (CR 702.37a) and disguise (CR 702.168a) both allow it.
            if (canPlayAllFromTop && context.canPlaySorcerySpeed && topCardDef != null) {
                val castableFaceDown = topCardDef.keywordAbilities.any {
                    it is KeywordAbility.Morph || it is KeywordAbility.Disguise
                }
                if (castableFaceDown) {
                    val morphCost = context.costCalculator.calculateFaceDownCost(state, playerId)
                    val canAffordMorph = context.manaSolver.canPay(
                        state, playerId, morphCost,
                        // CR 708.2 — nameless 2/2 creature spell, so "spend this mana only to cast
                        // face-down spells" (Tin Street Gossip) counts here.
                        spellContext = SpellPaymentContext.faceDownCast(isFromHand = false),
                        precomputedSources = context.availableManaSources
                    )
                    result.add(
                        LegalAction(
                            actionType = "CastFaceDown",
                            description = "Cast ${topCardComponent.name} face-down",
                            action = CastSpell(playerId, topCardId, castFaceDown = true),
                            affordable = canAffordMorph,
                            manaCostString = morphCost.toString(),
                            sourceZone = "LIBRARY"
                        )
                    )
                }
            }
        }
    }

    // =========================================================================
    // Cards from exile (MayPlayPermission)
    // =========================================================================

    private fun enumerateExileCards(
        context: EnumerationContext,
        result: MutableList<LegalAction>
    ): MutableSet<EntityId> {
        val state = context.state
        val playerId = context.playerId
        val linkedExileCardIds = mutableSetOf<EntityId>()
        // Cards whose from-exile/graveyard cast may convoke (printed, or granted — Hoarding
        // Broodlord's "Spells you cast from exile have convoke"), with the creatures that can tap.
        val convokeByCard = mutableMapOf<EntityId, List<ConvokeCreatureData>>()

        // Check all players' exile zones because cards like Villainous Wealth exile from
        // an opponent's library (cards stay in their owner's exile zone). Graveyards
        // are included so per-card grants that leave the discarded card in the graveyard
        // (e.g. Malcolm, Alluring Scoundrel) are castable without a detour through exile.
        val exileCandidates = state.turnOrder.flatMap { pid -> state.getExile(pid).map { it to "EXILE" } }
        val graveyardCandidates = state.turnOrder.flatMap { pid -> state.getGraveyard(pid).map { it to "GRAVEYARD" } }
        for ((cardId, sourceZoneLabel) in exileCandidates + graveyardCandidates) {
            val container = state.getEntity(cardId) ?: continue
            val permissions = state.activeMayPlayFor(cardId, playerId, context.conditionEvaluator, context.cardRegistry)
            if (permissions.isEmpty()) continue
            val playForFree = container.get<PlayWithoutPayingCostComponent>()?.controllerId == playerId
            val runtimeAdditionalCost = container.get<PlayWithAdditionalCostComponent>()
                ?.takeIf { it.controllerId == playerId }
            val runtimeCostIncrease = container.get<PlayWithCostIncreaseComponent>()
                ?.takeIf { it.controllerId == playerId }
            val cardComponent = container.get<CardComponent>() ?: continue

            // Land with play permission — skipped when every applicable permission is
            // nonLandOnly ("you may cast that card" wording never covers a land's play-as-a-
            // special-action, CR 305.1; only "you may play" wording does).
            if (cardComponent.typeLine.isLand && permissions.any { !it.nonLandOnly }) {
                result.add(
                    LegalAction(
                        actionType = "PlayLand",
                        description = "Play ${cardComponent.name}",
                        action = PlayLand(playerId, cardId),
                        affordable = context.canPlayLand,
                        sourceZone = sourceZoneLabel
                    )
                )
            }

            // Non-land spell with play permission
            if (!cardComponent.typeLine.isLand) {
                if (context.cantCastSpell(cardId)) {
                    val costString = if (playForFree) "{0}" else cardComponent.manaCost.toString()
                    result.add(
                        LegalAction(
                            actionType = "CastSpell",
                            description = "Cast ${cardComponent.name}",
                            action = CastSpell(playerId, cardId),
                            affordable = false,
                            manaCostString = costString,
                            sourceZone = sourceZoneLabel
                        )
                    )
                } else {
                    val cardDef = context.cardRegistry.getCard(cardComponent.name)
                    // Prepare-spell copy (Secrets of Strixhaven): this exiled copy is cast as the
                    // card's prepare spell — face index 0. Read the face's script for timing,
                    // restrictions, cost, and targets, and thread faceIndex onto the CastSpell so
                    // the handler/resolver use the prepare spell's characteristics.
                    //
                    // The same face-swap covers any permission that authorizes an alternative
                    // face rather than the card's primary characteristics — "you may cast it
                    // from your graveyard as an Adventure" (Mosswood Dreadknight, CR 715.3)
                    // grants `castFaceIndex = 0`, so only the Adventure half is offered and the
                    // creature half stays uncastable from the graveyard.
                    val prepareCopyFaceIndex: Int? =
                        if (container.has<com.wingedsheep.engine.state.components.battlefield.PreparedSpellCopyComponent>() &&
                            cardDef?.layout == com.wingedsheep.sdk.model.CardLayout.PREPARE
                        ) 0
                        else permissions.firstNotNullOfOrNull { it.castFaceIndex }
                            ?.takeIf { cardDef?.cardFaces?.getOrNull(it) != null }
                    val prepareFace = prepareCopyFaceIndex?.let { cardDef?.cardFaces?.getOrNull(it) }
                    // "You may cast red spells from among them" (Chandra, Dressed to Kill −7): the
                    // colour is checked against the face actually being cast, not the exiled card,
                    // so a red MDFC's blue back face stays uncastable through this permission. A
                    // permission with no restriction authorizes any colour, and only *one* of the
                    // applicable permissions needs to authorize the cast.
                    val castColors = prepareFace?.manaCost?.colors ?: cardDef?.colors ?: cardComponent.manaCost.colors
                    if (permissions.none { it.authorizesSpellColors(castColors) }) {
                        continue
                    }
                    val castName = prepareFace?.name ?: cardComponent.name
                    val effectiveScript = prepareFace?.script ?: cardDef?.script
                    val effectiveTypeLine = prepareFace?.typeLine ?: cardComponent.typeLine
                    val isInstant = effectiveTypeLine.isInstant
                    // A may-play permission with an "as though it had flash" rider (Azula, Cunning
                    // Usurper) unlocks instant-speed casting of even a sorcery/creature exiled with it.
                    val grantsFlashTiming = permissions.any { it.asThoughFlash }
                    val hasCorrectTiming = isInstant || grantsFlashTiming || context.canPlaySorcerySpeed
                    val castRestrictions = effectiveScript?.castRestrictions ?: emptyList()
                    val meetsRestrictions = context.legality.castRestrictionsMet(state, playerId, castRestrictions)
                    val castZone = if (sourceZoneLabel == "EXILE") Zone.EXILE else Zone.GRAVEYARD
                    // The paid cast, priced by its target count (a prepare-spell copy is its face).
                    // A fixed alternative cost (below) replaces this price, so it carries no table.
                    val runtimeFixedAltCost = container.get<PlayWithFixedAlternativeManaCostComponent>()
                        ?.takeIf { it.controllerId == playerId }
                    val paidPricing = if (playForFree || cardDef == null) null else
                        context.costCalculator.priceByTargets(
                            state,
                            if (prepareFace != null) context.costCalculator.faceCharacteristics(cardDef, prepareFace) else cardDef,
                            playerId, cardId,
                            withTable = !context.skipAutoTapPreview && runtimeFixedAltCost == null,
                            fromZone = castZone, card = cardDef,
                        )
                    // A free cast still owes the cost increases (CR 601.2f).
                    val baseEffectiveCost = if (playForFree) {
                        context.freeCastCost(
                            if (cardDef != null && prepareFace != null) context.costCalculator.faceCharacteristics(cardDef, prepareFace) else cardDef,
                            castZone,
                        )
                    } else if (paidPricing != null) {
                        paidPricing.min
                    } else {
                        cardComponent.manaCost
                    }
                    // Airbend: a fixed alternative cost ({2}) is paid *instead of* the printed
                    // cost — it replaces the base. A generic tax (Soul Partition / Thalia-style
                    // cost increase) is not part of what it replaces, so it still applies on top.
                    val baseCost = if (!playForFree && runtimeFixedAltCost != null) {
                        runtimeFixedAltCost.fixedCost
                    } else {
                        baseEffectiveCost
                    }
                    // Mana of any type: either the may-play permission itself carries the rider
                    // (Taster of Wares, Tinybones) or a blanket static covers this spell wherever
                    // it is cast from (Vizier of the Menagerie).
                    val anyManaType = permissions.any { it.withAnyManaType } ||
                        context.castPermissionUtils.canSpendAnyManaTypeForSpell(state, playerId, cardId)
                    // "As though it were mana of any color" (Stolen Strategy) relaxes only the
                    // colored pips — a {C} pip still needs colorless (CR 609.4b).
                    fun permitted(cost: ManaCost): ManaCost {
                        val relaxed = when {
                            anyManaType -> cost.relaxColors()
                            permissions.any { it.withAnyManaColor } -> cost.relaxColoredRequirements()
                            else -> cost
                        }
                        return if (runtimeCostIncrease != null) relaxed + runtimeCostIncrease.cost else relaxed
                    }
                    val effectiveCost = permitted(baseCost)
                    val costString = if (playForFree) freeCostString(effectiveCost) else effectiveCost.toString()
                    val costByTargetCount = paidPricing?.byCount?.mapValues { permitted(it.value).toString() }
                    // Hama, the Bloodbender: the fixed alternative cost is a *waterbend* cost, so its
                    // whole generic can be paid by tapping artifacts/creatures. Fold the tap help into
                    // affordability (CR 701.67); the tap metadata is attached to the emitted actions
                    // by the post-pass at the end of this method.
                    val fixedAltWaterbend = runtimeFixedAltCost?.takeIf { !playForFree && it.waterbend }
                    val spellContext = spellPaymentContextFor(
                        cardComponent,
                        isFromExile = sourceZoneLabel == "EXILE",
                        isFromHand = false,
                        isFromGraveyard = cardId in state.getGraveyard(playerId),
                    ).copy(colorlessAsAnyColor = permissions.any { it.colorlessAsAnyColor })
                    // Convoke (CR 702.51) — printed, or granted by a zone-scoped grant keyed to the
                    // zone this card is cast from. A free cast has nothing for convoke to pay.
                    val convokeCreatures = if (!playForFree && cardDef != null &&
                        context.grantedKeywordResolver.hasKeyword(state, playerId, cardDef, Keyword.CONVOKE, cardId)
                    ) {
                        context.costUtils.findConvokeCreatures(state, playerId).also { convokeByCard[cardId] = it }
                    } else emptyList()
                    val canAfford = if (playForFree) context.canPayMana(effectiveCost, spellContext) else
                        context.manaSolver.canPay(
                            state, playerId, effectiveCost,
                            precomputedSources = context.availableManaSources, spellContext = spellContext
                        ) ||
                        (convokeCreatures.isNotEmpty() && context.costUtils.canAffordWithConvoke(
                            state, playerId, effectiveCost, convokeCreatures,
                            precomputedSources = context.availableManaSources, spellContext = spellContext
                        )) ||
                        (fixedAltWaterbend != null && context.costUtils.canAffordWithTapForGeneric(
                            state, playerId, effectiveCost,
                            context.costUtils.findTapForGenericPermanents(state, playerId, TapForGeneric.WATERBEND)
                                .take(fixedAltWaterbend.fixedCost.genericAmount),
                            precomputedSources = context.availableManaSources,
                            spellContext = spellContext
                        ))

                    // Calculate X cost info if the spell has X in its cost (cost still paid even with may-play)
                    val hasXCost = !playForFree && effectiveCost.hasX
                    val maxAffordableX: Int? = if (hasXCost) {
                        val availableSources = context.manaSolver.getAvailableManaCount(state, playerId, precomputedSources = context.availableManaSources)
                        val fixedCost = effectiveCost.cmc  // X contributes 0 to CMC
                        val xSymbolCount = effectiveCost.xCount.coerceAtLeast(1)
                        ((availableSources - fixedCost) / xSymbolCount).coerceAtLeast(0)
                    } else null

                    // Build additional cost info from runtime component
                    val exileAdditionalCostInfo = runtimeAdditionalCost?.let { comp ->
                        buildRuntimeAdditionalCostInfo(state, playerId, cardId, comp)
                    }
                    val canPayAdditionalCost = exileAdditionalCostInfo == null ||
                        checkRuntimeAdditionalCostAffordability(state, playerId, cardId, runtimeAdditionalCost, context.reservedLife)

                    // Honour the card's printed BlightOrPay additional cost when casting from exile
                    // (e.g. Cinder Strike granted via Sanar's "you may cast" permission). If the
                    // card has a payable blight path, emit a second legal action with
                    // costType = "Blight" so the client surfaces both the pay and blight options.
                    val printedBlightOrPay = effectiveScript?.additionalCosts
                        ?.flatMap { if (it is AdditionalCost.Composite) it.steps else listOf(it) }
                        ?.filterIsInstance<AdditionalCost.BlightOrPay>()
                        ?.firstOrNull()
                    val blightCreatures = if (printedBlightOrPay != null) {
                        val projected = state.projectedState
                        projected.getBattlefieldControlledBy(playerId)
                            .filter { projected.isCreature(it) && projected.canReceiveCounters(it) }
                    } else emptyList()
                    val canAffordBlightPath = printedBlightOrPay != null &&
                        blightCreatures.isNotEmpty() &&
                        !playForFree

                    // A battlefield `MayCastWithoutPayingManaCost(fromZones = setOf(EXILE))` source
                    // (Warped Space) lets this exile spell be cast for {0}. Offered as its own
                    // variant (CR 118.9a) alongside the pay path; X is treated as 0 (CR 107.3b).
                    // Excludes prepare-spell copies and cards already granted a free play.
                    val freeFromExileCost by lazy { context.freeCastCost(cardDef, Zone.EXILE) }
                    val freeCastFromExile = !playForFree &&
                        prepareCopyFaceIndex == null &&
                        hasCorrectTiming && meetsRestrictions && canPayAdditionalCost &&
                        context.freeCastPermissionFor(cardId, Zone.EXILE) && context.canPayMana(freeFromExileCost, spellContext)

                    if (hasCorrectTiming && meetsRestrictions && canAfford && canPayAdditionalCost) {
                        val targetReqs = buildList {
                            addAll(effectiveScript?.targetRequirements ?: emptyList())
                            effectiveScript?.castAuraTarget?.let { add(it) }
                        }

                        if (targetReqs.isNotEmpty()) {
                            val targetInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs, targetingSourceType = TargetingSourceType.SPELL)
                            val allSatisfied = context.targetUtils.allRequirementsSatisfied(targetInfos)
                            if (allSatisfied) {
                                val firstReq = targetReqs.first()
                                val firstInfo = targetInfos.first()
                                // Emit the pay path first.
                                result.add(
                                    LegalAction(
                                        actionType = "CastSpell",
                                        description = "Cast ${castName}",
                                        action = CastSpell(playerId, cardId, faceIndex = prepareCopyFaceIndex),
                                        validTargets = firstInfo.validTargets,
                                        requiresTargets = true,
                                        targetCount = firstInfo.maxTargets,
                                        minTargets = firstReq.effectiveMinCount,
                                        targetDescription = firstReq.description,
                                        targetRequirements = targetInfos.surfacedRequirements(),
                                        xConstrainsTargetManaValue = firstInfo.xConstrainsManaValue,
                                        xConstrainsTargetManaValueExactly = firstInfo.xConstrainsManaValueExactly,
                                        xConstrainsTargetPower = firstInfo.xConstrainsPower,
                                        xConstrainsTargetCount = firstInfo.xConstrainsCount,
                                        xConstrainsTargetCountExactly = firstInfo.xConstrainsCountExactly,
                                        manaCostString = costString,
                                        manaCostByTargetCount = costByTargetCount,
                                        hasXCost = hasXCost,
                                        maxAffordableX = maxAffordableX,
                                        sourceZone = sourceZoneLabel,
                                        additionalCostInfo = exileAdditionalCostInfo
                                    )
                                )
                                // Emit the blight path as a second action when the card has
                                // a printed BlightOrPay and the player controls a creature.
                                if (canAffordBlightPath) {
                                    result.add(
                                        LegalAction(
                                            actionType = "CastSpell",
                                            description = "Cast ${castName} (Blight ${printedBlightOrPay.blightAmount})",
                                            action = CastSpell(playerId, cardId, faceIndex = prepareCopyFaceIndex),
                                            validTargets = firstInfo.validTargets,
                                            requiresTargets = true,
                                            targetCount = firstInfo.maxTargets,
                                            minTargets = firstReq.effectiveMinCount,
                                            targetDescription = firstReq.description,
                                            targetRequirements = targetInfos.surfacedRequirements(),
                                            manaCostString = costString,
                                            manaCostByTargetCount = costByTargetCount,
                                            hasXCost = hasXCost,
                                            maxAffordableX = maxAffordableX,
                                            sourceZone = sourceZoneLabel,
                                            additionalCostInfo = AdditionalCostData(
                                                description = "creature to blight",
                                                costType = "Blight",
                                                validBlightTargets = blightCreatures,
                                                blightAmount = printedBlightOrPay.blightAmount
                                            )
                                        )
                                    )
                                }
                            }
                        } else {
                            result.add(
                                LegalAction(
                                    actionType = "CastSpell",
                                    description = "Cast ${castName}",
                                    action = CastSpell(playerId, cardId, faceIndex = prepareCopyFaceIndex),
                                    manaCostString = costString,
                                    manaCostByTargetCount = costByTargetCount,
                                    hasXCost = hasXCost,
                                    maxAffordableX = maxAffordableX,
                                    sourceZone = sourceZoneLabel,
                                    additionalCostInfo = exileAdditionalCostInfo
                                )
                            )
                            if (canAffordBlightPath) {
                                result.add(
                                    LegalAction(
                                        actionType = "CastSpell",
                                        description = "Cast ${castName} (Blight ${printedBlightOrPay.blightAmount})",
                                        action = CastSpell(playerId, cardId, faceIndex = prepareCopyFaceIndex),
                                        manaCostString = costString,
                                        manaCostByTargetCount = costByTargetCount,
                                        hasXCost = hasXCost,
                                        maxAffordableX = maxAffordableX,
                                        sourceZone = sourceZoneLabel,
                                        additionalCostInfo = AdditionalCostData(
                                            description = "creature to blight",
                                            costType = "Blight",
                                            validBlightTargets = blightCreatures,
                                            blightAmount = printedBlightOrPay.blightAmount
                                        )
                                    )
                                )
                            }
                        }
                    } else if (!freeCastFromExile) {
                        // Can't cast right now — show as unaffordable (unless a free-from-exile
                        // cast is available below, which is the affordable path the player wants).
                        result.add(
                            LegalAction(
                                actionType = "CastSpell",
                                description = "Cast ${castName}",
                                action = CastSpell(playerId, cardId, faceIndex = prepareCopyFaceIndex),
                                affordable = false,
                                manaCostString = costString,
                                manaCostByTargetCount = costByTargetCount,
                                hasXCost = hasXCost,
                                maxAffordableX = maxAffordableX,
                                sourceZone = sourceZoneLabel,
                                additionalCostInfo = exileAdditionalCostInfo,
                            )
                        )
                    }

                    // Warped Space: emit the {0} free-from-exile variant (its own action so the
                    // player chooses it over the pay path — CR 118.9a).
                    if (freeCastFromExile) {
                        val freeTargetReqs = buildList {
                            addAll(effectiveScript?.targetRequirements ?: emptyList())
                            effectiveScript?.castAuraTarget?.let { add(it) }
                        }
                        if (freeTargetReqs.isNotEmpty()) {
                            val freeTargetInfos = context.targetUtils.buildTargetInfos(state, playerId, freeTargetReqs, targetingSourceType = TargetingSourceType.SPELL)
                            if (context.targetUtils.allRequirementsSatisfied(freeTargetInfos)) {
                                val firstReq = freeTargetReqs.first()
                                val firstInfo = freeTargetInfos.first()
                                result.add(
                                    LegalAction(
                                        actionType = "CastSpell",
                                        description = "Cast ${castName} (Free)",
                                        action = CastSpell(playerId, cardId, faceIndex = prepareCopyFaceIndex, useWithoutPayingManaCost = true),
                                        validTargets = firstInfo.validTargets,
                                        requiresTargets = true,
                                        targetCount = firstInfo.maxTargets,
                                        minTargets = firstReq.effectiveMinCount,
                                        targetDescription = firstReq.description,
                                        targetRequirements = freeTargetInfos.surfacedRequirements(),
                                        manaCostString = freeCostString(freeFromExileCost),
                                        autoTapPreview = freeCostPreview(context, freeFromExileCost, spellContext),
                                        sourceZone = sourceZoneLabel
                                    )
                                )
                            }
                        } else {
                            result.add(
                                LegalAction(
                                    actionType = "CastSpell",
                                    description = "Cast ${castName} (Free)",
                                    action = CastSpell(playerId, cardId, faceIndex = prepareCopyFaceIndex, useWithoutPayingManaCost = true),
                                    manaCostString = freeCostString(freeFromExileCost),
                                    autoTapPreview = freeCostPreview(context, freeFromExileCost, spellContext),
                                    sourceZone = sourceZoneLabel
                                )
                            )
                        }
                    }
                }
            }
        }

        // Hama, the Bloodbender: attach waterbend tap metadata to every from-exile cast whose fixed
        // alternative cost is a waterbend cost, so the client surfaces the tap-to-help option and the
        // eligible permanents (mirrors CastSpellEnumerator.applySpellWaterbendMetadata for spell-level
        // waterbend). The handler reads the waterbend flag off the card component, so no action-field
        // change is needed beyond the display metadata.
        for (i in result.indices) {
            val la = result[i]
            val cs = la.action as? CastSpell ?: continue
            if (la.actionType != "CastSpell") continue
            // Convoke metadata for the paid variants, so the client offers the creature-tap step
            // (mirrors what CastSpellEnumerator attaches to a hand cast).
            if (!cs.useWithoutPayingManaCost) {
                convokeByCard[cs.cardId]?.let { creatures ->
                    result[i] = la.copy(hasConvoke = true, convokeCreatures = creatures)
                }
            }
            val fixedAlt = state.getEntity(cs.cardId)
                ?.get<PlayWithFixedAlternativeManaCostComponent>()
                ?.takeIf { it.controllerId == playerId && it.waterbend } ?: continue
            result[i] = result[i].copy(
                hasTapForGeneric = true,
                tapForGenericPermanents =
                    context.costUtils.findTapForGenericPermanents(state, playerId, TapForGeneric.WATERBEND),
                tapForGenericAmount = fixedAlt.fixedCost.genericAmount,
                tapForGenericLabel = TapForGeneric.WATERBEND.label
            )
        }

        return linkedExileCardIds
    }

    // =========================================================================
    // Linked exile (GrantMayCastFromLinkedExile)
    // =========================================================================

    private fun enumerateLinkedExile(
        context: EnumerationContext,
        result: MutableList<LegalAction>,
        linkedExileCardIds: MutableSet<EntityId>
    ) {
        val state = context.state
        val playerId = context.playerId

        // Which permanents grant, and which of their exiled cards they admit, is the legality
        // kernel's answer — the same one CastSpellHandler and the client view read.
        for (granter in context.legality.linkedExileGranters(state, playerId)) {
            val entityId = granter.granterId
            val grantAbility = granter.ability

            for (exiledId in granter.exiledIds) {
                // Skip if already handled by a direct MayPlayPermission — but only when its
                // gate is open. A closed conditional gate must fall through so the linked-exile
                // path remains a viable independent permission source.
                val exiledContainer = state.getEntity(exiledId) ?: continue
                if (state.hasMayPlayFor(exiledId, playerId, context.conditionEvaluator, context.cardRegistry)) continue
                val exiledCard = exiledContainer.get<CardComponent>() ?: continue

                // Ownership, "exiled this turn", mana-value cap and card filter.
                if (!context.legality.linkedExileAdmits(state, playerId, granter, exiledId)) continue

                // Verify card is actually in exile
                val inExile = state.turnOrder.any { pid -> exiledId in state.getZone(ZoneKey(pid, Zone.EXILE)) }
                if (!inExile) continue
                if (exiledId in linkedExileCardIds) continue
                linkedExileCardIds.add(exiledId)

                // The granter's additional cost (e.g. "remove three counters from your creatures").
                val (linkedAdditionalCostInfo, canPayLinkedAdditionalCost) = presentOwedCosts(
                    context, exiledId, listOfNotNull(grantAbility.additionalCost)
                )

                // Lands in linked exile cannot be cast (oracle says "spells")
                if (exiledCard.typeLine.isLand) continue

                val exiledCardDef = context.cardRegistry.getCard(exiledCard.name)
                if (context.cantCastSpell(exiledId)) {
                    result.add(
                        LegalAction(
                            actionType = "CastSpell",
                            description = "Cast ${exiledCard.name}",
                            action = CastSpell(playerId, exiledId),
                            affordable = false,
                            manaCostString = exiledCard.manaCost.toString(),
                            sourceZone = "EXILE"
                        )
                    )
                } else {
                    val isInstant = exiledCard.typeLine.isInstant
                    val hasCorrectTiming = isInstant || context.canPlaySorcerySpeed ||
                        exiledCardDef?.keywords?.contains(com.wingedsheep.sdk.core.Keyword.FLASH) == true ||
                        context.castPermissionUtils.hasGrantedFlash(state, exiledId, context.playerId)
                    val castRestrictions = exiledCardDef?.script?.castRestrictions ?: emptyList()
                    val meetsRestrictions = context.legality.castRestrictionsMet(state, playerId, castRestrictions)
                    val freeCastFromGranter = grantAbility.withoutPayingManaCost
                    val pricing = if (freeCastFromGranter || exiledCardDef == null) null else
                        context.costCalculator.priceByTargets(
                            state, exiledCardDef, playerId, exiledId,
                            withTable = !context.skipAutoTapPreview, fromZone = Zone.EXILE,
                        )
                    // "…and mana of any type can be spent to cast that spell" (Null Summoner,
                    // CR 609.4b) — the same relaxation CastCostPayer applies at payment.
                    val anyManaType = grantAbility.withAnyManaType ||
                        context.castPermissionUtils.canSpendAnyManaTypeForSpell(state, playerId, exiledId)
                    fun permitted(cost: ManaCost): ManaCost = if (anyManaType) cost.relaxColors() else cost
                    val effectiveCost = if (freeCastFromGranter) null else permitted(pricing?.min ?: exiledCard.manaCost)
                    val costString = effectiveCost?.toString() ?: "0"
                    val costByTargetCount = pricing?.byCount?.mapValues { permitted(it.value).toString() }
                    val canAfford = effectiveCost == null ||
                        context.manaSolver.canPay(state, playerId, effectiveCost, precomputedSources = context.availableManaSources)

                    // "Pay life equal to its mana value rather than pay its mana cost" (Valgavoth):
                    // the life cost is per-card (the cast card's mana value), so its affordability
                    // and display are computed here rather than once-per-granter.
                    val payLifeMv = grantAbility.additionalCost is AdditionalCost.PayLifeEqualToManaValueOfSpell
                    val lifeForThisCard = if (payLifeMv) exiledCard.manaCost.cmc else 0
                    val lifeAffordable = !payLifeMv || context.canPayLife(lifeForThisCard)
                    val perCardAdditionalCostInfo = if (payLifeMv) {
                        AdditionalCostData(
                            description = "Pay $lifeForThisCard life",
                            costType = "PayLife"
                        )
                    } else {
                        linkedAdditionalCostInfo
                    }

                    val fullyAffordable = hasCorrectTiming && meetsRestrictions && canAfford &&
                        canPayLinkedAdditionalCost && lifeAffordable
                    if (fullyAffordable) {
                        val targetReqs = buildList {
                            addAll(exiledCardDef?.script?.targetRequirements ?: emptyList())
                            exiledCardDef?.script?.castAuraTarget?.let { add(it) }
                        }
                        if (targetReqs.isNotEmpty()) {
                            val targetInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs, targetingSourceType = TargetingSourceType.SPELL)
                            val allSatisfied = context.targetUtils.allRequirementsSatisfied(targetInfos)
                            if (allSatisfied) {
                                val firstReq = targetReqs.first()
                                val firstInfo = targetInfos.first()
                                result.add(
                                    LegalAction(
                                        actionType = "CastSpell",
                                        description = "Cast ${exiledCard.name}",
                                        action = CastSpell(playerId, exiledId),
                                        validTargets = firstInfo.validTargets,
                                        requiresTargets = true,
                                        targetCount = firstInfo.maxTargets,
                                        minTargets = firstReq.effectiveMinCount,
                                        targetDescription = firstReq.description,
                                        targetRequirements = targetInfos.surfacedRequirements(),
                                        xConstrainsTargetManaValue = firstInfo.xConstrainsManaValue,
                                        xConstrainsTargetManaValueExactly = firstInfo.xConstrainsManaValueExactly,
                                        xConstrainsTargetPower = firstInfo.xConstrainsPower,
                                        xConstrainsTargetCount = firstInfo.xConstrainsCount,
                                        xConstrainsTargetCountExactly = firstInfo.xConstrainsCountExactly,
                                        manaCostString = costString,
                                        manaCostByTargetCount = costByTargetCount,
                                        sourceZone = "EXILE",
                                        additionalCostInfo = perCardAdditionalCostInfo
                                    )
                                )
                            }
                        } else {
                            result.add(
                                LegalAction(
                                    actionType = "CastSpell",
                                    description = "Cast ${exiledCard.name}",
                                    action = CastSpell(playerId, exiledId),
                                    manaCostString = costString,
                                    manaCostByTargetCount = costByTargetCount,
                                    sourceZone = "EXILE",
                                    additionalCostInfo = perCardAdditionalCostInfo
                                )
                            )
                        }
                    } else {
                        result.add(
                            LegalAction(
                                actionType = "CastSpell",
                                description = "Cast ${exiledCard.name}",
                                action = CastSpell(playerId, exiledId),
                                affordable = false,
                                manaCostString = costString,
                                manaCostByTargetCount = costByTargetCount,
                                sourceZone = "EXILE",
                                additionalCostInfo = perCardAdditionalCostInfo
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * The picker payload and payability of the non-mana costs a cast from another zone owes, read
     * off the shared spell-cost seam so every cost kind brings its own picker and affordability
     * check. Composites are flattened so each step is checked on its own; the client drives one
     * picker per cast, so the first cost that has one supplies it.
     */
    private fun presentOwedCosts(
        context: EnumerationContext,
        castCardId: EntityId,
        costs: List<AdditionalCost>,
    ): Pair<AdditionalCostData?, Boolean> {
        val env = SpellCostEnumeration(context, castCardId)
        var info: AdditionalCostData? = null
        var payable = true
        for (cost in SpellCosts.flattenComposites(costs)) {
            val candidates = SpellCosts.candidates(env, cost)
            if (!SpellCosts.canPayFrom(env, cost, candidates)) payable = false
            if (info == null) info = SpellCosts.present(env, cost, candidates)?.second
        }
        return info to payable
    }

    // =========================================================================
    // Intrinsic MayCastSelfFromZones (e.g., Squee, the Immortal)
    // =========================================================================

    private fun enumerateIntrinsicZoneCast(
        context: EnumerationContext,
        result: MutableList<LegalAction>,
        linkedExileCardIds: Set<EntityId>
    ) {
        val state = context.state
        val playerId = context.playerId

        for (zone in listOf(Zone.GRAVEYARD, Zone.EXILE)) {
            val zoneCards = state.getZone(ZoneKey(playerId, zone))
            for (cardId in zoneCards) {
                val container = state.getEntity(cardId) ?: continue
                // Skip cards already handled by a direct MayPlayPermission (gate-open only;
                // a closed conditional gate must fall through to intrinsic cast permission)
                // or linked exile.
                if (zone == Zone.EXILE && state.hasMayPlayFor(cardId, playerId, context.conditionEvaluator, context.cardRegistry)) continue
                if (zone == Zone.EXILE && cardId in linkedExileCardIds) continue
                val cardComponent = container.get<CardComponent>() ?: continue
                val cardDef = context.cardRegistry.getCard(cardComponent.name) ?: continue
                // The card grants its own cast-from-zone permission only if some MayCastSelfFromZones
                // names this zone AND its optional condition (e.g. Undead Sprinter's "a non-Zombie
                // creature died this turn") currently holds in the casting player's context. A
                // permission restricted to one casting ability (`castUsing`, "using its bestow
                // ability") is offered by that ability's own enumerator, never as an ordinary cast.
                val zoneCastAbility = cardDef.script.staticAbilities
                    .filterIsInstance<MayCastSelfFromZones>()
                    .firstOrNull { ability ->
                        zone in ability.zones && ability.castUsing == null && (ability.condition == null ||
                            context.conditionEvaluator.evaluate(
                                state,
                                ability.condition!!,
                                com.wingedsheep.engine.handlers.EffectContext(
                                    sourceId = cardId, controllerId = playerId
                                )
                            ))
                    }
                if (zoneCastAbility == null) continue

                // Only non-land spells (Squee is a creature)
                if (cardComponent.typeLine.isLand) continue

                // Additional cost bundled with the permission (e.g. Alien Symbiosis: "by
                // discarding a card in addition to paying its other costs").
                val (zoneAdditionalCostInfo, canPayZoneAdditionalCost) = presentOwedCosts(
                    context, cardId, listOfNotNull(zoneCastAbility.additionalCost)
                )

                val sourceZoneName = zone.name
                // A priced permission is announced as its own alternative cost, so the same card
                // keeps any plain-cast route another permission (Muldrotha) gives it.
                val zoneCast = if (zoneCastAbility.alternativeManaCost != null) {
                    CastSpell(playerId, cardId, useAlternativeCost = true, alternativeCostType = AlternativeCostType.SELF_ZONE)
                } else {
                    CastSpell(playerId, cardId)
                }
                if (context.cantCastSpell(cardId)) {
                    result.add(
                        LegalAction(
                            actionType = "CastSpell",
                            description = "Cast ${cardComponent.name}",
                            action = zoneCast,
                            affordable = false,
                            manaCostString = (zoneCastAbility.alternativeManaCost ?: cardComponent.manaCost).toString(),
                            sourceZone = sourceZoneName,
                            additionalCostInfo = zoneAdditionalCostInfo
                        )
                    )
                } else {
                    val isInstant = cardComponent.typeLine.isInstant
                    val hasCorrectTiming = isInstant || context.canPlaySorcerySpeed
                    val castRestrictions = cardDef.script.castRestrictions
                    val meetsRestrictions = context.legality.castRestrictionsMet(state, playerId, castRestrictions)
                    // A permission that names its own price pays it rather than the mana cost.
                    val pricing = context.costCalculator.priceByTargets(
                        state, cardDef, playerId, cardId, baseCost = zoneCastAbility.alternativeManaCost ?: cardDef.manaCost,
                        withTable = !context.skipAutoTapPreview, fromZone = zone,
                    )
                    val effectiveCost = pricing.min
                    val costString = effectiveCost.toString()
                    val costByTargetCount = pricing.byCountStrings()
                    val canAfford = context.manaSolver.canPay(state, playerId, effectiveCost, precomputedSources = context.availableManaSources)

                    if (hasCorrectTiming && meetsRestrictions && canAfford && canPayZoneAdditionalCost) {
                        val targetReqs = buildList {
                            addAll(cardDef.script.targetRequirements)
                            cardDef.script.castAuraTarget?.let { add(it) }
                        }

                        if (targetReqs.isNotEmpty()) {
                            val targetInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs, targetingSourceType = TargetingSourceType.SPELL)
                            val allSatisfied = context.targetUtils.allRequirementsSatisfied(targetInfos)
                            if (allSatisfied) {
                                val firstReq = targetReqs.first()
                                val firstInfo = targetInfos.first()
                                result.add(
                                    LegalAction(
                                        actionType = "CastSpell",
                                        description = "Cast ${cardComponent.name}",
                                        action = zoneCast,
                                        validTargets = firstInfo.validTargets,
                                        requiresTargets = true,
                                        targetCount = firstInfo.maxTargets,
                                        minTargets = firstReq.effectiveMinCount,
                                        targetDescription = firstReq.description,
                                        targetRequirements = targetInfos.surfacedRequirements(),
                                        xConstrainsTargetManaValue = firstInfo.xConstrainsManaValue,
                                        xConstrainsTargetManaValueExactly = firstInfo.xConstrainsManaValueExactly,
                                        xConstrainsTargetPower = firstInfo.xConstrainsPower,
                                        xConstrainsTargetCount = firstInfo.xConstrainsCount,
                                        xConstrainsTargetCountExactly = firstInfo.xConstrainsCountExactly,
                                        manaCostString = costString,
                                        manaCostByTargetCount = costByTargetCount,
                                        sourceZone = sourceZoneName,
                                        additionalCostInfo = zoneAdditionalCostInfo
                                    )
                                )
                            }
                        } else {
                            result.add(
                                LegalAction(
                                    actionType = "CastSpell",
                                    description = "Cast ${cardComponent.name}",
                                    action = zoneCast,
                                    manaCostString = costString,
                                    manaCostByTargetCount = costByTargetCount,
                                    sourceZone = sourceZoneName,
                                    additionalCostInfo = zoneAdditionalCostInfo
                                )
                            )
                        }
                    } else {
                        result.add(
                            LegalAction(
                                actionType = "CastSpell",
                                description = "Cast ${cardComponent.name}",
                                action = zoneCast,
                                affordable = false,
                                manaCostString = costString,
                                manaCostByTargetCount = costByTargetCount,
                                sourceZone = sourceZoneName,
                                additionalCostInfo = zoneAdditionalCostInfo
                            )
                        )
                    }
                }
            }
        }
    }

    // =========================================================================
    // Graveyard permanents (MayPlayPermanentsFromGraveyard / Muldrotha)
    // =========================================================================

    private fun enumerateGraveyardPermanents(
        context: EnumerationContext,
        result: MutableList<LegalAction>
    ) {
        val state = context.state
        val playerId = context.playerId

        // Only the active player can use Muldrotha-style permissions
        if (!state.isActiveTurnFor(playerId)) return

        val graveyardCards = state.getZone(ZoneKey(playerId, Zone.GRAVEYARD))
        for (cardId in graveyardCards) {
            val container = state.getEntity(cardId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Only permanent spells (not lands — handled in PlayLandEnumerator; not instant/sorcery)
            if (cardComponent.typeLine.isLand) continue
            if (!cardComponent.typeLine.isPermanent) continue

            // Check if any permanent type on this card has available graveyard permission
            val permanentTypes = cardComponent.typeLine.cardTypes.filter { it.isPermanent }
            val hasPermission = permanentTypes.any { type ->
                context.castPermissionUtils.hasGraveyardPlayPermissionForType(state, playerId, type.name)
            }
            if (!hasPermission) continue

            val cardDef = context.cardRegistry.getCard(cardComponent.name) ?: continue

            if (context.cantCastSpell(cardId)) {
                result.add(
                    LegalAction(
                        actionType = "CastSpell",
                        description = "Cast ${cardComponent.name}",
                        action = CastSpell(playerId, cardId),
                        affordable = false,
                        manaCostString = cardComponent.manaCost.toString(),
                        sourceZone = "GRAVEYARD"
                    )
                )
            } else {
                val isInstant = cardComponent.typeLine.isInstant
                val hasCorrectTiming = isInstant || context.canPlaySorcerySpeed
                val castRestrictions = cardDef.script.castRestrictions
                val meetsRestrictions = context.legality.castRestrictionsMet(state, playerId, castRestrictions)
                val pricing = context.costCalculator.priceByTargets(
                    state, cardDef, playerId, cardId, withTable = !context.skipAutoTapPreview, fromZone = Zone.GRAVEYARD,
                )
                val effectiveCost = pricing.min
                val costString = effectiveCost.toString()
                val costByTargetCount = pricing.byCountStrings()
                val canAfford = context.manaSolver.canPay(state, playerId, effectiveCost, precomputedSources = context.availableManaSources)

                if (hasCorrectTiming && meetsRestrictions && canAfford) {
                    val targetReqs = buildList {
                        addAll(cardDef.script.targetRequirements)
                        cardDef.script.castAuraTarget?.let { add(it) }
                    }

                    if (targetReqs.isNotEmpty()) {
                        val targetInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs, targetingSourceType = TargetingSourceType.SPELL)
                        val allSatisfied = context.targetUtils.allRequirementsSatisfied(targetInfos)
                        if (allSatisfied) {
                            val firstReq = targetReqs.first()
                            val firstInfo = targetInfos.first()
                            result.add(
                                LegalAction(
                                    actionType = "CastSpell",
                                    description = "Cast ${cardComponent.name}",
                                    action = CastSpell(playerId, cardId),
                                    validTargets = firstInfo.validTargets,
                                    requiresTargets = true,
                                    targetCount = firstInfo.maxTargets,
                                    minTargets = firstReq.effectiveMinCount,
                                    targetDescription = firstReq.description,
                                    targetRequirements = targetInfos.surfacedRequirements(),
                                    manaCostString = costString,
                                    manaCostByTargetCount = costByTargetCount,
                                    sourceZone = "GRAVEYARD"
                                )
                            )
                        }
                    } else {
                        result.add(
                            LegalAction(
                                actionType = "CastSpell",
                                description = "Cast ${cardComponent.name}",
                                action = CastSpell(playerId, cardId),
                                manaCostString = costString,
                                manaCostByTargetCount = costByTargetCount,
                                sourceZone = "GRAVEYARD"
                            )
                        )
                    }
                } else {
                    result.add(
                        LegalAction(
                            actionType = "CastSpell",
                            description = "Cast ${cardComponent.name}",
                            action = CastSpell(playerId, cardId),
                            affordable = false,
                            manaCostString = costString,
                            manaCostByTargetCount = costByTargetCount,
                            sourceZone = "GRAVEYARD"
                        )
                    )
                }
            }
        }
    }

    // =========================================================================
    // Graveyard creatures with forage (Osteomancer Adept-style)
    // =========================================================================

    // =========================================================================
    // Aftermath (the aftermath half of a split card in your graveyard)
    // =========================================================================

    /**
     * Aftermath (CR 702.127a): "You may cast this half of this split card from your graveyard."
     * Offers only the aftermath half, for its own printed mana cost at the normal timing for its
     * type; the card's other half gains nothing here. The half's own target requirements and cast
     * restrictions apply, exactly as for a split half cast from hand.
     */
    private fun enumerateAftermath(
        context: EnumerationContext,
        result: MutableList<LegalAction>
    ) {
        val state = context.state
        val playerId = context.playerId
        for (cardId in state.getZone(ZoneKey(playerId, Zone.GRAVEYARD))) {
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
            val cardDef = context.cardRegistry.getCard(cardComponent.cardDefinitionId) ?: continue
            if (cardDef.layout != com.wingedsheep.sdk.model.CardLayout.SPLIT) continue
            cardDef.cardFaces.forEachIndexed { faceIndex, face ->
                if (!AftermathCasts.isAftermath(face)) return@forEachIndexed
                if (context.cantCastSpell(cardId)) return@forEachIndexed
                if (!context.legality.castRestrictionsMet(state, playerId, face.script.castRestrictions)) return@forEachIndexed
                SplitFaceCastOffers.offer(
                    context, cardId, cardDef, faceIndex, face, result,
                    sourceZone = "GRAVEYARD", descriptionSuffix = " (Aftermath)",
                )
            }
        }
    }

    // =========================================================================
    // Flashback (cards in graveyard with Flashback keyword)
    // =========================================================================

    private fun enumerateFlashback(
        context: EnumerationContext,
        result: MutableList<LegalAction>
    ) {
        val state = context.state
        val playerId = context.playerId
        val graveyardCards = state.getZone(ZoneKey(playerId, Zone.GRAVEYARD))

        for (cardId in graveyardCards) {
            val container = state.getEntity(cardId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            val cardDef = context.cardRegistry.getCard(cardComponent.cardDefinitionId) ?: continue

            // Flashback may be printed, granted per-entity (Archmage's Newt), or granted to the
            // whole graveyard by a battlefield static (Iroh, Grand Lotus).
            val flashback = FlashbackGrants.effectiveFlashback(
                state, cardId, cardDef, playerId, context.cardRegistry, context.predicateEvaluator
            ) ?: continue

            // Check timing: instants at instant speed, sorceries at sorcery speed
            val isInstant = cardComponent.typeLine.isInstant
            if (!isInstant && !context.canPlaySorcerySpeed) continue

            if (context.cantCastSpell(cardId)) {
                result.add(
                    LegalAction(
                        actionType = "CastWithFlashback",
                        description = "Cast ${cardComponent.name} (Flashback)",
                        action = CastSpell(playerId, cardId, useAlternativeCost = true, alternativeCostType = AlternativeCostType.FLASHBACK),
                        affordable = false,
                        manaCostString = flashback.cost.toString(),
                        sourceZone = "GRAVEYARD"
                    )
                )
                continue
            }

            // Check cast restrictions
            val castRestrictions = cardDef.script.castRestrictions
            if (!context.legality.castRestrictionsMet(state, playerId, castRestrictions)) continue

            // Calculate effective flashback cost (applying cost reductions/increases)
            val pricing = context.costCalculator.priceByTargets(
                state, cardDef, playerId, cardId, baseCost = flashback.cost, withTable = !context.skipAutoTapPreview,
                fromZone = Zone.GRAVEYARD,
            )
            val effectiveCost = pricing.min
            val costString = effectiveCost.toString()
            val costByTargetCount = pricing.byCountStrings()
            // Flashback casts from the caster's own graveyard, so conditional mana is judged with
            // that origin (Rootcoil Creeper's "cast spells from your graveyard").
            val spellContext = spellPaymentContextFor(cardComponent, isFromHand = false, isFromGraveyard = true)
            val canAfford = context.manaSolver.canPay(
                state, playerId, effectiveCost, precomputedSources = context.availableManaSources, spellContext = spellContext
            )

            // A flashback cast owes the card's printed additional costs as well as flashback's own
            // bundled one ("its flashback cost and any additional costs").
            val (flashbackCostInfo, canPayAdditionalCost) = presentOwedCosts(
                context, cardId, cardDef.script.additionalCosts + listOfNotNull(flashback.additionalCost)
            )

            if (!canAfford || !canPayAdditionalCost) {
                result.add(
                    LegalAction(
                        actionType = "CastWithFlashback",
                        description = "Cast ${cardComponent.name} (Flashback)",
                        action = CastSpell(playerId, cardId, useAlternativeCost = true, alternativeCostType = AlternativeCostType.FLASHBACK),
                        affordable = false,
                        manaCostString = costString,
                        manaCostByTargetCount = costByTargetCount,
                        additionalCostInfo = flashbackCostInfo,
                        sourceZone = "GRAVEYARD"
                    )
                )
                continue
            }

            val targetReqs = buildList {
                addAll(cardDef.script.targetRequirements)
                cardDef.script.castAuraTarget?.let { add(it) }
            }

            val autoTapPreview = if (context.skipAutoTapPreview) null else {
                context.manaSolver.solve(
                    state, playerId, effectiveCost, precomputedSources = context.availableManaSources, spellContext = spellContext
                )?.sources?.map { it.entityId }
            }

            if (targetReqs.isNotEmpty()) {
                val targetInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs, targetingSourceType = TargetingSourceType.SPELL)
                val allSatisfied = context.targetUtils.allRequirementsSatisfied(targetInfos)
                if (allSatisfied) {
                    val firstReq = targetReqs.first()
                    val firstInfo = targetInfos.first()
                    result.add(
                        LegalAction(
                            actionType = "CastWithFlashback",
                            description = "Cast ${cardComponent.name} (Flashback)",
                            action = CastSpell(playerId, cardId, useAlternativeCost = true, alternativeCostType = AlternativeCostType.FLASHBACK),
                            validTargets = firstInfo.validTargets,
                            requiresTargets = true,
                            targetCount = firstInfo.maxTargets,
                            minTargets = firstReq.effectiveMinCount,
                            targetDescription = firstReq.description,
                            targetRequirements = targetInfos.surfacedRequirements(),
                            manaCostString = costString,
                            manaCostByTargetCount = costByTargetCount,
                            additionalCostInfo = flashbackCostInfo,
                            autoTapPreview = autoTapPreview,
                            sourceZone = "GRAVEYARD"
                        )
                    )
                }
            } else {
                result.add(
                    LegalAction(
                        actionType = "CastWithFlashback",
                        description = "Cast ${cardComponent.name} (Flashback)",
                        action = CastSpell(playerId, cardId, useAlternativeCost = true, alternativeCostType = AlternativeCostType.FLASHBACK),
                        manaCostString = costString,
                        manaCostByTargetCount = costByTargetCount,
                        additionalCostInfo = flashbackCostInfo,
                        autoTapPreview = autoTapPreview,
                        sourceZone = "GRAVEYARD"
                    )
                )
            }
        }
    }

    // =========================================================================
    // Mayhem (cards in graveyard with the Mayhem keyword, discarded this turn)
    // =========================================================================

    /**
     * Mayhem (CR 702.187): cast a card from your graveyard for its Mayhem cost, but only if you
     * discarded it this turn. Grants no timing permission (normal timing — sorcery speed unless the
     * card is an instant or has flash) and, unlike Flashback/Harmonize, does NOT exile the spell on
     * resolution (handled by the absence of any Mayhem branch in `StackResolver`). Lands with the
     * no-cost Mayhem form (CR 702.187c, Oscorp Industries) are *played*, not cast, and are handled
     * by the land-play path — skipped here.
     */
    private fun enumerateMayhem(
        context: EnumerationContext,
        result: MutableList<LegalAction>
    ) {
        val state = context.state
        val playerId = context.playerId
        val graveyardCards = state.getZone(ZoneKey(playerId, Zone.GRAVEYARD))
        val discardedThisTurn = state.getEntity(playerId)
            ?.get<com.wingedsheep.engine.state.components.player.CardsDiscardedThisTurnComponent>()
            ?.cardIds ?: emptyList()
        if (discardedThisTurn.isEmpty()) return

        for (cardId in graveyardCards) {
            // The Mayhem gate (CR 702.187b): "as long as you discarded this card this turn".
            if (cardId !in discardedThisTurn) continue

            val container = state.getEntity(cardId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            // Lands are played (CR 702.187c), not cast — handled elsewhere.
            if (cardComponent.typeLine.isLand) continue
            val cardDef = context.cardRegistry.getCard(cardComponent.cardDefinitionId) ?: continue

            val mayhem = MayhemGrants.effectiveMayhem(
                state, cardId, cardDef, playerId, context.cardRegistry, context.predicateEvaluator
            ) ?: continue

            // Timing: Mayhem grants no permission — instants/flash any time, else sorcery speed.
            val isInstant = cardComponent.typeLine.isInstant
            val hasFlash = cardDef.keywords.contains(com.wingedsheep.sdk.core.Keyword.FLASH) ||
                context.castPermissionUtils.hasGrantedFlash(state, cardId, context.playerId)
            if (!isInstant && !hasFlash && !context.canPlaySorcerySpeed) continue

            if (context.cantCastSpell(cardId)) {
                result.add(
                    LegalAction(
                        actionType = "CastWithMayhem",
                        description = "Cast ${cardComponent.name} (Mayhem)",
                        action = CastSpell(playerId, cardId, useAlternativeCost = true, alternativeCostType = AlternativeCostType.MAYHEM),
                        affordable = false,
                        manaCostString = mayhem.cost.toString(),
                        sourceZone = "GRAVEYARD"
                    )
                )
                continue
            }

            val castRestrictions = cardDef.script.castRestrictions
            if (!context.legality.castRestrictionsMet(state, playerId, castRestrictions)) continue

            val pricing = context.costCalculator.priceByTargets(

                state, cardDef, playerId, cardId, baseCost = mayhem.cost, withTable = !context.skipAutoTapPreview,
                fromZone = Zone.GRAVEYARD,

            )

            val effectiveCost = pricing.min
            val costString = effectiveCost.toString()
            val costByTargetCount = pricing.byCountStrings()
            val canAfford = context.manaSolver.canPay(state, playerId, effectiveCost, precomputedSources = context.availableManaSources)

            if (!canAfford) {
                result.add(
                    LegalAction(
                        actionType = "CastWithMayhem",
                        description = "Cast ${cardComponent.name} (Mayhem)",
                        action = CastSpell(playerId, cardId, useAlternativeCost = true, alternativeCostType = AlternativeCostType.MAYHEM),
                        affordable = false,
                        manaCostString = costString,
                        manaCostByTargetCount = costByTargetCount,
                        sourceZone = "GRAVEYARD"
                    )
                )
                continue
            }

            val targetReqs = buildList {
                addAll(cardDef.script.targetRequirements)
                cardDef.script.castAuraTarget?.let { add(it) }
            }

            val autoTapPreview = if (context.skipAutoTapPreview) null else {
                context.manaSolver.solve(state, playerId, effectiveCost, precomputedSources = context.availableManaSources)
                    ?.sources?.map { it.entityId }
            }

            if (targetReqs.isNotEmpty()) {
                val targetInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs, targetingSourceType = TargetingSourceType.SPELL)
                val allSatisfied = context.targetUtils.allRequirementsSatisfied(targetInfos)
                if (allSatisfied) {
                    val firstReq = targetReqs.first()
                    val firstInfo = targetInfos.first()
                    result.add(
                        LegalAction(
                            actionType = "CastWithMayhem",
                            description = "Cast ${cardComponent.name} (Mayhem)",
                            action = CastSpell(playerId, cardId, useAlternativeCost = true, alternativeCostType = AlternativeCostType.MAYHEM),
                            validTargets = firstInfo.validTargets,
                            requiresTargets = true,
                            targetCount = firstInfo.maxTargets,
                            minTargets = firstReq.effectiveMinCount,
                            targetDescription = firstReq.description,
                            targetRequirements = targetInfos.surfacedRequirements(),
                            manaCostString = costString,
                            manaCostByTargetCount = costByTargetCount,
                            autoTapPreview = autoTapPreview,
                            sourceZone = "GRAVEYARD"
                        )
                    )
                }
            } else {
                result.add(
                    LegalAction(
                        actionType = "CastWithMayhem",
                        description = "Cast ${cardComponent.name} (Mayhem)",
                        action = CastSpell(playerId, cardId, useAlternativeCost = true, alternativeCostType = AlternativeCostType.MAYHEM),
                        manaCostString = costString,
                        manaCostByTargetCount = costByTargetCount,
                        autoTapPreview = autoTapPreview,
                        sourceZone = "GRAVEYARD"
                    )
                )
            }
        }
    }

    // =========================================================================
    // Escape (cards in graveyard with the Escape keyword)
    // =========================================================================

    /**
     * Escape (CR 702.138a): cast a card from your graveyard for its escape cost — the escape mana
     * plus the bundled non-mana half (usually "exile N other cards from your graveyard"), on top of
     * any additional costs the card itself prints. Grants no timing permission (instants and flash
     * cards any time, everything else at sorcery speed) and, like Mayhem, does NOT exile the spell
     * on resolution. The exile pool never includes the card being cast (`SpellCostEnumeration`
     * excludes it), so a graveyard holding the card and four others can't pay "exile five other".
     */
    private fun enumerateEscape(
        context: EnumerationContext,
        result: MutableList<LegalAction>
    ) {
        val state = context.state
        val playerId = context.playerId

        for (cardId in state.getZone(ZoneKey(playerId, Zone.GRAVEYARD))) {
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
            if (cardComponent.typeLine.isLand) continue
            val cardDef = context.cardRegistry.getCard(cardComponent.cardDefinitionId) ?: continue
            val escapeOptions = EscapeCasts.availableEscape(
                state, cardId, cardDef, playerId, context.cardRegistry, context.predicateEvaluator
            )
            if (escapeOptions.isEmpty()) continue
            val faceIndices: List<Int?> = listOf(null) + if (
                cardDef.layout == com.wingedsheep.sdk.model.CardLayout.ADVENTURE ||
                cardDef.layout == com.wingedsheep.sdk.model.CardLayout.OMEN
            ) cardDef.cardFaces.indices.toList() else emptyList()
            for (escape in escapeOptions) {
                for (faceIndex in faceIndices) {
                    val spell = faceIndex?.let {
                        context.costCalculator.faceCharacteristics(cardDef, cardDef.cardFaces[it])
                    } ?: cardDef
                    val isInstant = spell.typeLine.isInstant
                    val hasFlash = spell.keywords.contains(com.wingedsheep.sdk.core.Keyword.FLASH) ||
                        context.castPermissionUtils.hasGrantedFlash(state, cardId, context.playerId)
                    if (!isInstant && !hasFlash && !context.canPlaySorcerySpeed) continue

                    val action = CastSpell(playerId, cardId, useAlternativeCost = true, alternativeCostType = AlternativeCostType.ESCAPE, escapeAbility = escape, faceIndex = faceIndex)
                    fun offer(
                        affordable: Boolean,
                        costString: String,
                        costInfo: com.wingedsheep.engine.legalactions.AdditionalCostData? = null,
                        costByTargetCount: Map<Int, String>? = null,
                    ) = LegalAction(
                        actionType = "CastWithEscape",
                        description = "Cast ${spell.name} (Escape: ${escape.cost}${escape.additionalCost?.let { "; ${it.description}" }.orEmpty()})",
                        action = action,
                        affordable = affordable,
                        manaCostString = costString,
                        manaCostByTargetCount = costByTargetCount,
                        additionalCostInfo = costInfo,
                        sourceZone = "GRAVEYARD"
                    )

                    if (context.cantCastSpell(cardId)) {
                        result.add(offer(affordable = false, costString = escape.cost.toString()))
                        continue
                    }
                    if (!context.legality.castRestrictionsMet(state, playerId, spell.script.castRestrictions)) continue

                    val pricing = context.costCalculator.priceByTargets(
                        state, spell, playerId, cardId, baseCost = escape.cost, withTable = !context.skipAutoTapPreview,
                        fromZone = Zone.GRAVEYARD, card = cardDef,
                    )
                    val effectiveCost = pricing.min
                    val costString = effectiveCost.toString()
                    val costByTargetCount = pricing.byCountStrings()
                    val canAfford = context.manaSolver.canPay(state, playerId, effectiveCost, precomputedSources = context.availableManaSources)
                    val (escapeCostInfo, canPayAdditionalCost) = presentOwedCosts(
                        context, cardId, spell.script.additionalCosts + listOfNotNull(escape.additionalCost)
                    )
                    if (!canAfford || !canPayAdditionalCost) {
                        result.add(offer(affordable = false, costString = costString, costInfo = escapeCostInfo, costByTargetCount = costByTargetCount))
                        continue
                    }

                    val autoTapPreview = if (context.skipAutoTapPreview) null else {
                        context.manaSolver.solve(state, playerId, effectiveCost, precomputedSources = context.availableManaSources)
                            ?.sources?.map { it.entityId }
                    }
                    val targetReqs = buildList {
                        addAll(spell.script.targetRequirements)
                        spell.script.castAuraTarget?.let { add(it) }
                    }
                    if (targetReqs.isEmpty()) {
                        result.add(offer(affordable = true, costString = costString, costInfo = escapeCostInfo, costByTargetCount = costByTargetCount).copy(autoTapPreview = autoTapPreview))
                        continue
                    }
                    val targetInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs, targetingSourceType = TargetingSourceType.SPELL)
                    if (!context.targetUtils.allRequirementsSatisfied(targetInfos)) continue
                    val firstReq = targetReqs.first()
                    val firstInfo = targetInfos.first()
                    result.add(
                        offer(affordable = true, costString = costString, costInfo = escapeCostInfo, costByTargetCount = costByTargetCount).copy(
                            validTargets = firstInfo.validTargets,
                            requiresTargets = true,
                            targetCount = firstInfo.maxTargets,
                            minTargets = firstReq.effectiveMinCount,
                            targetDescription = firstReq.description,
                            targetRequirements = targetInfos.surfacedRequirements(),
                            autoTapPreview = autoTapPreview,
                        )
                    )
                }
            }
        }
    }

    // =========================================================================
    // Disturb (cards in graveyard with the Disturb keyword)
    // =========================================================================

    /**
     * Disturb (CR 702.146a): "You may cast this card transformed from your graveyard by paying
     * [cost] rather than its mana cost."
     *
     * Everything except the cost is read off the **back** face, because that is the face the spell
     * will have on the stack (CR 712.8c): its card types decide the timing, its target requirements
     * and `auraTarget` decide what has to be chosen (the Innistrad cycle's Aura backs enchant a
     * creature), and its name is what the offer is labelled with — the player is picking "cast this
     * as Luminous Phantom", not "cast Lunarch Veteran". The cost is the front face's disturb
     * keyword, run through the same battlefield cost-modifier pipeline as any other cast.
     *
     * Disturb grants no timing permission of its own, so a permanent back face is sorcery-speed
     * unless it has flash.
     */
    private fun enumerateDisturb(
        context: EnumerationContext,
        result: MutableList<LegalAction>
    ) {
        val state = context.state
        val playerId = context.playerId
        val graveyardCards = state.getZone(ZoneKey(playerId, Zone.GRAVEYARD))

        for (cardId in graveyardCards) {
            val container = state.getEntity(cardId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            val cardDef = context.cardRegistry.getCard(cardComponent.cardDefinitionId) ?: continue

            val disturb = DisturbCasts.printedDisturb(cardDef) ?: continue
            val backFace = DisturbCasts.castFace(cardDef) ?: continue

            val isInstant = backFace.typeLine.isInstant
            val hasFlash = backFace.keywords.contains(com.wingedsheep.sdk.core.Keyword.FLASH) ||
                context.castPermissionUtils.hasGrantedFlash(state, cardId, context.playerId)
            if (!isInstant && !hasFlash && !context.canPlaySorcerySpeed) continue

            val description = "Cast ${backFace.name} (Disturb)"
            val disturbAction = CastSpell(
                playerId, cardId,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.DISTURB
            )

            if (context.cantCastSpell(cardId)) {
                result.add(
                    LegalAction(
                        actionType = "CastWithDisturb",
                        description = description,
                        action = disturbAction,
                        affordable = false,
                        manaCostString = disturb.cost.toString(),
                        sourceZone = "GRAVEYARD",
                        // CR 712.8c: the spell is the back face, so the client renders the offer as it.
                        castsTransformed = true
                    )
                )
                continue
            }

            // Cast restrictions are a property of the whole card, so they are read from the front
            // face's script exactly as a normal cast of this card would.
            if (!context.legality.castRestrictionsMet(state, playerId, cardDef.script.castRestrictions)) continue

            val pricing = context.costCalculator.priceByTargets(

                state, cardDef, playerId, cardId, baseCost = disturb.cost, withTable = !context.skipAutoTapPreview,
                fromZone = Zone.GRAVEYARD,

            )

            val effectiveCost = pricing.min
            val costString = effectiveCost.toString()
            val costByTargetCount = pricing.byCountStrings()
            // Cast from the caster's own graveyard for its disturb cost — the facts "spend this
            // mana only to pay a disturb cost" / "cast spells from your graveyard" are judged by.
            // Built from the card's own component exactly as the payment path builds it.
            val spellContext = spellPaymentContextFor(
                cardComponent, isFromHand = false, isFromGraveyard = true, isDisturbCast = true
            )
            val canAfford = context.manaSolver.canPay(
                state, playerId, effectiveCost, precomputedSources = context.availableManaSources, spellContext = spellContext
            )

            if (!canAfford) {
                result.add(
                    LegalAction(
                        actionType = "CastWithDisturb",
                        description = description,
                        action = disturbAction,
                        affordable = false,
                        manaCostString = costString,
                        manaCostByTargetCount = costByTargetCount,
                        sourceZone = "GRAVEYARD",
                        // CR 712.8c: the spell is the back face, so the client renders the offer as it.
                        castsTransformed = true
                    )
                )
                continue
            }

            val targetReqs = buildList {
                addAll(backFace.script.targetRequirements)
                backFace.script.castAuraTarget?.let { add(it) }
            }

            val autoTapPreview = if (context.skipAutoTapPreview) null else {
                context.manaSolver.solve(
                    state, playerId, effectiveCost, precomputedSources = context.availableManaSources, spellContext = spellContext
                )?.sources?.map { it.entityId }
            }

            if (targetReqs.isNotEmpty()) {
                val targetInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs, targetingSourceType = TargetingSourceType.SPELL,
                    spellSubtypes = backFace.typeLine.subtypes.mapTo(mutableSetOf()) { it.value })
                if (!context.targetUtils.allRequirementsSatisfied(targetInfos)) continue
                val firstReq = targetReqs.first()
                val firstInfo = targetInfos.first()
                result.add(
                    LegalAction(
                        actionType = "CastWithDisturb",
                        description = description,
                        action = disturbAction,
                        validTargets = firstInfo.validTargets,
                        requiresTargets = true,
                        targetCount = firstInfo.maxTargets,
                        minTargets = firstReq.effectiveMinCount,
                        targetDescription = firstReq.description,
                        targetRequirements = targetInfos.surfacedRequirements(),
                        manaCostString = costString,
                        manaCostByTargetCount = costByTargetCount,
                        autoTapPreview = autoTapPreview,
                        sourceZone = "GRAVEYARD",
                        // CR 712.8c: the spell is the back face, so the client renders the offer as it.
                        castsTransformed = true
                    )
                )
            } else {
                result.add(
                    LegalAction(
                        actionType = "CastWithDisturb",
                        description = description,
                        action = disturbAction,
                        manaCostString = costString,
                        manaCostByTargetCount = costByTargetCount,
                        autoTapPreview = autoTapPreview,
                        sourceZone = "GRAVEYARD",
                        // CR 712.8c: the spell is the back face, so the client renders the offer as it.
                        castsTransformed = true
                    )
                )
            }
        }
    }

    // =========================================================================
    // Harmonize (cards in graveyard with Harmonize keyword)
    // =========================================================================

    private fun enumerateHarmonize(
        context: EnumerationContext,
        result: MutableList<LegalAction>
    ) {
        val state = context.state
        val playerId = context.playerId
        val graveyardCards = state.getZone(ZoneKey(playerId, Zone.GRAVEYARD))

        for (cardId in graveyardCards) {
            val container = state.getEntity(cardId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            val cardDef = context.cardRegistry.getCard(cardComponent.cardDefinitionId) ?: continue

            // Harmonize may be printed on the card or granted at runtime (Songcrafter Mage).
            val harmonize = HarmonizeGrants.effectiveHarmonize(state, cardId, cardDef) ?: continue

            // Timing: instants at instant speed, sorceries at sorcery speed (all current
            // Harmonize cards are sorceries, but gate generically).
            val isInstant = cardComponent.typeLine.isInstant
            if (!isInstant && !context.canPlaySorcerySpeed) continue

            if (context.cantCastSpell(cardId)) {
                result.add(
                    LegalAction(
                        actionType = "CastWithHarmonize",
                        description = "Cast ${cardComponent.name} (Harmonize)",
                        action = CastSpell(playerId, cardId, useAlternativeCost = true, alternativeCostType = AlternativeCostType.HARMONIZE),
                        affordable = false,
                        manaCostString = harmonize.cost.toString(),
                        hasHarmonize = true,
                        sourceZone = "GRAVEYARD"
                    )
                )
                continue
            }

            val castRestrictions = cardDef.script.castRestrictions
            if (!context.legality.castRestrictionsMet(state, playerId, castRestrictions)) continue

            val pricing = context.costCalculator.priceByTargets(

                state, cardDef, playerId, cardId, baseCost = harmonize.cost, withTable = !context.skipAutoTapPreview,
                fromZone = Zone.GRAVEYARD,

            )

            val effectiveCost = pricing.min
            val costString = effectiveCost.toString()
            val costByTargetCount = pricing.byCountStrings()
            val harmonizeCreatures = context.costUtils.findHarmonizeCreatures(state, playerId)
            // Harmonize casts from the graveyard, so eligible conditional mana is judged with
            // isFromHand = false (a "cast from a non-hand zone only" restriction applies here).
            val harmonizeSpellContext = spellPaymentContextFor(cardComponent, isFromHand = false, isFromGraveyard = true)
            val canAfford = context.costUtils.canAffordWithHarmonize(
                state, playerId, effectiveCost, harmonizeCreatures,
                precomputedSources = context.availableManaSources, spellContext = harmonizeSpellContext
            )
            // X-cost Harmonize (e.g. Nature's Rhythm {X}{G}{G}{G}{G}): advertise X so the
            // client prompts for it. maxAffordableX folds in the best single-creature tap
            // reduction, since {X} is generic mana the tap can reduce (TDM release notes).
            val hasXCost = effectiveCost.hasX
            val maxAffordableX: Int? = if (hasXCost) {
                context.costUtils.maxAffordableXWithHarmonize(
                    state, playerId, effectiveCost, harmonizeCreatures,
                    precomputedSources = context.availableManaSources, spellContext = harmonizeSpellContext
                )
            } else null

            if (!canAfford) {
                result.add(
                    LegalAction(
                        actionType = "CastWithHarmonize",
                        description = "Cast ${cardComponent.name} (Harmonize)",
                        action = CastSpell(playerId, cardId, useAlternativeCost = true, alternativeCostType = AlternativeCostType.HARMONIZE),
                        affordable = false,
                        manaCostString = costString,
                        manaCostByTargetCount = costByTargetCount,
                        hasHarmonize = true,
                        harmonizeCreatures = harmonizeCreatures,
                        sourceZone = "GRAVEYARD"
                    )
                )
                continue
            }

            val targetReqs = buildList {
                addAll(cardDef.script.targetRequirements)
                cardDef.script.castAuraTarget?.let { add(it) }
            }

            if (targetReqs.isNotEmpty()) {
                val targetInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs, targetingSourceType = TargetingSourceType.SPELL)
                val allSatisfied = context.targetUtils.allRequirementsSatisfied(targetInfos)
                if (allSatisfied) {
                    val firstReq = targetReqs.first()
                    val firstInfo = targetInfos.first()
                    result.add(
                        LegalAction(
                            actionType = "CastWithHarmonize",
                            description = "Cast ${cardComponent.name} (Harmonize)",
                            action = CastSpell(playerId, cardId, useAlternativeCost = true, alternativeCostType = AlternativeCostType.HARMONIZE),
                            validTargets = firstInfo.validTargets,
                            requiresTargets = true,
                            targetCount = firstInfo.maxTargets,
                            minTargets = firstReq.effectiveMinCount,
                            targetDescription = firstReq.description,
                            targetRequirements = targetInfos.surfacedRequirements(),
                            manaCostString = costString,
                            manaCostByTargetCount = costByTargetCount,
                            hasXCost = hasXCost,
                            maxAffordableX = maxAffordableX,
                            hasHarmonize = true,
                            harmonizeCreatures = harmonizeCreatures,
                            sourceZone = "GRAVEYARD"
                        )
                    )
                }
            } else {
                result.add(
                    LegalAction(
                        actionType = "CastWithHarmonize",
                        description = "Cast ${cardComponent.name} (Harmonize)",
                        action = CastSpell(playerId, cardId, useAlternativeCost = true, alternativeCostType = AlternativeCostType.HARMONIZE),
                        manaCostString = costString,
                        manaCostByTargetCount = costByTargetCount,
                        hasXCost = hasXCost,
                        maxAffordableX = maxAffordableX,
                        hasHarmonize = true,
                        harmonizeCreatures = harmonizeCreatures,
                        sourceZone = "GRAVEYARD"
                    )
                )
            }
        }
    }

    // =========================================================================
    // Warp from hand (and graveyard for warp abilities that opt in)
    // =========================================================================

    private fun enumerateWarp(
        context: EnumerationContext,
        result: MutableList<LegalAction>
    ) {
        enumerateWarpFromZone(context, result, Zone.HAND)
        enumerateWarpFromZone(context, result, Zone.GRAVEYARD)
    }

    private fun enumerateWarpFromZone(
        context: EnumerationContext,
        result: MutableList<LegalAction>,
        zone: Zone
    ) {
        val state = context.state
        val playerId = context.playerId
        val zoneCards = state.getZone(ZoneKey(playerId, zone))
        val sourceZoneLabel = zone.name

        for (cardId in zoneCards) {
            val container = state.getEntity(cardId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Skip lands — warp is for spells only
            if (cardComponent.typeLine.isLand) continue

            val cardDef = context.cardRegistry.getCard(cardComponent.cardDefinitionId) ?: continue

            // Printed warp wins; otherwise consult battlefield grants
            // ([com.wingedsheep.sdk.scripting.GrantWarpToCardsInHand]). Granted warp is
            // hand-only, so it never satisfies a graveyard pass.
            val printedWarp = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Warp>().firstOrNull()
            val warpAbility: KeywordAbility.Warp = printedWarp
                ?: if (zone == Zone.HAND) {
                    WarpGrants.effectiveWarp(
                        state, cardId, cardDef, playerId, context.cardRegistry, context.predicateEvaluator
                    ) ?: continue
                } else continue

            // Graveyard casts are only legal for warp abilities that explicitly opt in
            // (CR 702.185a — default warp is hand-only).
            if (zone == Zone.GRAVEYARD && !warpAbility.fromGraveyard) continue

            // Warp permanents at sorcery speed, instants at instant speed
            val isInstant = cardComponent.typeLine.isInstant
            if (!isInstant && !context.canPlaySorcerySpeed) continue

            if (context.cantCastSpell(cardId)) {
                result.add(
                    LegalAction(
                        actionType = "CastWithWarp",
                        description = "Cast ${cardComponent.name} (Warp)",
                        action = CastSpell(playerId, cardId, useAlternativeCost = true, alternativeCostType = AlternativeCostType.WARP),
                        affordable = false,
                        manaCostString = warpAbility.cost.toString(),
                        sourceZone = sourceZoneLabel
                    )
                )
                continue
            }

            // Check cast restrictions
            val castRestrictions = cardDef.script.castRestrictions
            if (!context.legality.castRestrictionsMet(state, playerId, castRestrictions)) continue

            // A warp card can always be cast two ways — its normal cost or its warp cost — and
            // which to use is the caster's choice (CR 118.9a). Surface both in the action window
            // even when only one is payable, mirroring how morph (MorphCastEnumerator) shows the
            // normal cast alongside the face-down cast. The normal cast is otherwise legal here
            // (timing, cast restrictions, and the no-cast lock have all been checked above); when
            // it's affordable, CastSpellEnumerator emits the full-featured action, so we only add a
            // greyed-out placeholder when the player can't pay the normal cost. Hand-only: there is
            // no normal cast from the graveyard.
            if (zone == Zone.HAND) {
                // Priced like CastSpellEnumerator's hand cast, so the placeholder appears exactly when that
                // offer is unaffordable.
                val normalPricing = context.costCalculator.priceByTargets(
                    state, cardDef, playerId, cardId, withTable = !context.skipAutoTapPreview,
                )
                val normalCost = normalPricing.min
                val canAffordNormal = context.manaSolver.canPay(
                    state, playerId, normalCost, precomputedSources = context.availableManaSources
                )
                if (!canAffordNormal) {
                    result.add(
                        LegalAction(
                            actionType = "CastSpell",
                            description = "Cast ${cardComponent.name}",
                            action = CastSpell(playerId, cardId),
                            affordable = false,
                            manaCostString = normalCost.toString(),
                            manaCostByTargetCount = normalPricing.byCountStrings(),
                        )
                    )
                }
            }

            val pricing = context.costCalculator.priceByTargets(

                state, cardDef, playerId, cardId, baseCost = warpAbility.cost, withTable = !context.skipAutoTapPreview,
                fromZone = zone,

            )

            val effectiveCost = pricing.min
            val costString = effectiveCost.toString()
            val costByTargetCount = pricing.byCountStrings()
            val canAfford = context.manaSolver.canPay(state, playerId, effectiveCost, precomputedSources = context.availableManaSources)

            if (!canAfford) {
                result.add(
                    LegalAction(
                        actionType = "CastWithWarp",
                        description = "Cast ${cardComponent.name} (Warp)",
                        action = CastSpell(playerId, cardId, useAlternativeCost = true, alternativeCostType = AlternativeCostType.WARP),
                        affordable = false,
                        manaCostString = costString,
                        manaCostByTargetCount = costByTargetCount,
                        sourceZone = sourceZoneLabel
                    )
                )
                continue
            }

            // The warp cost itself can contain {X} (e.g. Broodguard Elite, Warp {X}{G}).
            // Mirror the normal cast path so the client prompts for X — without this the
            // spell is cast with X = 0. Warp has no delve, so there's no delve ceiling.
            val hasXCost = effectiveCost.hasX
            val maxAffordableX: Int? = if (hasXCost) {
                val availableSources = context.manaSolver.getAvailableManaCount(
                    state, playerId, precomputedSources = context.availableManaSources
                )
                val fixedCost = effectiveCost.cmc  // X contributes 0 to CMC
                val xSymbolCount = effectiveCost.xCount.coerceAtLeast(1)
                ((availableSources - fixedCost) / xSymbolCount).coerceAtLeast(0)
            } else null

            val targetReqs = buildList {
                addAll(cardDef.script.targetRequirements)
                cardDef.script.castAuraTarget?.let { add(it) }
            }

            val autoTapPreview = if (context.skipAutoTapPreview) null else {
                context.manaSolver.solve(state, playerId, effectiveCost, precomputedSources = context.availableManaSources)
                    ?.sources?.map { it.entityId }
            }

            if (targetReqs.isNotEmpty()) {
                val targetInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs, targetingSourceType = TargetingSourceType.SPELL)
                val allSatisfied = context.targetUtils.allRequirementsSatisfied(targetInfos)
                if (allSatisfied) {
                    val firstReq = targetReqs.first()
                    val firstInfo = targetInfos.first()
                    result.add(
                        LegalAction(
                            actionType = "CastWithWarp",
                            description = "Cast ${cardComponent.name} (Warp)",
                            action = CastSpell(playerId, cardId, useAlternativeCost = true, alternativeCostType = AlternativeCostType.WARP),
                            validTargets = firstInfo.validTargets,
                            requiresTargets = true,
                            targetCount = firstInfo.maxTargets,
                            minTargets = firstReq.effectiveMinCount,
                            targetDescription = firstReq.description,
                            targetRequirements = targetInfos.surfacedRequirements(),
                            manaCostString = costString,
                            manaCostByTargetCount = costByTargetCount,
                            autoTapPreview = autoTapPreview,
                            hasXCost = hasXCost,
                            maxAffordableX = maxAffordableX,
                            sourceZone = sourceZoneLabel
                        )
                    )
                }
            } else {
                result.add(
                    LegalAction(
                        actionType = "CastWithWarp",
                        description = "Cast ${cardComponent.name} (Warp)",
                        action = CastSpell(playerId, cardId, useAlternativeCost = true, alternativeCostType = AlternativeCostType.WARP),
                        manaCostString = costString,
                        manaCostByTargetCount = costByTargetCount,
                        autoTapPreview = autoTapPreview,
                        hasXCost = hasXCost,
                        maxAffordableX = maxAffordableX,
                        sourceZone = sourceZoneLabel
                    )
                )
            }
        }
    }

    // =========================================================================
    // Dash (CR 702.109) and blitz (CR 702.152)
    // =========================================================================

    /**
     * Enumerate dash casts of hand cards. Hand-only (CR 702.109a) — no graveyard variant, no
     * granted-dash resolver yet.
     */
    private fun enumerateDash(
        context: EnumerationContext,
        result: MutableList<LegalAction>
    ) {
        for (cardId in context.state.getHand(context.playerId)) {
            val cardComponent = context.state.getEntity(cardId)?.get<CardComponent>() ?: continue
            if (cardComponent.typeLine.isLand) continue
            val cardDef = context.cardRegistry.getCard(cardComponent.cardDefinitionId) ?: continue
            val dash = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Dash>().firstOrNull() ?: continue
            enumerateKeywordAlternativeCast(
                context, result, cardId, cardComponent, cardDef, Zone.HAND,
                dash.cost, emptyList(), AlternativeCostType.DASH, "CastWithDash", "Dash"
            )
        }
    }

    /**
     * Enumerate blitz casts (CR 702.152a): from the hand, and from the graveyard when the card's
     * `MayCastSelfFromZones(castUsing = Keyword.BLITZ)` permission reaches it (Tenacious Underdog).
     */
    private fun enumerateBlitz(
        context: EnumerationContext,
        result: MutableList<LegalAction>
    ) {
        val state = context.state
        val playerId = context.playerId
        val zones = com.wingedsheep.engine.handlers.actions.spell.CastZoneResolver(
            context.cardRegistry, context.conditionEvaluator, context.legality
        )
        val fromHand = if (context.cantPlayCardsFromHand) emptyList() else state.getHand(playerId).map { it to Zone.HAND }
        val candidates = fromHand + state.getGraveyard(playerId).map { it to Zone.GRAVEYARD }
        for ((cardId, zone) in candidates) {
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
            if (cardComponent.typeLine.isLand) continue
            val cardDef = context.cardRegistry.getCard(cardComponent.cardDefinitionId) ?: continue
            val blitz = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Blitz>().firstOrNull() ?: continue
            if (!zones.hasBlitzPermission(state, playerId, cardId)) continue
            enumerateKeywordAlternativeCast(
                context, result, cardId, cardComponent, cardDef, zone,
                blitz.cost, listOfNotNull(blitz.additionalCost), AlternativeCostType.BLITZ, "CastWithBlitz", "Blitz"
            )
        }
    }

    /**
     * One card's cast for a keyword alternative cost that replaces the mana cost and otherwise
     * follows the normal casting rules — dash and blitz. Same "always show both the normal cast and
     * the alternative cast" UX as [enumerateWarpFromZone], with target-requirement handling, {X} in
     * the alternative cost, and the cost's bundled non-mana half ([bundledCosts]).
     */
    private fun enumerateKeywordAlternativeCast(
        context: EnumerationContext,
        result: MutableList<LegalAction>,
        cardId: EntityId,
        cardComponent: CardComponent,
        cardDef: com.wingedsheep.sdk.model.CardDefinition,
        zone: Zone,
        alternativeCost: com.wingedsheep.sdk.core.ManaCost,
        bundledCosts: List<AdditionalCost>,
        alternativeCostType: AlternativeCostType,
        actionType: String,
        label: String,
    ) {
        val state = context.state
        val playerId = context.playerId
        val description = "Cast ${cardComponent.name} ($label)"

        // Permanents at sorcery speed, instants at instant speed — dash (CR 702.109a) and blitz
        // (CR 702.152a) fold into the normal alternative-cost casting rules (601.2b, 601.2f–h)
        // rather than waiving timing; the dash ruling spells it out: "You can cast a creature
        // spell for its dash cost only when you otherwise could cast that creature spell."
        val isInstant = cardComponent.typeLine.isInstant
        if (!isInstant && !context.canPlaySorcerySpeed) return

        if (context.cantCastSpell(cardId)) {
            result.add(
                LegalAction(
                    actionType = actionType,
                    description = description,
                    action = CastSpell(playerId, cardId, useAlternativeCost = true, alternativeCostType = alternativeCostType),
                    affordable = false,
                    manaCostString = alternativeCost.toString(),
                    sourceZone = zone.name
                )
            )
            return
        }

        val castRestrictions = cardDef.script.castRestrictions
        if (!context.legality.castRestrictionsMet(state, playerId, castRestrictions)) return

        // A card in hand can always be cast two ways — its normal cost or its alternative cost —
        // and which to use is the caster's choice (CR 118.9a / 601.2b). Surface both in the
        // action window, mirroring enumerateWarpFromZone: when the normal cast is
        // unaffordable, add a greyed-out placeholder so the player still sees it. There is no
        // normal cast from any other zone.
        if (zone == Zone.HAND) {
            // Priced like CastSpellEnumerator's hand cast, so the placeholder appears exactly when that
            // offer is unaffordable.
            val normalPricing = context.costCalculator.priceByTargets(
                state, cardDef, playerId, cardId, withTable = !context.skipAutoTapPreview,
            )
            val normalCost = normalPricing.min
            val canAffordNormal = context.manaSolver.canPay(
                state, playerId, normalCost, precomputedSources = context.availableManaSources
            )
            if (!canAffordNormal) {
                result.add(
                    LegalAction(
                        actionType = "CastSpell",
                        description = "Cast ${cardComponent.name}",
                        action = CastSpell(playerId, cardId),
                        affordable = false,
                        manaCostString = normalCost.toString(),
                        manaCostByTargetCount = normalPricing.byCountStrings(),
                    )
                )
            }
        }

        val pricing = context.costCalculator.priceByTargets(

            state, cardDef, playerId, cardId, baseCost = alternativeCost, withTable = !context.skipAutoTapPreview,
            fromZone = zone,

        )

        val effectiveCost = pricing.min
        val costString = effectiveCost.toString()
        val costByTargetCount = pricing.byCountStrings()
        val canAffordMana = context.manaSolver.canPay(state, playerId, effectiveCost, precomputedSources = context.availableManaSources)
        // The alternative cost's bundled non-mana half (Tenacious Underdog's "Pay 2 life").
        val (bundledCostInfo, canPayBundled) = presentOwedCosts(context, cardId, bundledCosts)

        if (!canAffordMana || !canPayBundled) {
            result.add(
                LegalAction(
                    actionType = actionType,
                    description = description,
                    action = CastSpell(playerId, cardId, useAlternativeCost = true, alternativeCostType = alternativeCostType),
                    affordable = false,
                    manaCostString = costString,
                    manaCostByTargetCount = costByTargetCount,
                    additionalCostInfo = bundledCostInfo,
                    sourceZone = zone.name
                )
            )
            return
        }

        // The alternative cost itself can contain {X} (mirrors enumerateWarpFromZone — without
        // this the client never prompts for X and the spell is cast with X = 0). Neither has
        // delve, so there's no delve ceiling.
        val hasXCost = effectiveCost.hasX
        val maxAffordableX: Int? = if (hasXCost) {
            val availableSources = context.manaSolver.getAvailableManaCount(
                state, playerId, precomputedSources = context.availableManaSources
            )
            val fixedCost = effectiveCost.cmc  // X contributes 0 to CMC
            val xSymbolCount = effectiveCost.xCount.coerceAtLeast(1)
            ((availableSources - fixedCost) / xSymbolCount).coerceAtLeast(0)
        } else null

        val targetReqs = buildList {
            addAll(cardDef.script.targetRequirements)
            cardDef.script.castAuraTarget?.let { add(it) }
        }

        val autoTapPreview = if (context.skipAutoTapPreview) null else {
            context.manaSolver.solve(state, playerId, effectiveCost, precomputedSources = context.availableManaSources)
                ?.sources?.map { it.entityId }
        }

        if (targetReqs.isNotEmpty()) {
            val targetInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs, targetingSourceType = TargetingSourceType.SPELL)
            val allSatisfied = context.targetUtils.allRequirementsSatisfied(targetInfos)
            if (allSatisfied) {
                val firstReq = targetReqs.first()
                val firstInfo = targetInfos.first()
                result.add(
                    LegalAction(
                        actionType = actionType,
                        description = description,
                        action = CastSpell(playerId, cardId, useAlternativeCost = true, alternativeCostType = alternativeCostType),
                        validTargets = firstInfo.validTargets,
                        requiresTargets = true,
                        targetCount = firstInfo.maxTargets,
                        minTargets = firstReq.effectiveMinCount,
                        targetDescription = firstReq.description,
                        targetRequirements = targetInfos.surfacedRequirements(),
                        manaCostString = costString,
                        manaCostByTargetCount = costByTargetCount,
                        autoTapPreview = autoTapPreview,
                        hasXCost = hasXCost,
                        maxAffordableX = maxAffordableX,
                        additionalCostInfo = bundledCostInfo,
                        sourceZone = zone.name
                    )
                )
            }
        } else {
            result.add(
                LegalAction(
                    actionType = actionType,
                    description = description,
                    action = CastSpell(playerId, cardId, useAlternativeCost = true, alternativeCostType = alternativeCostType),
                    manaCostString = costString,
                    manaCostByTargetCount = costByTargetCount,
                    autoTapPreview = autoTapPreview,
                    hasXCost = hasXCost,
                    maxAffordableX = maxAffordableX,
                    additionalCostInfo = bundledCostInfo,
                    sourceZone = zone.name
                )
            )
        }
    }

    // =========================================================================
    // Graveyard creatures with forage (Osteomancer Adept-style)
    // =========================================================================

    private fun enumerateGraveyardCreaturesWithForage(
        context: EnumerationContext,
        result: MutableList<LegalAction>
    ) {
        val state = context.state
        val playerId = context.playerId

        // Check if player has the forage-graveyard permission
        val playerEntity = state.getEntity(playerId) ?: return
        if (!playerEntity.has<MayCastCreaturesFromGraveyardWithForageComponent>()) return

        // Only the active player can cast creature spells (sorcery timing)
        if (!state.isActiveTurnFor(playerId)) return

        val graveyardCards = state.getZone(ZoneKey(playerId, Zone.GRAVEYARD))

        for (cardId in graveyardCards) {
            val container = state.getEntity(cardId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Only creature spells
            if (!cardComponent.typeLine.isCreature) continue

            val cardDef = context.cardRegistry.getCard(cardComponent.name) ?: continue

            // Don't offer forage if it can't be paid (< 3 other graveyard cards and no Food).
            // The card being cast can't be one of the three it exiles, so it's excluded from the
            // exile pool — both for affordability and for the card-picker the client renders.
            val forageCandidates = com.wingedsheep.engine.handlers.costs.ForageCostResolver
                .candidates(state, playerId, excludeCardId = cardId)
            if (!forageCandidates.canPay) continue
            val forageModes = com.wingedsheep.engine.handlers.costs.ForageCostResolver.costInfos(forageCandidates)
            val primaryForageMode = forageModes.firstOrNull()

            if (context.cantCastSpell(cardId)) {
                result.add(
                    LegalAction(
                        actionType = "CastSpell",
                        description = "Cast ${cardComponent.name} (forage)",
                        action = CastSpell(playerId, cardId),
                        affordable = false,
                        manaCostString = cardComponent.manaCost.toString(),
                        sourceZone = "GRAVEYARD",
                        requiresForage = true
                    )
                )
                continue
            }

            if (!context.canPlaySorcerySpeed) continue

            val castRestrictions = cardDef.script.castRestrictions
            val meetsRestrictions = context.legality.castRestrictionsMet(state, playerId, castRestrictions)
            if (!meetsRestrictions) continue

            val pricing = context.costCalculator.priceByTargets(
                state, cardDef, playerId, cardId, withTable = !context.skipAutoTapPreview, fromZone = Zone.GRAVEYARD,
            )
            val effectiveCost = pricing.min
            val costString = effectiveCost.toString()
            val costByTargetCount = pricing.byCountStrings()
            val affordable = context.manaSolver.canPay(state, playerId, effectiveCost, precomputedSources = context.availableManaSources)

            // Affordable emissions advertise the forage cost so the client lets the player pick
            // which cards/Food to forage with (rather than the engine silently taking the first
            // three). When both modes are payable, a sibling action per mode is emitted below.
            val forageEmissionStart = result.size

            if (affordable) {
                val targetReqs = buildList {
                    addAll(cardDef.script.targetRequirements)
                    cardDef.script.castAuraTarget?.let { add(it) }
                }

                if (targetReqs.isNotEmpty()) {
                    val targetInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs, targetingSourceType = TargetingSourceType.SPELL)
                    val allSatisfied = context.targetUtils.allRequirementsSatisfied(targetInfos)
                    if (allSatisfied) {
                        val firstReq = targetReqs.first()
                        val firstInfo = targetInfos.first()
                        result.add(
                            LegalAction(
                                actionType = "CastSpell",
                                description = "Cast ${cardComponent.name} (forage)",
                                action = CastSpell(playerId, cardId),
                                validTargets = firstInfo.validTargets,
                                requiresTargets = true,
                                targetCount = firstInfo.maxTargets,
                                minTargets = firstReq.effectiveMinCount,
                                targetDescription = firstReq.description,
                                targetRequirements = targetInfos.surfacedRequirements(),
                                manaCostString = costString,
                                manaCostByTargetCount = costByTargetCount,
                                sourceZone = "GRAVEYARD",
                                requiresForage = true,
                                additionalCostInfo = primaryForageMode
                            )
                        )
                    }
                } else {
                    result.add(
                        LegalAction(
                            actionType = "CastSpell",
                            description = "Cast ${cardComponent.name} (forage)",
                            action = CastSpell(playerId, cardId),
                            manaCostString = costString,
                            manaCostByTargetCount = costByTargetCount,
                            sourceZone = "GRAVEYARD",
                            requiresForage = true,
                            additionalCostInfo = primaryForageMode
                        )
                    )
                }
            } else {
                result.add(
                    LegalAction(
                        actionType = "CastSpell",
                        description = "Cast ${cardComponent.name} (forage)",
                        action = CastSpell(playerId, cardId),
                        affordable = false,
                        manaCostString = costString,
                        manaCostByTargetCount = costByTargetCount,
                        sourceZone = "GRAVEYARD",
                        requiresForage = true
                    )
                )
            }

            // Emit a sibling action for the alternate forage mode (exile vs. sacrifice a Food) so
            // the player picks the mode in the action menu, mirroring the activated-ability path.
            if (affordable && forageModes.size > 1) {
                val alternateMode = forageModes[1]
                val emitted = result.subList(forageEmissionStart, result.size).toList()
                for ((offset, la) in emitted.withIndex()) {
                    if (la.action !is CastSpell || !la.affordable) continue
                    result[forageEmissionStart + offset] = la.copy(
                        description = "${la.description} — ${forageModes[0].description}"
                    )
                    result.add(
                        la.copy(
                            description = "${la.description} — ${alternateMode.description}",
                            additionalCostInfo = alternateMode
                        )
                    )
                }
            }
        }
    }

    // =========================================================================
    // Cast from graveyard, optionally for an additional life cost (MayCastFromGraveyard)
    // =========================================================================

    private fun enumerateGraveyardCast(
        context: EnumerationContext,
        result: MutableList<LegalAction>
    ) {
        val state = context.state
        val playerId = context.playerId

        // Find permanents with MayCastFromGraveyard static ability.
        //
        // An `oncePerTurn` grant (Gisa and Geralf) stops offering casts once its own permanent has
        // been used this turn, so the grant is only usable while the source is unmarked — the same
        // gate `CastZoneResolver.mayCastFromGraveyardGrantApplies` applies on the handler side, and
        // the two must stay in step or the client would offer an action the handler then rejects.
        fun grantIsSpent(sourceId: EntityId, sa: MayCastFromGraveyard): Boolean =
            sa.oncePerTurn && state.getEntity(sourceId)?.has<MayCastFromGraveyardUsedThisTurnComponent>() == true

        // A permission, plus the one graveyard card it is scoped to (null = every matching card).
        // Card-scoped entries come from a grant handed to the card itself — "creature cards in your
        // graveyard gain 'You may cast this card from your graveyard'" (Case of the Uneaten Feast),
        // where the set of cards is fixed when the ability resolves (CR 611.2c) rather than being a
        // standing player-wide permission that later arrivals would also enjoy.
        val permissions = mutableListOf<GraveyardCastPermission>()
        for (permId in state.getBattlefield()) {
            val container = state.getEntity(permId) ?: continue
            val controller = container.get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()?.playerId
            if (controller != playerId) continue
            val cardComp = container.get<CardComponent>() ?: continue
            val cardDef = context.cardRegistry.getCard(cardComp.cardDefinitionId) ?: continue
            for (sa in cardDef.liveStaticAbilities(state, permId)) {
                if (sa is MayCastFromGraveyard && !grantIsSpent(permId, sa)) {
                    permissions.add(GraveyardCastPermission(sa, permId, scopedCardId = null))
                }
            }
        }
        // An emblem the player has (Wrenn and Realmbreaker's −7): a player-wide permission held from
        // outside every zone. Kept in step with `CastZoneResolver.mayCastFromGraveyardGrantsWithSources`.
        for ((emblemId, sa) in state.emblemStaticAbilitiesOf(playerId)) {
            if (sa is MayCastFromGraveyard && !grantIsSpent(emblemId, sa)) {
                permissions.add(GraveyardCastPermission(sa, emblemId, scopedCardId = null))
            }
        }
        // Durational grants recorded in grantedStaticAbilities, in their two anchorings:
        // anchored to a permanent the player controls, the grant is a player-wide permission
        // (Forgotten Cellar's "you may cast spells from your graveyard this turn"); anchored to a
        // card sitting in the player's own graveyard, it is that card's own permission.
        //
        // The player-wide reading is gated on the anchor actually being *on the battlefield*, not
        // just on it having a controller. A card that reached the graveyard without passing through
        // the battlefield — milled, discarded — keeps the ControllerComponent it was minted with,
        // so a controller-only test would silently promote Case of the Uneaten Feast's per-card
        // grant into a standing "cast any creature card from your graveyard" permission, letting
        // the same card be recast every time it died.
        val battlefield = state.getBattlefield()
        val graveyard = state.getZone(ZoneKey(playerId, Zone.GRAVEYARD))
        for (grant in state.grantedStaticAbilities) {
            val ability = grant.ability
            if (ability !is MayCastFromGraveyard) continue
            if (grantIsSpent(grant.entityId, ability)) continue
            val anchor = state.getEntity(grant.entityId) ?: continue
            val controller = anchor.get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()?.playerId
            when {
                // Anchored to the player (The Great Work's chapter III): player-wide, and it outlives
                // the permanent whose ability created it.
                grant.entityId == playerId -> permissions.add(GraveyardCastPermission(ability, grant.entityId, null))
                grant.entityId in battlefield && controller == playerId ->
                    permissions.add(GraveyardCastPermission(ability, grant.entityId, null))
                grant.entityId in graveyard -> permissions.add(GraveyardCastPermission(ability, grant.entityId, grant.entityId))
            }
        }

        if (permissions.isEmpty()) return

        // One legal action per card per *distinct* permission (life cost + entry rider), so when two
        // grants apply to the same card (a free `Nonland` grant and The Tomb of Aclazotz's `Creature`
        // grant that adds a finality counter + Vampire) the player picks which up front — CR 601.2b —
        // instead of the handler auto-picking the rider. Keyed on (card, life cost, rider) so grants
        // that are indistinguishable to the player collapse to a single action.
        val emitted = HashSet<String>()

        // Check timing restrictions
        for ((permission, grantSourceId, scopedCardId) in permissions) {
            if (permission.duringYourTurnOnly && !state.isActiveTurnFor(playerId)) continue
            val lifeCost = permission.lifeCost
            val lifeSuffix = if (lifeCost > 0) " (pay $lifeCost life)" else ""
            // The permission's cast-this-way entry rider, recorded on the action so the handler
            // applies exactly the permission the player chose, and surfaced in the action text so the
            // options read differently when riders differ.
            val riderSelection = GraveyardCastRiderSelection(
                permission.entersWithCounter, permission.addedSubtypeOnEntry, permission.exileCastThisWay,
                permission.additionalCost, permission.grantedAbilityOnEntry
            )
            val riderSuffix = graveyardRiderSuffix(permission)

            // Your own graveyard, or every player's for a `fromAnyGraveyard` grant (The Great Work).
            // The card keeps its owner, so it still returns to — or is exiled instead of going to —
            // that owner's graveyard.
            val graveyardCards = if (permission.fromAnyGraveyard) {
                state.turnOrder.flatMap { state.getZone(ZoneKey(it, Zone.GRAVEYARD)) }
            } else {
                graveyard
            }
            for (cardId in graveyardCards) {
                if (scopedCardId != null && cardId != scopedCardId) continue
                val container = state.getEntity(cardId) ?: continue
                val cardComponent = container.get<CardComponent>() ?: continue
                // A land is played, never cast (CR 305.9 — even one with another card type) — a `playsLands` grant's filter admits land
                // cards, and those plays are offered by PlayLandEnumerator instead.
                if (cardComponent.typeLine.isLand) continue
                val cardDef = context.cardRegistry.getCard(cardComponent.cardDefinitionId) ?: continue

                // Check if card matches filter
                if (!context.predicateEvaluator.matches(
                        state, state.projectedState, cardId, permission.filter,
                        CastZoneResolver.graveyardGrantFilterContext(playerId, grantSourceId)
                    )
                ) continue

                // Check timing: instants at instant speed, everything else at sorcery speed
                val isInstant = cardComponent.typeLine.isInstant
                if (!isInstant && !context.canPlaySorcerySpeed) continue

                if (context.cantCastSpell(cardId)) continue

                // Check cast restrictions
                val castRestrictions = cardDef.script.castRestrictions
                if (!context.legality.castRestrictionsMet(state, playerId, castRestrictions)) continue

                // Check life affordability (only when there is a life cost)
                // CR 810.9a — team's shared total; CR 119.8 — nor while the player can't lose life.
                if (!context.canPayLife(lifeCost)) continue

                // The grant's own additional cost (Six's continuous retrace: "discard a land card") —
                // unpayable means no action, like an unaffordable life cost.
                val (grantCostInfo, canPayGrantCost) = presentOwedCosts(
                    context, cardId, listOfNotNull(permission.additionalCost)
                )
                if (!canPayGrantCost) continue

                // Collapse permissions indistinguishable to the player (same card, life cost, rider, extra cost).
                if (!emitted.add("$cardId|$lifeCost|${permission.entersWithCounter}|${permission.addedSubtypeOnEntry}|${permission.exileCastThisWay}|${permission.additionalCost}|${permission.grantedAbilityOnEntry}")) continue

                val pricing = context.costCalculator.priceByTargets(
                    state, cardDef, playerId, cardId, withTable = !context.skipAutoTapPreview, fromZone = Zone.GRAVEYARD,
                )
                val effectiveCost = pricing.min
                val costString = effectiveCost.toString()
                val costByTargetCount = pricing.byCountStrings()
                val canAfford = context.manaSolver.canPay(state, playerId, effectiveCost, precomputedSources = context.availableManaSources)

                if (!canAfford) {
                    result.add(
                        LegalAction(
                            actionType = "CastSpell",
                            description = "Cast ${cardComponent.name}$riderSuffix$lifeSuffix",
                            action = CastSpell(playerId, cardId, graveyardLifeCost = lifeCost, graveyardCastRider = riderSelection),
                            affordable = false,
                            manaCostString = costString,
                            manaCostByTargetCount = costByTargetCount,
                            sourceZone = "GRAVEYARD",
                            additionalLifeCost = lifeCost,
                            additionalCostInfo = grantCostInfo
                        )
                    )
                    continue
                }

                val targetReqs = buildList {
                    addAll(cardDef.script.targetRequirements)
                    cardDef.script.castAuraTarget?.let { add(it) }
                }

                val autoTapPreview = if (context.skipAutoTapPreview) null else {
                    context.manaSolver.solve(state, playerId, effectiveCost, precomputedSources = context.availableManaSources)
                        ?.sources?.map { it.entityId }
                }

                if (targetReqs.isNotEmpty()) {
                    val targetInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs, targetingSourceType = TargetingSourceType.SPELL)
                    val allSatisfied = context.targetUtils.allRequirementsSatisfied(targetInfos)
                    if (allSatisfied) {
                        val firstReq = targetReqs.first()
                        val firstInfo = targetInfos.first()
                        result.add(
                            LegalAction(
                                actionType = "CastSpell",
                                description = "Cast ${cardComponent.name}$riderSuffix$lifeSuffix",
                                action = CastSpell(playerId, cardId, graveyardLifeCost = lifeCost, graveyardCastRider = riderSelection),
                                validTargets = firstInfo.validTargets,
                                requiresTargets = true,
                                targetCount = firstInfo.maxTargets,
                                minTargets = firstReq.effectiveMinCount,
                                targetDescription = firstReq.description,
                                targetRequirements = targetInfos.surfacedRequirements(),
                                manaCostString = costString,
                                manaCostByTargetCount = costByTargetCount,
                                autoTapPreview = autoTapPreview,
                                sourceZone = "GRAVEYARD",
                                additionalLifeCost = lifeCost,
                                additionalCostInfo = grantCostInfo
                            )
                        )
                    }
                } else {
                    result.add(
                        LegalAction(
                            actionType = "CastSpell",
                            description = "Cast ${cardComponent.name}$riderSuffix$lifeSuffix",
                            action = CastSpell(playerId, cardId, graveyardLifeCost = lifeCost, graveyardCastRider = riderSelection),
                            manaCostString = costString,
                            manaCostByTargetCount = costByTargetCount,
                            autoTapPreview = autoTapPreview,
                            sourceZone = "GRAVEYARD",
                            additionalLifeCost = lifeCost,
                            additionalCostInfo = grantCostInfo
                        )
                    )
                }
            }
        }
    }

    /**
     * Human-readable suffix describing a [MayCastFromGraveyard] permission's cast-this-way entry
     * rider, so two graveyard-cast options for the same card read differently when their riders
     * differ (e.g. "Cast Skullcap Snail (enters with a finality counter; becomes a Vampire)" vs the
     * plain "Cast Skullcap Snail" from a free grant). A grant's additional cost ("discard a land card") leads. Empty when the permission carries neither.
     */
    private fun graveyardRiderSuffix(permission: MayCastFromGraveyard): String {
        val parts = buildList {
            permission.additionalCost?.let { add(it.description.replaceFirstChar { c -> c.lowercase() }) }
            permission.entersWithCounter?.let { add("enters with a ${it.name.lowercase()} counter") }
            permission.addedSubtypeOnEntry?.let { add("becomes a $it") }
            permission.grantedAbilityOnEntry?.let { add("gains \"${it.description}\"") }
        }
        if (parts.isEmpty()) return ""
        return " (" + parts.joinToString("; ") + ")"
    }

    // =========================================================================
    // Kicker/offspring variants for zone casts
    // =========================================================================

    /**
     * Post-processes the collected zone-cast results to generate kicked/offspring variants.
     * For each affordable CastSpell action already in the result list, checks if the card
     * has kicker or offspring and adds a CastWithKicker variant if so.
     */
    private fun enumerateKickerForZoneCasts(
        context: EnumerationContext,
        result: MutableList<LegalAction>
    ) {
        val state = context.state
        val playerId = context.playerId
        if (context.cantCastSpells) return

        // Collect affordable zone-cast actions (not flashback/warp)
        val zoneCastActions = result
            .filter { it.actionType == "CastSpell" && it.affordable && it.sourceZone != null }
            .mapNotNull { la -> (la.action as? CastSpell)?.let { it.cardId to la } }
            .toMap()

        val kickerActions = mutableListOf<LegalAction>()
        val offspringGrants = com.wingedsheep.engine.mechanics.OffspringGrants.grants(state, playerId, context.cardRegistry, context.predicateEvaluator)

        for ((cardId, originalAction) in zoneCastActions) {
            val originalCast = originalAction.action as CastSpell
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
            if (cardComponent.typeLine.isLand) continue

            val cardDef = context.cardRegistry.getCard(cardComponent.name) ?: continue
            val optionalCosts = com.wingedsheep.engine.mechanics.OffspringGrants.effectiveOptionalCosts(
                state, cardId, cardDef, playerId, context.cardRegistry, context.predicateEvaluator, offspringGrants
            )
            if (optionalCosts.isEmpty()) continue

            val sourceZone = originalAction.sourceZone

            // One variant per mechanic on the optional-additional-cost rail (kicker → KICKED,
            // bargain → BARGAINED, CR 702.166b), and one per combination of a two-kicker card's
            // kickers (CR 702.33b) — the same declarations the hand-cast enumerator offers.
            for ((declaredSlot, kickers, declaredIndices) in optionalCostDeclarations(optionalCosts)) {
                val repeatable = kickers.any { it.multi }
                for (times in 1..(if (repeatable) MAX_OPTIONAL_COST_REPEATS else 1)) {
                    val additionalCostKicker = kickers.firstOrNull { it.additionalCost != null }
                    val offspringAbility = kickers.firstOrNull { it.keyword == Keyword.OFFSPRING }
                    val collectEvidenceAtom = (
                        (additionalCostKicker?.additionalCost as? AdditionalCost.Atom)?.atom
                        ) as? CostAtom.CollectEvidence

                    // Calculate the cost for this branch — a declaration-gated reduction ("costs {2} less
                    // to cast if it's bargained") applies only to the variant that declares it.
                    // Priced by target count against the kicked spell's own requirements.
                    val kickedManaCost = optionalCostsManaPaid(
                        kickers.filter { it.manaCost != null && it.keyword != Keyword.OFFSPRING }, times
                    ) ?: offspringAbility?.manaCost
                    val kickedPricing = priceKickedCast(
                        context, cardId, cardDef, declaredSlot, kickedManaCost,
                        fromZone = sourceZone?.let { zone -> Zone.entries.firstOrNull { it.name == zone } },
                    )
                    val targetReqs = kickedPricing.targetRequirements
                    val kickedCost = kickedPricing.cost
                    val kickedCostByTargetCount = kickedPricing.costByTargetCount
                    // This enumerator only enumerates non-hand-zone casts (command, library, exile,
                    // graveyard, …) — `sourceZone` is never "HAND" here. Mark accordingly so
                    // [ManaRestriction.CastFromNonHandOnly] mana is eligible for the kicked variant.
                    val kickedSpellContext = spellPaymentContextFor(
                        cardComponent,
                        isKicked = declaredSlot == ChoiceSlot.KICKED,
                        isFromExile = sourceZone == "EXILE",
                        isFromHand = false,
                        isFromGraveyard = cardId in state.getGraveyard(playerId),
                    ).copy(
                        colorlessAsAnyColor = state.activeMayPlayFor(cardId, playerId, context.conditionEvaluator, context.cardRegistry)
                            .any { it.colorlessAsAnyColor }
                    )
                    val canAffordKickedMana = context.manaSolver.canPay(
                        state, playerId, kickedCost,
                        spellContext = kickedSpellContext,
                        precomputedSources = context.availableManaSources
                    )
                    val kickedCostString = kickedCost.toString()
                    val kickedAutoTapPreview = if (context.skipAutoTapPreview) null else {
                        context.manaSolver.solve(
                            state, playerId, kickedCost,
                            spellContext = kickedSpellContext,
                            precomputedSources = context.availableManaSources
                        )?.sources?.map { it.entityId }
                    }

                    // Check additional cost payability
                    var kickerCostInfo: AdditionalCostData? = null
                    var canPayKickerAdditionalCost = true
                    val repeatedCost = com.wingedsheep.engine.handlers.actions.spell.optionalCostsAdditionalPaid(kickers, times)
                    if (repeatedCost != null) {
                        when (val atom = (repeatedCost as? AdditionalCost.Atom)?.atom) {
                            is CostAtom.Sacrifice -> {
                                val validSacTargets = context.costUtils.findSacrificeTargets(state, playerId, atom)
                                if (validSacTargets.size < atom.count) {
                                    canPayKickerAdditionalCost = false
                                } else {
                                    kickerCostInfo = AdditionalCostData(
                                        description = atom.description.replaceFirstChar { it.uppercase() },
                                        costType = "SacrificePermanent",
                                        validSacrificeTargets = validSacTargets,
                                        sacrificeCount = atom.count
                                    )
                                }
                            }
                            // Teamwork N (CR 702.194a) — "tap any number of creatures you control with
                            // total power N or more". Mirrors the hand-cast enumerator so a teamwork
                            // spell cast from another zone (flashback, a graveyard-cast grant) offers
                            // the same candidates and threshold.
                            is CostAtom.VariablePermanents -> {
                                val projected = state.projectedState
                                val candidates = com.wingedsheep.engine.mechanics.cost.VariablePermanentsCost
                                    .candidates(state, playerId, atom, predicateEvaluator = predicateEvaluator)
                                canPayKickerAdditionalCost = com.wingedsheep.engine.mechanics.cost
                                    .VariablePermanentsCost.canPay(state, playerId, atom, predicateEvaluator = predicateEvaluator)
                                kickerCostInfo = AdditionalCostData(
                                    description = atom.description.replaceFirstChar { it.uppercase() },
                                    costType = "TapForTotalPower",
                                    tapForPowerRequired = atom.minMeasure,
                                    tapForPowerCreatures = candidates.map { creatureId ->
                                        com.wingedsheep.engine.legalactions.TapForPowerCreatureData(
                                            entityId = creatureId,
                                            name = state.getEntity(creatureId)
                                                ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                                                ?.name ?: "Unknown",
                                            power = projected.getPower(creatureId) ?: 0
                                        )
                                    }
                                )
                            }
                            else -> {
                                val env = SpellCostEnumeration(context, cardId)
                                val (payable, costInfo) = optionalAdditionalCostPresentation(env, repeatedCost)
                                canPayKickerAdditionalCost = payable
                                kickerCostInfo = costInfo
                            }
                        }
                    }

                    val canAffordKicked = canAffordKickedMana && canPayKickerAdditionalCost
                    if (times > 1 && !canAffordKicked) break

                    val kickLabel = when {
                        declaredSlot == ChoiceSlot.BARGAINED -> "Bargained"
                        // An unnamed optional additional cost names what it pays ("Sacrifice an artifact").
                        declaredSlot == ChoiceSlot.ADDITIONAL_COST_PAID ->
                            additionalCostKicker?.additionalCost?.description?.replaceFirstChar { it.uppercase() }
                                ?: "Additional cost"
                        declaredSlot == ChoiceSlot.SQUAD -> "Squad"
                        declaredSlot == ChoiceSlot.REPLICATED -> "Replicate"
                        // See CastSpellEnumerator — the amount is the choice.
                        declaredSlot == ChoiceSlot.EVIDENCE_COLLECTED ->
                            collectEvidenceAtom
                                ?.description?.replaceFirstChar { it.uppercase() }
                                ?: "Collect evidence"
                        // Teamwork prints its N, so the variant reads "Cast X (Teamwork 2)".
                        declaredSlot == ChoiceSlot.TEAMWORK ->
                            additionalCostKicker?.displayPrefix ?: "Teamwork"
                        offspringAbility != null -> "Offspring"
                        declaredIndices.isNotEmpty() ->
                            chosenKickersLabel(kickers)
                        else -> "Kicked"
                    }

                    val castLabel = if (repeatable) "$kickLabel ×$times" else kickLabel

                    // Check for DividedDamageEffect in the kicked spell effect
                    val kickerSpellEffect = cardDef.script.kickerSpellEffect ?: cardDef.script.spellEffect
                    val kickerDividedDamage = kickerSpellEffect as? DividedDamageEffect
                    val kickerRequiresDamageDistribution = kickerDividedDamage != null
                    val kickerTotalDamage = kickerDividedDamage?.totalDamage
                    val kickerMinDamagePerTarget = if (kickerDividedDamage != null) 1 else null

                    if (targetReqs.isNotEmpty()) {
                        val targetReqInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs, targetingSourceType = TargetingSourceType.SPELL)
                        val allRequirementsSatisfied = context.targetUtils.allRequirementsSatisfied(targetReqInfos)
                        if (allRequirementsSatisfied) {
                            val firstReq = targetReqs.first()
                            val firstReqInfo = targetReqInfos.first()

                            val canAutoSelect = targetReqs.size == 1 &&
                                TargetEnumerationUtils.shouldAutoSelectPlayerTarget(firstReq, firstReqInfo.validTargets)

                            if (canAutoSelect) {
                                val autoSelectedTarget = ChosenTarget.Player(firstReqInfo.validTargets.first())
                                kickerActions.add(LegalAction(
                                    actionType = "CastWithKicker",
                                    description = "Cast ${cardComponent.name} ($castLabel)",
                                    action = CastSpell(playerId, cardId, targets = listOf(autoSelectedTarget), declaredCostSlot = declaredSlot, declaredCostTimes = times, declaredCostIndices = declaredIndices, graveyardLifeCost = originalCast.graveyardLifeCost),
                                    affordable = canAffordKicked,
                                    manaCostString = kickedCostString,
                                    manaCostByTargetCount = kickedCostByTargetCount,
                                    autoTapPreview = kickedAutoTapPreview,
                                    additionalCostInfo = kickerCostInfo,
                                    requiresDamageDistribution = kickerRequiresDamageDistribution,
                                    totalDamageToDistribute = kickerTotalDamage,
                                    minDamagePerTarget = kickerMinDamagePerTarget,
                                    sourceZone = sourceZone,
                                    additionalLifeCost = originalAction.additionalLifeCost
                                ))
                            } else {
                                kickerActions.add(LegalAction(
                                    actionType = "CastWithKicker",
                                    description = "Cast ${cardComponent.name} ($castLabel)",
                                    action = CastSpell(playerId, cardId, declaredCostSlot = declaredSlot, declaredCostTimes = times, declaredCostIndices = declaredIndices, graveyardLifeCost = originalCast.graveyardLifeCost),
                                    validTargets = firstReqInfo.validTargets,
                                    requiresTargets = true,
                                    targetCount = firstReqInfo.maxTargets,
                                    minTargets = firstReq.effectiveMinCount,
                                    targetDescription = firstReq.description,
                                    targetRequirements = targetReqInfos.surfacedRequirements(),
                                    affordable = canAffordKicked,
                                    manaCostString = kickedCostString,
                                    manaCostByTargetCount = kickedCostByTargetCount,
                                    autoTapPreview = kickedAutoTapPreview,
                                    additionalCostInfo = kickerCostInfo,
                                    requiresDamageDistribution = kickerRequiresDamageDistribution,
                                    totalDamageToDistribute = kickerTotalDamage,
                                    minDamagePerTarget = kickerMinDamagePerTarget,
                                    sourceZone = sourceZone,
                                    additionalLifeCost = originalAction.additionalLifeCost
                                ))
                            }
                        }
                    } else {
                        kickerActions.add(LegalAction(
                            actionType = "CastWithKicker",
                            description = "Cast ${cardComponent.name} ($castLabel)",
                            action = CastSpell(playerId, cardId, declaredCostSlot = declaredSlot, declaredCostTimes = times, declaredCostIndices = declaredIndices, graveyardLifeCost = originalCast.graveyardLifeCost),
                            affordable = canAffordKicked,
                            manaCostString = kickedCostString,
                            manaCostByTargetCount = kickedCostByTargetCount,
                            autoTapPreview = kickedAutoTapPreview,
                            additionalCostInfo = kickerCostInfo,
                            sourceZone = sourceZone,
                            additionalLifeCost = originalAction.additionalLifeCost
                        ))
                    }
                }
            }
        }

        result.addAll(kickerActions)
    }

    // =========================================================================
    // Runtime additional cost helpers (PlayWithAdditionalCostComponent)
    // =========================================================================

    private fun buildRuntimeAdditionalCostInfo(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId,
        component: PlayWithAdditionalCostComponent
    ): AdditionalCostData? {
        val cost = component.additionalCosts.firstOrNull() ?: return null
        val discardAtom = (cost as? AdditionalCost.Atom)?.atom as? CostAtom.Discard
        return when {
            discardAtom != null -> {
                val handCards = state.getZone(ZoneKey(playerId, Zone.HAND))
                AdditionalCostData(
                    description = discardAtom.description.replaceFirstChar { it.uppercase() },
                    costType = "DiscardCard",
                    validDiscardTargets = handCards.toList(),
                    discardCount = discardAtom.count
                )
            }
            cost is AdditionalCost.PayLifeEqualToManaValueOfSpell -> {
                val amount = state.getEntity(cardId)?.get<CardComponent>()?.manaCost?.cmc ?: 0
                AdditionalCostData(description = "Pay $amount life", costType = "PayLife")
            }
            else -> AdditionalCostData(description = cost.description, costType = "Other")
        }
    }

    private fun checkRuntimeAdditionalCostAffordability(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId,
        component: PlayWithAdditionalCostComponent,
        reservedLife: Int = 0,
    ): Boolean {
        for (cost in component.additionalCosts) {
            when (val atom = (cost as? AdditionalCost.Atom)?.atom) {
                is CostAtom.Discard -> {
                    val handSize = state.getZone(ZoneKey(playerId, Zone.HAND)).size
                    if (handSize < atom.count) return false
                }
                else -> if (cost is AdditionalCost.PayLifeEqualToManaValueOfSpell) {
                    val amount = state.getEntity(cardId)?.get<CardComponent>()?.manaCost?.cmc ?: 0
                    if (!state.canPayLife(playerId, amount + reservedLife, LifePaymentPurpose.CAST_SPELL)) return false
                }
            }
        }
        return true
    }

    /** A free cast's price as the client shows it: "{0}", or the increases it still owes. */
    private fun freeCostString(cost: ManaCost): String = cost.toString().ifEmpty { "{0}" }

    /** The auto-tap preview for a free cast — nothing to tap unless an increase applies. */
    private fun freeCostPreview(
        context: EnumerationContext,
        cost: ManaCost,
        spellContext: com.wingedsheep.engine.mechanics.mana.SpellPaymentContext? = null,
    ): List<EntityId>? = when {
        cost.isEmpty() || context.skipAutoTapPreview -> null
        else -> context.manaSolver.solve(
            context.state, context.playerId, cost, spellContext = spellContext, precomputedSources = context.availableManaSources,
        )?.sources?.map { it.entityId }
    }
}

/**
 * One [MayCastFromGraveyard] permission the graveyard-cast scan offers: the grant, the object it
 * comes from (the filter's `Self` — see `CastZoneResolver.graveyardGrantFilterContext`), and the one
 * graveyard card it is scoped to (null = every card its filter admits).
 */
private data class GraveyardCastPermission(
    val grant: MayCastFromGraveyard,
    val sourceId: EntityId,
    val scopedCardId: EntityId?,
)
