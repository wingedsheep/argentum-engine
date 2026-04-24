package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.legalactions.ModalEnumerationMode
import com.wingedsheep.engine.legalactions.ModalLegalEnumeration
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
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.engine.mechanics.mana.ManaSource
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
            var blightOrPayCost: AdditionalCost.BlightOrPay? = null
            var blightCreatures = emptyList<EntityId>()
            var beholdOrPayCost: AdditionalCost.BeholdOrPay? = null
            var beholdOrPayTargets = emptyList<EntityId>()
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
                    is AdditionalCost.BlightOrPay -> {
                        // Always payable: player can always choose the "pay mana" path
                        // Find creatures for the blight path
                        blightOrPayCost = cost
                        val projected = state.projectedState
                        blightCreatures = projected.getBattlefieldControlledBy(playerId)
                            .filter { projected.isCreature(it) }
                    }
                    is AdditionalCost.BeholdOrPay -> {
                        // Always payable: player can always choose the "pay mana" path
                        // Find valid behold targets (battlefield permanents + hand cards matching filter)
                        beholdOrPayCost = cost
                        val projected = state.projectedState
                        val predicateContext = PredicateContext(controllerId = playerId)
                        val battlefieldMatches = projected.getBattlefieldControlledBy(playerId).filter { permId ->
                            context.predicateEvaluator.matchesWithProjection(state, projected, permId, cost.filter, predicateContext)
                        }
                        val handZone = ZoneKey(playerId, Zone.HAND)
                        val handMatches = state.getZone(handZone)
                            .filter { it != cardId } // Exclude the card being cast
                            .filter { context.predicateEvaluator.matches(state, it, cost.filter, predicateContext) }
                        beholdOrPayTargets = battlefieldMatches + handMatches
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

            // Save base cost for blight path, then add extra mana for the "pay" path
            val blightBaseCost = effectiveCost
            if (blightOrPayCost != null) {
                effectiveCost = effectiveCost + ManaCost.parse(blightOrPayCost.alternativeManaCost)
            }

            // Save base cost for behold path, then add extra mana for the "pay" path
            val beholdBaseCost = effectiveCost
            if (beholdOrPayCost != null) {
                effectiveCost = effectiveCost + ManaCost.parse(beholdOrPayCost.alternativeManaCost)
            }

            // Check mana affordability (including Convoke/Delve if available).
            // Convoke and Delve can be printed on the card or granted at runtime by a
            // battlefield permanent (e.g., Eirdu's "Creature spells you cast have convoke.").
            val hasConvoke = context.grantedKeywordResolver.hasKeyword(state, playerId, cardDef, Keyword.CONVOKE)
            val convokeCreatures = if (hasConvoke) {
                context.costUtils.findConvokeCreatures(state, playerId)
            } else null

            val hasDelve = context.grantedKeywordResolver.hasKeyword(state, playerId, cardDef, Keyword.DELVE)
            val delveCards = if (hasDelve) {
                context.costUtils.findDelveCards(state, playerId)
            } else null
            val minDelveNeeded = if (hasDelve && delveCards != null && delveCards.isNotEmpty()) {
                context.costUtils.calculateMinDelveNeeded(state, playerId, effectiveCost, delveCards, precomputedSources = context.availableManaSources)
            } else null

            // Build spell context for conditional mana restriction awareness
            val spellContext = SpellPaymentContext(
                isInstantOrSorcery = cardComponent.typeLine.isInstant || cardComponent.typeLine.isSorcery,
                isKicked = false,
                isCreature = cardComponent.typeLine.isCreature,
                manaValue = cardComponent.manaCost.cmc,
                hasXInCost = cardComponent.manaCost.hasX,
                subtypes = cardComponent.typeLine.subtypes.map { it.value }.toSet()
            )

            // For Convoke/Delve spells, check if affordable with alternative payment help
            val cachedSources = context.availableManaSources
            val canAfford = if (hasConvoke && convokeCreatures != null && convokeCreatures.isNotEmpty()) {
                context.manaSolver.canPay(state, playerId, effectiveCost, spellContext = spellContext, precomputedSources = cachedSources) ||
                    context.costUtils.canAffordWithConvoke(state, playerId, effectiveCost, convokeCreatures, precomputedSources = cachedSources)
            } else if (hasDelve && delveCards != null && delveCards.isNotEmpty()) {
                context.manaSolver.canPay(state, playerId, effectiveCost, spellContext = spellContext, precomputedSources = cachedSources) ||
                    context.costUtils.canAffordWithDelve(state, playerId, effectiveCost, delveCards, precomputedSources = cachedSources)
            } else {
                context.manaSolver.canPay(state, playerId, effectiveCost, spellContext = spellContext, precomputedSources = cachedSources)
            }

            // Check alternative casting cost affordability (e.g., Jodah's {W}{U}{B}{R}{G})
            val canAffordAlternative = context.alternativeCastingCosts.isNotEmpty() &&
                context.alternativeCastingCosts.any { altCost ->
                    val altEffective = context.costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, altCost)
                    context.manaSolver.canPay(state, playerId, altEffective, precomputedSources = cachedSources)
                }

            // Check self-alternative cost (e.g., Zahid's {3}{U} + tap an artifact)
            val selfAltCost = cardDef.script.selfAlternativeCost
            val canAffordSelfAlternative = if (selfAltCost != null) {
                val selfAltMana = ManaCost.parse(selfAltCost.manaCost)
                val selfAltEffective = context.costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, selfAltMana, playerId)
                val canPayMana = context.manaSolver.canPay(state, playerId, selfAltEffective, precomputedSources = cachedSources)
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

            // Check evoke cost (alternative cost from Evoke keyword)
            val evokeAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Evoke>().firstOrNull()
            val canAffordEvoke = if (evokeAbility != null) {
                val evokeMana = context.costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, evokeAbility.cost, playerId)
                context.manaSolver.canPay(state, playerId, evokeMana, precomputedSources = cachedSources)
            } else false

            // Check blight path affordability (base cost without the extra mana, but needs a creature)
            val canAffordBlightPath = if (blightOrPayCost != null && blightCreatures.isNotEmpty()) {
                context.manaSolver.canPay(state, playerId, blightBaseCost, spellContext = spellContext, precomputedSources = cachedSources)
            } else false

            // Check behold path affordability (base cost without the extra mana, but needs a beholdable target)
            val canAffordBeholdPath = if (beholdOrPayCost != null && beholdOrPayTargets.isNotEmpty()) {
                context.manaSolver.canPay(state, playerId, beholdBaseCost, spellContext = spellContext, precomputedSources = cachedSources)
            } else false

            if (!canAfford && !canAffordAlternative && !canAffordSelfAlternative && !canAffordEvoke && !canAffordBlightPath && !canAffordBeholdPath) continue

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

            // Compute blight path info (separate legal action with lower mana cost + blight target selection)
            val blightPathInfo = if (canAffordBlightPath && blightOrPayCost != null) {
                val blightManaCostString = blightBaseCost.toString()
                val blightAutoTapPreview = if (context.skipAutoTapPreview) null else {
                    context.manaSolver.solve(state, playerId, blightBaseCost, precomputedSources = cachedSources)
                        ?.sources?.map { it.entityId }
                }
                val blightCostInfo = AdditionalCostData(
                    description = "creature to blight",
                    costType = "Blight",
                    validBlightTargets = blightCreatures,
                    blightAmount = blightOrPayCost.blightAmount
                )
                Triple(blightManaCostString, blightAutoTapPreview, blightCostInfo)
            } else null

            // Compute behold path info (separate legal action with lower mana cost + behold target selection)
            val beholdPathInfo = if (canAffordBeholdPath && beholdOrPayCost != null) {
                val beholdManaCostString = beholdBaseCost.toString()
                val beholdAutoTapPreview = if (context.skipAutoTapPreview) null else {
                    context.manaSolver.solve(state, playerId, beholdBaseCost, precomputedSources = cachedSources)
                        ?.sources?.map { it.entityId }
                }
                val beholdCostInfo = AdditionalCostData(
                    description = beholdOrPayCost.description,
                    costType = "Behold",
                    validBeholdTargets = beholdOrPayTargets,
                    beholdCount = 1
                )
                Triple(beholdManaCostString, beholdAutoTapPreview, beholdCostInfo)
            } else null

            // Calculate X cost info if the spell has X in its cost
            val hasXCost = effectiveCost.hasX
            val maxAffordableX: Int? = if (hasXCost) {
                val availableSources = context.manaSolver.getAvailableManaCount(state, playerId, precomputedSources = cachedSources)
                val fixedCost = effectiveCost.cmc  // X contributes 0 to CMC
                val xSymbolCount = effectiveCost.xCount.coerceAtLeast(1)
                ((availableSources - fixedCost) / xSymbolCount).coerceAtLeast(0)
            } else null

            // Always include mana cost string for cast actions
            val manaCostString = effectiveCost.toString()

            // Compute auto-tap preview for UI highlighting (skipped in ACTIONS_ONLY mode).
            //
            // The solver runs against the worst-case *remaining* cost the player can be
            // on the hook for after a legal delve choice — i.e. the full effective cost
            // minus the minimum delve reduction that makes the spell affordable
            // (`minDelveNeeded`, 0 when the spell is affordable without any delve at all).
            // That gives an exact solve for the largest land set any legal cast might
            // need; the client's `trimAutoTapPreview` prunes the list as the player
            // delves past the minimum. The engine re-solves on submit anyway —
            // `CastPaymentProcessor.explicitPay` taps only the minimum subset needed
            // after the alt-payment reduction is actually applied.
            val autoTapPreview = if (context.skipAutoTapPreview) null else {
                val costForPreview = if (hasDelve && minDelveNeeded != null && minDelveNeeded > 0) {
                    effectiveCost.reduceGeneric(minDelveNeeded)
                } else {
                    effectiveCost
                }
                context.manaSolver.solve(state, playerId, costForPreview, precomputedSources = cachedSources)
                    ?.sources?.map { it.entityId }
            }

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
                val altPreview = if (context.skipAutoTapPreview) null else {
                    context.manaSolver.solve(state, playerId, altEffective, precomputedSources = cachedSources)
                        ?.sources?.map { it.entityId }
                }
                Triple(altEffective.toString(), altPreview, context.manaSolver.canPay(state, playerId, altEffective, precomputedSources = cachedSources))
            } else null

            // Compute self-alternative cost info (e.g., Zahid)
            val selfAltCostResult = if (canAffordSelfAlternative && selfAltCost != null) {
                val selfAltMana = ManaCost.parse(selfAltCost.manaCost)
                val selfAltEffective = context.costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, selfAltMana, playerId)
                val selfAltPreview = if (context.skipAutoTapPreview) null else {
                    context.manaSolver.solve(state, playerId, selfAltEffective, precomputedSources = cachedSources)
                        ?.sources?.map { it.entityId }
                }
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
                    autoTapPreview = selfAltPreview,
                    additionalCostInfo = addlCostInfo
                )
            } else null

            // Compute evoke cost info
            val evokeCostResult = if (canAffordEvoke && evokeAbility != null) {
                val evokeMana = context.costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, evokeAbility.cost, playerId)
                val evokePreview = if (context.skipAutoTapPreview) null else {
                    context.manaSolver.solve(state, playerId, evokeMana, precomputedSources = cachedSources)
                        ?.sources?.map { it.entityId }
                }
                SelfAltCostResult(
                    manaCostString = evokeMana.toString(),
                    autoTapPreview = evokePreview,
                    additionalCostInfo = null
                )
            } else null

            // Modal spells: choose-1 emits one LegalAction per mode so the opponent
            // sees which mode was picked on the stack. Choose-N emits a single
            // CastSpellModal action with a [ModalLegalEnumeration] payload and lets
            // the client drive the cast-time mode/target decision loop (rules 601.2b–c,
            // 700.2a). Choose-N cartesian enumeration would blow up for allowRepeat
            // (Escalate/Spree) and for wide target pools.
            val modalEffect = spellEffect as? ModalEffect
            if (modalEffect != null && canAfford) {
                val modeEnumerations = modalEffect.modes.mapIndexed { modeIndex, mode ->
                    computeModeEnumeration(
                        context = context,
                        cardId = cardId,
                        playerId = playerId,
                        modeIndex = modeIndex,
                        mode = mode,
                        baseEffectiveCost = effectiveCost,
                        cardLevelAdditionalCostInfo = costInfo,
                        baseAutoTapPreview = autoTapPreview,
                        spellContext = spellContext,
                        cachedSources = cachedSources
                    )
                }

                if (modalEffect.chooseCount == 1) {
                    for (modeEnum in modeEnumerations) {
                        if (!modeEnum.available) continue

                        val modeIndex = modeEnum.modeIndex
                        val mode = modeEnum.mode
                        val modeTargetReqs = mode.targetRequirements
                        val modeTargetInfos = modeEnum.targetInfos

                        if (modeTargetReqs.isNotEmpty()) {
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
                                    action = CastSpell(
                                        playerId,
                                        cardId,
                                        targets = listOf(autoTarget),
                                        chosenModes = listOf(modeIndex),
                                        modeTargetsOrdered = listOf(listOf(autoTarget))
                                    ),
                                    hasXCost = hasXCost,
                                    maxAffordableX = maxAffordableX,
                                    additionalCostInfo = modeEnum.additionalCostInfo,
                                    hasConvoke = hasConvoke,
                                    convokeCreatures = convokeCreatures,
                                    hasDelve = hasDelve,
                                    delveCards = delveCards,
                                    minDelveNeeded = minDelveNeeded,
                                    manaCostString = modeEnum.manaCostString,
                                    autoTapPreview = modeEnum.autoTapPreview
                                ))
                            } else {
                                result.add(LegalAction(
                                    actionType = "CastSpellMode",
                                    description = mode.description,
                                    action = CastSpell(playerId, cardId, chosenModes = listOf(modeIndex)),
                                    validTargets = firstInfo.validTargets,
                                    requiresTargets = true,
                                    targetCount = firstReq.count,
                                    minTargets = firstReq.effectiveMinCount,
                                    targetDescription = firstReq.description,
                                    targetRequirements = if (modeTargetInfos.size > 1) modeTargetInfos else null,
                                    hasXCost = hasXCost,
                                    maxAffordableX = maxAffordableX,
                                    additionalCostInfo = modeEnum.additionalCostInfo,
                                    hasConvoke = hasConvoke,
                                    convokeCreatures = convokeCreatures,
                                    hasDelve = hasDelve,
                                    delveCards = delveCards,
                                    minDelveNeeded = minDelveNeeded,
                                    manaCostString = modeEnum.manaCostString,
                                    autoTapPreview = modeEnum.autoTapPreview
                                ))
                            }
                        } else {
                            // Mode has no targets
                            result.add(LegalAction(
                                actionType = "CastSpellMode",
                                description = mode.description,
                                action = CastSpell(playerId, cardId, chosenModes = listOf(modeIndex)),
                                hasXCost = hasXCost,
                                maxAffordableX = maxAffordableX,
                                additionalCostInfo = modeEnum.additionalCostInfo,
                                hasConvoke = hasConvoke,
                                convokeCreatures = convokeCreatures,
                                hasDelve = hasDelve,
                                delveCards = delveCards,
                                minDelveNeeded = minDelveNeeded,
                                manaCostString = modeEnum.manaCostString,
                                autoTapPreview = modeEnum.autoTapPreview
                            ))
                        }
                    }
                } else {
                    // Choose-N (> 1): emit a single LegalAction carrying the per-mode
                    // enumeration. The client drives cast-time mode + target selection.
                    val enumerationModes = modeEnumerations.map { modeEnum ->
                        ModalEnumerationMode(
                            index = modeEnum.modeIndex,
                            description = modeEnum.mode.description,
                            available = modeEnum.available,
                            additionalManaCost = modeEnum.mode.additionalManaCost,
                            additionalCostInfo = modeEnum.additionalCostInfo,
                            targetRequirements = modeEnum.targetInfos
                        )
                    }
                    val unavailableIndices = enumerationModes
                        .filterNot { it.available }
                        .map { it.index }

                    // If every mode is unavailable, the spell can't legally be cast —
                    // drop the action entirely rather than offering an unplayable UI.
                    val hasAnyAvailable = enumerationModes.any { it.available }
                    if (hasAnyAvailable) {
                        result.add(LegalAction(
                            actionType = "CastSpellModal",
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
                            autoTapPreview = autoTapPreview,
                            modalEnumeration = ModalLegalEnumeration(
                                chooseCount = modalEffect.chooseCount,
                                minChooseCount = modalEffect.minChooseCount,
                                allowRepeat = modalEffect.allowRepeat,
                                modes = enumerationModes,
                                unavailableIndices = unavailableIndices
                            )
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
                        if (evokeCostResult != null) {
                            result.add(LegalAction(
                                actionType = "CastWithAlternativeCost",
                                description = "Evoke ${cardComponent.name} (${evokeCostResult.manaCostString})",
                                action = CastSpell(playerId, cardId, targets = listOf(autoSelectedTarget), useAlternativeCost = true),
                                manaCostString = evokeCostResult.manaCostString,
                                autoTapPreview = evokeCostResult.autoTapPreview
                            ))
                        }
                        if (blightPathInfo != null) {
                            result.add(LegalAction(
                                actionType = "CastSpell",
                                description = "Cast ${cardComponent.name} (Blight ${blightOrPayCost!!.blightAmount})",
                                action = CastSpell(playerId, cardId, targets = listOf(autoSelectedTarget)),
                                additionalCostInfo = blightPathInfo.third,
                                manaCostString = blightPathInfo.first,
                                requiresDamageDistribution = requiresDamageDistribution,
                                totalDamageToDistribute = totalDamageToDistribute,
                                minDamagePerTarget = minDamagePerTarget,
                                autoTapPreview = blightPathInfo.second
                            ))
                        }
                        if (beholdPathInfo != null) {
                            result.add(LegalAction(
                                actionType = "CastSpell",
                                description = "Cast ${cardComponent.name} (Behold)",
                                action = CastSpell(playerId, cardId, targets = listOf(autoSelectedTarget)),
                                additionalCostInfo = beholdPathInfo.third,
                                manaCostString = beholdPathInfo.first,
                                requiresDamageDistribution = requiresDamageDistribution,
                                totalDamageToDistribute = totalDamageToDistribute,
                                minDamagePerTarget = minDamagePerTarget,
                                autoTapPreview = beholdPathInfo.second
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
                        if (evokeCostResult != null) {
                            result.add(LegalAction(
                                actionType = "CastWithAlternativeCost",
                                description = "Evoke ${cardComponent.name} (${evokeCostResult.manaCostString})",
                                action = CastSpell(playerId, cardId, useAlternativeCost = true),
                                validTargets = firstReqInfo.validTargets,
                                requiresTargets = true,
                                targetCount = firstReq.count,
                                minTargets = firstReq.effectiveMinCount,
                                targetDescription = firstReq.description,
                                targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                                manaCostString = evokeCostResult.manaCostString,
                                autoTapPreview = evokeCostResult.autoTapPreview
                            ))
                        }
                        if (blightPathInfo != null) {
                            result.add(LegalAction(
                                actionType = "CastSpell",
                                description = "Cast ${cardComponent.name} (Blight ${blightOrPayCost!!.blightAmount})",
                                action = CastSpell(playerId, cardId),
                                validTargets = firstReqInfo.validTargets,
                                requiresTargets = true,
                                targetCount = firstReq.count,
                                minTargets = firstReq.effectiveMinCount,
                                targetDescription = firstReq.description,
                                targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                                additionalCostInfo = blightPathInfo.third,
                                manaCostString = blightPathInfo.first,
                                requiresDamageDistribution = requiresDamageDistribution,
                                totalDamageToDistribute = totalDamageToDistribute,
                                minDamagePerTarget = minDamagePerTarget,
                                autoTapPreview = blightPathInfo.second
                            ))
                        }
                        if (beholdPathInfo != null) {
                            result.add(LegalAction(
                                actionType = "CastSpell",
                                description = "Cast ${cardComponent.name} (Behold)",
                                action = CastSpell(playerId, cardId),
                                validTargets = firstReqInfo.validTargets,
                                requiresTargets = true,
                                targetCount = firstReq.count,
                                minTargets = firstReq.effectiveMinCount,
                                targetDescription = firstReq.description,
                                targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                                additionalCostInfo = beholdPathInfo.third,
                                manaCostString = beholdPathInfo.first,
                                requiresDamageDistribution = requiresDamageDistribution,
                                totalDamageToDistribute = totalDamageToDistribute,
                                minDamagePerTarget = minDamagePerTarget,
                                autoTapPreview = beholdPathInfo.second
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
                if (evokeCostResult != null) {
                    result.add(LegalAction(
                        actionType = "CastWithAlternativeCost",
                        description = "Evoke ${cardComponent.name} (${evokeCostResult.manaCostString})",
                        action = CastSpell(playerId, cardId, useAlternativeCost = true),
                        manaCostString = evokeCostResult.manaCostString,
                        autoTapPreview = evokeCostResult.autoTapPreview
                    ))
                }
                if (blightPathInfo != null) {
                    result.add(LegalAction(
                        actionType = "CastSpell",
                        description = "Cast ${cardComponent.name} (Blight ${blightOrPayCost!!.blightAmount})",
                        action = CastSpell(playerId, cardId),
                        additionalCostInfo = blightPathInfo.third,
                        manaCostString = blightPathInfo.first,
                        autoTapPreview = blightPathInfo.second
                    ))
                }
                if (beholdPathInfo != null) {
                    result.add(LegalAction(
                        actionType = "CastSpell",
                        description = "Cast ${cardComponent.name} (Behold)",
                        action = CastSpell(playerId, cardId),
                        additionalCostInfo = beholdPathInfo.third,
                        manaCostString = beholdPathInfo.first,
                        autoTapPreview = beholdPathInfo.second
                    ))
                }
            }
        }

        // --- Kicker ---
        enumerateKicker(context, hand, result)

        // --- Conspire ---
        enumerateConspire(context, hand, result)

        return result
    }

    /**
     * Enumerates a "Cast with Conspire" variant for each spell in hand that has Conspire
     * (printed or granted by a permanent in play via [GrantKeywordToOwnSpells]) and for which
     * the caster controls at least two untapped creatures whose projected colors overlap with
     * the spell's. The two-creature selection is submitted as [CastSpell.conspiredCreatures].
     *
     * Skip colorless spells — a color-sharing creature cannot exist for them (CR 702.78).
     */
    private fun enumerateConspire(
        context: EnumerationContext,
        hand: List<EntityId>,
        result: MutableList<LegalAction>
    ) {
        val state = context.state
        val playerId = context.playerId
        if (context.cantCastSpells) return

        val projected = state.projectedState

        for (cardId in hand) {
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
            if (cardComponent.typeLine.isLand) continue

            val cardDef = context.cardRegistry.getCard(cardComponent.name) ?: continue
            if (!context.grantedKeywordResolver.hasKeyword(state, playerId, cardDef, Keyword.CONSPIRE)) continue

            val spellColors = cardDef.colors
            if (spellColors.isEmpty()) continue

            // Check timing (same rules as normal cast)
            val isInstant = cardComponent.typeLine.isInstant
            val hasFlash = cardDef.keywords.contains(Keyword.FLASH)
            val grantedFlash = hasFlash || context.castPermissionUtils.hasGrantedFlash(state, cardId)
            if (!isInstant && !grantedFlash && !context.canPlaySorcerySpeed) continue

            val castRestrictions = cardDef.script.castRestrictions
            if (castRestrictions.isNotEmpty() && !context.castPermissionUtils.checkCastRestrictions(state, playerId, castRestrictions)) continue

            // Gather controlled, untapped creatures that share at least one color with the spell.
            val eligibleTapTargets = mutableListOf<EntityId>()
            for (permId in state.getBattlefield()) {
                val permContainer = state.getEntity(permId) ?: continue
                if (projected.getController(permId) != playerId) continue
                if (!projected.isCreature(permId)) continue
                if (permContainer.has<com.wingedsheep.engine.state.components.battlefield.TappedComponent>()) continue
                if (spellColors.none { projected.hasColor(permId, it) }) continue
                eligibleTapTargets.add(permId)
            }
            if (eligibleTapTargets.size < 2) continue

            val baseCost = context.costCalculator.calculateEffectiveCost(state, cardDef, playerId)
            val spellContext = SpellPaymentContext(
                isInstantOrSorcery = cardComponent.typeLine.isInstant || cardComponent.typeLine.isSorcery,
                isKicked = false,
                isCreature = cardComponent.typeLine.isCreature,
                manaValue = cardComponent.manaCost.cmc,
                hasXInCost = cardComponent.manaCost.hasX,
                subtypes = cardComponent.typeLine.subtypes.map { it.value }.toSet()
            )
            val canAfford = context.manaSolver.canPay(state, playerId, baseCost, spellContext = spellContext, precomputedSources = context.availableManaSources)
            val autoTapPreview = if (context.skipAutoTapPreview) null else {
                context.manaSolver.solve(state, playerId, baseCost, spellContext = spellContext, precomputedSources = context.availableManaSources)
                    ?.sources?.map { it.entityId }
            }

            val targetReqs = buildList {
                addAll(cardDef.script.targetRequirements)
                cardDef.script.auraTarget?.let { add(it) }
            }

            val conspireCostInfo = AdditionalCostData(
                description = "Tap two untapped creatures you control that share a color with this spell",
                costType = "Conspire",
                validTapTargets = eligibleTapTargets,
                tapCount = 2
            )

            if (targetReqs.isNotEmpty()) {
                val targetReqInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs)
                val allRequirementsSatisfied = context.targetUtils.allRequirementsSatisfied(targetReqInfos)
                if (!allRequirementsSatisfied) continue
                val firstReq = targetReqs.first()
                val firstReqInfo = targetReqInfos.first()
                result.add(LegalAction(
                    actionType = "CastWithConspire",
                    description = "Cast ${cardComponent.name} (Conspire)",
                    action = CastSpell(playerId, cardId),
                    validTargets = firstReqInfo.validTargets,
                    requiresTargets = true,
                    targetCount = firstReq.count,
                    minTargets = firstReq.effectiveMinCount,
                    targetDescription = firstReq.description,
                    targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                    affordable = canAfford,
                    manaCostString = baseCost.toString(),
                    autoTapPreview = autoTapPreview,
                    additionalCostInfo = conspireCostInfo
                ))
            } else {
                result.add(LegalAction(
                    actionType = "CastWithConspire",
                    description = "Cast ${cardComponent.name} (Conspire)",
                    action = CastSpell(playerId, cardId),
                    affordable = canAfford,
                    manaCostString = baseCost.toString(),
                    autoTapPreview = autoTapPreview,
                    additionalCostInfo = conspireCostInfo
                ))
            }
        }
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
                hasXInCost = cardComponent.manaCost.hasX,
                subtypes = cardComponent.typeLine.subtypes.map { it.value }.toSet()
            )
            val canAffordKickedMana = context.manaSolver.canPay(state, playerId, kickedCost, spellContext = kickedSpellContext, precomputedSources = context.availableManaSources)
            val kickedCostString = kickedCost.toString()
            val kickedAutoTapPreview = if (context.skipAutoTapPreview) null else {
                context.manaSolver.solve(state, playerId, kickedCost, spellContext = kickedSpellContext, precomputedSources = context.availableManaSources)
                    ?.sources?.map { it.entityId }
            }

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
            if (!context.manaSolver.canPay(state, playerId, baseCost, precomputedSources = context.availableManaSources)) {
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
            val flatCosts = additionalCosts.flatMap { if (it is AdditionalCost.Composite) it.steps else listOf(it) }
            val beholdCost = flatCosts.filterIsInstance<AdditionalCost.Behold>().firstOrNull()
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

    /**
     * Internal per-mode enumeration snapshot for modal spells.
     *
     * Captures affordability, additional-cost payability, target infos, and
     * rendering fields for a single printed mode. Shared between the choose-1
     * and choose-N emission paths.
     */
    private data class ModeEnumeration(
        val modeIndex: Int,
        val mode: Mode,
        val effectiveCost: ManaCost,
        val manaCostString: String,
        val canAffordMana: Boolean,
        val canPayAdditionalCosts: Boolean,
        val additionalCostInfo: AdditionalCostData?,
        val autoTapPreview: List<EntityId>?,
        val targetInfos: List<TargetInfo>,
        val allTargetRequirementsSatisfied: Boolean
    ) {
        /** True when this mode is both payable and has its target requirements satisfied (700.2a). */
        val available: Boolean
            get() = canAffordMana && canPayAdditionalCosts && allTargetRequirementsSatisfied
    }

    /**
     * Compute a [ModeEnumeration] for a single printed mode.
     *
     * Evaluates per-mode cost deltas ([Mode.additionalManaCost]), per-mode additional
     * costs ([Mode.additionalCosts]), and target legality. Always returns an
     * enumeration — callers decide whether `available = false` means "skip this mode"
     * (choose-1) or "offer it greyed-out / non-pickable" (choose-N, rules 700.2a).
     */
    private fun computeModeEnumeration(
        context: EnumerationContext,
        cardId: EntityId,
        playerId: EntityId,
        modeIndex: Int,
        mode: Mode,
        baseEffectiveCost: ManaCost,
        cardLevelAdditionalCostInfo: AdditionalCostData?,
        baseAutoTapPreview: List<EntityId>?,
        spellContext: SpellPaymentContext,
        cachedSources: List<ManaSource>
    ): ModeEnumeration {
        val state = context.state

        val modeExtraManaCost = mode.additionalManaCost
        val modeEffectiveCost = if (modeExtraManaCost != null) {
            baseEffectiveCost + ManaCost.parse(modeExtraManaCost)
        } else {
            baseEffectiveCost
        }
        val canAffordMana = if (modeExtraManaCost != null) {
            context.manaSolver.canPay(
                state, playerId, modeEffectiveCost,
                spellContext = spellContext,
                precomputedSources = cachedSources
            )
        } else {
            true // base cost already checked upstream
        }

        var canPayAdditionalCosts = true
        val modeSacrificeTargets = mutableListOf<EntityId>()
        var modeExileTargets = emptyList<EntityId>()
        var modeExileMinCount = 0
        var modeDiscardTargets = emptyList<EntityId>()
        var modeDiscardCount = 0

        val modeAdditionalCosts = mode.additionalCosts
        if (modeAdditionalCosts != null) {
            for (cost in modeAdditionalCosts) {
                when (cost) {
                    is AdditionalCost.SacrificePermanent -> {
                        val validSacTargets = context.costUtils.findSacrificeTargets(state, playerId, cost)
                        if (validSacTargets.size < cost.count) canPayAdditionalCosts = false
                        modeSacrificeTargets.addAll(validSacTargets)
                    }
                    is AdditionalCost.ExileCards -> {
                        val validExileTargets = context.costUtils.findExileTargets(state, playerId, cost.filter, cost.fromZone)
                        if (validExileTargets.size < cost.count) canPayAdditionalCosts = false
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
                        if (validDiscards.size < cost.count) canPayAdditionalCosts = false
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
                        if (graveyardSize < 3 && !hasFood) canPayAdditionalCosts = false
                    }
                    else -> {}
                }
            }
        }

        val modeCostInfo = if (modeAdditionalCosts != null) {
            buildAdditionalCostData(
                modeAdditionalCosts, modeSacrificeTargets, emptyList(),
                modeExileTargets, modeExileMinCount, modeDiscardTargets, modeDiscardCount
            )
        } else {
            cardLevelAdditionalCostInfo
        }

        val modeManaCostString = modeEffectiveCost.toString()
        val modeAutoTapPreview = if (context.skipAutoTapPreview) null
        else if (modeExtraManaCost != null) {
            context.manaSolver.solve(state, playerId, modeEffectiveCost, precomputedSources = cachedSources)
                ?.sources?.map { it.entityId }
        } else {
            baseAutoTapPreview
        }

        val modeTargetReqs = mode.targetRequirements
        val modeTargetInfos = if (modeTargetReqs.isNotEmpty()) {
            context.targetUtils.buildTargetInfos(state, playerId, modeTargetReqs)
        } else {
            emptyList()
        }
        val allTargetRequirementsSatisfied = modeTargetReqs.isEmpty() ||
            context.targetUtils.allRequirementsSatisfied(modeTargetInfos)

        return ModeEnumeration(
            modeIndex = modeIndex,
            mode = mode,
            effectiveCost = modeEffectiveCost,
            manaCostString = modeManaCostString,
            canAffordMana = canAffordMana,
            canPayAdditionalCosts = canPayAdditionalCosts,
            additionalCostInfo = modeCostInfo,
            autoTapPreview = modeAutoTapPreview,
            targetInfos = modeTargetInfos,
            allTargetRequirementsSatisfied = allTargetRequirementsSatisfied
        )
    }
}
