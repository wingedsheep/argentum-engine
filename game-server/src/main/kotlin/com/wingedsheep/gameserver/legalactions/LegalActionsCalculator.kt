package com.wingedsheep.gameserver.legalactions

import com.wingedsheep.engine.core.*
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.battlefield.GrantsControllerHexproofComponent
import com.wingedsheep.engine.state.components.battlefield.GrantsControllerShroudComponent
import com.wingedsheep.engine.state.components.player.CantCastSpellsComponent
import com.wingedsheep.engine.state.components.player.PlayerShroudComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.combat.AttackersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ChosenCreatureTypeComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.MayPlayFromExileComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.gameserver.protocol.AdditionalCostInfo
import com.wingedsheep.gameserver.protocol.CounterRemovalCreatureInfo
import com.wingedsheep.gameserver.protocol.ConvokeCreatureInfo
import com.wingedsheep.gameserver.protocol.CrewCreatureInfo
import com.wingedsheep.gameserver.protocol.DelveCardInfo
import com.wingedsheep.gameserver.protocol.LegalActionInfo
import com.wingedsheep.gameserver.protocol.LegalActionTargetInfo
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.CastRestriction
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.effects.DividedDamageEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.CanBlockAnyNumber
import com.wingedsheep.sdk.scripting.ExtraLoyaltyActivation
import com.wingedsheep.sdk.scripting.GrantActivatedAbilityToAttachedCreature
import com.wingedsheep.sdk.scripting.GrantActivatedAbilityToCreatureGroup
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.GrantMayCastFromLinkedExile
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import com.wingedsheep.sdk.scripting.MayPlayPermanentsFromGraveyard
import com.wingedsheep.sdk.scripting.PlayFromTopOfLibrary
import com.wingedsheep.engine.state.components.battlefield.GraveyardPlayPermissionUsedComponent
import com.wingedsheep.sdk.scripting.PreventCycling
import com.wingedsheep.sdk.scripting.effects.AddAnyColorManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaOfColorAmongEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(LegalActionsCalculator::class.java)

