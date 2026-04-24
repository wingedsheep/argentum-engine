package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.MayPlayFromExileComponent
import com.wingedsheep.engine.state.components.identity.WarpExiledComponent
import com.wingedsheep.engine.state.components.player.MayCastCreaturesFromGraveyardWithForageComponent
import com.wingedsheep.engine.state.components.identity.PlayWithAdditionalCostComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GrantMayCastFromLinkedExile
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.MayCastFromGraveyardWithLifeCost
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import com.wingedsheep.sdk.scripting.effects.DividedDamageEffect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.state.components.stack.ChosenTarget

/**
 * Enumerates spells and lands castable/playable from non-hand zones:
 * - Top of library (PlayFromTopOfLibrary, CastSpellTypesFromTopOfLibrary)
 * - Exile (MayPlayFromExileComponent)
 * - Linked exile (GrantMayCastFromLinkedExile)
 * - Intrinsic zone cast (MayCastSelfFromZones, e.g. Squee)
 * - Graveyard permanents (MayPlayPermanentsFromGraveyard, e.g. Muldrotha)
 */
class CastFromZoneEnumerator : ActionEnumerator {

    override fun enumerate(context: EnumerationContext): List<LegalAction> {
        val result = mutableListOf<LegalAction>()
        val state = context.state
        val playerId = context.playerId

        enumerateTopOfLibrary(context, result)
        val linkedExileCardIds = enumerateExileCards(context, result)
        enumerateLinkedExile(context, result, linkedExileCardIds)
        enumerateIntrinsicZoneCast(context, result, linkedExileCardIds)
        enumerateGraveyardPermanents(context, result)
        enumerateGraveyardCreaturesWithForage(context, result)
        enumerateFlashback(context, result)
        enumerateGraveyardWithLifeCost(context, result)
        enumerateWarpFromHand(context, result)
        enumerateWarpFromExile(context, result)
        enumerateKickerForZoneCasts(context, result)

        return result
    }

    // =========================================================================
    // Top of library
    // =========================================================================

