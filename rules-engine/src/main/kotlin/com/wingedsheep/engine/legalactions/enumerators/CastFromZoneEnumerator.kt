package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.MayPlayFromExileComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GrantMayCastFromLinkedExile
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

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

        if (!canPlayAllFromTop && castFromTopFilter == null) return

        val library = state.getLibrary(playerId)
        if (library.isEmpty()) return

        val topCardId = library.first()
        val topCardComponent = state.getEntity(topCardId)?.get<CardComponent>() ?: return
        val topCardDef = context.cardRegistry.getCard(topCardComponent.name)

        // Land on top of library (only for PlayFromTopOfLibrary, not filtered cast)
        if (canPlayAllFromTop && topCardComponent.typeLine.isLand && context.canPlayLand) {
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
                    val canAfford = context.manaSolver.canPay(state, playerId, topEffectiveCost)
                    if (canAfford) {
                        val targetReqs = buildList {
                            addAll(topCardDef?.script?.targetRequirements ?: emptyList())
                            topCardDef?.script?.auraTarget?.let { add(it) }
                        }

                        val manaCostString = topEffectiveCost.toString()
                        val hasXCost = topEffectiveCost.hasX
                        val maxAffordableX: Int? = if (hasXCost) {
                            val availableSources = context.manaSolver.getAvailableManaCount(state, playerId)
                            val fixedCost = topEffectiveCost.cmc
                            val xSymbolCount = topEffectiveCost.xCount.coerceAtLeast(1)
                            ((availableSources - fixedCost) / xSymbolCount).coerceAtLeast(0)
                        } else null
                        val autoTapSolution = context.manaSolver.solve(state, playerId, topEffectiveCost)
                        val autoTapPreview = autoTapSolution?.sources?.map { it.entityId }

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
                    val canAffordMorph = context.manaSolver.canPay(state, playerId, morphCost)
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
        // an opponent's library (cards stay in their owner's exile zone).
        val exileZone = state.turnOrder.flatMap { pid -> state.getExile(pid) }
        for (cardId in exileZone) {
            val container = state.getEntity(cardId) ?: continue
            val exileComponent = container.get<MayPlayFromExileComponent>() ?: continue
            if (exileComponent.controllerId != playerId) continue
            val playForFree = container.get<PlayWithoutPayingCostComponent>()?.controllerId == playerId
            val cardComponent = container.get<CardComponent>() ?: continue

            // Land in exile
            if (cardComponent.typeLine.isLand) {
                result.add(
                    LegalAction(
                        actionType = "PlayLand",
                        description = "Play ${cardComponent.name}",
                        action = PlayLand(playerId, cardId),
                        affordable = context.canPlayLand,
                        sourceZone = "EXILE"
                    )
                )
            }

            // Non-land spell from exile
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
                            sourceZone = "EXILE"
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
                        context.manaSolver.canPay(state, playerId, effectiveCost)
                    }

                    if (hasCorrectTiming && meetsRestrictions && canAfford) {
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
                                        sourceZone = "EXILE"
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
                                    sourceZone = "EXILE"
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
                                sourceZone = "EXILE"
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

            for (exiledId in linked.exiledIds) {
                // Skip if already handled by MayPlayFromExileComponent
                val exiledContainer = state.getEntity(exiledId) ?: continue
                if (exiledContainer.get<MayPlayFromExileComponent>()?.controllerId == playerId) continue
                val exiledCard = exiledContainer.get<CardComponent>() ?: continue

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
                    val costString = run {
                        val effectiveCost = if (exiledCardDef != null) {
                            context.costCalculator.calculateEffectiveCost(state, exiledCardDef, playerId)
                        } else {
                            exiledCard.manaCost
                        }
                        effectiveCost.toString()
                    }
                    val canAfford = run {
                        val effectiveCost = if (exiledCardDef != null) {
                            context.costCalculator.calculateEffectiveCost(state, exiledCardDef, playerId)
                        } else {
                            exiledCard.manaCost
                        }
                        context.manaSolver.canPay(state, playerId, effectiveCost)
                    }

                    if (hasCorrectTiming && meetsRestrictions && canAfford) {
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
                                        sourceZone = "EXILE"
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
                                    sourceZone = "EXILE"
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
                                sourceZone = "EXILE"
                            )
                        )
                    }
                }
            }
        }
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
                    val canAfford = context.manaSolver.canPay(state, playerId, effectiveCost)

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
                val canAfford = context.manaSolver.canPay(state, playerId, effectiveCost)

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
}
