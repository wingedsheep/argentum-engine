package com.wingedsheep.gameserver.legalactions

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.mechanics.text.SubtypeReplacer
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.GrantsControllerShroudComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.AttackersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ChosenCreatureTypeComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.protocol.AdditionalCostInfo
import com.wingedsheep.gameserver.protocol.ConvokeCreatureInfo
import com.wingedsheep.gameserver.protocol.LegalActionInfo
import com.wingedsheep.gameserver.protocol.LegalActionTargetInfo
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.CastRestriction
import com.wingedsheep.sdk.scripting.ControllerPredicate
import com.wingedsheep.sdk.scripting.DividedDamageEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayFromTopOfLibrary
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.targeting.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(LegalActionsCalculator::class.java)

class LegalActionsCalculator(
    private val cardRegistry: CardRegistry,
    private val stateProjector: StateProjector,
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
                return listOf(LegalActionInfo(
                    actionType = "DeclareAttackers",
                    description = "Declare attackers",
                    action = DeclareAttackers(playerId, emptyMap()),
                    validAttackers = validAttackers
                ))
            }
        }
        if (state.step == Step.DECLARE_BLOCKERS && state.activePlayerId != playerId) {
            val blockersAlreadyDeclared = state.getEntity(playerId)
                ?.get<BlockersDeclaredThisCombatComponent>() != null
            if (!blockersAlreadyDeclared) {
                val validBlockers = turnManager.getValidBlockers(state, playerId)
                return listOf(LegalActionInfo(
                    actionType = "DeclareBlockers",
                    description = "Declare blockers",
                    action = DeclareBlockers(playerId, emptyMap()),
                    validBlockers = validBlockers
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
        }

        // Check for castable spells (non-instant only at sorcery speed)
        val canPlaySorcerySpeed = state.step.isMainPhase &&
            state.stack.isEmpty() &&
            state.activePlayerId == playerId

        val hand = state.getHand(playerId)

        // Check for morph cards that can be cast face-down (sorcery speed only)
        if (canPlaySorcerySpeed) {
            val morphCost = costCalculator.calculateFaceDownCost(state, playerId)
            val canAffordMorph = manaSolver.canPay(state, playerId, morphCost)
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
                        manaCostString = morphCost.toString()
                    ))

                    // Check if we can afford to cast normally - if not, add unaffordable cast action
                    // This ensures the player sees both options in the cast modal
                    val canAffordNormal = manaSolver.canPay(state, playerId, cardComponent.manaCost)
                    if (!canAffordNormal) {
                        result.add(LegalActionInfo(
                            actionType = "CastSpell",
                            description = "Cast ${cardComponent.name}",
                            action = CastSpell(playerId, cardId),
                            isAffordable = false,
                            manaCostString = cardComponent.manaCost.toString()
                        ))
                    }
                }
            }
        }

        for (cardId in hand) {
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
            if (!cardComponent.typeLine.isLand) {
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
                if (isInstant || canPlaySorcerySpeed) {
                    // Check additional cost payability
                    val additionalCosts = cardDef?.script?.additionalCosts ?: emptyList()
                    val sacrificeTargets = mutableListOf<EntityId>()
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
                            else -> {}
                        }
                    }
                    if (!canPayAdditionalCosts) continue

                    // Check mana affordability (including Convoke if available)
                    val hasConvoke = cardDef?.keywords?.contains(Keyword.CONVOKE) == true
                    val convokeCreatures = if (hasConvoke) {
                        findConvokeCreatures(state, playerId)
                    } else null

                    // For Convoke spells, check if affordable with creature help
                    val canAfford = if (hasConvoke && convokeCreatures != null && convokeCreatures.isNotEmpty()) {
                        // Can afford if: mana alone is enough, OR mana + convoke creatures cover the cost
                        manaSolver.canPay(state, playerId, cardComponent.manaCost) ||
                            canAffordWithConvoke(state, playerId, cardComponent.manaCost, convokeCreatures)
                    } else {
                        manaSolver.canPay(state, playerId, cardComponent.manaCost)
                    }

                    if (canAfford) {
                        val targetReqs = buildList {
                            addAll(cardDef?.script?.targetRequirements ?: emptyList())
                            cardDef?.script?.auraTarget?.let { add(it) }
                        }

                        logger.debug("Card '${cardComponent.name}': cardDef=${cardDef != null}, targetReqs=${targetReqs.size}")

                        // Build additional cost info for the client
                        val costInfo = if (sacrificeTargets.isNotEmpty()) {
                            val sacCost = additionalCosts.filterIsInstance<com.wingedsheep.sdk.scripting.AdditionalCost.SacrificePermanent>().firstOrNull()
                            AdditionalCostInfo(
                                description = sacCost?.description ?: "Sacrifice a creature",
                                costType = "SacrificePermanent",
                                validSacrificeTargets = sacrificeTargets,
                                sacrificeCount = sacCost?.count ?: 1
                            )
                        } else null

                        // Calculate X cost info if the spell has X in its cost
                        val hasXCost = cardComponent.manaCost.hasX
                        val maxAffordableX: Int? = if (hasXCost) {
                            val availableSources = manaSolver.getAvailableManaCount(state, playerId)
                            val fixedCost = cardComponent.manaCost.cmc  // X contributes 0 to CMC
                            (availableSources - fixedCost).coerceAtLeast(0)
                        } else null

                        // Always include mana cost string for cast actions
                        val manaCostString = cardComponent.manaCost.toString()

                        // Compute auto-tap preview for UI highlighting
                        val autoTapSolution = manaSolver.solve(state, playerId, cardComponent.manaCost)
                        val autoTapPreview = autoTapSolution?.sources?.map { it.entityId }

                        // Check for DividedDamageEffect to flag damage distribution requirement
                        val spellEffect = cardDef?.script?.spellEffect
                        val dividedDamageEffect = spellEffect as? DividedDamageEffect
                        val requiresDamageDistribution = dividedDamageEffect != null
                        val totalDamageToDistribute = dividedDamageEffect?.totalDamage
                        val minDamagePerTarget = if (dividedDamageEffect != null) 1 else null

                        if (targetReqs.isNotEmpty()) {
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
                                    result.add(LegalActionInfo(
                                        actionType = "CastSpell",
                                        description = "Cast ${cardComponent.name}",
                                        action = CastSpell(playerId, cardId, targets = listOf(autoSelectedTarget)),
                                        hasXCost = hasXCost,
                                        maxAffordableX = maxAffordableX,
                                        additionalCostInfo = costInfo,
                                        hasConvoke = hasConvoke,
                                        validConvokeCreatures = convokeCreatures,
                                        manaCostString = manaCostString,
                                        requiresDamageDistribution = requiresDamageDistribution,
                                        totalDamageToDistribute = totalDamageToDistribute,
                                        minDamagePerTarget = minDamagePerTarget,
                                        autoTapPreview = autoTapPreview
                                    ))
                                } else {
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
                                        manaCostString = manaCostString,
                                        requiresDamageDistribution = requiresDamageDistribution,
                                        totalDamageToDistribute = totalDamageToDistribute,
                                        minDamagePerTarget = minDamagePerTarget,
                                        autoTapPreview = autoTapPreview
                                    ))
                                }
                            }
                        } else {
                            // No targets required
                            result.add(LegalActionInfo(
                                actionType = "CastSpell",
                                description = "Cast ${cardComponent.name}",
                                action = CastSpell(playerId, cardId),
                                hasXCost = hasXCost,
                                maxAffordableX = maxAffordableX,
                                additionalCostInfo = costInfo,
                                hasConvoke = hasConvoke,
                                validConvokeCreatures = convokeCreatures,
                                manaCostString = manaCostString,
                                autoTapPreview = autoTapPreview
                            ))
                        }
                    }
                }
            }
        }

        // Check for cycling abilities on cards in hand
        for (cardId in hand) {
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(cardComponent.name)
            if (cardDef == null) {
                continue
            }

            // Check for cycling ability - log at info level to ensure visibility
            val allAbilities = cardDef.keywordAbilities
            val cyclingAbility = allAbilities
                .filterIsInstance<com.wingedsheep.sdk.scripting.KeywordAbility.Cycling>()
                .firstOrNull()
            if (cyclingAbility == null) {
                continue
            }

            // Add cycling action (affordable or not) - client shows greyed out if unaffordable
            val canAffordCycling = manaSolver.canPay(state, playerId, cyclingAbility.cost)
            result.add(LegalActionInfo(
                actionType = "CycleCard",
                description = "Cycle ${cardComponent.name}",
                action = CycleCard(playerId, cardId),
                isAffordable = canAffordCycling,
                manaCostString = cyclingAbility.cost.toString()
            ))

            // For cards with cycling, also add the normal cast option (matching morph pattern)
            // This ensures the player sees both options in the cast modal
            if (!cardComponent.typeLine.isLand) {
                val isInstant = cardComponent.typeLine.isInstant
                val canCastTiming = isInstant || canPlaySorcerySpeed
                if (canCastTiming) {
                    // Check if we can afford to cast normally
                    val canAffordNormal = manaSolver.canPay(state, playerId, cardComponent.manaCost)
                    // Check if a cast action was already added (affordable, with proper targeting)
                    val hasCastAction = result.any { it.action is CastSpell && (it.action as CastSpell).cardId == cardId }
                    if (!hasCastAction) {
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
                                manaCostString = cardComponent.manaCost.toString()
                            ))
                        } else {
                            // Spell is unaffordable or has no valid targets — show greyed out
                            result.add(LegalActionInfo(
                                actionType = "CastSpell",
                                description = "Cast ${cardComponent.name}",
                                action = CastSpell(playerId, cardId),
                                isAffordable = false,
                                manaCostString = cardComponent.manaCost.toString()
                            ))
                        }
                    }
                }
            }
        }

        // Check for top-of-library cards playable via PlayFromTopOfLibrary (e.g., Future Sight)
        if (hasPlayFromTopOfLibrary(state, playerId)) {
            val library = state.getLibrary(playerId)
            if (library.isNotEmpty()) {
                val topCardId = library.first()
                val topCardComponent = state.getEntity(topCardId)?.get<CardComponent>()
                if (topCardComponent != null) {
                    val topCardDef = cardRegistry.getCard(topCardComponent.name)

                    // Land on top of library
                    if (topCardComponent.typeLine.isLand && canPlayLand) {
                        result.add(LegalActionInfo(
                            actionType = "PlayLand",
                            description = "Play ${topCardComponent.name}",
                            action = PlayLand(playerId, topCardId),
                            sourceZone = "LIBRARY"
                        ))
                    }

                    // Non-land spell on top of library
                    if (!topCardComponent.typeLine.isLand) {
                        // Check timing
                        val isInstant = topCardComponent.typeLine.isInstant
                        if (isInstant || canPlaySorcerySpeed) {
                            // Check cast restrictions
                            val castRestrictions = topCardDef?.script?.castRestrictions ?: emptyList()
                            if (checkCastRestrictions(state, playerId, castRestrictions)) {
                                val canAfford = manaSolver.canPay(state, playerId, topCardComponent.manaCost)
                                if (canAfford) {
                                    val targetReqs = buildList {
                                        addAll(topCardDef?.script?.targetRequirements ?: emptyList())
                                        topCardDef?.script?.auraTarget?.let { add(it) }
                                    }

                                    val manaCostString = topCardComponent.manaCost.toString()
                                    val hasXCost = topCardComponent.manaCost.hasX
                                    val maxAffordableX: Int? = if (hasXCost) {
                                        val availableSources = manaSolver.getAvailableManaCount(state, playerId)
                                        val fixedCost = topCardComponent.manaCost.cmc
                                        (availableSources - fixedCost).coerceAtLeast(0)
                                    } else null
                                    val autoTapSolution = manaSolver.solve(state, playerId, topCardComponent.manaCost)
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

                        // Check for morph on top of library
                        if (canPlaySorcerySpeed && topCardDef != null) {
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

        // Check for mana abilities on battlefield permanents
        // Use projected state to find all permanents controlled by this player
        // (accounts for control-changing effects like Annex)
        val projectedState = stateProjector.project(state)
        val battlefieldPermanents = projectedState.getBattlefieldControlledBy(playerId)
        for (entityId in battlefieldPermanents) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Face-down creatures have no abilities (Rule 707.2)
            if (container.has<FaceDownComponent>()) continue

            // Projected controller already verified - look up card definition for mana abilities
            val cardDef = cardRegistry.getCard(cardComponent.name) ?: continue
            // Include granted activated abilities that are mana abilities
            val grantedManaAbilities = state.grantedActivatedAbilities
                .filter { it.entityId == entityId }
                .map { it.ability }
                .filter { it.isManaAbility }
            val manaAbilities = cardDef.script.activatedAbilities.filter { it.isManaAbility } + grantedManaAbilities

            // Apply text-changing effects to mana ability costs
            val manaTextReplacement = container.get<TextReplacementComponent>()

            for (ability in manaAbilities) {
                // Apply text replacement to cost filters
                val effectiveCost = if (manaTextReplacement != null) {
                    SubtypeReplacer.replaceAbilityCost(ability.cost, manaTextReplacement)
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
                        sacrificeTargets = findAbilitySacrificeTargets(state, playerId, sacrificeCost.filter)
                        if (sacrificeTargets.isEmpty()) continue
                    }
                    is AbilityCost.SacrificeChosenCreatureType -> {
                        val chosenType = container.get<ChosenCreatureTypeComponent>()?.creatureType
                        if (chosenType == null) continue
                        val dynamicFilter = GameObjectFilter.Creature.withSubtype(chosenType)
                        sacrificeCost = AbilityCost.Sacrifice(dynamicFilter)
                        sacrificeTargets = findAbilitySacrificeTargets(state, playerId, dynamicFilter)
                        if (sacrificeTargets.isEmpty()) continue
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
                        sacrificeCount = 1
                    )
                } else null

                result.add(LegalActionInfo(
                    actionType = "ActivateAbility",
                    description = ability.description,
                    action = ActivateAbility(playerId, entityId, ability.id),
                    isManaAbility = true,
                    additionalCostInfo = costInfo,
                    requiresManaColorChoice = ability.effect is com.wingedsheep.sdk.scripting.AddAnyColorManaEffect
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

            // Check if player can afford the morph cost
            if (manaSolver.canPay(state, playerId, morphData.morphCost)) {
                val autoTapSolution = manaSolver.solve(state, playerId, morphData.morphCost)
                val autoTapPreview = autoTapSolution?.sources?.map { it.entityId }
                result.add(LegalActionInfo(
                    actionType = "ActivateAbility",
                    description = "Turn face-up (${morphData.morphCost})",
                    action = TurnFaceUp(playerId, entityId),
                    manaCostString = morphData.morphCost.toString(),
                    autoTapPreview = autoTapPreview
                ))
            }
        }

        // Check for non-mana activated abilities on battlefield permanents
        for (entityId in battlefieldPermanents) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Face-down creatures have no abilities (Rule 707.2)
            if (container.has<FaceDownComponent>()) continue

            val cardDef = cardRegistry.getCard(cardComponent.name) ?: continue
            // Include granted activated abilities alongside the card's own abilities
            val grantedAbilities = state.grantedActivatedAbilities
                .filter { it.entityId == entityId }
                .map { it.ability }
            val nonManaAbilities = cardDef.script.activatedAbilities.filter { !it.isManaAbility } + grantedAbilities.filter { !it.isManaAbility }

            // Apply text-changing effects to ability costs and targets
            val textReplacement = container.get<TextReplacementComponent>()

            for (ability in nonManaAbilities) {
                // Apply text replacement to cost filters (e.g., "Sacrifice a Goblin" → "Sacrifice a Bird")
                val effectiveCost = if (textReplacement != null) {
                    SubtypeReplacer.replaceAbilityCost(ability.cost, textReplacement)
                } else {
                    ability.cost
                }

                // Check cost requirements and gather sacrifice/tap targets if needed
                var sacrificeTargets: List<EntityId>? = null
                var sacrificeCost: AbilityCost.Sacrifice? = null
                var tapTargets: List<EntityId>? = null
                var tapCost: AbilityCost.TapPermanents? = null

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
                        if (!manaSolver.canPay(state, playerId, effectiveCost.cost)) continue
                    }
                    is AbilityCost.Sacrifice -> {
                        sacrificeCost = effectiveCost
                        sacrificeTargets = findAbilitySacrificeTargets(state, playerId, sacrificeCost.filter)
                        if (sacrificeTargets.isEmpty()) continue
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
                                    if (!manaSolver.canPay(state, playerId, subCost.cost)) {
                                        costCanBePaid = false
                                        break
                                    }
                                }
                                is AbilityCost.Sacrifice -> {
                                    sacrificeCost = subCost
                                    sacrificeTargets = findAbilitySacrificeTargets(state, playerId, subCost.filter)
                                    if (sacrificeTargets.isEmpty()) {
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
                                else -> {}
                            }
                        }
                        if (!costCanBePaid) continue
                    }
                    else -> {}
                }

                // Check activation restrictions
                var restrictionsMet = true
                for (restriction in ability.restrictions) {
                    if (!checkActivationRestriction(state, playerId, restriction)) {
                        restrictionsMet = false
                        break
                    }
                }
                if (!restrictionsMet) continue

                // Build additional cost info for sacrifice or tap costs
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
                        sacrificeCount = 1
                    )
                } else if (sacrificeTargets != null) {
                    // SacrificeSelf cost — sacrifice target is the source itself
                    AdditionalCostInfo(
                        description = "Sacrifice this permanent",
                        costType = "SacrificeSelf",
                        validSacrificeTargets = sacrificeTargets,
                        sacrificeCount = 1
                    )
                } else null

                // Calculate X cost info for activated abilities with X in their mana cost
                val abilityManaCost = when (ability.cost) {
                    is AbilityCost.Mana -> (ability.cost as AbilityCost.Mana).cost
                    is AbilityCost.Composite -> (ability.cost as AbilityCost.Composite).costs
                        .filterIsInstance<AbilityCost.Mana>().firstOrNull()?.cost
                    else -> null
                }
                val abilityHasXCost = abilityManaCost?.hasX == true
                val abilityMaxAffordableX: Int? = if (abilityHasXCost && abilityManaCost != null) {
                    val availableSources = manaSolver.getAvailableManaCount(state, playerId)
                    val fixedCost = abilityManaCost.cmc  // X contributes 0 to CMC
                    (availableSources - fixedCost).coerceAtLeast(0)
                } else null

                // Compute auto-tap preview for UI highlighting
                val abilityAutoTapPreview = if (abilityManaCost != null && !abilityHasXCost) {
                    manaSolver.solve(state, playerId, abilityManaCost)?.sources?.map { it.entityId }
                } else null

                // Check for target requirements (apply text-changing effects to filter)
                val targetReqs = if (textReplacement != null) {
                    ability.targetRequirements.map { SubtypeReplacer.replaceTargetRequirement(it, textReplacement) }
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
                            autoTapPreview = abilityAutoTapPreview
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
                            autoTapPreview = abilityAutoTapPreview
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
                        autoTapPreview = abilityAutoTapPreview
                    ))
                }
            }
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
                    if (!checkActivationRestriction(state, playerId, restriction)) {
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
                                else -> {}
                            }
                        }
                    }
                    else -> {}
                }
                if (!costCanBePaid) continue

                // Build discard cost info if needed
                val costInfo = if (hasDiscardCost) {
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
                val abilityHasXCost = abilityManaCost?.hasX == true
                val abilityMaxAffordableX: Int? = if (abilityHasXCost && abilityManaCost != null) {
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
                            autoTapPreview = abilityAutoTapPreview
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
                            autoTapPreview = abilityAutoTapPreview
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
                        autoTapPreview = abilityAutoTapPreview
                    ))
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
            is TargetCreature -> findValidCreatureTargets(state, playerId, requirement.filter, sourceId)
            is TargetPlayer -> state.turnOrder.filter { state.hasEntity(it) && !playerHasShroud(state, it) }
            is TargetOpponent -> state.turnOrder.filter { it != playerId && state.hasEntity(it) && !playerHasShroud(state, it) }
            is AnyTarget -> {
                // Any target = creatures + planeswalkers + players
                val creatures = findValidCreatureTargets(state, playerId, TargetFilter.Creature, sourceId)
                val players = state.turnOrder.filter { state.hasEntity(it) && !playerHasShroud(state, it) }
                creatures + players
            }
            is TargetCreatureOrPlayer -> {
                val creatures = findValidCreatureTargets(state, playerId, TargetFilter.Creature, sourceId)
                val players = state.turnOrder.filter { state.hasEntity(it) && !playerHasShroud(state, it) }
                creatures + players
            }
            is TargetPermanent -> findValidPermanentTargets(state, playerId, requirement.filter, sourceId)
            is TargetObject -> findValidObjectTargets(state, playerId, requirement.filter, sourceId)
            is TargetSpell -> findValidSpellTargets(state, playerId, requirement.filter)
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

    private fun findValidCreatureTargets(
        state: GameState,
        playerId: EntityId,
        filter: TargetFilter,
        sourceId: EntityId? = null
    ): List<EntityId> {
        val projected = stateProjector.project(state)
        val battlefield = state.getBattlefield()
        val context = PredicateContext(controllerId = playerId, sourceId = sourceId)
        return battlefield.filter { entityId ->
            val entityController = state.getEntity(entityId)?.get<ControllerComponent>()?.playerId

            // Check hexproof - can't be targeted by opponents
            if (projected.hasKeyword(entityId, Keyword.HEXPROOF) && entityController != playerId) {
                return@filter false
            }
            // Check shroud - can't be targeted by anyone
            if (projected.hasKeyword(entityId, Keyword.SHROUD)) {
                return@filter false
            }

            // Use projected state for correct face-down creature handling
            predicateEvaluator.matchesWithProjection(state, projected, entityId, filter.baseFilter, context)
        }
    }

    private fun findValidPermanentTargets(
        state: GameState,
        playerId: EntityId,
        filter: TargetFilter,
        sourceId: EntityId? = null
    ): List<EntityId> {
        val projected = stateProjector.project(state)
        val battlefield = state.getBattlefield()
        val context = PredicateContext(controllerId = playerId, sourceId = sourceId)
        return battlefield.filter { entityId ->
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
        return state.getBattlefield().any { entityId ->
            val container = state.getEntity(entityId) ?: return@any false
            container.get<GrantsControllerShroudComponent>() != null &&
                container.get<ControllerComponent>()?.playerId == playerId
        }
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
        restriction: ActivationRestriction
    ): Boolean {
        return when (restriction) {
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
            is ActivationRestriction.All -> restriction.restrictions.all {
                checkActivationRestriction(state, playerId, it)
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

    private fun findAbilitySacrificeTargets(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter
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

    private fun findAbilityTapTargets(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter
    ): List<EntityId> {
        val playerBattlefield = ZoneKey(playerId, Zone.BATTLEFIELD)
        val predicateContext = PredicateContext(controllerId = playerId)

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

            predicateEvaluator.matches(state, entityId, filter, predicateContext)
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
}