class LegalActionsCalculator(
    private val cardRegistry: CardRegistry,
    private val manaSolver: ManaSolver,
    private val costCalculator: CostCalculator,
    private val predicateEvaluator: PredicateEvaluator,
    private val conditionEvaluator: ConditionEvaluator,
    private val turnManager: TurnManager
) {
    fun calculate(state: GameState, playerId: EntityId): List<LegalActionInfo> {
        // Declaring attackers/blockers is a turn-based action that happens before priority (CR 507/508).
        // Only offer the declaration action — no spells, abilities, or PassPriority.
        if (state.step == Step.DECLARE_ATTACKERS && state.activePlayerId == playerId) {
            val attackersAlreadyDeclared = state.getEntity(playerId)
                ?.get<AttackersDeclaredThisCombatComponent>() != null
            if (!attackersAlreadyDeclared) {
                val validAttackers = turnManager.getValidAttackers(state, playerId)
                // Find opponent planeswalkers that can be attacked
                val projected = state.projectedState
                val opponents = state.turnOrder.filter { it != playerId }
                val validAttackTargets = state.getBattlefield().filter { entityId ->
                    projected.isPlaneswalker(entityId) &&
                        projected.getController(entityId) in opponents
                }
                return listOf(LegalActionInfo(
                    actionType = "DeclareAttackers",
                    description = "Declare attackers",
                    action = DeclareAttackers(playerId, emptyMap()),
                    validAttackers = validAttackers,
                    validAttackTargets = validAttackTargets.ifEmpty { null }
                ))
            }
        }
        if (state.step == Step.DECLARE_BLOCKERS && state.activePlayerId != playerId) {
            val blockersAlreadyDeclared = state.getEntity(playerId)
                ?.get<BlockersDeclaredThisCombatComponent>() != null
            if (!blockersAlreadyDeclared) {
                val validBlockers = turnManager.getValidBlockers(state, playerId)
                val projected = state.projectedState
                val blockerMaxBlockCounts = mutableMapOf<EntityId, Int>()
                for (blockerId in validBlockers) {
                    val container = state.getEntity(blockerId) ?: continue
                    val card = container.get<CardComponent>() ?: continue
                    val isFaceDown = container.has<FaceDownComponent>()
                    val canBlockAny = if (!isFaceDown) {
                        val cardDef = cardRegistry.getCard(card.name)
                        cardDef?.staticAbilities?.any { it is CanBlockAnyNumber } == true
                    } else false
                    if (canBlockAny) {
                        blockerMaxBlockCounts[blockerId] = Int.MAX_VALUE
                    } else {
                        val additionalBlocks = projected.getAdditionalBlockCount(blockerId)
                        if (additionalBlocks > 0) {
                            blockerMaxBlockCounts[blockerId] = 1 + additionalBlocks
                        }
                    }
                }
                val mandatoryAssignments = turnManager.getMandatoryBlockerAssignments(state, playerId)
                return listOf(LegalActionInfo(
                    actionType = "DeclareBlockers",
                    description = "Declare blockers",
                    action = DeclareBlockers(playerId, emptyMap()),
                    validBlockers = validBlockers,
                    blockerMaxBlockCounts = blockerMaxBlockCounts.ifEmpty { null },
                    mandatoryBlockerAssignments = mandatoryAssignments.ifEmpty { null }
                ))
            }
        }

        val result = mutableListOf<LegalActionInfo>()

        // Pass priority is always available when you have priority
        result.add(LegalActionInfo(
            actionType = "PassPriority",
            description = "Pass priority",
            action = PassPriority(playerId)
        ))

        // Check for playable lands (during main phase, with land drop available)
        val landDrops = state.getEntity(playerId)?.get<LandDropsComponent>()
        val canPlayLand = state.step.isMainPhase &&
            state.stack.isEmpty() &&
            state.activePlayerId == playerId &&
            (landDrops?.canPlayLand ?: false)

        if (canPlayLand) {
            val hand = state.getHand(playerId)
            for (cardId in hand) {
                val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
                if (cardComponent.typeLine.isLand) {
                    result.add(LegalActionInfo(
                        actionType = "PlayLand",
                        description = "Play ${cardComponent.name}",
                        action = PlayLand(playerId, cardId)
                    ))
                }
            }

            // Check for lands in graveyard playable via MayPlayPermanentsFromGraveyard (Muldrotha)
            if (hasGraveyardPlayPermissionForType(state, playerId, "LAND")) {
                val graveyardCards = state.getZone(ZoneKey(playerId, Zone.GRAVEYARD))
                for (cardId in graveyardCards) {
                    val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
                    if (cardComponent.typeLine.isLand) {
                        result.add(LegalActionInfo(
                            actionType = "PlayLand",
                            description = "Play ${cardComponent.name}",
                            action = PlayLand(playerId, cardId),
                            sourceZone = "GRAVEYARD"
                        ))
                    }
                }
            }
        }

        // Pre-compute available mana sources for pre-cast selection UI
        val manaSourceInfos = buildManaSourceInfos(state, playerId)

        // Check for castable spells (non-instant only at sorcery speed)
        val canPlaySorcerySpeed = state.step.isMainPhase &&
            state.stack.isEmpty() &&
            state.activePlayerId == playerId

        val hand = state.getHand(playerId)

        // Pre-compute alternative casting costs from battlefield permanents (e.g., Jodah)
        val alternativeCastingCosts = costCalculator.findAlternativeCastingCosts(state, playerId)

        // Check for morph cards that can be cast face-down (sorcery speed only)
        if (canPlaySorcerySpeed) {
            val morphCost = costCalculator.calculateFaceDownCost(state, playerId)
            val canAffordMorph = manaSolver.canPay(state, playerId, morphCost)
            val morphAutoTapSolution = manaSolver.solve(state, playerId, morphCost)
            val morphAutoTapPreview = morphAutoTapSolution?.sources?.map { it.entityId }
            for (cardId in hand) {
                val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
                val cardDef = cardRegistry.getCard(cardComponent.name) ?: continue

                // Check if card has Morph keyword
                val hasMorph = cardDef.keywordAbilities
                    .any { it is com.wingedsheep.sdk.scripting.KeywordAbility.Morph }

                if (hasMorph) {
                    // Add morph action (affordable or not) - client shows greyed out if unaffordable
                    result.add(LegalActionInfo(
                        actionType = "CastFaceDown",
                        description = "Cast ${cardComponent.name} face-down",
                        action = CastSpell(playerId, cardId, castFaceDown = true),
                        isAffordable = canAffordMorph,
                        manaCostString = morphCost.toString(),
                        autoTapPreview = morphAutoTapPreview
                    ))

                    // Check if we can afford to cast normally - if not, add unaffordable cast action
                    // This ensures the player sees both options in the cast modal
                    val normalEffectiveCost = costCalculator.calculateEffectiveCost(state, cardDef, playerId)
                    val canAffordNormal = manaSolver.canPay(state, playerId, normalEffectiveCost)
                    if (!canAffordNormal) {
                        result.add(LegalActionInfo(
                            actionType = "CastSpell",
                            description = "Cast ${cardComponent.name}",
                            action = CastSpell(playerId, cardId),
                            isAffordable = false,
                            manaCostString = normalEffectiveCost.toString()
                        ))
                    }
                }
            }
        }

        // Check if player is restricted from casting spells (e.g., Xantid Swarm)
        val cantCastSpells = state.getEntity(playerId)?.has<CantCastSpellsComponent>() == true

        for (cardId in hand) {
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
            if (!cardComponent.typeLine.isLand) {
                // Skip all spells if player can't cast spells this turn
                if (cantCastSpells) continue

                // Look up card definition for target requirements and cast restrictions
                val cardDef = cardRegistry.getCard(cardComponent.name)
                if (cardDef == null) {
                    logger.warn("Card definition not found in registry: '${cardComponent.name}'. Registry has ${cardRegistry.size} cards.")
                }

                // Check cast restrictions first
                val castRestrictions = cardDef?.script?.castRestrictions ?: emptyList()
                if (!checkCastRestrictions(state, playerId, castRestrictions)) {
                    continue // Skip this card if cast restrictions are not met
                }

                // Check timing - sorcery-speed spells need main phase, empty stack, your turn
                val isInstant = cardComponent.typeLine.isInstant
                val hasFlash = cardDef?.keywords?.contains(Keyword.FLASH) == true
                val grantedFlash = hasFlash || hasGrantedFlash(state, cardId)
                if (isInstant || grantedFlash || canPlaySorcerySpeed) {
                    // Check additional cost payability
                    val additionalCosts = cardDef?.script?.additionalCosts ?: emptyList()
                    val sacrificeTargets = mutableListOf<EntityId>()
                    var variableSacrificeTargets = emptyList<EntityId>()
                    var variableSacrificeReduction = 0
                    var exileTargets = emptyList<EntityId>()
                    var exileMinCount = 0
                    var discardTargets = emptyList<EntityId>()
                    var discardCount = 0
                    var canPayAdditionalCosts = true
                    for (cost in additionalCosts) {
                        when (cost) {
                            is com.wingedsheep.sdk.scripting.AdditionalCost.SacrificePermanent -> {
                                val validSacTargets = findSacrificeTargets(state, playerId, cost)
                                if (validSacTargets.size < cost.count) {
                                    canPayAdditionalCosts = false
                                }
                                sacrificeTargets.addAll(validSacTargets)
                            }
                            is com.wingedsheep.sdk.scripting.AdditionalCost.SacrificeCreaturesForCostReduction -> {
                                // Always payable (0 sacrifices is valid)
                                val validSacTargets = findVariableSacrificeTargets(state, playerId, cost.filter)
                                variableSacrificeTargets = validSacTargets
                                variableSacrificeReduction = cost.costReductionPerCreature
                            }
                            is com.wingedsheep.sdk.scripting.AdditionalCost.ExileVariableCards -> {
                                val validExileTargets = findExileTargets(state, playerId, cost.filter, cost.fromZone)
                                if (validExileTargets.size < cost.minCount) {
                                    canPayAdditionalCosts = false
                                }
                                exileTargets = validExileTargets
                                exileMinCount = cost.minCount
                            }
                            is com.wingedsheep.sdk.scripting.AdditionalCost.ExileCards -> {
                                val validExileTargets = findExileTargets(state, playerId, cost.filter, cost.fromZone)
                                if (validExileTargets.size < cost.count) {
                                    canPayAdditionalCosts = false
                                }
                                exileTargets = validExileTargets
                                exileMinCount = cost.count
                            }
                            is com.wingedsheep.sdk.scripting.AdditionalCost.DiscardCards -> {
                                val handZone = ZoneKey(playerId, Zone.HAND)
                                val handCards = state.getZone(handZone)
                                    .filter { it != cardId } // Exclude the card being cast
                                val context = PredicateContext(controllerId = playerId)
                                val validDiscards = if (cost.filter == com.wingedsheep.sdk.scripting.GameObjectFilter.Any) {
                                    handCards
                                } else {
                                    handCards.filter { predicateEvaluator.matches(state, it, cost.filter, context) }
                                }
                                if (validDiscards.size < cost.count) {
                                    canPayAdditionalCosts = false
                                }
                                discardTargets = validDiscards
                                discardCount = cost.count
                            }
                            else -> {}
                        }
                    }
                    if (!canPayAdditionalCosts) continue

                    // Calculate effective cost after reductions (e.g., Goblin Warchief)
                    var effectiveCost = if (cardDef != null) {
                        costCalculator.calculateEffectiveCost(state, cardDef, playerId)
                    } else {
                        cardComponent.manaCost
                    }

                    // Apply maximum possible sacrifice cost reduction for affordability check
                    if (variableSacrificeTargets.isNotEmpty() && variableSacrificeReduction > 0) {
                        val maxReduction = variableSacrificeTargets.size * variableSacrificeReduction
                        effectiveCost = effectiveCost.reduceGeneric(maxReduction)
                    }

                    // Check mana affordability (including Convoke/Delve if available)
                    val hasConvoke = cardDef?.keywords?.contains(Keyword.CONVOKE) == true
                    val convokeCreatures = if (hasConvoke) {
                        findConvokeCreatures(state, playerId)
                    } else null

                    val hasDelve = cardDef?.keywords?.contains(Keyword.DELVE) == true
                    val delveCards = if (hasDelve) {
                        findDelveCards(state, playerId)
                    } else null
                    val minDelveNeeded = if (hasDelve && delveCards != null && delveCards.isNotEmpty()) {
                        calculateMinDelveNeeded(state, playerId, effectiveCost, delveCards)
                    } else null

                    // For Convoke/Delve spells, check if affordable with alternative payment help
                    val canAfford = if (hasConvoke && convokeCreatures != null && convokeCreatures.isNotEmpty()) {
                        // Can afford if: mana alone is enough, OR mana + convoke creatures cover the cost
                        manaSolver.canPay(state, playerId, effectiveCost) ||
                            canAffordWithConvoke(state, playerId, effectiveCost, convokeCreatures)
                    } else if (hasDelve && delveCards != null && delveCards.isNotEmpty()) {
                        manaSolver.canPay(state, playerId, effectiveCost) ||
                            canAffordWithDelve(state, playerId, effectiveCost, delveCards)
                    } else {
                        manaSolver.canPay(state, playerId, effectiveCost)
                    }

                    // Check alternative casting cost affordability (e.g., Jodah's {W}{U}{B}{R}{G})
                    val canAffordAlternative = alternativeCastingCosts.isNotEmpty() && cardDef != null &&
                        alternativeCastingCosts.any { altCost ->
                            val altEffective = costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, altCost)
                            manaSolver.canPay(state, playerId, altEffective)
                        }

                    // Check self-alternative cost (e.g., Zahid's {3}{U} + tap an artifact)
                    val selfAltCost = cardDef?.script?.selfAlternativeCost
                    val canAffordSelfAlternative = if (selfAltCost != null) {
                        val selfAltMana = ManaCost.parse(selfAltCost.manaCost)
                        val selfAltEffective = costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef!!, selfAltMana, playerId)
                        val canPayMana = manaSolver.canPay(state, playerId, selfAltEffective)
                        val canPayAdditional = selfAltCost.additionalCosts.all { cost ->
                            when (cost) {
                                is com.wingedsheep.sdk.scripting.AdditionalCost.TapPermanents -> {
                                    findAbilityTapTargets(state, playerId, cost.filter).size >= cost.count
                                }
                                else -> true
                            }
                        }
                        canPayMana && canPayAdditional
                    } else false

                    if (canAfford || canAffordAlternative || canAffordSelfAlternative) {
                        val targetReqs = buildList {
                            addAll(cardDef?.script?.targetRequirements ?: emptyList())
                            cardDef?.script?.auraTarget?.let { add(it) }
                        }

                        logger.debug("Card '${cardComponent.name}': cardDef=${cardDef != null}, targetReqs=${targetReqs.size}")

                        // Build additional cost info for the client
                        val costInfo = if (variableSacrificeTargets.isNotEmpty()) {
                            val varSacCost = additionalCosts.filterIsInstance<com.wingedsheep.sdk.scripting.AdditionalCost.SacrificeCreaturesForCostReduction>().firstOrNull()
                            AdditionalCostInfo(
                                description = varSacCost?.description ?: "You may sacrifice any number of creatures",
                                costType = "SacrificeForCostReduction",
                                validSacrificeTargets = variableSacrificeTargets,
                                sacrificeCount = 0 // min 0 — sacrifice is optional
                            )
                        } else if (sacrificeTargets.isNotEmpty()) {
                            val sacCost = additionalCosts.filterIsInstance<com.wingedsheep.sdk.scripting.AdditionalCost.SacrificePermanent>().firstOrNull()
                            AdditionalCostInfo(
                                description = sacCost?.description ?: "Sacrifice a creature",
                                costType = "SacrificePermanent",
                                validSacrificeTargets = sacrificeTargets,
                                sacrificeCount = sacCost?.count ?: 1
                            )
                        } else if (exileTargets.isNotEmpty()) {
                            val exileCostDesc = additionalCosts
                                .filterIsInstance<com.wingedsheep.sdk.scripting.AdditionalCost.ExileVariableCards>()
                                .firstOrNull()?.description
                                ?: additionalCosts
                                    .filterIsInstance<com.wingedsheep.sdk.scripting.AdditionalCost.ExileCards>()
                                    .firstOrNull()?.description
                                ?: "Exile cards from your graveyard"
                            AdditionalCostInfo(
                                description = exileCostDesc,
                                costType = "ExileFromGraveyard",
                                validExileTargets = exileTargets,
                                exileMinCount = exileMinCount,
                                exileMaxCount = exileTargets.size
                            )
                        } else if (discardTargets.isNotEmpty()) {
                            val discardCost = additionalCosts.filterIsInstance<com.wingedsheep.sdk.scripting.AdditionalCost.DiscardCards>().firstOrNull()
                            AdditionalCostInfo(
                                description = discardCost?.description ?: "Discard a card",
                                costType = "DiscardCard",
                                validDiscardTargets = discardTargets,
                                discardCount = discardCount
                            )
                        } else null

                        // Calculate X cost info if the spell has X in its cost
                        val hasXCost = effectiveCost.hasX
                        val maxAffordableX: Int? = if (hasXCost) {
                            val availableSources = manaSolver.getAvailableManaCount(state, playerId)
                            val fixedCost = effectiveCost.cmc  // X contributes 0 to CMC
                            val xSymbolCount = effectiveCost.xCount.coerceAtLeast(1)
                            ((availableSources - fixedCost) / xSymbolCount).coerceAtLeast(0)
                        } else null

                        // Always include mana cost string for cast actions
                        val manaCostString = effectiveCost.toString()

                        // Compute auto-tap preview for UI highlighting
                        // For Delve/Convoke spells, solve against the reduced cost so the preview
                        // reflects what the player actually needs to tap after alternative payment
                        val autoTapCost = if (hasDelve && delveCards != null && delveCards.isNotEmpty()) {
                            val maxDelve = minOf(delveCards.size, effectiveCost.genericAmount)
                            effectiveCost.reduceGeneric(maxDelve)
                        } else {
                            effectiveCost
                        }
                        val autoTapSolution = manaSolver.solve(state, playerId, autoTapCost)
                        val autoTapPreview = autoTapSolution?.sources?.map { it.entityId }

                        // Check for DividedDamageEffect to flag damage distribution requirement
                        val spellEffect = cardDef?.script?.spellEffect
                        val dividedDamageEffect = spellEffect as? DividedDamageEffect
                        val requiresDamageDistribution = dividedDamageEffect != null
                        val totalDamageToDistribute = dividedDamageEffect?.totalDamage
                        val minDamagePerTarget = if (dividedDamageEffect != null) 1 else null

                        // Compute alternative cost info for this spell
                        val altCostInfo = if (canAffordAlternative) {
                            val altCost = alternativeCastingCosts.first()
                            val altEffective = costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, altCost)
                            val altSolution = manaSolver.solve(state, playerId, altEffective)
                            Triple(altEffective.toString(), altSolution?.sources?.map { it.entityId }, manaSolver.canPay(state, playerId, altEffective))
                        } else null

                        // Compute self-alternative cost info (e.g., Zahid)
                        data class SelfAltCostResult(
                            val manaCostString: String,
                            val autoTapPreview: List<EntityId>?,
                            val additionalCostInfo: AdditionalCostInfo?
                        )
                        val selfAltCostResult = if (canAffordSelfAlternative && selfAltCost != null && cardDef != null) {
                            val selfAltMana = ManaCost.parse(selfAltCost.manaCost)
                            val selfAltEffective = costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, selfAltMana, playerId)
                            val selfAltSolution = manaSolver.solve(state, playerId, selfAltEffective)
                            val tapCost = selfAltCost.additionalCosts.filterIsInstance<com.wingedsheep.sdk.scripting.AdditionalCost.TapPermanents>().firstOrNull()
                            val tapTargets = if (tapCost != null) findAbilityTapTargets(state, playerId, tapCost.filter) else null
                            val addlCostInfo = if (tapTargets != null && tapCost != null) {
                                AdditionalCostInfo(
                                    description = tapCost.description,
                                    costType = "TapPermanents",
                                    validTapTargets = tapTargets,
                                    tapCount = tapCost.count
                                )
                            } else null
                            SelfAltCostResult(
                                manaCostString = selfAltEffective.toString(),
                                autoTapPreview = selfAltSolution?.sources?.map { it.entityId },
                                additionalCostInfo = addlCostInfo
                            )
                        } else null

                        // Modal spells (chooseCount = 1): generate one LegalActionInfo per mode
                        // so the opponent sees which mode was chosen on the stack
                        val modalEffect = spellEffect as? com.wingedsheep.sdk.scripting.effects.ModalEffect
                        if (modalEffect != null && modalEffect.chooseCount == 1 && canAfford) {
                            for ((modeIndex, mode) in modalEffect.modes.withIndex()) {
                                val modeTargetReqs = mode.targetRequirements
                                if (modeTargetReqs.isNotEmpty()) {
                                    // Mode requires targets
                                    val modeTargetInfos = modeTargetReqs.mapIndexed { idx, req ->
                                        val validTargets = findValidTargets(state, playerId, req)
                                        LegalActionTargetInfo(
                                            index = idx,
                                            description = req.description,
                                            minTargets = req.effectiveMinCount,
                                            maxTargets = req.count,
                                            validTargets = validTargets,
                                            targetZone = getTargetZone(req)
                                        )
                                    }
                                    val allSatisfied = modeTargetInfos.all { info ->
                                        info.validTargets.isNotEmpty() || info.minTargets == 0
                                    }
                                    if (!allSatisfied) continue // Skip modes with unsatisfiable targets

                                    val firstReq = modeTargetReqs.first()
                                    val firstInfo = modeTargetInfos.first()

                                    // Check for auto-select (single player target, single valid choice)
                                    val canAutoSelect = modeTargetReqs.size == 1 &&
                                        shouldAutoSelectPlayerTarget(firstReq, firstInfo.validTargets)

                                    if (canAutoSelect) {
                                        val autoTarget = ChosenTarget.Player(firstInfo.validTargets.first())
                                        result.add(LegalActionInfo(
                                            actionType = "CastSpellMode",
                                            description = mode.description,
                                            action = CastSpell(playerId, cardId, targets = listOf(autoTarget), chosenMode = modeIndex),
                                            hasXCost = hasXCost,
                                            maxAffordableX = maxAffordableX,
                                            additionalCostInfo = costInfo,
                                            hasConvoke = hasConvoke,
                                            validConvokeCreatures = convokeCreatures,
                                            hasDelve = hasDelve,
                                            validDelveCards = delveCards,
                                            minDelveNeeded = minDelveNeeded,
                                            manaCostString = manaCostString,
                                            autoTapPreview = autoTapPreview
                                        ))
                                    } else {
                                        result.add(LegalActionInfo(
                                            actionType = "CastSpellMode",
                                            description = mode.description,
                                            action = CastSpell(playerId, cardId, chosenMode = modeIndex),
                                            validTargets = firstInfo.validTargets,
                                            requiresTargets = true,
                                            targetCount = firstReq.count,
                                            minTargets = firstReq.effectiveMinCount,
                                            targetDescription = firstReq.description,
                                            targetRequirements = if (modeTargetInfos.size > 1) modeTargetInfos else null,
                                            hasXCost = hasXCost,
                                            maxAffordableX = maxAffordableX,
                                            additionalCostInfo = costInfo,
                                            hasConvoke = hasConvoke,
                                            validConvokeCreatures = convokeCreatures,
                                            hasDelve = hasDelve,
                                            validDelveCards = delveCards,
                                            minDelveNeeded = minDelveNeeded,
                                            manaCostString = manaCostString,
                                            autoTapPreview = autoTapPreview
                                        ))
                                    }
                                } else {
                                    // Mode has no targets
                                    result.add(LegalActionInfo(
                                        actionType = "CastSpellMode",
                                        description = mode.description,
                                        action = CastSpell(playerId, cardId, chosenMode = modeIndex),
                                        hasXCost = hasXCost,
                                        maxAffordableX = maxAffordableX,
                                        additionalCostInfo = costInfo,
                                        hasConvoke = hasConvoke,
                                        validConvokeCreatures = convokeCreatures,
                                        hasDelve = hasDelve,
                                        validDelveCards = delveCards,
                                        minDelveNeeded = minDelveNeeded,
                                        manaCostString = manaCostString,
                                        autoTapPreview = autoTapPreview
                                    ))
                                }
                            }
                            // Skip the normal targeting logic for modal spells
                        } else if (targetReqs.isNotEmpty()) {
                            // Spell requires targets - find valid targets for all requirements
                            val targetReqInfos = targetReqs.mapIndexed { index, req ->
                                val validTargets = findValidTargets(state, playerId, req)
                                LegalActionTargetInfo(
                                    index = index,
                                    description = req.description,
                                    minTargets = req.effectiveMinCount,
                                    maxTargets = req.count,
                                    validTargets = validTargets,
                                    targetZone = getTargetZone(req)
                                )
                            }

                            // Check if all requirements can be satisfied
                            val allRequirementsSatisfied = targetReqInfos.all { reqInfo ->
                                reqInfo.validTargets.isNotEmpty() || reqInfo.minTargets == 0
                            }

                            val firstReq = targetReqs.first()
                            val firstReqInfo = targetReqInfos.first()

                            logger.debug("Card '${cardComponent.name}': targetReqs=${targetReqs.size}, firstReqValidTargets=${firstReqInfo.validTargets.size}")

                            // Only add the action if all requirements can be satisfied
                            if (allRequirementsSatisfied) {
                                // Check if we can auto-select player targets (single target, single valid choice)
                                // This applies to TargetPlayer and TargetOpponent - in a 2-player game with TargetOpponent,
                                // there's always exactly one choice so we skip the prompt for better UX.
                                val canAutoSelect = targetReqs.size == 1 &&
                                    shouldAutoSelectPlayerTarget(firstReq, firstReqInfo.validTargets)

                                if (canAutoSelect) {
                                    // Auto-select the single valid player target
                                    val autoSelectedTarget = ChosenTarget.Player(firstReqInfo.validTargets.first())
                                    if (canAfford) {
                                        result.add(LegalActionInfo(
                                            actionType = "CastSpell",
                                            description = "Cast ${cardComponent.name}",
                                            action = CastSpell(playerId, cardId, targets = listOf(autoSelectedTarget)),
                                            hasXCost = hasXCost,
                                            maxAffordableX = maxAffordableX,
                                            additionalCostInfo = costInfo,
                                            hasConvoke = hasConvoke,
                                            validConvokeCreatures = convokeCreatures,
                                            hasDelve = hasDelve,
                                            validDelveCards = delveCards,
                                            minDelveNeeded = minDelveNeeded,
                                            manaCostString = manaCostString,
                                            requiresDamageDistribution = requiresDamageDistribution,
                                            totalDamageToDistribute = totalDamageToDistribute,
                                            minDamagePerTarget = minDamagePerTarget,
                                            autoTapPreview = autoTapPreview
                                        ))
                                    }
                                    if (altCostInfo?.third == true) {
                                        result.add(LegalActionInfo(
                                            actionType = "CastWithAlternativeCost",
                                            description = "Cast ${cardComponent.name} (${altCostInfo.first})",
                                            action = CastSpell(playerId, cardId, targets = listOf(autoSelectedTarget), useAlternativeCost = true),
                                            manaCostString = altCostInfo.first,
                                            requiresDamageDistribution = requiresDamageDistribution,
                                            totalDamageToDistribute = totalDamageToDistribute,
                                            minDamagePerTarget = minDamagePerTarget,
                                            autoTapPreview = altCostInfo.second
                                        ))
                                    }
                                    if (selfAltCostResult != null) {
                                        result.add(LegalActionInfo(
                                            actionType = "CastWithAlternativeCost",
                                            description = "Cast ${cardComponent.name} (${selfAltCostResult.manaCostString})",
                                            action = CastSpell(playerId, cardId, targets = listOf(autoSelectedTarget), useAlternativeCost = true),
                                            manaCostString = selfAltCostResult.manaCostString,
                                            additionalCostInfo = selfAltCostResult.additionalCostInfo,
                                            requiresDamageDistribution = requiresDamageDistribution,
                                            totalDamageToDistribute = totalDamageToDistribute,
                                            minDamagePerTarget = minDamagePerTarget,
                                            autoTapPreview = selfAltCostResult.autoTapPreview
                                        ))
                                    }
                                } else {
                                    if (canAfford) {
                                        result.add(LegalActionInfo(
                                            actionType = "CastSpell",
                                            description = "Cast ${cardComponent.name}",
                                            action = CastSpell(playerId, cardId),
                                            validTargets = firstReqInfo.validTargets,
                                            requiresTargets = true,
                                            targetCount = firstReq.count,
                                            minTargets = firstReq.effectiveMinCount,
                                            targetDescription = firstReq.description,
                                            targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                                            hasXCost = hasXCost,
                                            maxAffordableX = maxAffordableX,
                                            additionalCostInfo = costInfo,
                                            hasConvoke = hasConvoke,
                                            validConvokeCreatures = convokeCreatures,
                                            hasDelve = hasDelve,
                                            validDelveCards = delveCards,
                                            minDelveNeeded = minDelveNeeded,
                                            manaCostString = manaCostString,
                                            requiresDamageDistribution = requiresDamageDistribution,
                                            totalDamageToDistribute = totalDamageToDistribute,
                                            minDamagePerTarget = minDamagePerTarget,
                                            autoTapPreview = autoTapPreview
                                        ))
                                    }
                                    if (altCostInfo?.third == true) {
                                        result.add(LegalActionInfo(
                                            actionType = "CastWithAlternativeCost",
                                            description = "Cast ${cardComponent.name} (${altCostInfo.first})",
                                            action = CastSpell(playerId, cardId, useAlternativeCost = true),
                                            validTargets = firstReqInfo.validTargets,
                                            requiresTargets = true,
                                            targetCount = firstReq.count,
                                            minTargets = firstReq.effectiveMinCount,
                                            targetDescription = firstReq.description,
                                            targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                                            manaCostString = altCostInfo.first,
                                            requiresDamageDistribution = requiresDamageDistribution,
                                            totalDamageToDistribute = totalDamageToDistribute,
                                            minDamagePerTarget = minDamagePerTarget,
                                            autoTapPreview = altCostInfo.second
                                        ))
                                    }
                                    if (selfAltCostResult != null) {
                                        result.add(LegalActionInfo(
                                            actionType = "CastWithAlternativeCost",
                                            description = "Cast ${cardComponent.name} (${selfAltCostResult.manaCostString})",
                                            action = CastSpell(playerId, cardId, useAlternativeCost = true),
                                            validTargets = firstReqInfo.validTargets,
                                            requiresTargets = true,
                                            targetCount = firstReq.count,
                                            minTargets = firstReq.effectiveMinCount,
                                            targetDescription = firstReq.description,
                                            targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                                            manaCostString = selfAltCostResult.manaCostString,
                                            additionalCostInfo = selfAltCostResult.additionalCostInfo,
                                            requiresDamageDistribution = requiresDamageDistribution,
                                            totalDamageToDistribute = totalDamageToDistribute,
                                            minDamagePerTarget = minDamagePerTarget,
                                            autoTapPreview = selfAltCostResult.autoTapPreview
                                        ))
                                    }
                                }
                            }
                        } else {
                            // No targets required
                            if (canAfford) {
                                result.add(LegalActionInfo(
                                    actionType = "CastSpell",
                                    description = "Cast ${cardComponent.name}",
                                    action = CastSpell(playerId, cardId),
                                    hasXCost = hasXCost,
                                    maxAffordableX = maxAffordableX,
                                    additionalCostInfo = costInfo,
                                    hasConvoke = hasConvoke,
                                    validConvokeCreatures = convokeCreatures,
                                    hasDelve = hasDelve,
                                    validDelveCards = delveCards,
                                    minDelveNeeded = minDelveNeeded,
                                    manaCostString = manaCostString,
                                    autoTapPreview = autoTapPreview
                                ))
                            }
                            if (altCostInfo?.third == true) {
                                result.add(LegalActionInfo(
                                    actionType = "CastWithAlternativeCost",
                                    description = "Cast ${cardComponent.name} (${altCostInfo.first})",
                                    action = CastSpell(playerId, cardId, useAlternativeCost = true),
                                    manaCostString = altCostInfo.first,
                                    autoTapPreview = altCostInfo.second
                                ))
                            }
                            if (selfAltCostResult != null) {
                                result.add(LegalActionInfo(
                                    actionType = "CastWithAlternativeCost",
                                    description = "Cast ${cardComponent.name} (${selfAltCostResult.manaCostString})",
                                    action = CastSpell(playerId, cardId, useAlternativeCost = true),
                                    manaCostString = selfAltCostResult.manaCostString,
                                    additionalCostInfo = selfAltCostResult.additionalCostInfo,
                                    autoTapPreview = selfAltCostResult.autoTapPreview
                                ))
                            }
                        }
                    }
                }
            }
        }

        // Check for kicker cards in hand — generate kicked action alongside normal cast
        for (cardId in hand) {
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
            if (cardComponent.typeLine.isLand) continue
            if (cantCastSpells) continue

            val cardDef = cardRegistry.getCard(cardComponent.name) ?: continue
            val manaKicker = cardDef.keywordAbilities
                .filterIsInstance<com.wingedsheep.sdk.scripting.KeywordAbility.Kicker>()
                .firstOrNull()
            val additionalCostKicker = cardDef.keywordAbilities
                .filterIsInstance<com.wingedsheep.sdk.scripting.KeywordAbility.KickerWithAdditionalCost>()
                .firstOrNull()
            if (manaKicker == null && additionalCostKicker == null) continue

            // Check timing (same rules as normal cast)
            val isInstant = cardComponent.typeLine.isInstant
            val hasFlash = cardDef.keywords.contains(Keyword.FLASH)
            val grantedFlash = hasFlash || hasGrantedFlash(state, cardId)
            if (!isInstant && !grantedFlash && !canPlaySorcerySpeed) continue

            // Check cast restrictions
            val castRestrictions = cardDef.script.castRestrictions
            if (castRestrictions.isNotEmpty() && !checkCastRestrictions(state, playerId, castRestrictions)) continue

            // Calculate kicked cost
            val baseCost = costCalculator.calculateEffectiveCost(state, cardDef, playerId)
            val kickedCost = if (manaKicker != null) {
                baseCost + manaKicker.cost
            } else {
                baseCost // No extra mana for additional-cost kicker
            }
            val canAffordKickedMana = manaSolver.canPay(state, playerId, kickedCost)
            val kickedCostString = kickedCost.toString()
            val kickedAutoTapSolution = manaSolver.solve(state, playerId, kickedCost)
            val kickedAutoTapPreview = kickedAutoTapSolution?.sources?.map { it.entityId }

            // Check additional cost payability (e.g., sacrifice a creature)
            var kickerCostInfo: AdditionalCostInfo? = null
            var canPayKickerAdditionalCost = true
            if (additionalCostKicker != null) {
                when (val cost = additionalCostKicker.cost) {
                    is com.wingedsheep.sdk.scripting.AdditionalCost.SacrificePermanent -> {
                        val validSacTargets = findSacrificeTargets(state, playerId, cost)
                        if (validSacTargets.size < cost.count) {
                            canPayKickerAdditionalCost = false
                        } else {
                            kickerCostInfo = AdditionalCostInfo(
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

            // Check for DividedDamageEffect in the kicked spell effect
            val kickerSpellEffect = cardDef.script.kickerSpellEffect ?: cardDef.script.spellEffect
            val kickerDividedDamage = kickerSpellEffect as? DividedDamageEffect
            val kickerRequiresDamageDistribution = kickerDividedDamage != null
            val kickerTotalDamage = kickerDividedDamage?.totalDamage
            val kickerMinDamagePerTarget = if (kickerDividedDamage != null) 1 else null

            if (targetReqs.isNotEmpty()) {
                val targetReqInfos = targetReqs.mapIndexed { index, req ->
                    val validTargets = findValidTargets(state, playerId, req)
                    LegalActionTargetInfo(
                        index = index,
                        description = req.description,
                        minTargets = req.effectiveMinCount,
                        maxTargets = req.count,
                        validTargets = validTargets,
                        targetZone = getTargetZone(req)
                    )
                }
                val allRequirementsSatisfied = targetReqInfos.all { reqInfo ->
                    reqInfo.validTargets.isNotEmpty() || reqInfo.minTargets == 0
                }
                if (allRequirementsSatisfied) {
                    val firstReq = targetReqs.first()
                    val firstReqInfo = targetReqInfos.first()

                    val canAutoSelect = targetReqs.size == 1 &&
                        shouldAutoSelectPlayerTarget(firstReq, firstReqInfo.validTargets)

                    if (canAutoSelect) {
                        val autoSelectedTarget = ChosenTarget.Player(firstReqInfo.validTargets.first())
                        result.add(LegalActionInfo(
                            actionType = "CastWithKicker",
                            description = "Cast ${cardComponent.name} (Kicked)",
                            action = CastSpell(playerId, cardId, targets = listOf(autoSelectedTarget), wasKicked = true),
                            isAffordable = canAffordKicked,
                            manaCostString = kickedCostString,
                            autoTapPreview = kickedAutoTapPreview,
                            additionalCostInfo = kickerCostInfo,
                            requiresDamageDistribution = kickerRequiresDamageDistribution,
                            totalDamageToDistribute = kickerTotalDamage,
                            minDamagePerTarget = kickerMinDamagePerTarget
                        ))
                    } else {
                        result.add(LegalActionInfo(
                            actionType = "CastWithKicker",
                            description = "Cast ${cardComponent.name} (Kicked)",
                            action = CastSpell(playerId, cardId, wasKicked = true),
                            validTargets = firstReqInfo.validTargets,
                            requiresTargets = true,
                            targetCount = firstReq.count,
                            minTargets = firstReq.effectiveMinCount,
                            targetDescription = firstReq.description,
                            targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                            isAffordable = canAffordKicked,
                            manaCostString = kickedCostString,
                            autoTapPreview = kickedAutoTapPreview,
                            additionalCostInfo = kickerCostInfo,
                            requiresDamageDistribution = kickerRequiresDamageDistribution,
                            totalDamageToDistribute = kickerTotalDamage,
                            minDamagePerTarget = kickerMinDamagePerTarget
                        ))
                    }
                }
            } else {
                result.add(LegalActionInfo(
                    actionType = "CastWithKicker",
                    description = "Cast ${cardComponent.name} (Kicked)",
                    action = CastSpell(playerId, cardId, wasKicked = true),
                    isAffordable = canAffordKicked,
                    manaCostString = kickedCostString,
                    autoTapPreview = kickedAutoTapPreview,
                    additionalCostInfo = kickerCostInfo
                ))
            }

            // If normal cast is not affordable but kicker is (unlikely), ensure normal cast shows unaffordable
            if (!manaSolver.canPay(state, playerId, baseCost)) {
                result.add(LegalActionInfo(
                    actionType = "CastSpell",
                    description = "Cast ${cardComponent.name}",
                    action = CastSpell(playerId, cardId),
                    isAffordable = false,
                    manaCostString = baseCost.toString()
                ))
            }
        }

        // Check if cycling is prevented by any permanent on the battlefield (e.g., Stabilizer)
        val cyclingPrevented = isCyclingPrevented(state)

        // Check for cycling and typecycling abilities on cards in hand
        for (cardId in hand) {
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(cardComponent.name) ?: continue

            val allAbilities = cardDef.keywordAbilities
            val cyclingAbility = allAbilities
                .filterIsInstance<com.wingedsheep.sdk.scripting.KeywordAbility.Cycling>()
                .firstOrNull()
            val typecyclingAbility = allAbilities
                .filterIsInstance<com.wingedsheep.sdk.scripting.KeywordAbility.Typecycling>()
                .firstOrNull()

            if (cyclingAbility == null && typecyclingAbility == null) {
                continue
            }

            // Skip cycling/typecycling if prevented by a permanent (e.g., Stabilizer)
            if (cyclingPrevented) {
                // Still check for normal cast option below
            }

            // Add cycling action if present
            if (cyclingAbility != null && !cyclingPrevented) {
                val canAffordCycling = manaSolver.canPay(state, playerId, cyclingAbility.cost)
                val cyclingAutoTapSolution = manaSolver.solve(state, playerId, cyclingAbility.cost)
                val cyclingAutoTapPreview = cyclingAutoTapSolution?.sources?.map { it.entityId }
                result.add(LegalActionInfo(
                    actionType = "CycleCard",
                    description = "Cycle ${cardComponent.name}",
                    action = CycleCard(playerId, cardId),
                    isAffordable = canAffordCycling,
                    manaCostString = cyclingAbility.cost.toString(),
                    autoTapPreview = cyclingAutoTapPreview
                ))
            }

            // Add typecycling action if present
            if (typecyclingAbility != null && !cyclingPrevented) {
                val canAffordTypecycling = manaSolver.canPay(state, playerId, typecyclingAbility.cost)
                val typecyclingAutoTapSolution = manaSolver.solve(state, playerId, typecyclingAbility.cost)
                val typecyclingAutoTapPreview = typecyclingAutoTapSolution?.sources?.map { it.entityId }
                result.add(LegalActionInfo(
                    actionType = "TypecycleCard",
                    description = "${typecyclingAbility.type}cycling ${cardComponent.name}",
                    action = TypecycleCard(playerId, cardId),
                    isAffordable = canAffordTypecycling,
                    manaCostString = typecyclingAbility.cost.toString(),
                    autoTapPreview = typecyclingAutoTapPreview
                ))
            }

            // For cards with cycling/typecycling, also add the normal cast option (matching morph pattern)
            // This ensures the player sees both options in the cast modal
            if (!cardComponent.typeLine.isLand) {
                val isInstant = cardComponent.typeLine.isInstant
                val canCastTiming = isInstant || canPlaySorcerySpeed
                // Check if a cast action was already added (affordable, with proper targeting)
                val hasCastAction = result.any { it.action is CastSpell && it.action.cardId == cardId }
                if (!hasCastAction) {
                    if (canCastTiming) {
                        // Check if we can afford to cast normally (with cost reductions)
                        val cycleEffectiveCost = costCalculator.calculateEffectiveCost(state, cardDef, playerId)
                        val canAffordNormal = manaSolver.canPay(state, playerId, cycleEffectiveCost)

                        // If the spell requires targets, check if valid targets exist.
                        // Without this, a spell with cycling+targeting would be castable
                        // without target selection when no valid targets are found.
                        val targetReqs = cardDef.script.targetRequirements
                        val hasRequiredTargets = targetReqs.any { it.effectiveMinCount > 0 }
                        val canSatisfyTargets = if (hasRequiredTargets) {
                            targetReqs.all { req ->
                                val validTargets = findValidTargets(state, playerId, req)
                                validTargets.isNotEmpty() || req.effectiveMinCount == 0
                            }
                        } else {
                            true
                        }

                        if (canAffordNormal && canSatisfyTargets && targetReqs.isNotEmpty()) {
                            // Spell is affordable and has valid targets — add with full targeting info
                            val targetReqInfos = targetReqs.mapIndexed { index, req ->
                                val validTargets = findValidTargets(state, playerId, req)
                                LegalActionTargetInfo(
                                    index = index,
                                    description = req.description,
                                    minTargets = req.effectiveMinCount,
                                    maxTargets = req.count,
                                    validTargets = validTargets,
                                    targetZone = getTargetZone(req)
                                )
                            }
                            val firstReq = targetReqs.first()
                            val firstReqInfo = targetReqInfos.first()
                            result.add(LegalActionInfo(
                                actionType = "CastSpell",
                                description = "Cast ${cardComponent.name}",
                                action = CastSpell(playerId, cardId),
                                validTargets = firstReqInfo.validTargets,
                                requiresTargets = true,
                                targetCount = firstReq.count,
                                minTargets = firstReq.effectiveMinCount,
                                targetDescription = firstReq.description,
                                targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                                isAffordable = true,
                                manaCostString = cycleEffectiveCost.toString()
                            ))
                        } else {
                            // Spell is unaffordable or has no valid targets — show greyed out
                            result.add(LegalActionInfo(
                                actionType = "CastSpell",
                                description = "Cast ${cardComponent.name}",
                                action = CastSpell(playerId, cardId),
                                isAffordable = false,
                                manaCostString = cycleEffectiveCost.toString()
                            ))
                        }
                    } else {
                        // Wrong timing (not main phase / not active player) — show greyed out
                        val wrongTimingCost = costCalculator.calculateEffectiveCost(state, cardDef, playerId)
                        result.add(LegalActionInfo(
                            actionType = "CastSpell",
                            description = "Cast ${cardComponent.name}",
                            action = CastSpell(playerId, cardId),
                            isAffordable = false,
                            manaCostString = wrongTimingCost.toString()
                        ))
                    }
                }
            }
        }

        // Check for top-of-library cards playable via PlayFromTopOfLibrary (e.g., Future Sight)
        // or CastSpellTypesFromTopOfLibrary (e.g., Precognition Field — instants and sorceries only)
        val canPlayAllFromTop = hasPlayFromTopOfLibrary(state, playerId)
        val castFromTopFilter = if (!canPlayAllFromTop) getCastFromTopOfLibraryFilter(state, playerId) else null
        if (canPlayAllFromTop || castFromTopFilter != null) {
            val library = state.getLibrary(playerId)
            if (library.isNotEmpty()) {
                val topCardId = library.first()
                val topCardComponent = state.getEntity(topCardId)?.get<CardComponent>()
                if (topCardComponent != null) {
                    val topCardDef = cardRegistry.getCard(topCardComponent.name)

                    // Land on top of library (only for PlayFromTopOfLibrary, not filtered cast)
                    if (canPlayAllFromTop && topCardComponent.typeLine.isLand && canPlayLand) {
                        result.add(LegalActionInfo(
                            actionType = "PlayLand",
                            description = "Play ${topCardComponent.name}",
                            action = PlayLand(playerId, topCardId),
                            sourceZone = "LIBRARY"
                        ))
                    }

                    // Non-land spell on top of library
                    // For CastSpellTypesFromTopOfLibrary, also check the filter matches
                    val topCardMatchesFilter = canPlayAllFromTop ||
                        (castFromTopFilter != null && predicateEvaluator.matches(
                            state, topCardId, castFromTopFilter, PredicateContext(controllerId = playerId)
                        ))
                    if (!topCardComponent.typeLine.isLand && topCardMatchesFilter) {
                        // Check timing
                        val isInstant = topCardComponent.typeLine.isInstant
                        if (isInstant || canPlaySorcerySpeed) {
                            // Check cast restrictions
                            val castRestrictions = topCardDef?.script?.castRestrictions ?: emptyList()
                            if (checkCastRestrictions(state, playerId, castRestrictions)) {
                                val topEffectiveCost = if (topCardDef != null) {
                                    costCalculator.calculateEffectiveCost(state, topCardDef, playerId)
                                } else {
                                    topCardComponent.manaCost
                                }
                                val canAfford = manaSolver.canPay(state, playerId, topEffectiveCost)
                                if (canAfford) {
                                    val targetReqs = buildList {
                                        addAll(topCardDef?.script?.targetRequirements ?: emptyList())
                                        topCardDef?.script?.auraTarget?.let { add(it) }
                                    }

                                    val manaCostString = topEffectiveCost.toString()
                                    val hasXCost = topEffectiveCost.hasX
                                    val maxAffordableX: Int? = if (hasXCost) {
                                        val availableSources = manaSolver.getAvailableManaCount(state, playerId)
                                        val fixedCost = topEffectiveCost.cmc
                                        val xSymbolCount = topEffectiveCost.xCount.coerceAtLeast(1)
                                        ((availableSources - fixedCost) / xSymbolCount).coerceAtLeast(0)
                                    } else null
                                    val autoTapSolution = manaSolver.solve(state, playerId, topEffectiveCost)
                                    val autoTapPreview = autoTapSolution?.sources?.map { it.entityId }

                                    if (targetReqs.isNotEmpty()) {
                                        val targetReqInfos = targetReqs.mapIndexed { index, req ->
                                            val validTargets = findValidTargets(state, playerId, req)
                                            LegalActionTargetInfo(
                                                index = index,
                                                description = req.description,
                                                minTargets = req.effectiveMinCount,
                                                maxTargets = req.count,
                                                validTargets = validTargets,
                                                targetZone = getTargetZone(req)
                                            )
                                        }
                                        val allRequirementsSatisfied = targetReqInfos.all { reqInfo ->
                                            reqInfo.validTargets.isNotEmpty() || reqInfo.minTargets == 0
                                        }
                                        if (allRequirementsSatisfied) {
                                            val firstReq = targetReqs.first()
                                            val firstReqInfo = targetReqInfos.first()
                                            result.add(LegalActionInfo(
                                                actionType = "CastSpell",
                                                description = "Cast ${topCardComponent.name}",
                                                action = CastSpell(playerId, topCardId),
                                                validTargets = firstReqInfo.validTargets,
                                                requiresTargets = true,
                                                targetCount = firstReq.count,
                                                minTargets = firstReq.effectiveMinCount,
                                                targetDescription = firstReq.description,
                                                targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                                                hasXCost = hasXCost,
                                                maxAffordableX = maxAffordableX,
                                                manaCostString = manaCostString,
                                                autoTapPreview = autoTapPreview,
                                                sourceZone = "LIBRARY"
                                            ))
                                        }
                                    } else {
                                        result.add(LegalActionInfo(
                                            actionType = "CastSpell",
                                            description = "Cast ${topCardComponent.name}",
                                            action = CastSpell(playerId, topCardId),
                                            hasXCost = hasXCost,
                                            maxAffordableX = maxAffordableX,
                                            manaCostString = manaCostString,
                                            autoTapPreview = autoTapPreview,
                                            sourceZone = "LIBRARY"
                                        ))
                                    }
                                }
                            }
                        }

                        // Check for morph on top of library (only for PlayFromTopOfLibrary)
                        if (canPlayAllFromTop && canPlaySorcerySpeed && topCardDef != null) {
                            val hasMorph = topCardDef.keywordAbilities
                                .any { it is com.wingedsheep.sdk.scripting.KeywordAbility.Morph }
                            if (hasMorph) {
                                val morphCost = costCalculator.calculateFaceDownCost(state, playerId)
                                val canAffordMorph = manaSolver.canPay(state, playerId, morphCost)
                                result.add(LegalActionInfo(
                                    actionType = "CastFaceDown",
                                    description = "Cast ${topCardComponent.name} face-down",
                                    action = CastSpell(playerId, topCardId, castFaceDown = true),
                                    isAffordable = canAffordMorph,
                                    manaCostString = morphCost.toString(),
                                    sourceZone = "LIBRARY"
                                ))
                            }
                        }
                    }
                }
            }
        }

        // Check for cards in exile that may be played (e.g., Mind's Desire, Villainous Wealth)
        // Always generate legal actions for exile cards with MayPlayFromExileComponent,
        // marking them unaffordable when timing/land drops don't allow it.
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
                result.add(LegalActionInfo(
                    actionType = "PlayLand",
                    description = "Play ${cardComponent.name}",
                    action = PlayLand(playerId, cardId),
                    isAffordable = canPlayLand,
                    sourceZone = "EXILE"
                ))
            }

            // Non-land spell from exile
            if (!cardComponent.typeLine.isLand) {
                if (cantCastSpells) {
                    // Can't cast spells at all — show as unaffordable
                    val costString = if (playForFree) "{0}" else cardComponent.manaCost.toString()
                    result.add(LegalActionInfo(
                        actionType = "CastSpell",
                        description = "Cast ${cardComponent.name}",
                        action = CastSpell(playerId, cardId),
                        isAffordable = false,
                        manaCostString = costString,
                        sourceZone = "EXILE"
                    ))
                } else {
                    val isInstant = cardComponent.typeLine.isInstant
                    val hasCorrectTiming = isInstant || canPlaySorcerySpeed
                    val cardDef = cardRegistry.getCard(cardComponent.name)
                    val castRestrictions = cardDef?.script?.castRestrictions ?: emptyList()
                    val meetsRestrictions = checkCastRestrictions(state, playerId, castRestrictions)
                    val costString = if (playForFree) "{0}" else {
                        val effectiveCost = if (cardDef != null) {
                            costCalculator.calculateEffectiveCost(state, cardDef, playerId)
                        } else {
                            cardComponent.manaCost
                        }
                        effectiveCost.toString()
                    }
                    val canAfford = playForFree || run {
                        val effectiveCost = if (cardDef != null) {
                            costCalculator.calculateEffectiveCost(state, cardDef, playerId)
                        } else {
                            cardComponent.manaCost
                        }
                        manaSolver.canPay(state, playerId, effectiveCost)
                    }

                    if (hasCorrectTiming && meetsRestrictions && canAfford) {
                        val targetReqs = buildList {
                            addAll(cardDef?.script?.targetRequirements ?: emptyList())
                            cardDef?.script?.auraTarget?.let { add(it) }
                        }

                        if (targetReqs.isNotEmpty()) {
                            val targetReqInfos = targetReqs.mapIndexed { index, req ->
                                val validTargets = findValidTargets(state, playerId, req)
                                LegalActionTargetInfo(
                                    index = index,
                                    description = req.description,
                                    minTargets = req.effectiveMinCount,
                                    maxTargets = req.count,
                                    validTargets = validTargets,
                                    targetZone = getTargetZone(req)
                                )
                            }
                            val allRequirementsSatisfied = targetReqInfos.all { reqInfo ->
                                reqInfo.validTargets.isNotEmpty() || reqInfo.minTargets == 0
                            }
                            if (allRequirementsSatisfied) {
                                val firstReq = targetReqs.first()
                                val firstReqInfo = targetReqInfos.first()
                                result.add(LegalActionInfo(
                                    actionType = "CastSpell",
                                    description = "Cast ${cardComponent.name}",
                                    action = CastSpell(playerId, cardId),
                                    validTargets = firstReqInfo.validTargets,
                                    requiresTargets = true,
                                    targetCount = firstReq.count,
                                    minTargets = firstReq.effectiveMinCount,
                                    targetDescription = firstReq.description,
                                    targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                                    manaCostString = costString,
                                    sourceZone = "EXILE"
                                ))
                            }
                        } else {
                            result.add(LegalActionInfo(
                                actionType = "CastSpell",
                                description = "Cast ${cardComponent.name}",
                                action = CastSpell(playerId, cardId),
                                manaCostString = costString,
                                sourceZone = "EXILE"
                            ))
                        }
                    } else {
                        // Can't cast right now (wrong timing, restrictions, or can't afford) — show as unaffordable
                        result.add(LegalActionInfo(
                            actionType = "CastSpell",
                            description = "Cast ${cardComponent.name}",
                            action = CastSpell(playerId, cardId),
                            isAffordable = false,
                            manaCostString = costString,
                            sourceZone = "EXILE"
                        ))
                    }
                }
            }
        }

        // Check for cards castable from linked exile (e.g., Rona, Disciple of Gix)
        // Scan battlefield for permanents with GrantMayCastFromLinkedExile that link exiled cards.
        val linkedExileCardIds = mutableSetOf<EntityId>()
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val controller = container.get<ControllerComponent>()?.playerId ?: continue
            if (controller != playerId) continue
            val linked = container.get<LinkedExileComponent>() ?: continue
            val entityCard = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(entityCard.cardDefinitionId) ?: continue
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
                        is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsNonland -> !exiledCard.typeLine.isLand
                        is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsCreature -> exiledCard.typeLine.isCreature
                        is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsArtifact -> exiledCard.typeLine.isArtifact
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

                val exiledCardDef = cardRegistry.getCard(exiledCard.name)
                if (cantCastSpells) {
                    result.add(LegalActionInfo(
                        actionType = "CastSpell",
                        description = "Cast ${exiledCard.name}",
                        action = CastSpell(playerId, exiledId),
                        isAffordable = false,
                        manaCostString = exiledCard.manaCost.toString(),
                        sourceZone = "EXILE"
                    ))
                } else {
                    val isInstant = exiledCard.typeLine.isInstant
                    val hasCorrectTiming = isInstant || canPlaySorcerySpeed
                    val castRestrictions = exiledCardDef?.script?.castRestrictions ?: emptyList()
                    val meetsRestrictions = checkCastRestrictions(state, playerId, castRestrictions)
                    val costString = run {
                        val effectiveCost = if (exiledCardDef != null) {
                            costCalculator.calculateEffectiveCost(state, exiledCardDef, playerId)
                        } else {
                            exiledCard.manaCost
                        }
                        effectiveCost.toString()
                    }
                    val canAfford = run {
                        val effectiveCost = if (exiledCardDef != null) {
                            costCalculator.calculateEffectiveCost(state, exiledCardDef, playerId)
                        } else {
                            exiledCard.manaCost
                        }
                        manaSolver.canPay(state, playerId, effectiveCost)
                    }

                    if (hasCorrectTiming && meetsRestrictions && canAfford) {
                        val targetReqs = buildList {
                            addAll(exiledCardDef?.script?.targetRequirements ?: emptyList())
                            exiledCardDef?.script?.auraTarget?.let { add(it) }
                        }
                        if (targetReqs.isNotEmpty()) {
                            val targetReqInfos = targetReqs.mapIndexed { index, req ->
                                val validTargets = findValidTargets(state, playerId, req)
                                LegalActionTargetInfo(
                                    index = index,
                                    description = req.description,
                                    minTargets = req.effectiveMinCount,
                                    maxTargets = req.count,
                                    validTargets = validTargets,
                                    targetZone = getTargetZone(req)
                                )
                            }
                            val allRequirementsSatisfied = targetReqInfos.all { reqInfo ->
                                reqInfo.validTargets.isNotEmpty() || reqInfo.minTargets == 0
                            }
                            if (allRequirementsSatisfied) {
                                val firstReq = targetReqs.first()
                                val firstReqInfo = targetReqInfos.first()
                                result.add(LegalActionInfo(
                                    actionType = "CastSpell",
                                    description = "Cast ${exiledCard.name}",
                                    action = CastSpell(playerId, exiledId),
                                    validTargets = firstReqInfo.validTargets,
                                    requiresTargets = true,
                                    targetCount = firstReq.count,
                                    minTargets = firstReq.effectiveMinCount,
                                    targetDescription = firstReq.description,
                                    targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                                    manaCostString = costString,
                                    sourceZone = "EXILE"
                                ))
                            }
                        } else {
                            result.add(LegalActionInfo(
                                actionType = "CastSpell",
                                description = "Cast ${exiledCard.name}",
                                action = CastSpell(playerId, exiledId),
                                manaCostString = costString,
                                sourceZone = "EXILE"
                            ))
                        }
                    } else {
                        result.add(LegalActionInfo(
                            actionType = "CastSpell",
                            description = "Cast ${exiledCard.name}",
                            action = CastSpell(playerId, exiledId),
                            isAffordable = false,
                            manaCostString = costString,
                            sourceZone = "EXILE"
                        ))
                    }
                }
            }
        }

        // Check for cards with intrinsic MayCastSelfFromZones ability (e.g., Squee, the Immortal)
        // These cards can be cast from their graveyard or exile without needing a component grant.
        for (zone in listOf(Zone.GRAVEYARD, Zone.EXILE)) {
            val zoneCards = state.getZone(ZoneKey(playerId, zone))
            for (cardId in zoneCards) {
                val container = state.getEntity(cardId) ?: continue
                // Skip cards already handled by MayPlayFromExileComponent or linked exile (to avoid duplicates)
                if (zone == Zone.EXILE && container.get<MayPlayFromExileComponent>()?.controllerId == playerId) continue
                if (zone == Zone.EXILE && cardId in linkedExileCardIds) continue
                val cardComponent = container.get<CardComponent>() ?: continue
                val cardDef = cardRegistry.getCard(cardComponent.name) ?: continue
                val hasCastFromZone = cardDef.script.staticAbilities
                    .filterIsInstance<MayCastSelfFromZones>()
                    .any { zone in it.zones }
                if (!hasCastFromZone) continue

                // Only non-land spells (Squee is a creature)
                if (cardComponent.typeLine.isLand) continue

                val sourceZoneName = zone.name
                if (cantCastSpells) {
                    val costString = cardComponent.manaCost.toString()
                    result.add(LegalActionInfo(
                        actionType = "CastSpell",
                        description = "Cast ${cardComponent.name}",
                        action = CastSpell(playerId, cardId),
                        isAffordable = false,
                        manaCostString = costString,
                        sourceZone = sourceZoneName
                    ))
                } else {
                    val isInstant = cardComponent.typeLine.isInstant
                    val hasCorrectTiming = isInstant || canPlaySorcerySpeed
                    val castRestrictions = cardDef.script.castRestrictions
                    val meetsRestrictions = checkCastRestrictions(state, playerId, castRestrictions)
                    val effectiveCost = costCalculator.calculateEffectiveCost(state, cardDef, playerId)
                    val costString = effectiveCost.toString()
                    val canAfford = manaSolver.canPay(state, playerId, effectiveCost)

                    if (hasCorrectTiming && meetsRestrictions && canAfford) {
                        val targetReqs = buildList {
                            addAll(cardDef.script.targetRequirements)
                            cardDef.script.auraTarget?.let { add(it) }
                        }

                        if (targetReqs.isNotEmpty()) {
                            val targetReqInfos = targetReqs.mapIndexed { index, req ->
                                val validTargets = findValidTargets(state, playerId, req)
                                LegalActionTargetInfo(
                                    index = index,
                                    description = req.description,
                                    minTargets = req.effectiveMinCount,
                                    maxTargets = req.count,
                                    validTargets = validTargets,
                                    targetZone = getTargetZone(req)
                                )
                            }
                            val allRequirementsSatisfied = targetReqInfos.all { reqInfo ->
                                reqInfo.validTargets.isNotEmpty() || reqInfo.minTargets == 0
                            }
                            if (allRequirementsSatisfied) {
                                val firstReq = targetReqs.first()
                                val firstReqInfo = targetReqInfos.first()
                                result.add(LegalActionInfo(
                                    actionType = "CastSpell",
                                    description = "Cast ${cardComponent.name}",
                                    action = CastSpell(playerId, cardId),
                                    validTargets = firstReqInfo.validTargets,
                                    requiresTargets = true,
                                    targetCount = firstReq.count,
                                    minTargets = firstReq.effectiveMinCount,
                                    targetDescription = firstReq.description,
                                    targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                                    manaCostString = costString,
                                    sourceZone = sourceZoneName
                                ))
                            }
                        } else {
                            result.add(LegalActionInfo(
                                actionType = "CastSpell",
                                description = "Cast ${cardComponent.name}",
                                action = CastSpell(playerId, cardId),
                                manaCostString = costString,
                                sourceZone = sourceZoneName
                            ))
                        }
                    } else {
                        result.add(LegalActionInfo(
                            actionType = "CastSpell",
                            description = "Cast ${cardComponent.name}",
                            action = CastSpell(playerId, cardId),
                            isAffordable = false,
                            manaCostString = costString,
                            sourceZone = sourceZoneName
                        ))
                    }
                }
            }
        }

        // Check for permanent spells in graveyard castable via MayPlayPermanentsFromGraveyard (Muldrotha)
        if (state.activePlayerId == playerId) {
            val graveyardCards = state.getZone(ZoneKey(playerId, Zone.GRAVEYARD))
            for (cardId in graveyardCards) {
                val container = state.getEntity(cardId) ?: continue
                val cardComponent = container.get<CardComponent>() ?: continue

                // Only permanent spells (not lands — those are handled above; not instant/sorcery)
                if (cardComponent.typeLine.isLand) continue
                if (!cardComponent.typeLine.isPermanent) continue

                // Check if any permanent type on this card has available graveyard permission
                val permanentTypes = cardComponent.typeLine.cardTypes.filter { it.isPermanent }
                val hasPermission = permanentTypes.any { type ->
                    hasGraveyardPlayPermissionForType(state, playerId, type.name)
                }
                if (!hasPermission) continue

                val cardDef = cardRegistry.getCard(cardComponent.name) ?: continue

                if (cantCastSpells) {
                    val costString = cardComponent.manaCost.toString()
                    result.add(LegalActionInfo(
                        actionType = "CastSpell",
                        description = "Cast ${cardComponent.name}",
                        action = CastSpell(playerId, cardId),
                        isAffordable = false,
                        manaCostString = costString,
                        sourceZone = "GRAVEYARD"
                    ))
                } else {
                    val isInstant = cardComponent.typeLine.isInstant
                    val hasCorrectTiming = isInstant || canPlaySorcerySpeed
                    val castRestrictions = cardDef.script.castRestrictions
                    val meetsRestrictions = checkCastRestrictions(state, playerId, castRestrictions)
                    val effectiveCost = costCalculator.calculateEffectiveCost(state, cardDef, playerId)
                    val costString = effectiveCost.toString()
                    val canAfford = manaSolver.canPay(state, playerId, effectiveCost)

                    if (hasCorrectTiming && meetsRestrictions && canAfford) {
                        val targetReqs = buildList {
                            addAll(cardDef.script.targetRequirements)
                            cardDef.script.auraTarget?.let { add(it) }
                        }

                        if (targetReqs.isNotEmpty()) {
                            val targetReqInfos = targetReqs.mapIndexed { index, req ->
                                val validTargets = findValidTargets(state, playerId, req)
                                LegalActionTargetInfo(
                                    index = index,
                                    description = req.description,
                                    minTargets = req.effectiveMinCount,
                                    maxTargets = req.count,
                                    validTargets = validTargets,
                                    targetZone = getTargetZone(req)
                                )
                            }
                            val allRequirementsSatisfied = targetReqInfos.all { reqInfo ->
                                reqInfo.validTargets.isNotEmpty() || reqInfo.minTargets == 0
                            }
                            if (allRequirementsSatisfied) {
                                val firstReq = targetReqs.first()
                                val firstReqInfo = targetReqInfos.first()
                                result.add(LegalActionInfo(
                                    actionType = "CastSpell",
                                    description = "Cast ${cardComponent.name}",
                                    action = CastSpell(playerId, cardId),
                                    validTargets = firstReqInfo.validTargets,
                                    requiresTargets = true,
                                    targetCount = firstReq.count,
                                    minTargets = firstReq.effectiveMinCount,
                                    targetDescription = firstReq.description,
                                    targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                                    manaCostString = costString,
                                    sourceZone = "GRAVEYARD"
                                ))
                            }
                        } else {
                            result.add(LegalActionInfo(
                                actionType = "CastSpell",
                                description = "Cast ${cardComponent.name}",
                                action = CastSpell(playerId, cardId),
                                manaCostString = costString,
                                sourceZone = "GRAVEYARD"
                            ))
                        }
                    } else {
                        result.add(LegalActionInfo(
                            actionType = "CastSpell",
                            description = "Cast ${cardComponent.name}",
                            action = CastSpell(playerId, cardId),
                            isAffordable = false,
                            manaCostString = costString,
                            sourceZone = "GRAVEYARD"
                        ))
                    }
                }
            }
        }

        // Check for mana abilities on battlefield permanents
        // Use projected state to find all permanents controlled by this player
        // (accounts for control-changing effects like Annex)
        val projectedState = state.projectedState
        val battlefieldPermanents = projectedState.getBattlefieldControlledBy(playerId)
        for (entityId in battlefieldPermanents) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Face-down creatures have no abilities (Rule 707.2)
            if (container.has<FaceDownComponent>()) continue

            val entityLostAllAbilities = projectedState.hasLostAllAbilities(entityId)

            // Projected controller already verified - look up card definition for mana abilities
            val cardDef = cardRegistry.getCard(cardComponent.name) ?: continue
            // Include granted activated abilities that are mana abilities (both temporary and static)
            val grantedManaAbilities = state.grantedActivatedAbilities
                .filter { it.entityId == entityId }
                .map { it.ability }
                .filter { it.isManaAbility }
            val staticManaAbilities = getStaticGrantedActivatedAbilities(entityId, state)
                .filter { it.isManaAbility }
            // If entity lost all abilities, only granted/static abilities remain (own abilities suppressed)
            val ownManaAbilities = if (entityLostAllAbilities) emptyList() else cardDef.script.activatedAbilities.filter { it.isManaAbility }
            val manaAbilities = ownManaAbilities + grantedManaAbilities + staticManaAbilities

            // Apply text-changing effects to mana ability costs
            val manaTextReplacement = container.get<TextReplacementComponent>()

            for (ability in manaAbilities) {
                // Apply text replacement to cost filters
                val effectiveCost = if (manaTextReplacement != null) {
                    ability.cost.applyTextReplacement(manaTextReplacement)
                } else {
                    ability.cost
                }

                // Check if the ability can be activated and gather cost info
                var tapTargets: List<EntityId>? = null
                var tapCost: AbilityCost.TapPermanents? = null
                var sacrificeTargets: List<EntityId>? = null
                var sacrificeCost: AbilityCost.Sacrifice? = null

                when (effectiveCost) {
                    is AbilityCost.Tap -> {
                        // Must be untapped
                        if (container.has<TappedComponent>()) continue

                        // Check summoning sickness for creatures (non-lands)
                        if (!cardComponent.typeLine.isLand && cardComponent.typeLine.isCreature) {
                            val hasSummoningSickness = container.has<SummoningSicknessComponent>()
                            val hasHaste = cardComponent.baseKeywords.contains(Keyword.HASTE)
                            if (hasSummoningSickness && !hasHaste) continue
                        }
                    }
                    is AbilityCost.TapAttachedCreature -> {
                        val attachedId = container.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()?.targetId
                        if (attachedId == null) continue
                        val attachedEntity = state.getEntity(attachedId) ?: continue
                        if (attachedEntity.has<TappedComponent>()) continue
                        val attachedCard = attachedEntity.get<CardComponent>()
                        if (attachedCard != null && attachedCard.typeLine.isCreature) {
                            val hasSummoningSickness = attachedEntity.has<SummoningSicknessComponent>()
                            val hasHaste = attachedCard.baseKeywords.contains(Keyword.HASTE)
                            if (hasSummoningSickness && !hasHaste) continue
                        }
                    }
                    is AbilityCost.TapPermanents -> {
                        tapCost = effectiveCost
                        tapTargets = findAbilityTapTargets(state, playerId, tapCost.filter)
                        if (tapTargets.size < tapCost.count) continue
                    }
                    is AbilityCost.Sacrifice -> {
                        sacrificeCost = effectiveCost
                        sacrificeTargets = findAbilitySacrificeTargets(state, playerId, sacrificeCost.filter, if (sacrificeCost.excludeSelf) entityId else null)
                        if (sacrificeTargets.size < sacrificeCost.count) continue
                    }
                    is AbilityCost.SacrificeChosenCreatureType -> {
                        val chosenType = container.get<ChosenCreatureTypeComponent>()?.creatureType
                        if (chosenType == null) continue
                        val dynamicFilter = GameObjectFilter.Creature.withSubtype(chosenType)
                        sacrificeCost = AbilityCost.Sacrifice(dynamicFilter)
                        sacrificeTargets = findAbilitySacrificeTargets(state, playerId, dynamicFilter)
                        if (sacrificeTargets.isEmpty()) continue
                    }
                    is AbilityCost.Composite -> {
                        val compositeCost = effectiveCost
                        var costCanBePaid = true
                        // If composite cost includes Tap, exclude the source from mana solving
                        val hasTapCost = compositeCost.costs.any { it is AbilityCost.Tap }
                        val excludeFromMana = if (hasTapCost) setOf(entityId) else emptySet()
                        for (subCost in compositeCost.costs) {
                            when (subCost) {
                                is AbilityCost.Tap -> {
                                    if (container.has<TappedComponent>()) {
                                        costCanBePaid = false
                                        break
                                    }
                                    if (!cardComponent.typeLine.isLand && cardComponent.typeLine.isCreature) {
                                        val hasSummoningSickness = container.has<SummoningSicknessComponent>()
                                        val hasHaste = cardComponent.baseKeywords.contains(Keyword.HASTE)
                                        if (hasSummoningSickness && !hasHaste) {
                                            costCanBePaid = false
                                            break
                                        }
                                    }
                                }
                                is AbilityCost.Mana -> {
                                    if (!manaSolver.canPay(state, playerId, subCost.cost, excludeSources = excludeFromMana)) {
                                        costCanBePaid = false
                                        break
                                    }
                                }
                                is AbilityCost.Sacrifice -> {
                                    sacrificeCost = subCost
                                    sacrificeTargets = findAbilitySacrificeTargets(state, playerId, subCost.filter, if (subCost.excludeSelf) entityId else null)
                                    if (sacrificeTargets.size < subCost.count) {
                                        costCanBePaid = false
                                        break
                                    }
                                }
                                is AbilityCost.SacrificeChosenCreatureType -> {
                                    val chosenType = container.get<ChosenCreatureTypeComponent>()?.creatureType
                                    if (chosenType == null) {
                                        costCanBePaid = false
                                        break
                                    }
                                    val dynamicFilter = GameObjectFilter.Creature.withSubtype(chosenType)
                                    sacrificeCost = AbilityCost.Sacrifice(dynamicFilter)
                                    sacrificeTargets = findAbilitySacrificeTargets(state, playerId, dynamicFilter)
                                    if (sacrificeTargets.isEmpty()) {
                                        costCanBePaid = false
                                        break
                                    }
                                }
                                is AbilityCost.SacrificeSelf -> {
                                    sacrificeTargets = listOf(entityId)
                                }
                                is AbilityCost.TapAttachedCreature -> {
                                    val attachedId = container.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()?.targetId
                                    if (attachedId == null) {
                                        costCanBePaid = false
                                        break
                                    }
                                    val attachedEntity = state.getEntity(attachedId)
                                    if (attachedEntity == null || attachedEntity.has<TappedComponent>()) {
                                        costCanBePaid = false
                                        break
                                    }
                                    val attachedCard = attachedEntity.get<CardComponent>()
                                    if (attachedCard != null && attachedCard.typeLine.isCreature) {
                                        val hasSummoningSickness = attachedEntity.has<SummoningSicknessComponent>()
                                        val hasHaste = attachedCard.baseKeywords.contains(Keyword.HASTE)
                                        if (hasSummoningSickness && !hasHaste) {
                                            costCanBePaid = false
                                            break
                                        }
                                    }
                                }
                                is AbilityCost.TapPermanents -> {
                                    tapCost = subCost
                                    tapTargets = findAbilityTapTargets(state, playerId, subCost.filter)
                                    if (tapTargets.size < subCost.count) {
                                        costCanBePaid = false
                                        break
                                    }
                                }
                                is AbilityCost.ReturnToHand -> {
                                    // Bounce costs not typical for mana abilities but handle for completeness
                                }
                                else -> {}
                            }
                        }
                        if (!costCanBePaid) continue
                    }
                    else -> {
                        // Other cost types - allow for now, engine will validate
                    }
                }

                val costInfo = if (tapTargets != null && tapCost != null) {
                    AdditionalCostInfo(
                        description = tapCost.description,
                        costType = "TapPermanents",
                        validTapTargets = tapTargets,
                        tapCount = tapCost.count
                    )
                } else if (sacrificeTargets != null && sacrificeCost != null) {
                    AdditionalCostInfo(
                        description = sacrificeCost.description,
                        costType = "SacrificePermanent",
                        validSacrificeTargets = sacrificeTargets,
                        sacrificeCount = sacrificeCost.count
                    )
                } else null

                val manaAbilityManaCostString = when (effectiveCost) {
                    is AbilityCost.Mana -> effectiveCost.cost.toString()
                    is AbilityCost.Composite -> effectiveCost.costs
                        .filterIsInstance<AbilityCost.Mana>().firstOrNull()?.cost?.toString()
                    else -> null
                }

                result.add(LegalActionInfo(
                    actionType = "ActivateAbility",
                    description = ability.description,
                    action = ActivateAbility(playerId, entityId, ability.id),
                    isManaAbility = true,
                    additionalCostInfo = costInfo,
                    requiresManaColorChoice = ability.effect is AddAnyColorManaEffect ||
                        ability.effect is AddManaOfColorAmongEffect ||
                        (ability.effect is CompositeEffect && (ability.effect as CompositeEffect).effects.any { it is AddAnyColorManaEffect }),
                    manaCostString = manaAbilityManaCostString
                ))
            }
        }

        // Check for face-down creatures that can be turned face-up (morph)
        // This is a special action that doesn't use the stack (can be done at any time with priority)
        for (entityId in battlefieldPermanents) {
            val container = state.getEntity(entityId) ?: continue

            // Must be face-down
            if (!container.has<FaceDownComponent>()) continue

            // Must have morph data (to get the morph cost)
            val morphData = container.get<MorphDataComponent>() ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Check if player can afford the morph cost (including any morph cost increases)
            val morphCostIncrease = costCalculator.calculateMorphCostIncrease(state)
            when (val cost = morphData.morphCost) {
                is PayCost.Mana -> {
                    val effectiveCost = costCalculator.increaseGenericCost(cost.cost, morphCostIncrease)
                    if (effectiveCost.hasX) {
                        // X morph cost (e.g., {X}{X}{R}) — always show as available with X selection
                        val availableSources = manaSolver.getAvailableManaCount(state, playerId)
                        val fixedCost = effectiveCost.cmc  // X contributes 0 to CMC
                        val xSymbolCount = effectiveCost.xCount.coerceAtLeast(1)
                        val maxX = ((availableSources - fixedCost) / xSymbolCount).coerceAtLeast(0)
                        result.add(LegalActionInfo(
                            actionType = "TurnFaceUp",
                            description = "Turn face-up (${cost.description})",
                            action = TurnFaceUp(playerId, entityId),
                            manaCostString = effectiveCost.toString(),
                            hasXCost = true,
                            maxAffordableX = maxX
                        ))
                    } else if (manaSolver.canPay(state, playerId, effectiveCost)) {
                        val autoTapSolution = manaSolver.solve(state, playerId, effectiveCost)
                        val autoTapPreview = autoTapSolution?.sources?.map { it.entityId }
                        result.add(LegalActionInfo(
                            actionType = "ActivateAbility",
                            description = "Turn face-up (${cost.description})",
                            action = TurnFaceUp(playerId, entityId),
                            manaCostString = effectiveCost.toString(),
                            autoTapPreview = autoTapPreview
                        ))
                    }
                }
                is PayCost.PayLife -> {
                    // Life-payment morph is always available (paying life that kills you is legal)
                    result.add(LegalActionInfo(
                        actionType = "ActivateAbility",
                        description = "Turn face-up (${cost.description})",
                        action = TurnFaceUp(playerId, entityId),
                    ))
                }
                is PayCost.ReturnToHand -> {
                    val validTargets = findReturnToHandTargets(state, playerId, cost.filter, entityId)
                    if (validTargets.size >= cost.count) {
                        result.add(LegalActionInfo(
                            actionType = "ActivateAbility",
                            description = "Turn face-up (${cost.description})",
                            action = TurnFaceUp(playerId, entityId),
                            additionalCostInfo = AdditionalCostInfo(
                                description = cost.description,
                                costType = "Sacrifice",
                                validSacrificeTargets = validTargets,
                                sacrificeCount = cost.count
                            )
                        ))
                    }
                }
                is PayCost.Sacrifice -> {
                    val validTargets = findMorphSacrificeTargets(state, playerId, cost.filter, entityId)
                    if (validTargets.size >= cost.count) {
                        result.add(LegalActionInfo(
                            actionType = "ActivateAbility",
                            description = "Turn face-up (${cost.description})",
                            action = TurnFaceUp(playerId, entityId),
                            additionalCostInfo = AdditionalCostInfo(
                                description = cost.description,
                                costType = "Sacrifice",
                                validSacrificeTargets = validTargets,
                                sacrificeCount = cost.count
                            )
                        ))
                    }
                }
                is PayCost.Discard -> {
                    val validTargets = findMorphDiscardTargets(state, playerId, cost.filter)
                    if (validTargets.size >= cost.count) {
                        result.add(LegalActionInfo(
                            actionType = "ActivateAbility",
                            description = "Turn face-up (${cost.description})",
                            action = TurnFaceUp(playerId, entityId),
                            additionalCostInfo = AdditionalCostInfo(
                                description = cost.description,
                                costType = "DiscardCard",
                                validDiscardTargets = validTargets,
                                discardCount = cost.count
                            )
                        ))
                    }
                }
                is PayCost.RevealCard -> {
                    val validTargets = findMorphRevealTargets(state, playerId, cost.filter)
                    if (validTargets.size >= cost.count) {
                        result.add(LegalActionInfo(
                            actionType = "ActivateAbility",
                            description = "Turn face-up (${cost.description})",
                            action = TurnFaceUp(playerId, entityId),
                            additionalCostInfo = AdditionalCostInfo(
                                description = cost.description,
                                costType = "RevealCard",
                                validDiscardTargets = validTargets,
                                discardCount = cost.count
                            )
                        ))
                    }
                }
                is PayCost.Exile -> {
                    val validTargets = findMorphExileTargets(state, playerId, cost.filter, cost.zone)
                    if (validTargets.size >= cost.count) {
                        result.add(LegalActionInfo(
                            actionType = "ActivateAbility",
                            description = "Turn face-up (${cost.description})",
                            action = TurnFaceUp(playerId, entityId),
                            additionalCostInfo = AdditionalCostInfo(
                                description = cost.description,
                                costType = "ExileFromZone",
                                validExileTargets = validTargets,
                                exileMinCount = cost.count,
                                exileMaxCount = cost.count
                            )
                        ))
                    }
                }
            }
        }

        // Check for non-mana activated abilities on battlefield permanents
        for (entityId in battlefieldPermanents) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Face-down creatures have no abilities (Rule 707.2)
            if (container.has<FaceDownComponent>()) continue

            val cardDef = cardRegistry.getCard(cardComponent.name) ?: continue
            // Include granted activated abilities alongside the card's own abilities (both temporary and static)
            val grantedAbilities = state.grantedActivatedAbilities
                .filter { it.entityId == entityId }
                .map { it.ability }
            val staticAbilities = getStaticGrantedActivatedAbilities(entityId, state)
            val allAbilities = grantedAbilities + staticAbilities
            // If entity lost all abilities, suppress its own non-mana abilities
            val ownNonManaAbilities = if (projectedState.hasLostAllAbilities(entityId)) emptyList()
                else cardDef.script.activatedAbilities.filter { !it.isManaAbility }
            val nonManaAbilities = ownNonManaAbilities + allAbilities.filter { !it.isManaAbility }

            // Apply text-changing effects to ability costs and targets
            val textReplacement = container.get<TextReplacementComponent>()

            for (ability in nonManaAbilities) {
                // Planeswalker loyalty abilities: sorcery speed + once per turn + loyalty cost check
                if (ability.isPlaneswalkerAbility) {
                    if (!canPlaySorcerySpeed) continue
                    val tracker = container.get<AbilityActivatedThisTurnComponent>()
                    if (tracker != null && tracker.loyaltyActivationCount > 0) {
                        val maxActivations = getMaxLoyaltyActivations(state, playerId)
                        if (tracker.hasReachedLoyaltyLimit(maxActivations)) continue
                    }
                    // Check loyalty cost payability for negative costs
                    val loyaltyCost = ability.cost as? AbilityCost.Loyalty
                    if (loyaltyCost != null && loyaltyCost.change < 0) {
                        val counters = container.get<CountersComponent>()
                        val currentLoyalty = counters?.getCount(CounterType.LOYALTY) ?: 0
                        if (currentLoyalty < -loyaltyCost.change) continue
                    }
                }

                // Apply text replacement to cost filters (e.g., "Sacrifice a Goblin" → "Sacrifice a Bird")
                val effectiveCost = if (textReplacement != null) {
                    ability.cost.applyTextReplacement(textReplacement)
                } else {
                    ability.cost
                }

                // Check cost requirements and gather sacrifice/tap/bounce targets if needed
                var sacrificeTargets: List<EntityId>? = null
                var sacrificeCost: AbilityCost.Sacrifice? = null
                var tapTargets: List<EntityId>? = null
                var tapCost: AbilityCost.TapPermanents? = null
                var bounceTargets: List<EntityId>? = null
                var bounceCost: AbilityCost.ReturnToHand? = null
                var costAffordable = true

                when (effectiveCost) {
                    is AbilityCost.Tap -> {
                        if (container.has<TappedComponent>()) continue
                        if (!cardComponent.typeLine.isLand && cardComponent.typeLine.isCreature) {
                            val hasSummoningSickness = container.has<SummoningSicknessComponent>()
                            val hasHaste = cardComponent.baseKeywords.contains(Keyword.HASTE)
                            if (hasSummoningSickness && !hasHaste) continue
                        }
                    }
                    is AbilityCost.TapAttachedCreature -> {
                        val attachedId = container.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()?.targetId
                        if (attachedId == null) continue
                        val attachedEntity = state.getEntity(attachedId) ?: continue
                        if (attachedEntity.has<TappedComponent>()) continue
                        val attachedCard = attachedEntity.get<CardComponent>()
                        if (attachedCard != null && attachedCard.typeLine.isCreature) {
                            val hasSummoningSickness = attachedEntity.has<SummoningSicknessComponent>()
                            val hasHaste = attachedCard.baseKeywords.contains(Keyword.HASTE)
                            if (hasSummoningSickness && !hasHaste) continue
                        }
                    }
                    is AbilityCost.Mana -> {
                        if (!manaSolver.canPay(state, playerId, effectiveCost.cost)) costAffordable = false
                    }
                    is AbilityCost.Sacrifice -> {
                        sacrificeCost = effectiveCost
                        sacrificeTargets = findAbilitySacrificeTargets(state, playerId, sacrificeCost.filter, if (sacrificeCost.excludeSelf) entityId else null)
                        if (sacrificeTargets.size < sacrificeCost.count) continue
                    }
                    is AbilityCost.ReturnToHand -> {
                        bounceCost = effectiveCost
                        bounceTargets = findAbilityBounceTargets(state, playerId, bounceCost.filter)
                        if (bounceTargets.size < bounceCost.count) continue
                    }
                    is AbilityCost.SacrificeChosenCreatureType -> {
                        val chosenType = container.get<ChosenCreatureTypeComponent>()?.creatureType
                        if (chosenType == null) continue
                        val dynamicFilter = GameObjectFilter.Creature.withSubtype(chosenType)
                        sacrificeCost = AbilityCost.Sacrifice(dynamicFilter)
                        sacrificeTargets = findAbilitySacrificeTargets(state, playerId, dynamicFilter)
                        if (sacrificeTargets.isEmpty()) continue
                    }
                    is AbilityCost.TapPermanents -> {
                        tapCost = effectiveCost
                        tapTargets = findAbilityTapTargets(state, playerId, tapCost.filter)
                        if (tapTargets.size < tapCost.count) continue
                    }
                    is AbilityCost.SacrificeSelf -> {
                        // Source must be on battlefield (always true when iterating battlefield)
                        sacrificeTargets = listOf(entityId)
                    }
                    is AbilityCost.Composite -> {
                        val compositeCost = effectiveCost
                        var costCanBePaid = true
                        // If composite cost includes Tap, exclude the source from mana solving
                        val hasTapCost = compositeCost.costs.any { it is AbilityCost.Tap }
                        val excludeFromMana = if (hasTapCost) setOf(entityId) else emptySet()
                        for (subCost in compositeCost.costs) {
                            when (subCost) {
                                is AbilityCost.Tap -> {
                                    if (container.has<TappedComponent>()) {
                                        costCanBePaid = false
                                        break
                                    }
                                    if (!cardComponent.typeLine.isLand && cardComponent.typeLine.isCreature) {
                                        val hasSummoningSickness = container.has<SummoningSicknessComponent>()
                                        val hasHaste = cardComponent.baseKeywords.contains(Keyword.HASTE)
                                        if (hasSummoningSickness && !hasHaste) {
                                            costCanBePaid = false
                                            break
                                        }
                                    }
                                }
                                is AbilityCost.Mana -> {
                                    if (!manaSolver.canPay(state, playerId, subCost.cost, excludeSources = excludeFromMana)) {
                                        costCanBePaid = false
                                        break
                                    }
                                }
                                is AbilityCost.Sacrifice -> {
                                    sacrificeCost = subCost
                                    sacrificeTargets = findAbilitySacrificeTargets(state, playerId, subCost.filter, if (subCost.excludeSelf) entityId else null)
                                    if (sacrificeTargets.size < subCost.count) {
                                        costCanBePaid = false
                                        break
                                    }
                                }
                                is AbilityCost.SacrificeChosenCreatureType -> {
                                    val chosenType = container.get<ChosenCreatureTypeComponent>()?.creatureType
                                    if (chosenType == null) {
                                        costCanBePaid = false
                                        break
                                    }
                                    val dynamicFilter = GameObjectFilter.Creature.withSubtype(chosenType)
                                    sacrificeCost = AbilityCost.Sacrifice(dynamicFilter)
                                    sacrificeTargets = findAbilitySacrificeTargets(state, playerId, dynamicFilter)
                                    if (sacrificeTargets.isEmpty()) {
                                        costCanBePaid = false
                                        break
                                    }
                                }
                                is AbilityCost.SacrificeSelf -> {
                                    // Source must be on battlefield (always true when iterating battlefield)
                                    sacrificeTargets = listOf(entityId)
                                }
                                is AbilityCost.TapAttachedCreature -> {
                                    val attachedId = container.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()?.targetId
                                    if (attachedId == null) {
                                        costCanBePaid = false
                                        break
                                    }
                                    val attachedEntity = state.getEntity(attachedId)
                                    if (attachedEntity == null || attachedEntity.has<TappedComponent>()) {
                                        costCanBePaid = false
                                        break
                                    }
                                    val attachedCard = attachedEntity.get<CardComponent>()
                                    if (attachedCard != null && attachedCard.typeLine.isCreature) {
                                        val hasSummoningSickness = attachedEntity.has<SummoningSicknessComponent>()
                                        val hasHaste = attachedCard.baseKeywords.contains(Keyword.HASTE)
                                        if (hasSummoningSickness && !hasHaste) {
                                            costCanBePaid = false
                                            break
                                        }
                                    }
                                }
                                is AbilityCost.TapPermanents -> {
                                    tapCost = subCost
                                    tapTargets = findAbilityTapTargets(state, playerId, subCost.filter)
                                    if (tapTargets.size < subCost.count) {
                                        costCanBePaid = false
                                        break
                                    }
                                }
                                is AbilityCost.ReturnToHand -> {
                                    bounceCost = subCost
                                    bounceTargets = findAbilityBounceTargets(state, playerId, subCost.filter)
                                    if (bounceTargets.size < subCost.count) {
                                        costCanBePaid = false
                                        break
                                    }
                                }
                                is AbilityCost.ExileFromGraveyard -> {
                                    val graveyardZone = com.wingedsheep.engine.state.ZoneKey(playerId, Zone.GRAVEYARD)
                                    val graveyardCards = state.getZone(graveyardZone)
                                    if (graveyardCards.size < subCost.count) {
                                        costCanBePaid = false
                                        break
                                    }
                                }
                                is AbilityCost.ExileXFromGraveyard -> {
                                    // ExileXFromGraveyard: validated via maxAffordableX cap below
                                }
                                is AbilityCost.RemoveXPlusOnePlusOneCounters -> {
                                    // RemoveXPlusOnePlusOneCounters: validated via maxAffordableX cap below
                                }
                                is AbilityCost.TapXPermanents -> {
                                    // TapXPermanents: validated via maxAffordableX cap below
                                    // Also provide tap targets for the UI
                                    tapTargets = findAbilityTapTargets(state, playerId, subCost.filter)
                                }
                                else -> {}
                            }
                        }
                        if (!costCanBePaid) costAffordable = false
                    }
                    else -> {}
                }

                // Check activation restrictions
                var restrictionsMet = true
                for (restriction in ability.restrictions) {
                    if (!checkActivationRestriction(state, playerId, restriction, entityId, ability.id)) {
                        restrictionsMet = false
                        break
                    }
                }
                if (!restrictionsMet) continue

                // If cost is unaffordable, add as greyed-out option and skip expensive computations
                if (!costAffordable) {
                    val abilityManaCostString = when (ability.cost) {
                        is AbilityCost.Mana -> (ability.cost as AbilityCost.Mana).cost.toString()
                        is AbilityCost.Composite -> (ability.cost as AbilityCost.Composite).costs
                            .filterIsInstance<AbilityCost.Mana>().firstOrNull()?.cost?.toString()
                        else -> null
                    }
                    result.add(LegalActionInfo(
                        actionType = "ActivateAbility",
                        description = ability.description,
                        action = ActivateAbility(playerId, entityId, ability.id),
                        isAffordable = false,
                        manaCostString = abilityManaCostString
                    ))
                    continue
                }

                // Check for X-variable costs early (needed for counter removal info and cost info)
                val hasRemoveXCountersCostEarly = when (ability.cost) {
                    is AbilityCost.RemoveXPlusOnePlusOneCounters -> true
                    is AbilityCost.Composite -> (ability.cost as AbilityCost.Composite).costs
                        .any { it is AbilityCost.RemoveXPlusOnePlusOneCounters }
                    else -> false
                }

                val hasTapXPermanentsCost = when (ability.cost) {
                    is AbilityCost.TapXPermanents -> true
                    is AbilityCost.Composite -> (ability.cost as AbilityCost.Composite).costs
                        .any { it is AbilityCost.TapXPermanents }
                    else -> false
                }

                // Build counter removal creature info if ability has RemoveXPlusOnePlusOneCounters cost
                val counterRemovalCreatures = if (hasRemoveXCountersCostEarly) {
                    state.entities.mapNotNull { (eid, c) ->
                        if (c.get<ControllerComponent>()?.playerId == playerId &&
                            c.get<CardComponent>()?.typeLine?.isCreature == true) {
                            val counters = c.get<CountersComponent>()
                                ?.getCount(com.wingedsheep.sdk.core.CounterType.PLUS_ONE_PLUS_ONE) ?: 0
                            if (counters > 0) {
                                val card = c.get<CardComponent>()!!
                                CounterRemovalCreatureInfo(
                                    entityId = eid,
                                    name = card.name,
                                    availableCounters = counters,
                                    imageUri = card.imageUri
                                )
                            } else null
                        } else null
                    }
                } else emptyList()

                // Build additional cost info for sacrifice, tap, bounce, or counter removal costs
                val costInfo = if (tapTargets != null && tapCost != null) {
                    AdditionalCostInfo(
                        description = tapCost.description,
                        costType = "TapPermanents",
                        validTapTargets = tapTargets,
                        tapCount = tapCost.count,
                        counterRemovalCreatures = counterRemovalCreatures
                    )
                } else if (tapTargets != null && hasTapXPermanentsCost) {
                    // TapXPermanents: tap count is variable (chosen by player as X value)
                    val tapXDesc = when (ability.cost) {
                        is AbilityCost.TapXPermanents -> (ability.cost as AbilityCost.TapXPermanents).description
                        is AbilityCost.Composite -> (ability.cost as AbilityCost.Composite).costs
                            .filterIsInstance<AbilityCost.TapXPermanents>().firstOrNull()?.description
                            ?: "Tap X permanents you control"
                        else -> "Tap X permanents you control"
                    }
                    AdditionalCostInfo(
                        description = tapXDesc,
                        costType = "TapPermanents",
                        validTapTargets = tapTargets,
                        tapCount = 0, // Variable — UI uses X value selector instead
                        counterRemovalCreatures = counterRemovalCreatures
                    )
                } else if (sacrificeTargets != null && sacrificeCost != null) {
                    AdditionalCostInfo(
                        description = sacrificeCost.description,
                        costType = "SacrificePermanent",
                        validSacrificeTargets = sacrificeTargets,
                        sacrificeCount = sacrificeCost.count,
                        counterRemovalCreatures = counterRemovalCreatures
                    )
                } else if (bounceTargets != null && bounceCost != null) {
                    AdditionalCostInfo(
                        description = bounceCost.description,
                        costType = "BouncePermanent",
                        validBounceTargets = bounceTargets,
                        bounceCount = bounceCost.count,
                        counterRemovalCreatures = counterRemovalCreatures
                    )
                } else if (sacrificeTargets != null) {
                    // SacrificeSelf cost — sacrifice target is the source itself
                    AdditionalCostInfo(
                        description = "Sacrifice this permanent",
                        costType = "SacrificeSelf",
                        validSacrificeTargets = sacrificeTargets,
                        sacrificeCount = 1,
                        counterRemovalCreatures = counterRemovalCreatures
                    )
                } else if (counterRemovalCreatures.isNotEmpty()) {
                    AdditionalCostInfo(
                        description = "Remove +1/+1 counters from creatures you control",
                        costType = "RemoveCounters",
                        counterRemovalCreatures = counterRemovalCreatures
                    )
                } else null

                // Calculate X cost info for activated abilities with X in their mana cost
                // or X determined by a variable cost (e.g., RemoveXPlusOnePlusOneCounters)
                val abilityManaCost = when (ability.cost) {
                    is AbilityCost.Mana -> (ability.cost as AbilityCost.Mana).cost
                    is AbilityCost.Composite -> (ability.cost as AbilityCost.Composite).costs
                        .filterIsInstance<AbilityCost.Mana>().firstOrNull()?.cost
                    else -> null
                }
                val abilityManaCostString = abilityManaCost?.toString()
                val abilityHasXInManaCost = abilityManaCost?.hasX == true

                // Reuse the early checks for X-variable costs
                val hasRemoveXCountersCost = hasRemoveXCountersCostEarly
                val abilityHasXCost = abilityHasXInManaCost || hasRemoveXCountersCost || hasTapXPermanentsCost

                val abilityMaxAffordableX: Int? = if (abilityHasXCost) {
                    var maxX = if (abilityHasXInManaCost) {
                        val availableSources = manaSolver.getAvailableManaCount(state, playerId)
                        val fixedCost = abilityManaCost.cmc  // X contributes 0 to CMC
                        (availableSources - fixedCost).coerceAtLeast(0)
                    } else {
                        Int.MAX_VALUE // No mana-based X constraint
                    }

                    // Cap by graveyard size if ability has ExileXFromGraveyard cost
                    val hasExileXCost = when (ability.cost) {
                        is AbilityCost.Composite -> (ability.cost as AbilityCost.Composite).costs
                            .any { it is AbilityCost.ExileXFromGraveyard }
                        else -> false
                    }
                    if (hasExileXCost) {
                        val graveyardZone = com.wingedsheep.engine.state.ZoneKey(playerId, Zone.GRAVEYARD)
                        val graveyardSize = state.getZone(graveyardZone).size
                        maxX = minOf(maxX, graveyardSize)
                    }

                    // Cap by total +1/+1 counters if ability has RemoveXPlusOnePlusOneCounters cost
                    if (hasRemoveXCountersCost) {
                        var totalCounters = 0
                        for ((_, container) in state.entities) {
                            if (container.get<ControllerComponent>()?.playerId == playerId &&
                                container.get<CardComponent>()?.typeLine?.isCreature == true) {
                                totalCounters += container.get<CountersComponent>()
                                    ?.getCount(com.wingedsheep.sdk.core.CounterType.PLUS_ONE_PLUS_ONE) ?: 0
                            }
                        }
                        maxX = minOf(maxX, totalCounters)
                    }

                    // Cap by untapped matching permanents if ability has TapXPermanents cost
                    if (hasTapXPermanentsCost) {
                        val tapXCost = when (ability.cost) {
                            is AbilityCost.TapXPermanents -> ability.cost as AbilityCost.TapXPermanents
                            is AbilityCost.Composite -> (ability.cost as AbilityCost.Composite).costs
                                .filterIsInstance<AbilityCost.TapXPermanents>().firstOrNull()
                            else -> null
                        }
                        if (tapXCost != null) {
                            val availableTapTargets = findAbilityTapTargets(state, playerId, tapXCost.filter)
                            maxX = minOf(maxX, availableTapTargets.size)
                        }
                    }

                    maxX
                } else null

                // Compute auto-tap preview for UI highlighting
                val abilityAutoTapPreview = if (abilityManaCost != null && !abilityHasXCost) {
                    manaSolver.solve(state, playerId, abilityManaCost)?.sources?.map { it.entityId }
                } else null

                // Compute maxRepeatableActivations for eligible self-targeting abilities
                // Eligible: pure mana cost, no X, no once-per-turn restriction
                val isRepeatEligible = ability.cost is AbilityCost.Mana
                    && !abilityHasXCost
                    && !ability.restrictions.any {
                        it is ActivationRestriction.OncePerTurn ||
                        (it is ActivationRestriction.All && it.restrictions.any { r -> r is ActivationRestriction.OncePerTurn })
                    }
                val maxRepeatableActivations: Int? = if (isRepeatEligible && abilityManaCost != null) {
                    val availableSources = manaSolver.getAvailableManaCount(state, playerId)
                    val costPerActivation = abilityManaCost.cmc
                    if (costPerActivation > 0) {
                        val maxRepeats = availableSources / costPerActivation
                        if (maxRepeats > 1) maxRepeats else null
                    } else null
                } else null

                // Check for target requirements (apply text-changing effects to filter)
                val targetReqs = if (textReplacement != null) {
                    ability.targetRequirements.map { it.applyTextReplacement(textReplacement) }
                } else {
                    ability.targetRequirements
                }
                if (targetReqs.isNotEmpty()) {
                    // Build target info for each requirement (same pattern as spells)
                    val targetReqInfos = targetReqs.mapIndexed { index, req ->
                        val validTargets = findValidTargets(state, playerId, req, sourceId = entityId)
                        LegalActionTargetInfo(
                            index = index,
                            description = req.description,
                            minTargets = req.effectiveMinCount,
                            maxTargets = req.count,
                            validTargets = validTargets,
                            targetZone = getTargetZone(req)
                        )
                    }

                    // All requirements must be satisfiable
                    val allRequirementsSatisfied = targetReqInfos.all { reqInfo ->
                        reqInfo.validTargets.isNotEmpty() || reqInfo.minTargets == 0
                    }
                    if (!allRequirementsSatisfied) continue

                    val firstReq = targetReqs.first()
                    val firstReqInfo = targetReqInfos.first()

                    // Check if we can auto-select player targets (single target requirement, single valid choice)
                    if (targetReqs.size == 1 && shouldAutoSelectPlayerTarget(firstReq, firstReqInfo.validTargets)) {
                        val autoSelectedTarget = ChosenTarget.Player(firstReqInfo.validTargets.first())
                        result.add(LegalActionInfo(
                            actionType = "ActivateAbility",
                            description = ability.description,
                            action = ActivateAbility(playerId, entityId, ability.id, targets = listOf(autoSelectedTarget)),
                            additionalCostInfo = costInfo,
                            hasXCost = abilityHasXCost,
                            maxAffordableX = abilityMaxAffordableX,
                            autoTapPreview = abilityAutoTapPreview,
                            manaCostString = abilityManaCostString
                        ))
                    } else if (targetReqs.size == 1 && firstReqInfo.validTargets.size == 1 && firstReqInfo.validTargets.first() == entityId) {
                        // Self-targeting: only valid target is the source itself — auto-select and offer repeat
                        val autoSelectedTarget = ChosenTarget.Permanent(entityId)
                        result.add(LegalActionInfo(
                            actionType = "ActivateAbility",
                            description = ability.description,
                            action = ActivateAbility(playerId, entityId, ability.id, targets = listOf(autoSelectedTarget)),
                            additionalCostInfo = costInfo,
                            hasXCost = abilityHasXCost,
                            maxAffordableX = abilityMaxAffordableX,
                            autoTapPreview = abilityAutoTapPreview,
                            maxRepeatableActivations = maxRepeatableActivations,
                            manaCostString = abilityManaCostString
                        ))
                    } else {
                        result.add(LegalActionInfo(
                            actionType = "ActivateAbility",
                            description = ability.description,
                            action = ActivateAbility(playerId, entityId, ability.id),
                            validTargets = firstReqInfo.validTargets,
                            requiresTargets = true,
                            targetCount = firstReq.count,
                            minTargets = firstReq.effectiveMinCount,
                            targetDescription = firstReq.description,
                            targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                            additionalCostInfo = costInfo,
                            hasXCost = abilityHasXCost,
                            maxAffordableX = abilityMaxAffordableX,
                            autoTapPreview = abilityAutoTapPreview,
                            manaCostString = abilityManaCostString
                        ))
                    }
                } else {
                    result.add(LegalActionInfo(
                        actionType = "ActivateAbility",
                        description = ability.description,
                        action = ActivateAbility(playerId, entityId, ability.id),
                        additionalCostInfo = costInfo,
                        hasXCost = abilityHasXCost,
                        maxAffordableX = abilityMaxAffordableX,
                        autoTapPreview = abilityAutoTapPreview,
                        maxRepeatableActivations = maxRepeatableActivations,
                        manaCostString = abilityManaCostString
                    ))
                }
            }
        }

        // Check for "any player may activate" abilities on opponent's permanents (e.g., Lethal Vapors)
        val opponentId = state.turnOrder.firstOrNull { it != playerId }
        if (opponentId != null) {
            val opponentPermanents = projectedState.getBattlefieldControlledBy(opponentId)
            for (entityId in opponentPermanents) {
                val container = state.getEntity(entityId) ?: continue
                val cardComponent = container.get<CardComponent>() ?: continue
                if (container.has<FaceDownComponent>()) continue
                if (projectedState.hasLostAllAbilities(entityId)) continue

                val cardDef = cardRegistry.getCard(cardComponent.name) ?: continue
                val anyPlayerAbilities = cardDef.script.activatedAbilities.filter { ability ->
                    !ability.isManaAbility && ability.restrictions.any { it is ActivationRestriction.AnyPlayerMay }
                }
                if (anyPlayerAbilities.isEmpty()) continue

                val textReplacement = container.get<TextReplacementComponent>()

                for (ability in anyPlayerAbilities) {
                    val effectiveCost = if (textReplacement != null) {
                        ability.cost.applyTextReplacement(textReplacement)
                    } else {
                        ability.cost
                    }

                    // Check cost payability (Free cost always passes)
                    val anyPlayerManaCostString = when (effectiveCost) {
                        is AbilityCost.Free -> null
                        is AbilityCost.Mana -> {
                            if (!manaSolver.canPay(state, playerId, effectiveCost.cost)) continue
                            effectiveCost.cost.toString()
                        }
                        else -> continue // Other costs on opponent's permanents not yet supported
                    }

                    // Check activation restrictions
                    var restrictionsMet = true
                    for (restriction in ability.restrictions) {
                        if (!checkActivationRestriction(state, playerId, restriction, entityId, ability.id)) {
                            restrictionsMet = false
                            break
                        }
                    }
                    if (!restrictionsMet) continue

                    // Check target requirements
                    val targetReqs = if (textReplacement != null) {
                        ability.targetRequirements.map { it.applyTextReplacement(textReplacement) }
                    } else {
                        ability.targetRequirements
                    }
                    if (targetReqs.isNotEmpty()) {
                        val targetReqInfos = targetReqs.mapIndexed { index, req ->
                            val validTargets = findValidTargets(state, playerId, req, sourceId = entityId)
                            LegalActionTargetInfo(
                                index = index,
                                description = req.description,
                                minTargets = req.effectiveMinCount,
                                maxTargets = req.count,
                                validTargets = validTargets,
                                targetZone = getTargetZone(req)
                            )
                        }
                        val allRequirementsSatisfied = targetReqInfos.all { reqInfo ->
                            reqInfo.validTargets.isNotEmpty() || reqInfo.minTargets == 0
                        }
                        if (!allRequirementsSatisfied) continue

                        val firstReq = targetReqs.first()
                        val firstReqInfo = targetReqInfos.first()
                        result.add(LegalActionInfo(
                            actionType = "ActivateAbility",
                            description = ability.description,
                            action = ActivateAbility(playerId, entityId, ability.id),
                            validTargets = firstReqInfo.validTargets,
                            requiresTargets = true,
                            targetCount = firstReq.count,
                            minTargets = firstReq.effectiveMinCount,
                            targetDescription = firstReq.description,
                            targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                            manaCostString = anyPlayerManaCostString
                        ))
                    } else {
                        result.add(LegalActionInfo(
                            actionType = "ActivateAbility",
                            description = ability.description,
                            action = ActivateAbility(playerId, entityId, ability.id),
                            manaCostString = anyPlayerManaCostString
                        ))
                    }
                }
            }
        }

        // Check for Crew abilities on Vehicles controlled by the player
        for (entityId in battlefieldPermanents) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(cardComponent.name) ?: continue

            val crewAbility = cardDef.keywordAbilities
                .filterIsInstance<com.wingedsheep.sdk.scripting.KeywordAbility.Crew>()
                .firstOrNull() ?: continue

            // Find all untapped creatures controlled by the player that can crew
            val validCrewCreatures = mutableListOf<CrewCreatureInfo>()
            var totalAvailablePower = 0
            for (creatureId in battlefieldPermanents) {
                if (creatureId == entityId) continue // Vehicle can't crew itself
                if (!projectedState.isCreature(creatureId)) continue
                val creatureContainer = state.getEntity(creatureId) ?: continue
                if (creatureContainer.has<TappedComponent>()) continue
                // Summoning sickness does NOT prevent crewing
                val power = projectedState.getPower(creatureId) ?: 0
                val creatureName = creatureContainer.get<CardComponent>()?.name ?: "Unknown"
                validCrewCreatures.add(CrewCreatureInfo(creatureId, creatureName, power))
                totalAvailablePower += power
            }

            val canAfford = totalAvailablePower >= crewAbility.power
            result.add(LegalActionInfo(
                actionType = "CrewVehicle",
                description = "Crew ${cardComponent.name}",
                action = CrewVehicle(playerId, entityId, emptyList()),
                isAffordable = canAfford,
                hasCrew = true,
                crewPower = crewAbility.power,
                validCrewCreatures = validCrewCreatures
            ))
        }

        // Check for activated abilities on cards in the graveyard (e.g., Undead Gladiator)
        val graveyardCards = state.getGraveyard(playerId)
        for (entityId in graveyardCards) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            val cardDef = cardRegistry.getCard(cardComponent.name) ?: continue
            val graveyardAbilities = cardDef.script.activatedAbilities.filter {
                it.activateFromZone == Zone.GRAVEYARD
            }

            for (ability in graveyardAbilities) {
                // Check activation restrictions
                var restrictionsMet = true
                for (restriction in ability.restrictions) {
                    if (!checkActivationRestriction(state, playerId, restriction, entityId, ability.id)) {
                        restrictionsMet = false
                        break
                    }
                }
                if (!restrictionsMet) continue

                // Check cost requirements and build cost info
                val effectiveCost = ability.cost
                var costCanBePaid = true
                val handCards = state.getZone(playerId, Zone.HAND)
                var hasDiscardCost = false
                when (effectiveCost) {
                    is AbilityCost.Mana -> {
                        if (!manaSolver.canPay(state, playerId, effectiveCost.cost)) costCanBePaid = false
                    }
                    is AbilityCost.Discard -> {
                        hasDiscardCost = true
                        if (handCards.isEmpty()) costCanBePaid = false
                    }
                    is AbilityCost.Composite -> {
                        for (subCost in effectiveCost.costs) {
                            when (subCost) {
                                is AbilityCost.Mana -> {
                                    if (!manaSolver.canPay(state, playerId, subCost.cost)) {
                                        costCanBePaid = false
                                        break
                                    }
                                }
                                is AbilityCost.Discard -> {
                                    hasDiscardCost = true
                                    if (handCards.isEmpty()) {
                                        costCanBePaid = false
                                        break
                                    }
                                }
                                is AbilityCost.ReturnToHand -> {
                                    val targets = findAbilityBounceTargets(state, playerId, subCost.filter)
                                    if (targets.size < subCost.count) {
                                        costCanBePaid = false
                                        break
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                    else -> {}
                }
                if (!costCanBePaid) continue

                // Build cost info for bounce or discard costs
                val bounceCostFromGraveyard = when (effectiveCost) {
                    is AbilityCost.Composite -> effectiveCost.costs.filterIsInstance<AbilityCost.ReturnToHand>().firstOrNull()
                    is AbilityCost.ReturnToHand -> effectiveCost
                    else -> null
                }
                val costInfo = if (bounceCostFromGraveyard != null) {
                    val bounceTargets = findAbilityBounceTargets(state, playerId, bounceCostFromGraveyard.filter)
                    AdditionalCostInfo(
                        description = bounceCostFromGraveyard.description,
                        costType = "BouncePermanent",
                        validBounceTargets = bounceTargets,
                        bounceCount = bounceCostFromGraveyard.count
                    )
                } else if (hasDiscardCost) {
                    AdditionalCostInfo(
                        description = "Discard a card",
                        costType = "DiscardCard",
                        validDiscardTargets = handCards,
                        discardCount = 1
                    )
                } else null

                // Calculate X cost info
                val abilityManaCost = when (ability.cost) {
                    is AbilityCost.Mana -> (ability.cost as AbilityCost.Mana).cost
                    is AbilityCost.Composite -> (ability.cost as AbilityCost.Composite).costs
                        .filterIsInstance<AbilityCost.Mana>().firstOrNull()?.cost
                    else -> null
                }
                val graveyardManaCostString = abilityManaCost?.toString()
                val abilityHasXCost = abilityManaCost?.hasX == true
                val abilityMaxAffordableX: Int? = if (abilityHasXCost) {
                    val availableSources = manaSolver.getAvailableManaCount(state, playerId)
                    val fixedCost = abilityManaCost.cmc
                    (availableSources - fixedCost).coerceAtLeast(0)
                } else null

                // Compute auto-tap preview for UI highlighting
                val abilityAutoTapPreview = if (abilityManaCost != null && !abilityHasXCost) {
                    manaSolver.solve(state, playerId, abilityManaCost)?.sources?.map { it.entityId }
                } else null

                // Check for target requirements
                val targetReqs = ability.targetRequirements
                if (targetReqs.isNotEmpty()) {
                    val targetReqInfos = targetReqs.mapIndexed { index, req ->
                        val validTargets = findValidTargets(state, playerId, req)
                        LegalActionTargetInfo(
                            index = index,
                            description = req.description,
                            minTargets = req.effectiveMinCount,
                            maxTargets = req.count,
                            validTargets = validTargets,
                            targetZone = getTargetZone(req)
                        )
                    }

                    val allRequirementsSatisfied = targetReqInfos.all { reqInfo ->
                        reqInfo.validTargets.isNotEmpty() || reqInfo.minTargets == 0
                    }
                    if (!allRequirementsSatisfied) continue

                    val firstReq = targetReqs.first()
                    val firstReqInfo = targetReqInfos.first()

                    if (targetReqs.size == 1 && shouldAutoSelectPlayerTarget(firstReq, firstReqInfo.validTargets)) {
                        val autoSelectedTarget = ChosenTarget.Player(firstReqInfo.validTargets.first())
                        result.add(LegalActionInfo(
                            actionType = "ActivateAbility",
                            description = ability.description,
                            action = ActivateAbility(playerId, entityId, ability.id, targets = listOf(autoSelectedTarget)),
                            additionalCostInfo = costInfo,
                            hasXCost = abilityHasXCost,
                            maxAffordableX = abilityMaxAffordableX,
                            autoTapPreview = abilityAutoTapPreview,
                            manaCostString = graveyardManaCostString
                        ))
                    } else {
                        result.add(LegalActionInfo(
                            actionType = "ActivateAbility",
                            description = ability.description,
                            action = ActivateAbility(playerId, entityId, ability.id),
                            validTargets = firstReqInfo.validTargets,
                            requiresTargets = true,
                            targetCount = firstReq.count,
                            minTargets = firstReq.effectiveMinCount,
                            targetDescription = firstReq.description,
                            targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                            additionalCostInfo = costInfo,
                            hasXCost = abilityHasXCost,
                            maxAffordableX = abilityMaxAffordableX,
                            autoTapPreview = abilityAutoTapPreview,
                            manaCostString = graveyardManaCostString
                        ))
                    }
                } else {
                    result.add(LegalActionInfo(
                        actionType = "ActivateAbility",
                        description = ability.description,
                        action = ActivateAbility(playerId, entityId, ability.id),
                        additionalCostInfo = costInfo,
                        hasXCost = abilityHasXCost,
                        maxAffordableX = abilityMaxAffordableX,
                        autoTapPreview = abilityAutoTapPreview,
                        manaCostString = graveyardManaCostString
                    ))
                }
            }
        }

        // Attach available mana sources to every action that has a mana cost (identified by autoTapPreview)
        if (manaSourceInfos != null) {
            return result.map { info ->
                if (info.autoTapPreview != null) {
                    info.copy(availableManaSources = manaSourceInfos)
                } else {
                    info
                }
            }
        }

        return result
    }

    private fun findValidTargets(
        state: GameState,
        playerId: EntityId,
        requirement: TargetRequirement,
        sourceId: EntityId? = null
    ): List<EntityId> {
        return when (requirement) {
            is TargetPlayer -> state.turnOrder.filter { state.hasEntity(it) && !playerHasShroud(state, it) &&
                !playerHasHexproofAgainst(state, it, playerId) }
            is TargetOpponent -> state.turnOrder.filter { it != playerId && state.hasEntity(it) && !playerHasShroud(state, it) &&
                !playerHasHexproof(state, it) }
            is AnyTarget -> {
                // Any target = creatures + planeswalkers + players
                val creatures = findValidPermanentTargets(state, playerId, TargetFilter.Creature, sourceId)
                val players = state.turnOrder.filter { state.hasEntity(it) && !playerHasShroud(state, it) &&
                    !playerHasHexproofAgainst(state, it, playerId) }
                creatures + players
            }
            is TargetCreatureOrPlayer -> {
                val creatures = findValidPermanentTargets(state, playerId, TargetFilter.Creature, sourceId)
                val players = state.turnOrder.filter { state.hasEntity(it) && !playerHasShroud(state, it) &&
                    !playerHasHexproofAgainst(state, it, playerId) }
                creatures + players
            }
            is TargetPlayerOrPlaneswalker -> {
                val players = state.turnOrder.filter { state.hasEntity(it) && !playerHasShroud(state, it) &&
                    !playerHasHexproofAgainst(state, it, playerId) }
                val planeswalkers = findValidPermanentTargets(state, playerId, TargetFilter.Planeswalker, sourceId)
                players + planeswalkers
            }
            is TargetCreatureOrPlaneswalker -> {
                val creatures = findValidPermanentTargets(state, playerId, TargetFilter.Creature, sourceId)
                val planeswalkers = findValidPermanentTargets(state, playerId, TargetFilter.Planeswalker, sourceId)
                creatures + planeswalkers
            }
            is TargetObject -> findValidObjectTargets(state, playerId, requirement.filter, sourceId)
            is TargetSpellOrPermanent -> {
                val permanents = findValidPermanentTargets(state, playerId, TargetFilter.Permanent, sourceId)
                val spells = findValidSpellTargets(state, playerId, TargetFilter.SpellOnStack)
                permanents + spells
            }
            else -> emptyList() // Other target types not yet implemented
        }
    }

    private fun shouldAutoSelectPlayerTarget(
        requirement: TargetRequirement,
        validTargets: List<EntityId>
    ): Boolean {
        val isPlayerTarget = requirement is TargetPlayer || requirement is TargetOpponent
        val requiresExactlyOne = requirement.count == 1 && requirement.effectiveMinCount == 1
        val hasExactlyOneChoice = validTargets.size == 1

        return isPlayerTarget && requiresExactlyOne && hasExactlyOneChoice
    }

    private fun findValidPermanentTargets(
        state: GameState,
        playerId: EntityId,
        filter: TargetFilter,
        sourceId: EntityId? = null
    ): List<EntityId> {
        val projected = state.projectedState
        val battlefield = state.getBattlefield()
        val context = PredicateContext(controllerId = playerId, sourceId = sourceId)
        return battlefield.filter { entityId ->
            // Exclude self if filter says "other"
            if (filter.excludeSelf && entityId == sourceId) {
                return@filter false
            }

            val entityController = state.getEntity(entityId)?.get<ControllerComponent>()?.playerId

            // Check hexproof - can't be targeted by opponents
            if (projected.hasKeyword(entityId, Keyword.HEXPROOF) && entityController != playerId) {
                return@filter false
            }
            // Check shroud - can't be targeted by anyone
            if (projected.hasKeyword(entityId, Keyword.SHROUD)) {
                return@filter false
            }

            predicateEvaluator.matchesWithProjection(state, projected, entityId, filter.baseFilter, context)
        }
    }

    private fun findValidGraveyardTargets(
        state: GameState,
        playerId: EntityId,
        filter: TargetFilter
    ): List<EntityId> {
        // Determine which graveyards to search based on filter's controller predicate
        val playerIds = if (filter.baseFilter.controllerPredicate == ControllerPredicate.ControlledByYou) {
            listOf(playerId)
        } else {
            state.turnOrder.toList()
        }
        val context = PredicateContext(controllerId = playerId)
        return playerIds.flatMap { pid ->
            state.getGraveyard(pid).filter { entityId ->
                predicateEvaluator.matches(state, entityId, filter.baseFilter, context)
            }
        }
    }

    private fun findValidObjectTargets(
        state: GameState,
        playerId: EntityId,
        filter: TargetFilter,
        sourceId: EntityId? = null
    ): List<EntityId> {
        return when (filter.zone) {
            Zone.BATTLEFIELD -> findValidPermanentTargets(state, playerId, filter, sourceId)
            Zone.GRAVEYARD -> findValidGraveyardTargets(state, playerId, filter)
            Zone.STACK -> findValidSpellTargets(state, playerId, filter)
            else -> emptyList()
        }
    }

    private fun findValidSpellTargets(
        state: GameState,
        playerId: EntityId,
        filter: TargetFilter
    ): List<EntityId> {
        val context = PredicateContext(controllerId = playerId)
        return state.stack.filter { spellId ->
            predicateEvaluator.matches(state, spellId, filter.baseFilter, context)
        }
    }

    private fun playerHasShroud(state: GameState, playerId: EntityId): Boolean {
        val playerEntity = state.getEntity(playerId)
        if (playerEntity?.has<PlayerShroudComponent>() == true) return true

        return state.getBattlefield().any { entityId ->
            val container = state.getEntity(entityId) ?: return@any false
            container.get<GrantsControllerShroudComponent>() != null &&
                container.get<ControllerComponent>()?.playerId == playerId
        }
    }

    private fun playerHasHexproof(state: GameState, playerId: EntityId): Boolean {
        return state.getBattlefield().any { entityId ->
            val container = state.getEntity(entityId) ?: return@any false
            container.get<GrantsControllerHexproofComponent>() != null &&
                container.get<ControllerComponent>()?.playerId == playerId
        }
    }

    private fun playerHasHexproofAgainst(state: GameState, playerId: EntityId, controllerId: EntityId): Boolean {
        return playerId != controllerId && playerHasHexproof(state, playerId)
    }

    private fun getTargetZone(requirement: TargetRequirement): String? {
        return when (requirement) {
            is TargetObject -> requirement.filter.zone.takeIf { it != Zone.BATTLEFIELD }?.name?.let {
                // Use the serialization name to match client Zone enum
                when (requirement.filter.zone) {
                    Zone.GRAVEYARD -> "Graveyard"
                    Zone.STACK -> "Stack"
                    Zone.EXILE -> "Exile"
                    Zone.HAND -> "Hand"
                    Zone.LIBRARY -> "Library"
                    Zone.COMMAND -> "Command"
                    else -> null
                }
            }
            else -> null
        }
    }

    private fun checkActivationRestriction(
        state: GameState,
        playerId: EntityId,
        restriction: ActivationRestriction,
        sourceId: EntityId? = null,
        abilityId: com.wingedsheep.sdk.scripting.AbilityId? = null
    ): Boolean {
        return when (restriction) {
            is ActivationRestriction.AnyPlayerMay -> true // Not a restriction; handled in ability loop
            is ActivationRestriction.OnlyDuringYourTurn -> state.activePlayerId == playerId
            is ActivationRestriction.BeforeStep -> state.step.ordinal < restriction.step.ordinal
            is ActivationRestriction.DuringPhase -> state.phase == restriction.phase
            is ActivationRestriction.DuringStep -> state.step == restriction.step
            is ActivationRestriction.OnlyIfCondition -> {
                val opponentId = state.turnOrder.firstOrNull { it != playerId }
                val context = EffectContext(
                    sourceId = null,
                    controllerId = playerId,
                    opponentId = opponentId,
                    targets = emptyList(),
                    xValue = 0
                )
                conditionEvaluator.evaluate(state, restriction.condition, context)
            }
            is ActivationRestriction.OncePerTurn -> {
                if (sourceId == null || abilityId == null) true
                else {
                    val tracker = state.getEntity(sourceId)?.get<com.wingedsheep.engine.state.components.battlefield.AbilityActivatedThisTurnComponent>()
                    tracker == null || !tracker.hasActivated(abilityId)
                }
            }
            is ActivationRestriction.All -> restriction.restrictions.all {
                checkActivationRestriction(state, playerId, it, sourceId, abilityId)
            }
        }
    }

    private fun hasPlayFromTopOfLibrary(state: GameState, playerId: EntityId): Boolean {
        for (entityId in state.getBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            if (cardDef.script.staticAbilities.any { it is PlayFromTopOfLibrary }) {
                return true
            }
        }
        return false
    }

    /**
     * Get the filter for CastSpellTypesFromTopOfLibrary if the player controls
     * a permanent with that ability. Returns null if no such ability exists.
     * If multiple exist, returns the most permissive (Any).
     */
    private fun getCastFromTopOfLibraryFilter(state: GameState, playerId: EntityId): GameObjectFilter? {
        var filter: GameObjectFilter? = null
        for (entityId in state.getBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (ability in cardDef.script.staticAbilities) {
                if (ability is CastSpellTypesFromTopOfLibrary) {
                    if (ability.filter == GameObjectFilter.Any) return GameObjectFilter.Any
                    filter = ability.filter
                }
            }
        }
        return filter
    }

    /**
     * Check if a spell card has been granted flash by a GrantFlashToSpellType static ability
     * on any permanent on the battlefield (any player's battlefield), or by its own
     * conditionalFlash condition.
     */
    private fun hasGrantedFlash(state: GameState, spellCardId: EntityId): Boolean {
        val spellOwner = state.getEntity(spellCardId)?.get<ControllerComponent>()?.playerId
            ?: return false

        // Check the card's own conditionalFlash (e.g., Ferocious)
        val spellCard = state.getEntity(spellCardId)?.get<CardComponent>()
        val spellDef = spellCard?.let { cardRegistry.getCard(it.cardDefinitionId) }
        val conditionalFlash = spellDef?.script?.conditionalFlash
        if (conditionalFlash != null) {
            val opponentId = state.turnOrder.firstOrNull { it != spellOwner }
            val effectContext = EffectContext(
                sourceId = spellCardId,
                controllerId = spellOwner,
                opponentId = opponentId
            )
            if (conditionEvaluator.evaluate(state, conditionalFlash, effectContext)) {
                return true
            }
        }

        // Check GrantFlashToSpellType static abilities on battlefield permanents
        val context = PredicateContext(controllerId = spellOwner)
        for (playerId in state.turnOrder) {
            for (entityId in state.getBattlefield(playerId)) {
                val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
                val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
                for (ability in cardDef.script.staticAbilities) {
                    if (ability is GrantFlashToSpellType) {
                        // If controllerOnly, only the permanent's controller benefits
                        if (ability.controllerOnly && playerId != spellOwner) continue
                        if (predicateEvaluator.matches(state, spellCardId, ability.filter, context)) {
                            return true
                        }
                    }
                }
            }
        }
        return false
    }

    private fun checkCastRestrictions(
        state: GameState,
        playerId: EntityId,
        restrictions: List<CastRestriction>
    ): Boolean {
        if (restrictions.isEmpty()) return true

        // Create an EffectContext for condition evaluation
        val opponentId = state.turnOrder.firstOrNull { it != playerId }
        val context = EffectContext(
            sourceId = null,
            controllerId = playerId,
            opponentId = opponentId,
            targets = emptyList(),
            xValue = 0
        )

        for (restriction in restrictions) {
            val satisfied = when (restriction) {
                is CastRestriction.OnlyDuringStep -> state.step == restriction.step
                is CastRestriction.OnlyDuringPhase -> state.phase == restriction.phase
                is CastRestriction.OnlyIfCondition -> conditionEvaluator.evaluate(state, restriction.condition, context)
                is CastRestriction.TimingRequirement -> {
                    // TimingRequirement is handled separately in the main timing check
                    true
                }
                is CastRestriction.All -> restriction.restrictions.all { subRestriction ->
                    checkCastRestrictions(state, playerId, listOf(subRestriction))
                }
            }
            if (!satisfied) return false
        }
        return true
    }

    private fun findSacrificeTargets(
        state: GameState,
        playerId: EntityId,
        cost: com.wingedsheep.sdk.scripting.AdditionalCost.SacrificePermanent
    ): List<EntityId> {
        val playerBattlefield = ZoneKey(playerId, Zone.BATTLEFIELD)
        val predicateContext = PredicateContext(controllerId = playerId)

        return state.getZone(playerBattlefield).filter { entityId ->
            val container = state.getEntity(entityId) ?: return@filter false
            container.get<CardComponent>() ?: return@filter false
            val controllerId = container.get<ControllerComponent>()?.playerId
            if (controllerId != playerId) return@filter false

            predicateEvaluator.matches(state, entityId, cost.filter, predicateContext)
        }
    }

    private fun findVariableSacrificeTargets(
        state: GameState,
        playerId: EntityId,
        filter: com.wingedsheep.sdk.scripting.GameObjectFilter
    ): List<EntityId> {
        val playerBattlefield = ZoneKey(playerId, Zone.BATTLEFIELD)
        val predicateContext = PredicateContext(controllerId = playerId)

        return state.getZone(playerBattlefield).filter { entityId ->
            val container = state.getEntity(entityId) ?: return@filter false
            container.get<CardComponent>() ?: return@filter false
            val controllerId = container.get<ControllerComponent>()?.playerId
            if (controllerId != playerId) return@filter false

            predicateEvaluator.matches(state, entityId, filter, predicateContext)
        }
    }

    private fun findExileTargets(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter,
        fromZone: com.wingedsheep.sdk.scripting.CostZone
    ): List<EntityId> {
        val zone = when (fromZone) {
            com.wingedsheep.sdk.scripting.CostZone.GRAVEYARD -> Zone.GRAVEYARD
            com.wingedsheep.sdk.scripting.CostZone.HAND -> Zone.HAND
            com.wingedsheep.sdk.scripting.CostZone.LIBRARY -> Zone.LIBRARY
            com.wingedsheep.sdk.scripting.CostZone.BATTLEFIELD -> Zone.BATTLEFIELD
        }
        val zoneKey = ZoneKey(playerId, zone)
        val predicateContext = PredicateContext(controllerId = playerId)

        return state.getZone(zoneKey).filter { entityId ->
            predicateEvaluator.matches(state, entityId, filter, predicateContext)
        }
    }

    /**
     * Find valid cards in hand that can be discarded to pay a morph cost.
     * Hand cards use base state (not projected) per convention for non-battlefield zones.
     */
    private fun findMorphDiscardTargets(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter
    ): List<EntityId> {
        val handZone = ZoneKey(playerId, Zone.HAND)
        val hand = state.getZone(handZone)
        val predicateContext = PredicateContext(controllerId = playerId)

        return hand.filter { cardId ->
            predicateEvaluator.matches(state, cardId, filter, predicateContext)
        }
    }

    /**
     * Find valid cards in hand that can be revealed to pay a morph cost.
     * Hand cards use base state (not projected) per convention for non-battlefield zones.
     */
    private fun findMorphRevealTargets(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter
    ): List<EntityId> {
        val handZone = ZoneKey(playerId, Zone.HAND)
        val hand = state.getZone(handZone)
        val predicateContext = PredicateContext(controllerId = playerId)

        return hand.filter { cardId ->
            predicateEvaluator.matches(state, cardId, filter, predicateContext)
        }
    }

    /**
     * Find permanents that can be sacrificed for a morph cost.
     * Uses projected state to account for type-changing effects.
     */
    private fun findMorphSacrificeTargets(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter,
        excludeEntityId: EntityId
    ): List<EntityId> {
        val projected = state.projectedState
        val playerBattlefield = ZoneKey(playerId, Zone.BATTLEFIELD)
        val predicateContext = PredicateContext(controllerId = playerId)

        return state.getZone(playerBattlefield).filter { entityId ->
            if (entityId == excludeEntityId) return@filter false
            val container = state.getEntity(entityId) ?: return@filter false
            container.get<CardComponent>() ?: return@filter false
            val controllerId = container.get<ControllerComponent>()?.playerId
            if (controllerId != playerId) return@filter false

            predicateEvaluator.matchesWithProjection(state, projected, entityId, filter, predicateContext)
        }
    }

    /**
     * Find valid cards in a zone that can be exiled to pay a morph cost.
     * Non-battlefield zones use base state per convention.
     */
    private fun findMorphExileTargets(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter,
        zone: Zone
    ): List<EntityId> {
        val zoneKey = ZoneKey(playerId, zone)
        val cards = state.getZone(zoneKey)
        val predicateContext = PredicateContext(controllerId = playerId)

        return cards.filter { cardId ->
            predicateEvaluator.matches(state, cardId, filter, predicateContext)
        }
    }

    /**
     * Find permanents that can be returned to hand for a morph cost.
     * Uses projected state to account for type-changing effects.
     */
    private fun findReturnToHandTargets(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter,
        excludeEntityId: EntityId
    ): List<EntityId> {
        val projected = state.projectedState
        val playerBattlefield = ZoneKey(playerId, Zone.BATTLEFIELD)
        val predicateContext = PredicateContext(controllerId = playerId)

        return state.getZone(playerBattlefield).filter { entityId ->
            if (entityId == excludeEntityId) return@filter false
            val container = state.getEntity(entityId) ?: return@filter false
            container.get<CardComponent>() ?: return@filter false
            val controllerId = container.get<ControllerComponent>()?.playerId
            if (controllerId != playerId) return@filter false

            predicateEvaluator.matchesWithProjection(state, projected, entityId, filter, predicateContext)
        }
    }

    /**
     * Uses projected state to account for type-changing effects.
     */
    private fun findAbilitySacrificeTargets(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter,
        excludeEntityId: EntityId? = null
    ): List<EntityId> {
        val playerBattlefield = ZoneKey(playerId, Zone.BATTLEFIELD)
        val predicateContext = PredicateContext(controllerId = playerId)
        val projected = state.projectedState

        return state.getZone(playerBattlefield).filter { entityId ->
            if (entityId == excludeEntityId) return@filter false
            val container = state.getEntity(entityId) ?: return@filter false
            container.get<CardComponent>() ?: return@filter false
            val controllerId = container.get<ControllerComponent>()?.playerId
            if (controllerId != playerId) return@filter false

            predicateEvaluator.matchesWithProjection(state, projected, entityId, filter, predicateContext)
        }
    }

    /**
     * Uses projected state to account for type-changing effects.
     */
    private fun findAbilityTapTargets(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter
    ): List<EntityId> {
        val playerBattlefield = ZoneKey(playerId, Zone.BATTLEFIELD)
        val predicateContext = PredicateContext(controllerId = playerId)
        val projected = state.projectedState

        return state.getZone(playerBattlefield).filter { entityId ->
            val container = state.getEntity(entityId) ?: return@filter false
            val cardComponent = container.get<CardComponent>() ?: return@filter false
            val controllerId = container.get<ControllerComponent>()?.playerId
            if (controllerId != playerId) return@filter false

            // Must be untapped
            if (container.has<TappedComponent>()) return@filter false

            // Note: summoning sickness does NOT prevent creatures from being tapped by
            // another permanent's TapPermanents cost (it only restricts attacking and
            // activating the creature's own {T} abilities)

            predicateEvaluator.matchesWithProjection(state, projected, entityId, filter, predicateContext)
        }
    }

    private fun findAbilityBounceTargets(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter
    ): List<EntityId> {
        val playerBattlefield = ZoneKey(playerId, Zone.BATTLEFIELD)
        val predicateContext = PredicateContext(controllerId = playerId)
        val projected = state.projectedState

        return state.getZone(playerBattlefield).filter { entityId ->
            val container = state.getEntity(entityId) ?: return@filter false
            container.get<CardComponent>() ?: return@filter false
            val controllerId = container.get<ControllerComponent>()?.playerId
            if (controllerId != playerId) return@filter false

            predicateEvaluator.matchesWithProjection(state, projected, entityId, filter, predicateContext)
        }
    }

    private fun findConvokeCreatures(state: GameState, playerId: EntityId): List<ConvokeCreatureInfo> {
        val playerBattlefield = ZoneKey(playerId, Zone.BATTLEFIELD)
        return state.getZone(playerBattlefield).mapNotNull { entityId ->
            val container = state.getEntity(entityId) ?: return@mapNotNull null
            val cardComponent = container.get<CardComponent>() ?: return@mapNotNull null

            // Must be a creature
            if (!cardComponent.typeLine.isCreature) return@mapNotNull null

            // Must be controlled by the player
            val controllerId = container.get<ControllerComponent>()?.playerId
            if (controllerId != playerId) return@mapNotNull null

            // Must be untapped
            if (container.has<TappedComponent>()) return@mapNotNull null

            ConvokeCreatureInfo(
                entityId = entityId,
                name = cardComponent.name,
                colors = cardComponent.colors
            )
        }
    }

    private fun canAffordWithConvoke(
        state: GameState,
        playerId: EntityId,
        manaCost: com.wingedsheep.sdk.core.ManaCost,
        convokeCreatures: List<ConvokeCreatureInfo>
    ): Boolean {
        // Get available mana from all sources
        val availableMana = manaSolver.getAvailableManaCount(state, playerId)

        // Total resources = mana + convoke creatures
        val totalResources = availableMana + convokeCreatures.size

        // Simple check: can we cover the total CMC?
        // This is a conservative estimate - colored requirements might still fail,
        // but it allows us to highlight the card as potentially castable.
        if (totalResources < manaCost.cmc) {
            return false
        }

        // More precise check: for each colored mana symbol, we need either:
        // - A mana source that can produce that color, OR
        // - A creature of that color to convoke
        // For simplicity, we'll use a greedy approach

        // Count colored requirements using the colorCount property
        val coloredRequirements = manaCost.colorCount

        // Count creatures by color for convoke
        val creatureColors = mutableMapOf<com.wingedsheep.sdk.core.Color, Int>()
        for (creature in convokeCreatures) {
            for (color in creature.colors) {
                creatureColors[color] = (creatureColors[color] ?: 0) + 1
            }
        }

        // Check if we can cover colored requirements with creatures
        // (mana sources can produce any color, so they're always valid)
        var creaturesUsedForColors = 0
        for ((color, needed) in coloredRequirements) {
            val creaturesOfColor = creatureColors[color] ?: 0
            // We can use creatures of this color, but need to track how many we use
            creaturesUsedForColors += minOf(needed, creaturesOfColor)
        }

        // Calculate generic mana requirement
        val genericRequired = manaCost.genericAmount

        // Creatures not used for colors can pay generic
        val creaturesForGeneric = convokeCreatures.size - creaturesUsedForColors

        // Total resources for generic = mana sources + unused creatures
        val resourcesForGeneric = availableMana + creaturesForGeneric

        // We can afford if we have enough for generic (colored is covered by creatures or mana)
        return resourcesForGeneric >= genericRequired
    }

    private fun findDelveCards(state: GameState, playerId: EntityId): List<DelveCardInfo> {
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
        return state.getZone(graveyardZone).mapNotNull { entityId ->
            val container = state.getEntity(entityId) ?: return@mapNotNull null
            val cardComponent = container.get<CardComponent>() ?: return@mapNotNull null
            val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)
            DelveCardInfo(
                entityId = entityId,
                name = cardComponent.name,
                imageUri = cardDef?.metadata?.imageUri
            )
        }
    }

    private fun canAffordWithDelve(
        state: GameState,
        playerId: EntityId,
        manaCost: com.wingedsheep.sdk.core.ManaCost,
        delveCards: List<DelveCardInfo>
    ): Boolean {
        // Delve only pays generic mana — reduce generic portion, then check if mana sources cover the rest
        val maxDelve = minOf(delveCards.size, manaCost.genericAmount)
        val reducedCost = manaCost.reduceGeneric(maxDelve)
        return manaSolver.canPay(state, playerId, reducedCost)
    }

    /**
     * Calculate the minimum number of cards that must be exiled via Delve to afford the spell.
     * Returns the number of cards needed (0 if castable without delve, up to maxDelve).
     */
    private fun calculateMinDelveNeeded(
        state: GameState,
        playerId: EntityId,
        manaCost: com.wingedsheep.sdk.core.ManaCost,
        delveCards: List<DelveCardInfo>
    ): Int {
        // Check if castable without any delve
        if (manaSolver.canPay(state, playerId, manaCost)) return 0
        // Try increasing delve counts until the reduced cost is payable
        val maxDelve = minOf(delveCards.size, manaCost.genericAmount)
        for (delveCount in 1..maxDelve) {
            if (manaSolver.canPay(state, playerId, manaCost.reduceGeneric(delveCount))) {
                return delveCount
            }
        }
        return maxDelve
    }

    /**
     * Get activated abilities granted to an entity by static abilities on battlefield permanents.
     * E.g., Spectral Sliver grants "{2}: This creature gets +1/+1 until end of turn" to all Slivers
     * via GrantActivatedAbilityToCreatureGroup.
     */
    private fun getStaticGrantedActivatedAbilities(
        entityId: EntityId,
        state: GameState
    ): List<ActivatedAbility> {
        val targetContainer = state.getEntity(entityId) ?: return emptyList()
        val targetCard = targetContainer.get<CardComponent>() ?: return emptyList()

        val result = mutableListOf<ActivatedAbility>()

        for (permanentId in state.getBattlefield()) {
            val container = state.getEntity(permanentId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            if (container.has<FaceDownComponent>()) continue

            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (ability in cardDef.staticAbilities) {
                when (ability) {
                    is GrantActivatedAbilityToCreatureGroup -> {
                        val filter = ability.filter.baseFilter
                        val matchesAll = filter.cardPredicates.all { predicate ->
                            when (predicate) {
                                is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsCreature ->
                                    targetCard.typeLine.isCreature
                                is com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasSubtype ->
                                    targetCard.typeLine.hasSubtype(predicate.subtype)
                                else -> true
                            }
                        }
                        if (matchesAll) {
                            result.add(ability.ability)
                        }
                    }
                    is GrantActivatedAbilityToAttachedCreature -> {
                        val attachedTo = container.get<AttachedToComponent>()
                        if (attachedTo != null && attachedTo.targetId == entityId) {
                            result.add(ability.ability)
                        }
                    }
                    else -> {}
                }
            }
        }

        return result
    }

    private fun isCyclingPrevented(state: GameState): Boolean {
        for (entityId in state.getBattlefield()) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            if (cardDef.script.staticAbilities.any { it is PreventCycling }) {
                return true
            }
        }
        return false
    }

    /**
     * Build ManaSourceInfo list for all untapped mana sources a player controls.
     * Returns null if the player has no available sources (avoids sending empty arrays).
     */
    private fun buildManaSourceInfos(state: GameState, playerId: EntityId): List<ServerMessage.ManaSourceInfo>? {
        val sources = manaSolver.findAvailableManaSources(state, playerId)
        if (sources.isEmpty()) return null
        return sources.map { source ->
            val card = state.getEntity(source.entityId)?.get<CardComponent>()
            val imageUri = card?.let { cardRegistry.getCard(it.cardDefinitionId)?.metadata?.imageUri }
            ServerMessage.ManaSourceInfo(
                entityId = source.entityId,
                name = source.name,
                imageUri = imageUri,
                producesColors = source.producesColors.map { it.symbol.toString() },
                producesColorless = source.producesColorless,
                manaAmount = source.manaAmount
            )
        }
    }

    /**
     * Returns the maximum number of loyalty ability activations per planeswalker per turn
     * for the given player. Normally 1, but ExtraLoyaltyActivation (Oath of Teferi) raises it to 2.
     */
    private fun getMaxLoyaltyActivations(state: GameState, playerId: EntityId): Int {
        for (permanentId in state.getBattlefield()) {
            val container = state.getEntity(permanentId) ?: continue
            val controller = container.get<ControllerComponent>()?.playerId ?: continue
            if (controller != playerId) continue
            val card = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            if (cardDef.script.staticAbilities.any { it is ExtraLoyaltyActivation }) {
                return 2
            }
        }
        return 1
    }

    /**
     * Check if the player has a Muldrotha-like permanent with unused graveyard play permission
     * for the given permanent type name (e.g., "CREATURE", "ARTIFACT", "LAND").
     */
    private fun hasGraveyardPlayPermissionForType(
        state: GameState,
        playerId: EntityId,
        typeName: String
    ): Boolean {
        for (entityId in state.getBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            if (cardDef.script.staticAbilities.any { it is MayPlayPermanentsFromGraveyard }) {
                val tracker = state.getEntity(entityId)?.get<GraveyardPlayPermissionUsedComponent>()
                if (tracker == null || !tracker.hasUsedType(typeName)) {
                    return true
                }
            }
        }
        return false
    }
}
