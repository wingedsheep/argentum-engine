package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.legalactions.TargetInfo
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.DividedDamageEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext

/**
 * Enumerates legal CastSpell actions for spells in hand.
 *
 * Handles all casting complexity: additional costs, alternative costs,
 * self-alternative costs, convoke, delve, X costs, modal spells,
 * targeting, auto-select player targets, and kicker.
 */
class CastSpellEnumerator : ActionEnumerator {

    override fun enumerate(context: EnumerationContext): List<LegalAction> {
        val result = mutableListOf<LegalAction>()
        val state = context.state
        val playerId = context.playerId

        val hand = state.getHand(playerId)

        // --- Normal spell casting ---
        for (cardId in hand) {
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
            if (cardComponent.typeLine.isLand) continue

            // Skip all spells if player can't cast spells this turn
            if (context.cantCastSpells) continue

            // Look up card definition for target requirements and cast restrictions
            val cardDef = context.cardRegistry.getCard(cardComponent.name) ?: continue

            // Check cast restrictions first
            val castRestrictions = cardDef.script.castRestrictions
            if (!context.castPermissionUtils.checkCastRestrictions(state, playerId, castRestrictions)) {
                continue
            }

            // Check timing - sorcery-speed spells need main phase, empty stack, your turn
            val isInstant = cardComponent.typeLine.isInstant
            val hasFlash = cardDef.keywords.contains(Keyword.FLASH)
            val grantedFlash = hasFlash || context.castPermissionUtils.hasGrantedFlash(state, cardId)
            if (!isInstant && !grantedFlash && !context.canPlaySorcerySpeed) continue

            // Check additional cost payability
            val additionalCosts = cardDef.script.additionalCosts
            val sacrificeTargets = mutableListOf<EntityId>()
            var variableSacrificeTargets = emptyList<EntityId>()
            var variableSacrificeReduction = 0
            var exileTargets = emptyList<EntityId>()
            var exileMinCount = 0
            var discardTargets = emptyList<EntityId>()
            var discardCount = 0
            var beholdTargets = emptyList<EntityId>()
            var beholdCount = 0
            var canPayAdditionalCosts = true
            val flattenedCosts = additionalCosts.flatMap {
                if (it is AdditionalCost.Composite) it.steps else listOf(it)
            }
            for (cost in flattenedCosts) {
                when (cost) {
                    is AdditionalCost.SacrificePermanent -> {
                        val validSacTargets = context.costUtils.findSacrificeTargets(state, playerId, cost)
                        if (validSacTargets.size < cost.count) {
                            canPayAdditionalCosts = false
                        }
                        sacrificeTargets.addAll(validSacTargets)
                    }
                    is AdditionalCost.SacrificeCreaturesForCostReduction -> {
                        // Always payable (0 sacrifices is valid)
                        val validSacTargets = context.costUtils.findVariableSacrificeTargets(state, playerId, cost.filter)
                        variableSacrificeTargets = validSacTargets
                        variableSacrificeReduction = cost.costReductionPerCreature
                    }
                    is AdditionalCost.ExileVariableCards -> {
                        val validExileTargets = context.costUtils.findExileTargets(state, playerId, cost.filter, cost.fromZone)
                        if (validExileTargets.size < cost.minCount) {
                            canPayAdditionalCosts = false
                        }
                        exileTargets = validExileTargets
                        exileMinCount = cost.minCount
                    }
                    is AdditionalCost.ExileCards -> {
                        val validExileTargets = context.costUtils.findExileTargets(state, playerId, cost.filter, cost.fromZone)
                        if (validExileTargets.size < cost.count) {
                            canPayAdditionalCosts = false
                        }
                        exileTargets = validExileTargets
                        exileMinCount = cost.count
                    }
                    is AdditionalCost.Behold -> {
                        // Find matching permanents on battlefield (projected) + matching cards in hand
                        val projected = state.projectedState
                        val predicateContext = PredicateContext(controllerId = playerId)
                        val battlefieldMatches = projected.getBattlefieldControlledBy(playerId).filter { permId ->
                            context.predicateEvaluator.matchesWithProjection(state, projected, permId, cost.filter, predicateContext)
                        }
                        val handZone = ZoneKey(playerId, Zone.HAND)
                        val handMatches = state.getZone(handZone)
                            .filter { it != cardId } // Exclude the card being cast
                            .filter { context.predicateEvaluator.matches(state, it, cost.filter, predicateContext) }
                        val allTargets = battlefieldMatches + handMatches
                        if (allTargets.size < cost.count) {
                            canPayAdditionalCosts = false
                        }
                        beholdTargets = allTargets
                        beholdCount = cost.count
                    }
                    is AdditionalCost.ExileFromStorage -> {
                        // Payability determined by the preceding Behold cost
                    }
                    is AdditionalCost.DiscardCards -> {
                        val handZone = ZoneKey(playerId, Zone.HAND)
                        val handCards = state.getZone(handZone)
                            .filter { it != cardId } // Exclude the card being cast
                        val predicateContext = PredicateContext(controllerId = playerId)
                        val validDiscards = if (cost.filter == com.wingedsheep.sdk.scripting.GameObjectFilter.Any) {
                            handCards
                        } else {
                            handCards.filter { context.predicateEvaluator.matches(state, it, cost.filter, predicateContext) }
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

            // Calculate effective cost after reductions (e.g., Goblin Warchief).
            // Uses minimum possible cost so target-conditional reductions (e.g., Dire Downdraft)
            // mark the spell as castable when a valid discounted target exists on the battlefield.
            var effectiveCost = context.costCalculator.calculateMinPossibleCost(state, cardDef, playerId)

            // Apply maximum possible sacrifice cost reduction for affordability check
            if (variableSacrificeTargets.isNotEmpty() && variableSacrificeReduction > 0) {
                val maxReduction = variableSacrificeTargets.size * variableSacrificeReduction
                effectiveCost = effectiveCost.reduceGeneric(maxReduction)
            }

            // Check mana affordability (including Convoke/Delve if available)
            val hasConvoke = cardDef.keywords.contains(Keyword.CONVOKE)
            val convokeCreatures = if (hasConvoke) {
                context.costUtils.findConvokeCreatures(state, playerId)
            } else null

            val hasDelve = cardDef.keywords.contains(Keyword.DELVE)
            val delveCards = if (hasDelve) {
                context.costUtils.findDelveCards(state, playerId)
            } else null
            val minDelveNeeded = if (hasDelve && delveCards != null && delveCards.isNotEmpty()) {
                context.costUtils.calculateMinDelveNeeded(state, playerId, effectiveCost, delveCards)
            } else null

            // Build spell context for conditional mana restriction awareness
            val spellContext = SpellPaymentContext(
                isInstantOrSorcery = cardComponent.typeLine.isInstant || cardComponent.typeLine.isSorcery,
                isKicked = false,
                isCreature = cardComponent.typeLine.isCreature,
                manaValue = cardComponent.manaCost.cmc,
                hasXInCost = cardComponent.manaCost.hasX
            )

            // For Convoke/Delve spells, check if affordable with alternative payment help
            val canAfford = if (hasConvoke && convokeCreatures != null && convokeCreatures.isNotEmpty()) {
                context.manaSolver.canPay(state, playerId, effectiveCost, spellContext = spellContext) ||
                    context.costUtils.canAffordWithConvoke(state, playerId, effectiveCost, convokeCreatures)
            } else if (hasDelve && delveCards != null && delveCards.isNotEmpty()) {
                context.manaSolver.canPay(state, playerId, effectiveCost, spellContext = spellContext) ||
                    context.costUtils.canAffordWithDelve(state, playerId, effectiveCost, delveCards)
            } else {
                context.manaSolver.canPay(state, playerId, effectiveCost, spellContext = spellContext)
            }

            // Check alternative casting cost affordability (e.g., Jodah's {W}{U}{B}{R}{G})
            val canAffordAlternative = context.alternativeCastingCosts.isNotEmpty() &&
                context.alternativeCastingCosts.any { altCost ->
                    val altEffective = context.costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, altCost)
                    context.manaSolver.canPay(state, playerId, altEffective)
                }

            // Check self-alternative cost (e.g., Zahid's {3}{U} + tap an artifact)
            val selfAltCost = cardDef.script.selfAlternativeCost
            val canAffordSelfAlternative = if (selfAltCost != null) {
                val selfAltMana = ManaCost.parse(selfAltCost.manaCost)
                val selfAltEffective = context.costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, selfAltMana, playerId)
                val canPayMana = context.manaSolver.canPay(state, playerId, selfAltEffective)
                val canPayAdditional = selfAltCost.additionalCosts.all { cost ->
                    when (cost) {
                        is AdditionalCost.TapPermanents -> {
                            context.costUtils.findAbilityTapTargets(state, playerId, cost.filter).size >= cost.count
                        }
                        else -> true
                    }
                }
                canPayMana && canPayAdditional
            } else false

            if (!canAfford && !canAffordAlternative && !canAffordSelfAlternative) continue

            val targetReqs = buildList {
                addAll(cardDef.script.targetRequirements)
                cardDef.script.auraTarget?.let { add(it) }
            }

            // Build additional cost info for the client
            val costInfo = buildAdditionalCostData(
                additionalCosts, sacrificeTargets, variableSacrificeTargets,
                exileTargets, exileMinCount, discardTargets, discardCount,
                beholdTargets, beholdCount
            )

            // Calculate X cost info if the spell has X in its cost
            val hasXCost = effectiveCost.hasX
            val maxAffordableX: Int? = if (hasXCost) {
                val availableSources = context.manaSolver.getAvailableManaCount(state, playerId)
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
            val autoTapSolution = context.manaSolver.solve(state, playerId, autoTapCost)
            val autoTapPreview = autoTapSolution?.sources?.map { it.entityId }

            // Check for DividedDamageEffect to flag damage distribution requirement
            val spellEffect = cardDef.script.spellEffect
            val dividedDamageEffect = spellEffect as? DividedDamageEffect
            val requiresDamageDistribution = dividedDamageEffect != null
            val totalDamageToDistribute = dividedDamageEffect?.totalDamage
            val minDamagePerTarget = if (dividedDamageEffect != null) 1 else null

            // Compute alternative cost info for this spell
            val altCostInfo = if (canAffordAlternative) {
                val altCost = context.alternativeCastingCosts.first()
                val altEffective = context.costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, altCost)
                val altSolution = context.manaSolver.solve(state, playerId, altEffective)
                Triple(altEffective.toString(), altSolution?.sources?.map { it.entityId }, context.manaSolver.canPay(state, playerId, altEffective))
            } else null

            // Compute self-alternative cost info (e.g., Zahid)
            val selfAltCostResult = if (canAffordSelfAlternative && selfAltCost != null) {
                val selfAltMana = ManaCost.parse(selfAltCost.manaCost)
                val selfAltEffective = context.costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, selfAltMana, playerId)
                val selfAltSolution = context.manaSolver.solve(state, playerId, selfAltEffective)
                val tapCost = selfAltCost.additionalCosts.filterIsInstance<AdditionalCost.TapPermanents>().firstOrNull()
                val tapTargets = if (tapCost != null) context.costUtils.findAbilityTapTargets(state, playerId, tapCost.filter) else null
                val addlCostInfo = if (tapTargets != null && tapCost != null) {
                    AdditionalCostData(
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

            // Modal spells (chooseCount = 1): generate one LegalAction per mode
            // so the opponent sees which mode was chosen on the stack
            val modalEffect = spellEffect as? ModalEffect
            if (modalEffect != null && modalEffect.chooseCount == 1 && canAfford) {
                for ((modeIndex, mode) in modalEffect.modes.withIndex()) {
                    // Per-mode cost overrides: if a mode has additionalManaCost or additionalCosts,
                    // compute mode-specific affordability and cost info
                    val modeExtraManaCost = mode.additionalManaCost
                    val modeEffectiveCost = if (modeExtraManaCost != null) {
                        effectiveCost + ManaCost.parse(modeExtraManaCost)
                    } else {
                        effectiveCost
                    }
                    val modeCanAfford = if (modeExtraManaCost != null) {
                        context.manaSolver.canPay(state, playerId, modeEffectiveCost, spellContext = spellContext)
                    } else {
                        true // already checked above
                    }
                    if (!modeCanAfford) continue

                    // Per-mode additional costs: when mode specifies its own, use those instead of card-level
                    val modeAdditionalCosts = mode.additionalCosts ?: additionalCosts
                    var modeCanPayAdditionalCosts = true
                    val modeSacrificeTargets = mutableListOf<EntityId>()
                    var modeExileTargets = emptyList<EntityId>()
                    var modeExileMinCount = 0
                    var modeDiscardTargets = emptyList<EntityId>()
                    var modeDiscardCount = 0
                    if (mode.additionalCosts != null) {
                        // Re-evaluate additional cost payability for this mode's specific costs
                        for (cost in modeAdditionalCosts) {
                            when (cost) {
                                is AdditionalCost.SacrificePermanent -> {
                                    val validSacTargets = context.costUtils.findSacrificeTargets(state, playerId, cost)
                                    if (validSacTargets.size < cost.count) modeCanPayAdditionalCosts = false
                                    modeSacrificeTargets.addAll(validSacTargets)
                                }
                                is AdditionalCost.ExileCards -> {
                                    val validExileTargets = context.costUtils.findExileTargets(state, playerId, cost.filter, cost.fromZone)
                                    if (validExileTargets.size < cost.count) modeCanPayAdditionalCosts = false
                                    modeExileTargets = validExileTargets
                                    modeExileMinCount = cost.count
                                }
                                is AdditionalCost.DiscardCards -> {
                                    val handZone = ZoneKey(playerId, Zone.HAND)
                                    val handCards = state.getZone(handZone).filter { it != cardId }
                                    val predicateContext = PredicateContext(controllerId = playerId)
                                    val validDiscards = if (cost.filter == com.wingedsheep.sdk.scripting.GameObjectFilter.Any) {
                                        handCards
                                    } else {
                                        handCards.filter { context.predicateEvaluator.matches(state, it, cost.filter, predicateContext) }
                                    }
                                    if (validDiscards.size < cost.count) modeCanPayAdditionalCosts = false
                                    modeDiscardTargets = validDiscards
                                    modeDiscardCount = cost.count
                                }
                                is AdditionalCost.Forage -> {
                                    val graveyardSize = state.getZone(ZoneKey(playerId, Zone.GRAVEYARD)).size
                                    val projected = state.projectedState
                                    val hasFood = state.getBattlefield().any { permId ->
                                        state.getEntity(permId) ?: return@any false
                                        projected.getController(permId) == playerId &&
                                            projected.hasSubtype(permId, com.wingedsheep.sdk.core.Subtype.FOOD.value)
                                    }
                                    if (graveyardSize < 3 && !hasFood) modeCanPayAdditionalCosts = false
                                }
                                else -> {}
                            }
                        }
                    }
                    if (!modeCanPayAdditionalCosts) continue

                    val modeCostInfo = if (mode.additionalCosts != null) {
                        buildAdditionalCostData(modeAdditionalCosts, modeSacrificeTargets, emptyList(), modeExileTargets, modeExileMinCount, modeDiscardTargets, modeDiscardCount)
                    } else {
                        costInfo
                    }

                    val modeManaCostString = modeEffectiveCost.toString()
                    val modeAutoTapSolution = if (modeExtraManaCost != null) {
                        context.manaSolver.solve(state, playerId, modeEffectiveCost)
                    } else {
                        autoTapSolution
                    }
                    val modeAutoTapPreview = if (modeExtraManaCost != null) {
                        modeAutoTapSolution?.sources?.map { it.entityId }
                    } else {
                        autoTapPreview
                    }

                    val modeTargetReqs = mode.targetRequirements
                    if (modeTargetReqs.isNotEmpty()) {
                        // Mode requires targets
                        val modeTargetInfos = context.targetUtils.buildTargetInfos(state, playerId, modeTargetReqs)
                        val allSatisfied = context.targetUtils.allRequirementsSatisfied(modeTargetInfos)
                        if (!allSatisfied) continue // Skip modes with unsatisfiable targets

                        val firstReq = modeTargetReqs.first()
                        val firstInfo = modeTargetInfos.first()

                        // Check for auto-select (single player target, single valid choice)
                        val canAutoSelect = modeTargetReqs.size == 1 &&
                            context.targetUtils.shouldAutoSelectPlayerTarget(firstReq, firstInfo.validTargets)

                        if (canAutoSelect) {
                            val autoTarget = ChosenTarget.Player(firstInfo.validTargets.first())
                            result.add(LegalAction(
                                actionType = "CastSpellMode",
                                description = mode.description,
                                action = CastSpell(playerId, cardId, targets = listOf(autoTarget), chosenMode = modeIndex),
                                hasXCost = hasXCost,
                                maxAffordableX = maxAffordableX,
                                additionalCostInfo = modeCostInfo,
                                hasConvoke = hasConvoke,
                                convokeCreatures = convokeCreatures,
                                hasDelve = hasDelve,
                                delveCards = delveCards,
                                minDelveNeeded = minDelveNeeded,
                                manaCostString = modeManaCostString,
                                autoTapPreview = modeAutoTapPreview
                            ))
                        } else {
                            result.add(LegalAction(
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
                                additionalCostInfo = modeCostInfo,
                                hasConvoke = hasConvoke,
                                convokeCreatures = convokeCreatures,
                                hasDelve = hasDelve,
                                delveCards = delveCards,
                                minDelveNeeded = minDelveNeeded,
                                manaCostString = modeManaCostString,
                                autoTapPreview = modeAutoTapPreview
                            ))
                        }
                    } else {
                        // Mode has no targets
                        result.add(LegalAction(
                            actionType = "CastSpellMode",
                            description = mode.description,
                            action = CastSpell(playerId, cardId, chosenMode = modeIndex),
                            hasXCost = hasXCost,
                            maxAffordableX = maxAffordableX,
                            additionalCostInfo = modeCostInfo,
                            hasConvoke = hasConvoke,
                            convokeCreatures = convokeCreatures,
                            hasDelve = hasDelve,
                            delveCards = delveCards,
                            minDelveNeeded = minDelveNeeded,
                            manaCostString = modeManaCostString,
                            autoTapPreview = modeAutoTapPreview
                        ))
                    }
                }
                // Skip the normal targeting logic for modal spells
            } else if (targetReqs.isNotEmpty()) {
                // Spell requires targets - find valid targets for all requirements
                val targetReqInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs)

                // Check if all requirements can be satisfied
                val allRequirementsSatisfied = context.targetUtils.allRequirementsSatisfied(targetReqInfos)

                val firstReq = targetReqs.first()
                val firstReqInfo = targetReqInfos.first()

                // Only add the action if all requirements can be satisfied
                if (allRequirementsSatisfied) {
                    // Check if we can auto-select player targets (single target, single valid choice)
                    val canAutoSelect = targetReqs.size == 1 &&
                        context.targetUtils.shouldAutoSelectPlayerTarget(firstReq, firstReqInfo.validTargets)

                    if (canAutoSelect) {
                        // Auto-select the single valid player target
                        val autoSelectedTarget = ChosenTarget.Player(firstReqInfo.validTargets.first())
                        if (canAfford) {
                            result.add(LegalAction(
                                actionType = "CastSpell",
                                description = "Cast ${cardComponent.name}",
                                action = CastSpell(playerId, cardId, targets = listOf(autoSelectedTarget)),
                                hasXCost = hasXCost,
                                maxAffordableX = maxAffordableX,
                                additionalCostInfo = costInfo,
                                hasConvoke = hasConvoke,
                                convokeCreatures = convokeCreatures,
                                hasDelve = hasDelve,
                                delveCards = delveCards,
                                minDelveNeeded = minDelveNeeded,
                                manaCostString = manaCostString,
                                requiresDamageDistribution = requiresDamageDistribution,
                                totalDamageToDistribute = totalDamageToDistribute,
                                minDamagePerTarget = minDamagePerTarget,
                                autoTapPreview = autoTapPreview
                            ))
                        }
                        if (altCostInfo?.third == true) {
                            result.add(LegalAction(
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
                            result.add(LegalAction(
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
                            result.add(LegalAction(
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
                                convokeCreatures = convokeCreatures,
                                hasDelve = hasDelve,
                                delveCards = delveCards,
                                minDelveNeeded = minDelveNeeded,
                                manaCostString = manaCostString,
                                requiresDamageDistribution = requiresDamageDistribution,
                                totalDamageToDistribute = totalDamageToDistribute,
                                minDamagePerTarget = minDamagePerTarget,
                                autoTapPreview = autoTapPreview
                            ))
                        }
                        if (altCostInfo?.third == true) {
                            result.add(LegalAction(
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
                            result.add(LegalAction(
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
                    result.add(LegalAction(
                        actionType = "CastSpell",
                        description = "Cast ${cardComponent.name}",
                        action = CastSpell(playerId, cardId),
                        hasXCost = hasXCost,
                        maxAffordableX = maxAffordableX,
                        additionalCostInfo = costInfo,
                        hasConvoke = hasConvoke,
                        convokeCreatures = convokeCreatures,
                        hasDelve = hasDelve,
                        delveCards = delveCards,
                        minDelveNeeded = minDelveNeeded,
                        manaCostString = manaCostString,
                        autoTapPreview = autoTapPreview
                    ))
                }
                if (altCostInfo?.third == true) {
                    result.add(LegalAction(
                        actionType = "CastWithAlternativeCost",
                        description = "Cast ${cardComponent.name} (${altCostInfo.first})",
                        action = CastSpell(playerId, cardId, useAlternativeCost = true),
                        manaCostString = altCostInfo.first,
                        autoTapPreview = altCostInfo.second
                    ))
                }
                if (selfAltCostResult != null) {
                    result.add(LegalAction(
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

        // --- Kicker ---
        enumerateKicker(context, hand, result)

        return result
    }

    /**
     * Enumerates kicked spell actions for cards with Kicker or KickerWithAdditionalCost.
     */
    private fun enumerateKicker(
        context: EnumerationContext,
        hand: List<EntityId>,
        result: MutableList<LegalAction>
    ) {
        val state = context.state
        val playerId = context.playerId

        for (cardId in hand) {
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
            if (cardComponent.typeLine.isLand) continue
            if (context.cantCastSpells) continue

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

            // Check timing (same rules as normal cast)
            val isInstant = cardComponent.typeLine.isInstant
            val hasFlash = cardDef.keywords.contains(Keyword.FLASH)
            val grantedFlash = hasFlash || context.castPermissionUtils.hasGrantedFlash(state, cardId)
            if (!isInstant && !grantedFlash && !context.canPlaySorcerySpeed) continue

            // Check cast restrictions
            val castRestrictions = cardDef.script.castRestrictions
            if (castRestrictions.isNotEmpty() && !context.castPermissionUtils.checkCastRestrictions(state, playerId, castRestrictions)) continue

            // Calculate kicked/offspring cost
            val baseCost = context.costCalculator.calculateEffectiveCost(state, cardDef, playerId)
            val kickedCost = if (manaKicker != null) {
                baseCost + manaKicker.cost
            } else if (offspringAbility != null) {
                baseCost + offspringAbility.cost
            } else {
                baseCost // No extra mana for additional-cost kicker
            }
            val kickedSpellContext = SpellPaymentContext(
                isInstantOrSorcery = cardComponent.typeLine.isInstant || cardComponent.typeLine.isSorcery,
                isKicked = true,
                isCreature = cardComponent.typeLine.isCreature,
                manaValue = cardComponent.manaCost.cmc,
                hasXInCost = cardComponent.manaCost.hasX
            )
            val canAffordKickedMana = context.manaSolver.canPay(state, playerId, kickedCost, spellContext = kickedSpellContext)
            val kickedCostString = kickedCost.toString()
            val kickedAutoTapSolution = context.manaSolver.solve(state, playerId, kickedCost, spellContext = kickedSpellContext)
            val kickedAutoTapPreview = kickedAutoTapSolution?.sources?.map { it.entityId }

            // Check additional cost payability (e.g., sacrifice a creature)
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
                        result.add(LegalAction(
                            actionType = "CastWithKicker",
                            description = "Cast ${cardComponent.name} ($kickLabel)",
                            action = CastSpell(playerId, cardId, targets = listOf(autoSelectedTarget), wasKicked = true),
                            affordable = canAffordKicked,
                            manaCostString = kickedCostString,
                            autoTapPreview = kickedAutoTapPreview,
                            additionalCostInfo = kickerCostInfo,
                            requiresDamageDistribution = kickerRequiresDamageDistribution,
                            totalDamageToDistribute = kickerTotalDamage,
                            minDamagePerTarget = kickerMinDamagePerTarget
                        ))
                    } else {
                        result.add(LegalAction(
                            actionType = "CastWithKicker",
                            description = "Cast ${cardComponent.name} ($kickLabel)",
                            action = CastSpell(playerId, cardId, wasKicked = true),
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
                            minDamagePerTarget = kickerMinDamagePerTarget
                        ))
                    }
                }
            } else {
                result.add(LegalAction(
                    actionType = "CastWithKicker",
                    description = "Cast ${cardComponent.name} ($kickLabel)",
                    action = CastSpell(playerId, cardId, wasKicked = true),
                    affordable = canAffordKicked,
                    manaCostString = kickedCostString,
                    autoTapPreview = kickedAutoTapPreview,
                    additionalCostInfo = kickerCostInfo
                ))
            }

            // If normal cast is not affordable but kicker is (unlikely), ensure normal cast shows unaffordable
            if (!context.manaSolver.canPay(state, playerId, baseCost)) {
                result.add(LegalAction(
                    actionType = "CastSpell",
                    description = "Cast ${cardComponent.name}",
                    action = CastSpell(playerId, cardId),
                    affordable = false,
                    manaCostString = baseCost.toString()
                ))
            }
        }
    }

    /**
     * Builds the AdditionalCostData for the client based on what additional costs the spell requires.
     */
    private fun buildAdditionalCostData(
        additionalCosts: List<AdditionalCost>,
        sacrificeTargets: List<EntityId>,
        variableSacrificeTargets: List<EntityId>,
        exileTargets: List<EntityId>,
        exileMinCount: Int,
        discardTargets: List<EntityId>,
        discardCount: Int,
        beholdTargets: List<EntityId> = emptyList(),
        beholdCount: Int = 0
    ): AdditionalCostData? {
        return if (variableSacrificeTargets.isNotEmpty()) {
            val varSacCost = additionalCosts.filterIsInstance<AdditionalCost.SacrificeCreaturesForCostReduction>().firstOrNull()
            AdditionalCostData(
                description = varSacCost?.description ?: "You may sacrifice any number of creatures",
                costType = "SacrificeForCostReduction",
                validSacrificeTargets = variableSacrificeTargets,
                sacrificeCount = 0 // min 0 — sacrifice is optional
            )
        } else if (sacrificeTargets.isNotEmpty()) {
            val sacCost = additionalCosts.filterIsInstance<AdditionalCost.SacrificePermanent>().firstOrNull()
            AdditionalCostData(
                description = sacCost?.description ?: "Sacrifice a creature",
                costType = "SacrificePermanent",
                validSacrificeTargets = sacrificeTargets,
                sacrificeCount = sacCost?.count ?: 1
            )
        } else if (exileTargets.isNotEmpty()) {
            val exileCostDesc = additionalCosts
                .filterIsInstance<AdditionalCost.ExileVariableCards>()
                .firstOrNull()?.description
                ?: additionalCosts
                    .filterIsInstance<AdditionalCost.ExileCards>()
                    .firstOrNull()?.description
                ?: "Exile cards from your graveyard"
            AdditionalCostData(
                description = exileCostDesc,
                costType = "ExileFromGraveyard",
                validExileTargets = exileTargets,
                exileMinCount = exileMinCount,
                exileMaxCount = exileTargets.size
            )
        } else if (discardTargets.isNotEmpty()) {
            val discardCost = additionalCosts.filterIsInstance<AdditionalCost.DiscardCards>().firstOrNull()
            AdditionalCostData(
                description = discardCost?.description ?: "Discard a card",
                costType = "DiscardCard",
                validDiscardTargets = discardTargets,
                discardCount = discardCount
            )
        } else if (beholdTargets.isNotEmpty()) {
            val beholdCost = additionalCosts.filterIsInstance<AdditionalCost.Behold>().firstOrNull()
            AdditionalCostData(
                description = beholdCost?.description ?: "Behold a card",
                costType = "Behold",
                validBeholdTargets = beholdTargets,
                beholdCount = beholdCount
            )
        } else null
    }

    /**
     * Internal data holder for self-alternative cost computation results.
     */
    private data class SelfAltCostResult(
        val manaCostString: String,
        val autoTapPreview: List<EntityId>?,
        val additionalCostInfo: AdditionalCostData?
    )
}