    private fun enumerateTopOfLibrary(context: EnumerationContext, result: MutableList<LegalAction>) {
        val state = context.state
        val playerId = context.playerId

        val canPlayAllFromTop = context.castPermissionUtils.hasPlayFromTopOfLibrary(state, playerId)
        val castFromTopFilter = if (!canPlayAllFromTop) {
            context.castPermissionUtils.getCastFromTopOfLibraryFilter(state, playerId)
        } else null
        val canPlayLandsFromTop = canPlayAllFromTop ||
            context.castPermissionUtils.hasPlayLandsFromTopOfLibrary(state, playerId)
        val castFilteredFromTopFilter = if (!canPlayAllFromTop) {
            context.castPermissionUtils.getCastFilteredFromTopOfLibraryFilter(state, playerId)
        } else null

        if (!canPlayAllFromTop && castFromTopFilter == null && !canPlayLandsFromTop && castFilteredFromTopFilter == null) return

        val library = state.getLibrary(playerId)
        if (library.isEmpty()) return

        val topCardId = library.first()
        val topCardComponent = state.getEntity(topCardId)?.get<CardComponent>() ?: return
        val topCardDef = context.cardRegistry.getCard(topCardComponent.name)

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
        val topCardMatchesFilter = canPlayAllFromTop ||
            (castFromTopFilter != null && context.predicateEvaluator.matches(
                state, topCardId, castFromTopFilter, PredicateContext(controllerId = playerId)
            )) ||
            (castFilteredFromTopFilter != null && context.predicateEvaluator.matches(
                state, topCardId, castFilteredFromTopFilter, PredicateContext(controllerId = playerId)
            ))

        if (!topCardComponent.typeLine.isLand && topCardMatchesFilter) {
            // Check timing
            val isInstant = topCardComponent.typeLine.isInstant
            if (isInstant || context.canPlaySorcerySpeed) {
                // Check cast restrictions
                val castRestrictions = topCardDef?.script?.castRestrictions ?: emptyList()
                if (context.castPermissionUtils.checkCastRestrictions(state, playerId, castRestrictions)) {
                    val topEffectiveCost = if (topCardDef != null) {
                        context.costCalculator.calculateEffectiveCost(state, topCardDef, playerId)
                    } else {
                        topCardComponent.manaCost
                    }
                    val cachedSources = context.availableManaSources
                    val canAfford = context.manaSolver.canPay(state, playerId, topEffectiveCost, precomputedSources = cachedSources)
                    if (canAfford) {
                        val targetReqs = buildList {
                            addAll(topCardDef?.script?.targetRequirements ?: emptyList())
                            topCardDef?.script?.auraTarget?.let { add(it) }
                        }

                        val manaCostString = topEffectiveCost.toString()
                        val hasXCost = topEffectiveCost.hasX
                        val maxAffordableX: Int? = if (hasXCost) {
                            val availableSources = context.manaSolver.getAvailableManaCount(state, playerId, precomputedSources = cachedSources)
                            val fixedCost = topEffectiveCost.cmc
                            val xSymbolCount = topEffectiveCost.xCount.coerceAtLeast(1)
                            ((availableSources - fixedCost) / xSymbolCount).coerceAtLeast(0)
                        } else null
                        val autoTapPreview = if (context.skipAutoTapPreview) null else {
                            context.manaSolver.solve(state, playerId, topEffectiveCost, precomputedSources = cachedSources)
                                ?.sources?.map { it.entityId }
                        }

                        if (targetReqs.isNotEmpty()) {
                            val targetInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs)
                            val allSatisfied = context.targetUtils.allRequirementsSatisfied(targetInfos)
                            if (allSatisfied) {
                                val firstReq = targetReqs.first()
                                val firstInfo = targetInfos.first()
                                result.add(
                                    LegalAction(
                                        actionType = "CastSpell",
                                        description = "Cast ${topCardComponent.name}",
                                        action = CastSpell(playerId, topCardId),
                                        validTargets = firstInfo.validTargets,
                                        requiresTargets = true,
                                        targetCount = firstReq.count,
                                        minTargets = firstReq.effectiveMinCount,
                                        targetDescription = firstReq.description,
                                        targetRequirements = if (targetInfos.size > 1) targetInfos else null,
                                        hasXCost = hasXCost,
                                        maxAffordableX = maxAffordableX,
                                        manaCostString = manaCostString,
                                        autoTapPreview = autoTapPreview,
                                        sourceZone = "LIBRARY"
                                    )
                                )
                            }
                        } else {
                            result.add(
                                LegalAction(
                                    actionType = "CastSpell",
                                    description = "Cast ${topCardComponent.name}",
                                    action = CastSpell(playerId, topCardId),
                                    hasXCost = hasXCost,
                                    maxAffordableX = maxAffordableX,
                                    manaCostString = manaCostString,
                                    autoTapPreview = autoTapPreview,
                                    sourceZone = "LIBRARY"
                                )
                            )
                        }
                    }
                }
            }

            // Check for morph on top of library (only for PlayFromTopOfLibrary)
            if (canPlayAllFromTop && context.canPlaySorcerySpeed && topCardDef != null) {
                val hasMorph = topCardDef.keywordAbilities.any { it is KeywordAbility.Morph }
                if (hasMorph) {
                    val morphCost = context.costCalculator.calculateFaceDownCost(state, playerId)
                    val canAffordMorph = context.manaSolver.canPay(state, playerId, morphCost, precomputedSources = context.availableManaSources)
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
    // Cards from exile (MayPlayFromExileComponent)
    // =========================================================================

    private fun enumerateExileCards(
        context: EnumerationContext,
        result: MutableList<LegalAction>
    ): MutableSet<EntityId> {
        val state = context.state
        val playerId = context.playerId
        val linkedExileCardIds = mutableSetOf<EntityId>()

        // Check all players' exile zones because cards like Villainous Wealth exile from
        // an opponent's library (cards stay in their owner's exile zone). Graveyards
        // are included so per-card grants that leave the discarded card in the graveyard
        // (e.g. Malcolm, Alluring Scoundrel) are castable without a detour through exile.
        val exileCandidates = state.turnOrder.flatMap { pid -> state.getExile(pid).map { it to "EXILE" } }
        val graveyardCandidates = state.turnOrder.flatMap { pid -> state.getGraveyard(pid).map { it to "GRAVEYARD" } }
        for ((cardId, sourceZoneLabel) in exileCandidates + graveyardCandidates) {
            val container = state.getEntity(cardId) ?: continue
            val exileComponent = container.get<MayPlayFromExileComponent>() ?: continue
            if (exileComponent.controllerId != playerId) continue
            val playForFree = container.get<PlayWithoutPayingCostComponent>()?.controllerId == playerId
            val runtimeAdditionalCost = container.get<PlayWithAdditionalCostComponent>()
                ?.takeIf { it.controllerId == playerId }
            val cardComponent = container.get<CardComponent>() ?: continue

            // Land with play permission
            if (cardComponent.typeLine.isLand) {
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
                if (context.cantCastSpells) {
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
                    val isInstant = cardComponent.typeLine.isInstant
                    val hasCorrectTiming = isInstant || context.canPlaySorcerySpeed
                    val cardDef = context.cardRegistry.getCard(cardComponent.name)
                    val castRestrictions = cardDef?.script?.castRestrictions ?: emptyList()
                    val meetsRestrictions = context.castPermissionUtils.checkCastRestrictions(state, playerId, castRestrictions)
                    val costString = if (playForFree) "{0}" else {
                        val effectiveCost = if (cardDef != null) {
                            context.costCalculator.calculateEffectiveCost(state, cardDef, playerId)
                        } else {
                            cardComponent.manaCost
                        }
                        effectiveCost.toString()
                    }
                    val canAfford = playForFree || run {
                        val effectiveCost = if (cardDef != null) {
                            context.costCalculator.calculateEffectiveCost(state, cardDef, playerId)
                        } else {
                            cardComponent.manaCost
                        }
                        context.manaSolver.canPay(state, playerId, effectiveCost, precomputedSources = context.availableManaSources)
                    }

                    // Build additional cost info from runtime component
                    val exileAdditionalCostInfo = runtimeAdditionalCost?.let { comp ->
                        buildRuntimeAdditionalCostInfo(state, playerId, comp)
                    }
                    val canPayAdditionalCost = exileAdditionalCostInfo == null ||
                        checkRuntimeAdditionalCostAffordability(state, playerId, runtimeAdditionalCost)

                    if (hasCorrectTiming && meetsRestrictions && canAfford && canPayAdditionalCost) {
                        val targetReqs = buildList {
                            addAll(cardDef?.script?.targetRequirements ?: emptyList())
                            cardDef?.script?.auraTarget?.let { add(it) }
                        }

                        if (targetReqs.isNotEmpty()) {
                            val targetInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs)
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
                                        targetCount = firstReq.count,
                                        minTargets = firstReq.effectiveMinCount,
                                        targetDescription = firstReq.description,
                                        targetRequirements = if (targetInfos.size > 1) targetInfos else null,
                                        manaCostString = costString,
                                        sourceZone = sourceZoneLabel,
                                        additionalCostInfo = exileAdditionalCostInfo
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
                                    sourceZone = sourceZoneLabel,
                                    additionalCostInfo = exileAdditionalCostInfo
                                )
                            )
                        }
                    } else {
                        // Can't cast right now — show as unaffordable
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
                    }
                }
            }
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

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val controller = container.get<ControllerComponent>()?.playerId ?: continue
            if (controller != playerId) continue
            val linked = container.get<LinkedExileComponent>() ?: continue
            val entityCard = container.get<CardComponent>() ?: continue
            val cardDef = context.cardRegistry.getCard(entityCard.cardDefinitionId) ?: continue
            val grantAbility = cardDef.script.staticAbilities
                .filterIsInstance<GrantMayCastFromLinkedExile>()
                .firstOrNull() ?: continue

            // Timing restriction — e.g. Dawnhand Dissident's "during your turn"
            if (grantAbility.duringYourTurnOnly && state.activePlayerId != playerId) continue

            // Once-per-turn restriction (Maralen, Fae Ascendant) — skip the granter entirely
            // once it's already been used this turn.
            if (grantAbility.oncePerTurn &&
                container.get<com.wingedsheep.engine.state.components.battlefield.MayCastFromLinkedExileUsedThisTurnComponent>() != null
            ) continue

            // Resolve dynamic mana-value cap (e.g. "spell with mana value ≤ number of Elves
            // and Faeries you control"). Computed once per granter.
            val maxManaValueCap: Int? = grantAbility.maxManaValue?.let { amount ->
                val opponentId = state.turnOrder.firstOrNull { it != playerId }
                val effectContext = com.wingedsheep.engine.handlers.EffectContext(
                    sourceId = entityId,
                    controllerId = playerId,
                    opponentId = opponentId
                )
                com.wingedsheep.engine.handlers.DynamicAmountEvaluator().evaluate(state, amount, effectContext)
            }

            // Pre-compute additional-cost affordability for the granter's optional
            // additional cost (e.g. "remove three counters from your creatures").
            val (linkedAdditionalCostInfo, canPayLinkedAdditionalCost) = buildLinkedExileAdditionalCostInfo(
                state, playerId, grantAbility.additionalCost
            )

            for (exiledId in linked.exiledIds) {
                // Skip if already handled by MayPlayFromExileComponent
                val exiledContainer = state.getEntity(exiledId) ?: continue
                if (exiledContainer.get<MayPlayFromExileComponent>()?.controllerId == playerId) continue
                val exiledCard = exiledContainer.get<CardComponent>() ?: continue

                // Ownership restriction — "cards you own exiled with this creature"
                if (grantAbility.ownedByYou && exiledCard.ownerId != playerId) continue

                // "exiled with this permanent this turn" gating (Maralen, Fae Ascendant)
                if (grantAbility.exiledThisTurnOnly) {
                    val turn = exiledContainer.get<com.wingedsheep.engine.state.components.battlefield.ExileEntryTurnComponent>()?.turnNumber
                    if (turn == null || turn != state.turnNumber) continue
                }

                // Mana-value cap (Maralen)
                if (maxManaValueCap != null && exiledCard.manaCost.cmc > maxManaValueCap) continue

                // Check filter (e.g., nonland)
                val passesFilter = grantAbility.filter.cardPredicates.all { pred ->
                    when (pred) {
                        is CardPredicate.IsNonland -> !exiledCard.typeLine.isLand
                        is CardPredicate.IsCreature -> exiledCard.typeLine.isCreature
                        is CardPredicate.IsArtifact -> exiledCard.typeLine.isArtifact
                        else -> true
                    }
                }
                if (!passesFilter) continue

                // Verify card is actually in exile
                val inExile = state.turnOrder.any { pid -> exiledId in state.getZone(ZoneKey(pid, Zone.EXILE)) }
                if (!inExile) continue
                if (exiledId in linkedExileCardIds) continue
                linkedExileCardIds.add(exiledId)

                // Lands in linked exile cannot be cast (oracle says "spells")
                if (exiledCard.typeLine.isLand) continue

                val exiledCardDef = context.cardRegistry.getCard(exiledCard.name)
                if (context.cantCastSpells) {
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
                    val hasCorrectTiming = isInstant || context.canPlaySorcerySpeed
                    val castRestrictions = exiledCardDef?.script?.castRestrictions ?: emptyList()
                    val meetsRestrictions = context.castPermissionUtils.checkCastRestrictions(state, playerId, castRestrictions)
                    val freeCastFromGranter = grantAbility.withoutPayingManaCost
                    val costString = if (freeCastFromGranter) "0" else run {
                        val effectiveCost = if (exiledCardDef != null) {
                            context.costCalculator.calculateEffectiveCost(state, exiledCardDef, playerId)
                        } else {
                            exiledCard.manaCost
                        }
                        effectiveCost.toString()
                    }
                    val canAfford = if (freeCastFromGranter) true else run {
                        val effectiveCost = if (exiledCardDef != null) {
                            context.costCalculator.calculateEffectiveCost(state, exiledCardDef, playerId)
                        } else {
                            exiledCard.manaCost
                        }
                        context.manaSolver.canPay(state, playerId, effectiveCost, precomputedSources = context.availableManaSources)
                    }

                    val fullyAffordable = hasCorrectTiming && meetsRestrictions && canAfford &&
                        canPayLinkedAdditionalCost
                    if (fullyAffordable) {
                        val targetReqs = buildList {
                            addAll(exiledCardDef?.script?.targetRequirements ?: emptyList())
                            exiledCardDef?.script?.auraTarget?.let { add(it) }
                        }
                        if (targetReqs.isNotEmpty()) {
                            val targetInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs)
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
                                        targetCount = firstReq.count,
                                        minTargets = firstReq.effectiveMinCount,
                                        targetDescription = firstReq.description,
                                        targetRequirements = if (targetInfos.size > 1) targetInfos else null,
                                        manaCostString = costString,
                                        sourceZone = "EXILE",
                                        additionalCostInfo = linkedAdditionalCostInfo
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
                                    sourceZone = "EXILE",
                                    additionalCostInfo = linkedAdditionalCostInfo
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
                                sourceZone = "EXILE",
                                additionalCostInfo = linkedAdditionalCostInfo
                            )
                        )
                    }
                }
            }
        }
    }

    private fun buildLinkedExileAdditionalCostInfo(
        state: GameState,
        playerId: EntityId,
        additionalCost: AdditionalCost?
    ): Pair<AdditionalCostData?, Boolean> {
        if (additionalCost == null) return null to true
        return when (additionalCost) {
            is AdditionalCost.RemoveCountersFromYourCreatures -> {
                val creatures = buildAnyCounterCreatures(state, playerId)
                val totalAvailable = creatures.sumOf { it.availableCounters }
                val canPay = totalAvailable >= additionalCost.totalCount
                val info = AdditionalCostData(
                    description = additionalCost.description,
                    costType = "RemoveCountersFromYourCreatures",
                    counterRemovalCreatures = creatures,
                    distributedCounterRemovalTotal = additionalCost.totalCount
                )
                info to canPay
            }
            else -> AdditionalCostData(
                description = additionalCost.description,
                costType = "Other"
            ) to true
        }
    }

    /**
     * List the player's creatures with at least one counter of any type, alongside a total
     * counter count — used to populate [AdditionalCostData.counterRemovalCreatures] for
     * distributed counter-removal costs like Dawnhand Dissident's.
     */
    private fun buildAnyCounterCreatures(
        state: GameState,
        playerId: EntityId
    ): List<com.wingedsheep.engine.legalactions.CounterRemovalCreatureData> {
        val projected = state.projectedState
        val result = mutableListOf<com.wingedsheep.engine.legalactions.CounterRemovalCreatureData>()
        for (permId in state.getBattlefield()) {
            if (projected.getController(permId) != playerId) continue
            if (!projected.isCreature(permId)) continue
            val container = state.getEntity(permId) ?: continue
            val counters = container.get<com.wingedsheep.engine.state.components.battlefield.CountersComponent>()
                ?: continue
            val total = counters.counters.values.sum()
            if (total <= 0) continue
            val card = container.get<CardComponent>() ?: continue
            result.add(
                com.wingedsheep.engine.legalactions.CounterRemovalCreatureData(
                    entityId = permId,
                    name = card.name,
                    availableCounters = total,
                    imageUri = card.imageUri
                )
            )
        }
        return result
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
                // Skip cards already handled by MayPlayFromExileComponent or linked exile
                if (zone == Zone.EXILE && container.get<MayPlayFromExileComponent>()?.controllerId == playerId) continue
                if (zone == Zone.EXILE && cardId in linkedExileCardIds) continue
                val cardComponent = container.get<CardComponent>() ?: continue
                val cardDef = context.cardRegistry.getCard(cardComponent.name) ?: continue
                val hasCastFromZone = cardDef.script.staticAbilities
                    .filterIsInstance<MayCastSelfFromZones>()
                    .any { zone in it.zones }
                if (!hasCastFromZone) continue

                // Only non-land spells (Squee is a creature)
                if (cardComponent.typeLine.isLand) continue

                val sourceZoneName = zone.name
                if (context.cantCastSpells) {
                    result.add(
                        LegalAction(
                            actionType = "CastSpell",
                            description = "Cast ${cardComponent.name}",
                            action = CastSpell(playerId, cardId),
                            affordable = false,
                            manaCostString = cardComponent.manaCost.toString(),
                            sourceZone = sourceZoneName
                        )
                    )
                } else {
                    val isInstant = cardComponent.typeLine.isInstant
                    val hasCorrectTiming = isInstant || context.canPlaySorcerySpeed
                    val castRestrictions = cardDef.script.castRestrictions
                    val meetsRestrictions = context.castPermissionUtils.checkCastRestrictions(state, playerId, castRestrictions)
                    val effectiveCost = context.costCalculator.calculateEffectiveCost(state, cardDef, playerId)
                    val costString = effectiveCost.toString()
                    val canAfford = context.manaSolver.canPay(state, playerId, effectiveCost, precomputedSources = context.availableManaSources)

                    if (hasCorrectTiming && meetsRestrictions && canAfford) {
                        val targetReqs = buildList {
                            addAll(cardDef.script.targetRequirements)
                            cardDef.script.auraTarget?.let { add(it) }
                        }

                        if (targetReqs.isNotEmpty()) {
                            val targetInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs)
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
                                        targetCount = firstReq.count,
                                        minTargets = firstReq.effectiveMinCount,
                                        targetDescription = firstReq.description,
                                        targetRequirements = if (targetInfos.size > 1) targetInfos else null,
                                        manaCostString = costString,
                                        sourceZone = sourceZoneName
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
                                    sourceZone = sourceZoneName
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
                                sourceZone = sourceZoneName
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
        if (state.activePlayerId != playerId) return

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

            if (context.cantCastSpells) {
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
                val meetsRestrictions = context.castPermissionUtils.checkCastRestrictions(state, playerId, castRestrictions)
                val effectiveCost = context.costCalculator.calculateEffectiveCost(state, cardDef, playerId)
                val costString = effectiveCost.toString()
                val canAfford = context.manaSolver.canPay(state, playerId, effectiveCost, precomputedSources = context.availableManaSources)

                if (hasCorrectTiming && meetsRestrictions && canAfford) {
                    val targetReqs = buildList {
                        addAll(cardDef.script.targetRequirements)
                        cardDef.script.auraTarget?.let { add(it) }
                    }

                    if (targetReqs.isNotEmpty()) {
                        val targetInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs)
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
                                    targetCount = firstReq.count,
                                    minTargets = firstReq.effectiveMinCount,
                                    targetDescription = firstReq.description,
                                    targetRequirements = if (targetInfos.size > 1) targetInfos else null,
                                    manaCostString = costString,
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

            val flashback = cardDef.keywordAbilities
                .filterIsInstance<KeywordAbility.Flashback>()
                .firstOrNull() ?: continue

            // Check timing: instants at instant speed, sorceries at sorcery speed
            val isInstant = cardComponent.typeLine.isInstant
            if (!isInstant && !context.canPlaySorcerySpeed) continue

            if (context.cantCastSpells) {
                result.add(
                    LegalAction(
                        actionType = "CastWithFlashback",
                        description = "Cast ${cardComponent.name} (Flashback)",
                        action = CastSpell(playerId, cardId, useAlternativeCost = true),
                        affordable = false,
                        manaCostString = flashback.cost.toString(),
                        sourceZone = "GRAVEYARD"
                    )
                )
                continue
            }

            // Check cast restrictions
            val castRestrictions = cardDef.script.castRestrictions
            if (!context.castPermissionUtils.checkCastRestrictions(state, playerId, castRestrictions)) continue

            // Calculate effective flashback cost (applying cost reductions/increases)
            val effectiveCost = context.costCalculator.calculateEffectiveCostWithAlternativeBase(
                state, cardDef, flashback.cost, playerId
            )
            val costString = effectiveCost.toString()
            val canAfford = context.manaSolver.canPay(state, playerId, effectiveCost, precomputedSources = context.availableManaSources)

            if (!canAfford) {
                result.add(
                    LegalAction(
                        actionType = "CastWithFlashback",
                        description = "Cast ${cardComponent.name} (Flashback)",
                        action = CastSpell(playerId, cardId, useAlternativeCost = true),
                        affordable = false,
                        manaCostString = costString,
                        sourceZone = "GRAVEYARD"
                    )
                )
                continue
            }

            val targetReqs = buildList {
                addAll(cardDef.script.targetRequirements)
                cardDef.script.auraTarget?.let { add(it) }
            }

            val autoTapPreview = if (context.skipAutoTapPreview) null else {
                context.manaSolver.solve(state, playerId, effectiveCost, precomputedSources = context.availableManaSources)
                    ?.sources?.map { it.entityId }
            }

            if (targetReqs.isNotEmpty()) {
                val targetInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs)
                val allSatisfied = context.targetUtils.allRequirementsSatisfied(targetInfos)
                if (allSatisfied) {
                    val firstReq = targetReqs.first()
                    val firstInfo = targetInfos.first()
                    result.add(
                        LegalAction(
                            actionType = "CastWithFlashback",
                            description = "Cast ${cardComponent.name} (Flashback)",
                            action = CastSpell(playerId, cardId, useAlternativeCost = true),
                            validTargets = firstInfo.validTargets,
                            requiresTargets = true,
                            targetCount = firstReq.count,
                            minTargets = firstReq.effectiveMinCount,
                            targetDescription = firstReq.description,
                            targetRequirements = if (targetInfos.size > 1) targetInfos else null,
                            manaCostString = costString,
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
                        action = CastSpell(playerId, cardId, useAlternativeCost = true),
                        manaCostString = costString,
                        autoTapPreview = autoTapPreview,
                        sourceZone = "GRAVEYARD"
                    )
                )
            }
        }
    }

    // =========================================================================
    // Warp from hand
    // =========================================================================

    private fun enumerateWarpFromHand(
        context: EnumerationContext,
        result: MutableList<LegalAction>
    ) {
        val state = context.state
        val playerId = context.playerId
        val handCards = state.getZone(ZoneKey(playerId, Zone.HAND))

        for (cardId in handCards) {
            val container = state.getEntity(cardId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Skip lands — warp is for spells only
            if (cardComponent.typeLine.isLand) continue

            val cardDef = context.cardRegistry.getCard(cardComponent.cardDefinitionId) ?: continue

            val warpAbility = cardDef.keywordAbilities
                .filterIsInstance<KeywordAbility.Warp>()
                .firstOrNull() ?: continue

            // Warp permanents at sorcery speed, instants at instant speed
            val isInstant = cardComponent.typeLine.isInstant
            if (!isInstant && !context.canPlaySorcerySpeed) continue

            if (context.cantCastSpells) {
                result.add(
                    LegalAction(
                        actionType = "CastWithWarp",
                        description = "Cast ${cardComponent.name} (Warp)",
                        action = CastSpell(playerId, cardId, useAlternativeCost = true),
                        affordable = false,
                        manaCostString = warpAbility.cost.toString(),
                        sourceZone = "HAND"
                    )
                )
                continue
            }

            // Check cast restrictions
            val castRestrictions = cardDef.script.castRestrictions
            if (!context.castPermissionUtils.checkCastRestrictions(state, playerId, castRestrictions)) continue

            val effectiveCost = context.costCalculator.calculateEffectiveCostWithAlternativeBase(
                state, cardDef, warpAbility.cost, playerId
            )
            val costString = effectiveCost.toString()
            val canAfford = context.manaSolver.canPay(state, playerId, effectiveCost, precomputedSources = context.availableManaSources)

            if (!canAfford) {
                result.add(
                    LegalAction(
                        actionType = "CastWithWarp",
                        description = "Cast ${cardComponent.name} (Warp)",
                        action = CastSpell(playerId, cardId, useAlternativeCost = true),
                        affordable = false,
                        manaCostString = costString,
                        sourceZone = "HAND"
                    )
                )
                continue
            }

            val targetReqs = buildList {
                addAll(cardDef.script.targetRequirements)
                cardDef.script.auraTarget?.let { add(it) }
            }

            val autoTapPreview = if (context.skipAutoTapPreview) null else {
                context.manaSolver.solve(state, playerId, effectiveCost, precomputedSources = context.availableManaSources)
                    ?.sources?.map { it.entityId }
            }

            if (targetReqs.isNotEmpty()) {
                val targetInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs)
                val allSatisfied = context.targetUtils.allRequirementsSatisfied(targetInfos)
                if (allSatisfied) {
                    val firstReq = targetReqs.first()
                    val firstInfo = targetInfos.first()
                    result.add(
                        LegalAction(
                            actionType = "CastWithWarp",
                            description = "Cast ${cardComponent.name} (Warp)",
                            action = CastSpell(playerId, cardId, useAlternativeCost = true),
                            validTargets = firstInfo.validTargets,
                            requiresTargets = true,
                            targetCount = firstReq.count,
                            minTargets = firstReq.effectiveMinCount,
                            targetDescription = firstReq.description,
                            targetRequirements = if (targetInfos.size > 1) targetInfos else null,
                            manaCostString = costString,
                            autoTapPreview = autoTapPreview,
                            sourceZone = "HAND"
                        )
                    )
                }
            } else {
                result.add(
                    LegalAction(
                        actionType = "CastWithWarp",
                        description = "Cast ${cardComponent.name} (Warp)",
                        action = CastSpell(playerId, cardId, useAlternativeCost = true),
                        manaCostString = costString,
                        autoTapPreview = autoTapPreview,
                        sourceZone = "HAND"
                    )
                )
            }
        }
    }

    // =========================================================================
    // Warp from exile
    // =========================================================================

    private fun enumerateWarpFromExile(
        context: EnumerationContext,
        result: MutableList<LegalAction>
    ) {
        val state = context.state
        val playerId = context.playerId

        // Check all players' exile zones for cards with WarpExiledComponent
        val exileCards = state.turnOrder.flatMap { pid -> state.getExile(pid) }

        for (cardId in exileCards) {
            val container = state.getEntity(cardId) ?: continue
            val warpExiled = container.get<WarpExiledComponent>() ?: continue
            if (warpExiled.controllerId != playerId) continue

            val cardComponent = container.get<CardComponent>() ?: continue
            val cardDef = context.cardRegistry.getCard(cardComponent.cardDefinitionId) ?: continue

            val warpAbility = cardDef.keywordAbilities
                .filterIsInstance<KeywordAbility.Warp>()
                .firstOrNull() ?: continue

            // Warp is creature-speed (sorcery speed) — only permanents at sorcery speed
            if (!context.canPlaySorcerySpeed) continue

            if (context.cantCastSpells) {
                result.add(
                    LegalAction(
                        actionType = "CastWithWarp",
                        description = "Cast ${cardComponent.name} (Warp)",
                        action = CastSpell(playerId, cardId, useAlternativeCost = true),
                        affordable = false,
                        manaCostString = warpAbility.cost.toString(),
                        sourceZone = "EXILE"
                    )
                )
                continue
            }

            // Check cast restrictions
            val castRestrictions = cardDef.script.castRestrictions
            if (!context.castPermissionUtils.checkCastRestrictions(state, playerId, castRestrictions)) continue

            // Calculate effective warp cost (applying cost reductions/increases)
            val effectiveCost = context.costCalculator.calculateEffectiveCostWithAlternativeBase(
                state, cardDef, warpAbility.cost, playerId
            )
            val costString = effectiveCost.toString()
            val canAfford = context.manaSolver.canPay(state, playerId, effectiveCost, precomputedSources = context.availableManaSources)

            if (!canAfford) {
                result.add(
                    LegalAction(
                        actionType = "CastWithWarp",
                        description = "Cast ${cardComponent.name} (Warp)",
                        action = CastSpell(playerId, cardId, useAlternativeCost = true),
                        affordable = false,
                        manaCostString = costString,
                        sourceZone = "EXILE"
                    )
                )
                continue
            }

            val targetReqs = buildList {
                addAll(cardDef.script.targetRequirements)
                cardDef.script.auraTarget?.let { add(it) }
            }

            val autoTapPreview = if (context.skipAutoTapPreview) null else {
                context.manaSolver.solve(state, playerId, effectiveCost, precomputedSources = context.availableManaSources)
                    ?.sources?.map { it.entityId }
            }

            if (targetReqs.isNotEmpty()) {
                val targetInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs)
                val allSatisfied = context.targetUtils.allRequirementsSatisfied(targetInfos)
                if (allSatisfied) {
                    val firstReq = targetReqs.first()
                    val firstInfo = targetInfos.first()
                    result.add(
                        LegalAction(
                            actionType = "CastWithWarp",
                            description = "Cast ${cardComponent.name} (Warp)",
                            action = CastSpell(playerId, cardId, useAlternativeCost = true),
                            validTargets = firstInfo.validTargets,
                            requiresTargets = true,
                            targetCount = firstReq.count,
                            minTargets = firstReq.effectiveMinCount,
                            targetDescription = firstReq.description,
                            targetRequirements = if (targetInfos.size > 1) targetInfos else null,
                            manaCostString = costString,
                            autoTapPreview = autoTapPreview,
                            sourceZone = "EXILE"
                        )
                    )
                }
            } else {
                result.add(
                    LegalAction(
                        actionType = "CastWithWarp",
                        description = "Cast ${cardComponent.name} (Warp)",
                        action = CastSpell(playerId, cardId, useAlternativeCost = true),
                        manaCostString = costString,
                        autoTapPreview = autoTapPreview,
                        sourceZone = "EXILE"
                    )
                )
            }
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
        if (state.activePlayerId != playerId) return

        val graveyardCards = state.getZone(ZoneKey(playerId, Zone.GRAVEYARD))

        // Check if forage can be paid at all (Food on battlefield)
        val projected = state.projectedState
        val hasFood = state.getBattlefield().any { permId ->
            state.getEntity(permId) ?: return@any false
            projected.getController(permId) == playerId &&
                projected.hasSubtype(permId, Subtype.FOOD.value)
        }

        for (cardId in graveyardCards) {
            val container = state.getEntity(cardId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Only creature spells
            if (!cardComponent.typeLine.isCreature) continue

            val cardDef = context.cardRegistry.getCard(cardComponent.name) ?: continue

            // Don't offer forage if it can't be paid (< 3 other graveyard cards and no Food)
            val otherGraveyardCards = graveyardCards.filter { it != cardId }
            if (otherGraveyardCards.size < 3 && !hasFood) continue

            if (context.cantCastSpells) {
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
            val meetsRestrictions = context.castPermissionUtils.checkCastRestrictions(state, playerId, castRestrictions)
            if (!meetsRestrictions) continue

            val effectiveCost = context.costCalculator.calculateEffectiveCost(state, cardDef, playerId)
            val costString = effectiveCost.toString()
            val affordable = context.manaSolver.canPay(state, playerId, effectiveCost, precomputedSources = context.availableManaSources)

            if (affordable) {
                val targetReqs = buildList {
                    addAll(cardDef.script.targetRequirements)
                    cardDef.script.auraTarget?.let { add(it) }
                }

                if (targetReqs.isNotEmpty()) {
                    val targetInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs)
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
                                targetCount = firstReq.count,
                                minTargets = firstReq.effectiveMinCount,
                                targetDescription = firstReq.description,
                                targetRequirements = if (targetInfos.size > 1) targetInfos else null,
                                manaCostString = costString,
                                sourceZone = "GRAVEYARD",
                                requiresForage = true
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
                            sourceZone = "GRAVEYARD",
                            requiresForage = true
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
                        sourceZone = "GRAVEYARD",
                        requiresForage = true
                    )
                )
            }
        }
    }

    // =========================================================================
    // Cast from graveyard with additional life cost (MayCastFromGraveyardWithLifeCost)
    // =========================================================================

    private fun enumerateGraveyardWithLifeCost(
        context: EnumerationContext,
        result: MutableList<LegalAction>
    ) {
        val state = context.state
        val playerId = context.playerId

        // Find permanents with MayCastFromGraveyardWithLifeCost static ability
        val permissions = mutableListOf<MayCastFromGraveyardWithLifeCost>()
        for (permId in state.getBattlefield()) {
            val container = state.getEntity(permId) ?: continue
            val controller = container.get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()?.playerId
            if (controller != playerId) continue
            val cardComp = container.get<CardComponent>() ?: continue
            val cardDef = context.cardRegistry.getCard(cardComp.cardDefinitionId) ?: continue
            for (sa in cardDef.script.staticAbilities) {
                if (sa is MayCastFromGraveyardWithLifeCost) {
                    permissions.add(sa)
                }
            }
        }

        if (permissions.isEmpty()) return

        // Check timing restrictions
        for (permission in permissions) {
            if (permission.duringYourTurnOnly && state.activePlayerId != playerId) continue

            val graveyardCards = state.getZone(ZoneKey(playerId, Zone.GRAVEYARD))
            for (cardId in graveyardCards) {
                val container = state.getEntity(cardId) ?: continue
                val cardComponent = container.get<CardComponent>() ?: continue
                val cardDef = context.cardRegistry.getCard(cardComponent.cardDefinitionId) ?: continue

                // Check if card matches filter
                if (!context.predicateEvaluator.matches(
                        state, cardId, permission.filter,
                        PredicateContext(controllerId = playerId)
                    )
                ) continue

                // Check timing: instants at instant speed, sorceries at sorcery speed
                val isInstant = cardComponent.typeLine.isInstant
                if (!isInstant && !context.canPlaySorcerySpeed) continue

                if (context.cantCastSpells) continue

                // Check cast restrictions
                val castRestrictions = cardDef.script.castRestrictions
                if (!context.castPermissionUtils.checkCastRestrictions(state, playerId, castRestrictions)) continue

                // Check life affordability
                val currentLife = state.getEntity(playerId)
                    ?.get<com.wingedsheep.engine.state.components.identity.LifeTotalComponent>()?.life ?: 0
                if (currentLife < permission.lifeCost) continue

                val effectiveCost = context.costCalculator.calculateEffectiveCost(state, cardDef, playerId)
                val costString = effectiveCost.toString()
                val canAfford = context.manaSolver.canPay(state, playerId, effectiveCost, precomputedSources = context.availableManaSources)

                if (!canAfford) {
                    result.add(
                        LegalAction(
                            actionType = "CastSpell",
                            description = "Cast ${cardComponent.name} (pay ${permission.lifeCost} life)",
                            action = CastSpell(playerId, cardId, graveyardLifeCost = permission.lifeCost),
                            affordable = false,
                            manaCostString = costString,
                            sourceZone = "GRAVEYARD",
                            additionalLifeCost = permission.lifeCost
                        )
                    )
                    continue
                }

                val targetReqs = buildList {
                    addAll(cardDef.script.targetRequirements)
                    cardDef.script.auraTarget?.let { add(it) }
                }

                val autoTapPreview = if (context.skipAutoTapPreview) null else {
                    context.manaSolver.solve(state, playerId, effectiveCost, precomputedSources = context.availableManaSources)
                        ?.sources?.map { it.entityId }
                }

                if (targetReqs.isNotEmpty()) {
                    val targetInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs)
                    val allSatisfied = context.targetUtils.allRequirementsSatisfied(targetInfos)
                    if (allSatisfied) {
                        val firstReq = targetReqs.first()
                        val firstInfo = targetInfos.first()
                        result.add(
                            LegalAction(
                                actionType = "CastSpell",
                                description = "Cast ${cardComponent.name} (pay ${permission.lifeCost} life)",
                                action = CastSpell(playerId, cardId, graveyardLifeCost = permission.lifeCost),
                                validTargets = firstInfo.validTargets,
                                requiresTargets = true,
                                targetCount = firstReq.count,
                                minTargets = firstReq.effectiveMinCount,
                                targetDescription = firstReq.description,
                                targetRequirements = if (targetInfos.size > 1) targetInfos else null,
                                manaCostString = costString,
                                autoTapPreview = autoTapPreview,
                                sourceZone = "GRAVEYARD",
                                additionalLifeCost = permission.lifeCost
                            )
                        )
                    }
                } else {
                    result.add(
                        LegalAction(
                            actionType = "CastSpell",
                            description = "Cast ${cardComponent.name} (pay ${permission.lifeCost} life)",
                            action = CastSpell(playerId, cardId, graveyardLifeCost = permission.lifeCost),
                            manaCostString = costString,
                            autoTapPreview = autoTapPreview,
                            sourceZone = "GRAVEYARD",
                            additionalLifeCost = permission.lifeCost
                        )
                    )
                }
            }
        }
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

        for ((cardId, originalAction) in zoneCastActions) {
            val originalCast = originalAction.action as CastSpell
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
            if (cardComponent.typeLine.isLand) continue

            val cardDef = context.cardRegistry.getCard(cardComponent.name) ?: continue
            val manaKicker = cardDef.keywordAbilities
                .filterIsInstance<KeywordAbility.Kicker>()
                .firstOrNull()
            val additionalCostKicker = cardDef.keywordAbilities
                .filterIsInstance<KeywordAbility.KickerWithAdditionalCost>()
                .firstOrNull()
            val offspringAbility = cardDef.keywordAbilities
                .filterIsInstance<KeywordAbility.Offspring>()
                .firstOrNull()
            if (manaKicker == null && additionalCostKicker == null && offspringAbility == null) continue

            val sourceZone = originalAction.sourceZone

            // Calculate kicked cost
            val baseCost = context.costCalculator.calculateEffectiveCost(state, cardDef, playerId)
            val kickedCost = if (manaKicker != null) {
                baseCost + manaKicker.cost
            } else if (offspringAbility != null) {
                baseCost + offspringAbility.cost
            } else {
                baseCost
            }
            val kickedSpellContext = SpellPaymentContext(
                isInstantOrSorcery = cardComponent.typeLine.isInstant || cardComponent.typeLine.isSorcery,
                isKicked = true,
                isCreature = cardComponent.typeLine.isCreature,
                manaValue = cardComponent.manaCost.cmc,
                hasXInCost = cardComponent.manaCost.hasX
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
            if (additionalCostKicker != null) {
                when (val cost = additionalCostKicker.cost) {
                    is AdditionalCost.SacrificePermanent -> {
                        val validSacTargets = context.costUtils.findSacrificeTargets(state, playerId, cost)
                        if (validSacTargets.size < cost.count) {
                            canPayKickerAdditionalCost = false
                        } else {
                            kickerCostInfo = AdditionalCostData(
                                description = cost.description,
                                costType = "SacrificePermanent",
                                validSacrificeTargets = validSacTargets,
                                sacrificeCount = cost.count
                            )
                        }
                    }
                    else -> {}
                }
            }

            val canAffordKicked = canAffordKickedMana && canPayKickerAdditionalCost

            // Build target info — use kickerTargetRequirements if available
            val kickerBaseReqs = if (cardDef.script.kickerTargetRequirements.isNotEmpty()) {
                cardDef.script.kickerTargetRequirements
            } else {
                cardDef.script.targetRequirements
            }
            val targetReqs = buildList {
                addAll(kickerBaseReqs)
                cardDef.script.auraTarget?.let { add(it) }
            }

            val kickLabel = if (offspringAbility != null) "Offspring" else "Kicked"

            // Check for DividedDamageEffect in the kicked spell effect
            val kickerSpellEffect = cardDef.script.kickerSpellEffect ?: cardDef.script.spellEffect
            val kickerDividedDamage = kickerSpellEffect as? DividedDamageEffect
            val kickerRequiresDamageDistribution = kickerDividedDamage != null
            val kickerTotalDamage = kickerDividedDamage?.totalDamage
            val kickerMinDamagePerTarget = if (kickerDividedDamage != null) 1 else null

            if (targetReqs.isNotEmpty()) {
                val targetReqInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs)
                val allRequirementsSatisfied = context.targetUtils.allRequirementsSatisfied(targetReqInfos)
                if (allRequirementsSatisfied) {
                    val firstReq = targetReqs.first()
                    val firstReqInfo = targetReqInfos.first()

                    val canAutoSelect = targetReqs.size == 1 &&
                        context.targetUtils.shouldAutoSelectPlayerTarget(firstReq, firstReqInfo.validTargets)

                    if (canAutoSelect) {
                        val autoSelectedTarget = ChosenTarget.Player(firstReqInfo.validTargets.first())
                        kickerActions.add(LegalAction(
                            actionType = "CastWithKicker",
                            description = "Cast ${cardComponent.name} ($kickLabel)",
                            action = CastSpell(playerId, cardId, targets = listOf(autoSelectedTarget), wasKicked = true, graveyardLifeCost = originalCast.graveyardLifeCost),
                            affordable = canAffordKicked,
                            manaCostString = kickedCostString,
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
                            description = "Cast ${cardComponent.name} ($kickLabel)",
                            action = CastSpell(playerId, cardId, wasKicked = true, graveyardLifeCost = originalCast.graveyardLifeCost),
                            validTargets = firstReqInfo.validTargets,
                            requiresTargets = true,
                            targetCount = firstReq.count,
                            minTargets = firstReq.effectiveMinCount,
                            targetDescription = firstReq.description,
                            targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                            affordable = canAffordKicked,
                            manaCostString = kickedCostString,
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
                    description = "Cast ${cardComponent.name} ($kickLabel)",
                    action = CastSpell(playerId, cardId, wasKicked = true, graveyardLifeCost = originalCast.graveyardLifeCost),
                    affordable = canAffordKicked,
                    manaCostString = kickedCostString,
                    autoTapPreview = kickedAutoTapPreview,
                    additionalCostInfo = kickerCostInfo,
                    sourceZone = sourceZone,
                    additionalLifeCost = originalAction.additionalLifeCost
                ))
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
        component: PlayWithAdditionalCostComponent
    ): AdditionalCostData? {
        val cost = component.additionalCosts.firstOrNull() ?: return null
        return when (cost) {
            is com.wingedsheep.sdk.scripting.AdditionalCost.DiscardCards -> {
                val handCards = state.getZone(ZoneKey(playerId, Zone.HAND))
                AdditionalCostData(
                    description = cost.description,
                    costType = "DiscardCard",
                    validDiscardTargets = handCards.toList(),
                    discardCount = cost.count
                )
            }
            else -> AdditionalCostData(
                description = cost.description,
                costType = "Other"
            )
        }
    }

    private fun checkRuntimeAdditionalCostAffordability(
        state: GameState,
        playerId: EntityId,
        component: PlayWithAdditionalCostComponent
    ): Boolean {
        for (cost in component.additionalCosts) {
            when (cost) {
                is com.wingedsheep.sdk.scripting.AdditionalCost.DiscardCards -> {
                    val handSize = state.getZone(ZoneKey(playerId, Zone.HAND)).size
                    if (handSize < cost.count) return false
                }
                else -> {} // Other cost types can be added as needed
            }
        }
        return true
    }
}
