package com.wingedsheep.engine.handlers.actions.spell

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.CastWithCreatureTypeContinuation
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ManaSpentEvent
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.PermanentsSacrificedEvent
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.core.TurnManager
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.event.TriggerProcessor
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.CostHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.actions.ActionContext
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.mechanics.mana.AlternativePaymentHandler
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.mechanics.targeting.TargetValidator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.MayPlayFromExileComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.CastRestriction
import com.wingedsheep.sdk.scripting.effects.DividedDamageEffect
import com.wingedsheep.sdk.scripting.effects.StormCopyEffect
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import com.wingedsheep.sdk.scripting.GrantMayCastFromLinkedExile
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayFromTopOfLibrary
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.toEntityId
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import kotlin.reflect.KClass

/**
 * Handler for the CastSpell action.
 *
 * This is the most complex handler, handling:
 * - Timing validation
 * - Cast restrictions
 * - Additional costs (sacrifice, etc.)
 * - Alternative payments (Delve, Convoke)
 * - Three payment strategies (AutoPay, FromPool, Explicit)
 * - Target validation
 * - Morph/face-down casting
 * - Trigger detection
 */
class CastSpellHandler(
    private val cardRegistry: CardRegistry?,
    private val turnManager: TurnManager,
    private val manaSolver: ManaSolver,
    private val costCalculator: CostCalculator,
    private val alternativePaymentHandler: AlternativePaymentHandler,
    private val costHandler: CostHandler,
    private val stackResolver: StackResolver,
    private val targetValidator: TargetValidator,
    private val conditionEvaluator: ConditionEvaluator,
    private val triggerDetector: TriggerDetector,
    private val triggerProcessor: TriggerProcessor,
) : ActionHandler<CastSpell> {
    override val actionType: KClass<CastSpell> = CastSpell::class

    private val predicateEvaluator = PredicateEvaluator()

    override fun validate(state: GameState, action: CastSpell): String? {
        if (state.priorityPlayerId != action.playerId) {
            return "You don't have priority"
        }

        val container = state.getEntity(action.cardId)
            ?: return "Card not found: ${action.cardId}"

        val cardComponent = container.get<CardComponent>()
            ?: return "Not a card: ${action.cardId}"

        val handZone = ZoneKey(action.playerId, Zone.HAND)
        val inHand = action.cardId in state.getZone(handZone)
        val onTopOfLibrary = !inHand && isOnTopOfLibraryWithPermission(state, action.playerId, action.cardId)
        val mayPlayFromExile = !inHand && !onTopOfLibrary && isInExileWithPlayPermission(state, action.playerId, action.cardId)
        val mayCastFromZone = !inHand && !onTopOfLibrary && !mayPlayFromExile &&
            hasMayCastSelfFromZonePermission(state, action.playerId, action.cardId)
        if (!inHand && !onTopOfLibrary && !mayPlayFromExile && !mayCastFromZone) {
            return "Card is not in your hand"
        }

        val cardDef = cardRegistry?.getCard(cardComponent.cardDefinitionId)

        // Handle face-down casting (morph)
        if (action.castFaceDown) {
            val morphAbility = cardDef?.keywordAbilities?.filterIsInstance<KeywordAbility.Morph>()?.firstOrNull()
                ?: return "This card cannot be cast face down (no morph ability)"

            if (!turnManager.canPlaySorcerySpeed(state, action.playerId)) {
                return "You can only cast face-down creatures at sorcery speed"
            }

            val morphCastCost = costCalculator.calculateFaceDownCost(state, action.playerId)
            return validatePayment(state, action, morphCastCost)
        }

        // Check timing
        if (!cardComponent.typeLine.isInstant) {
            val hasFlash = cardDef?.keywords?.contains(Keyword.FLASH) == true
            val grantedFlash = hasFlash || hasGrantedFlash(state, action.cardId)
            if (!grantedFlash && !turnManager.canPlaySorcerySpeed(state, action.playerId)) {
                return "You can only cast sorcery-speed spells during your main phase with an empty stack"
            }
        }

        // Check cast restrictions
        if (cardDef != null && cardDef.script.castRestrictions.isNotEmpty()) {
            val restrictionError = validateCastRestrictions(state, cardDef.script.castRestrictions, action.playerId)
            if (restrictionError != null) {
                return restrictionError
            }
        }

        // Validate additional costs
        if (cardDef != null) {
            val additionalCostError = validateAdditionalCosts(state, cardDef.script.additionalCosts, action)
            if (additionalCostError != null) {
                return additionalCostError
            }
        }

        // Validate kicker: card must have Kicker or KickerWithAdditionalCost keyword ability
        if (action.wasKicked && cardDef != null) {
            val hasKicker = cardDef.keywordAbilities.any { it is KeywordAbility.Kicker || it is KeywordAbility.KickerWithAdditionalCost }
            if (!hasKicker) return "This card does not have kicker"

            // Validate kicker additional cost (sacrifice, etc.)
            val kickerAdditionalCost = cardDef.keywordAbilities
                .filterIsInstance<KeywordAbility.KickerWithAdditionalCost>()
                .firstOrNull()
            if (kickerAdditionalCost != null) {
                val kickerCostError = validateAdditionalCosts(state, listOf(kickerAdditionalCost.cost), action)
                if (kickerCostError != null) return kickerCostError
            }
        }

        // Calculate effective cost (free if PlayWithoutPayingCostComponent is present)
        val playForFree = hasPlayWithoutPayingCost(state, action.playerId, action.cardId)
        var effectiveCost = if (playForFree) {
            ManaCost.ZERO
        } else if (action.useAlternativeCost && cardDef != null) {
            // Use alternative casting cost (e.g., Jodah's {W}{U}{B}{R}{G})
            val altCosts = costCalculator.findAlternativeCastingCosts(state, action.playerId)
            if (altCosts.isEmpty()) return "No alternative casting cost available"
            costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, altCosts.first())
        } else if (cardDef != null) {
            costCalculator.calculateEffectiveCost(state, cardDef, action.playerId)
        } else {
            cardComponent.manaCost
        }

        // Add kicker mana cost if kicked (only for mana-based kicker)
        if (action.wasKicked && !playForFree && !action.useAlternativeCost && cardDef != null) {
            val kickerAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Kicker>().firstOrNull()
            if (kickerAbility != null) {
                effectiveCost = ManaCost(effectiveCost.symbols + kickerAbility.cost.symbols)
            }
        }

        // Apply sacrifice-for-cost-reduction before validating payment
        if (cardDef != null && action.additionalCostPayment != null) {
            for (cost in cardDef.script.additionalCosts) {
                if (cost is AdditionalCost.SacrificeCreaturesForCostReduction) {
                    val sacrificeCount = action.additionalCostPayment.sacrificedPermanents.size
                    val reduction = sacrificeCount * cost.costReductionPerCreature
                    if (reduction > 0) {
                        effectiveCost = effectiveCost.reduceGeneric(reduction)
                    }
                }
            }
        }

        // Account for Delve/Convoke reduction before validating payment
        val costAfterAltPayment = if (action.alternativePayment != null && !action.alternativePayment.isEmpty && cardDef != null) {
            alternativePaymentHandler.calculateReducedCost(effectiveCost, action.alternativePayment, cardDef)
        } else {
            effectiveCost
        }

        // Validate payment
        val paymentError = validatePayment(state, action, costAfterAltPayment)
        if (paymentError != null) {
            return paymentError
        }

        // Validate targets (include auraTarget as a target requirement for aura spells)
        // Use kickerTargetRequirements when spell is kicked and alternate targets are defined
        if (cardDef != null) {
            val baseTargetReqs = if (action.wasKicked && cardDef.script.kickerTargetRequirements.isNotEmpty()) {
                cardDef.script.kickerTargetRequirements
            } else {
                cardDef.script.targetRequirements
            }
            val targetRequirements = buildList {
                addAll(baseTargetReqs)
                cardDef.script.auraTarget?.let { add(it) }
            }
            if (targetRequirements.isNotEmpty()) {
                // Reject casting if spell requires targets but none were provided
                if (action.targets.isEmpty()) {
                    val requiredCount = targetRequirements.sumOf { it.effectiveMinCount }
                    if (requiredCount > 0) {
                        return "No valid targets available"
                    }
                }
                val targetError = targetValidator.validateTargets(
                    state,
                    action.targets,
                    targetRequirements,
                    action.playerId,
                    sourceColors = cardDef.colors,
                    sourceSubtypes = cardDef.typeLine.subtypes.map { it.value }.toSet(),
                    sourceId = action.cardId
                )
                if (targetError != null) {
                    return targetError
                }
            }
        }

        // Validate damage distribution for DividedDamageEffect spells
        // Use kickerSpellEffect when kicked and available
        val spellEffect = if (action.wasKicked && cardDef?.script?.kickerSpellEffect != null) {
            cardDef.script.kickerSpellEffect
        } else {
            cardDef?.script?.spellEffect
        }
        if (spellEffect is DividedDamageEffect && action.targets.size > 1) {
            val distribution = action.damageDistribution
            if (distribution == null) {
                return "Damage distribution required for this spell when targeting multiple creatures"
            }

            // Check that distribution targets match chosen targets
            val targetIds = action.targets.map { it.toEntityId() }.toSet()
            val distributionTargets = distribution.keys
            if (distributionTargets != targetIds) {
                return "Damage distribution targets must match chosen targets"
            }

            // Check that total damage equals the spell's total damage
            val totalDistributed = distribution.values.sum()
            if (totalDistributed != spellEffect.totalDamage) {
                return "Total distributed damage ($totalDistributed) must equal ${spellEffect.totalDamage}"
            }

            // Check that each target gets at least 1 damage (per MTG rules)
            val minPerTarget = 1
            for ((targetId, damage) in distribution) {
                if (damage < minPerTarget) {
                    return "Each target must receive at least $minPerTarget damage"
                }
            }
        }

        return null
    }

    private fun validatePayment(state: GameState, action: CastSpell, cost: ManaCost): String? {
        val xValue = action.xValue ?: 0

        return when (action.paymentStrategy) {
            is PaymentStrategy.AutoPay -> {
                if (!manaSolver.canPay(state, action.playerId, cost, xValue)) {
                    "Not enough mana to cast this spell"
                } else null
            }
            is PaymentStrategy.FromPool -> {
                val poolComponent = state.getEntity(action.playerId)?.get<ManaPoolComponent>()
                    ?: ManaPoolComponent()
                val pool = ManaPool(
                    white = poolComponent.white,
                    blue = poolComponent.blue,
                    black = poolComponent.black,
                    red = poolComponent.red,
                    green = poolComponent.green,
                    colorless = poolComponent.colorless
                )
                if (!costHandler.canPayManaCost(pool, cost)) {
                    "Insufficient mana in pool to cast this spell"
                } else null
            }
            is PaymentStrategy.Explicit -> {
                for (sourceId in action.paymentStrategy.manaAbilitiesToActivate) {
                    val sourceContainer = state.getEntity(sourceId)
                        ?: return "Mana source not found: $sourceId"
                    if (sourceContainer.has<TappedComponent>()) {
                        return "Mana source is already tapped: $sourceId"
                    }
                }
                null
            }
        }
    }

    private fun validateCastRestrictions(
        state: GameState,
        restrictions: List<CastRestriction>,
        playerId: EntityId
    ): String? {
        val opponentId = state.turnOrder.firstOrNull { it != playerId }
        val context = EffectContext(
            sourceId = null,
            controllerId = playerId,
            opponentId = opponentId,
            targets = emptyList(),
            xValue = 0
        )

        for (restriction in restrictions) {
            val error = validateSingleRestriction(state, restriction, context)
            if (error != null) return error
        }
        return null
    }

    private fun validateSingleRestriction(
        state: GameState,
        restriction: CastRestriction,
        context: EffectContext
    ): String? {
        return when (restriction) {
            is CastRestriction.OnlyDuringStep -> {
                if (state.step != restriction.step) {
                    "Can only be cast during the ${restriction.step.name.lowercase().replace('_', ' ')} step"
                } else null
            }
            is CastRestriction.OnlyDuringPhase -> {
                if (state.phase != restriction.phase) {
                    "Can only be cast during the ${restriction.phase.name.lowercase().replace('_', ' ')} phase"
                } else null
            }
            is CastRestriction.OnlyIfCondition -> {
                if (!conditionEvaluator.evaluate(state, restriction.condition, context)) {
                    "Casting condition not met"
                } else null
            }
            is CastRestriction.TimingRequirement -> null
            is CastRestriction.All -> {
                for (subRestriction in restriction.restrictions) {
                    val error = validateSingleRestriction(state, subRestriction, context)
                    if (error != null) return error
                }
                null
            }
        }
    }

    private fun validateAdditionalCosts(
        state: GameState,
        additionalCosts: List<AdditionalCost>,
        action: CastSpell
    ): String? {
        val projected = state.projectedState
        for (additionalCost in additionalCosts) {
            when (additionalCost) {
                is AdditionalCost.SacrificePermanent -> {
                    val sacrificed = action.additionalCostPayment?.sacrificedPermanents ?: emptyList()
                    val filterDesc = additionalCost.filter.description
                    if (sacrificed.size < additionalCost.count) {
                        return "You must sacrifice ${additionalCost.count} $filterDesc to cast this spell"
                    }
                    for (permId in sacrificed) {
                        val permContainer = state.getEntity(permId)
                            ?: return "Sacrificed permanent not found: $permId"
                        val permCard = permContainer.get<CardComponent>()
                            ?: return "Sacrificed entity is not a card: $permId"
                        val permController = projected.getController(permId)
                        if (permController != action.playerId) {
                            return "You can only sacrifice permanents you control"
                        }
                        if (permId !in state.getBattlefield()) {
                            return "Sacrificed permanent is not on the battlefield: $permId"
                        }
                        // Use unified filter with projected state
                        val context = PredicateContext(controllerId = action.playerId)
                        val matches = predicateEvaluator.matchesWithProjection(state, projected, permId, additionalCost.filter, context)
                        if (!matches) {
                            return "${permCard.name} doesn't match the required filter: $filterDesc"
                        }
                    }
                }
                is AdditionalCost.ExileVariableCards -> {
                    val exiled = action.additionalCostPayment?.exiledCards ?: emptyList()
                    if (exiled.size < additionalCost.minCount) {
                        return "You must exile at least ${additionalCost.minCount} ${additionalCost.filter.description}(s) from your ${additionalCost.fromZone.description}"
                    }
                    val zone = when (additionalCost.fromZone) {
                        com.wingedsheep.sdk.scripting.CostZone.GRAVEYARD -> Zone.GRAVEYARD
                        com.wingedsheep.sdk.scripting.CostZone.HAND -> Zone.HAND
                        com.wingedsheep.sdk.scripting.CostZone.LIBRARY -> Zone.LIBRARY
                        com.wingedsheep.sdk.scripting.CostZone.BATTLEFIELD -> Zone.BATTLEFIELD
                    }
                    val zoneKey = ZoneKey(action.playerId, zone)
                    val zoneCards = state.getZone(zoneKey)
                    val context = PredicateContext(controllerId = action.playerId)
                    for (cardId in exiled) {
                        if (cardId !in zoneCards) {
                            return "Card to exile is not in your ${additionalCost.fromZone.description}"
                        }
                        if (!predicateEvaluator.matchesWithProjection(state, projected, cardId, additionalCost.filter, context)) {
                            val cardName = state.getEntity(cardId)?.get<CardComponent>()?.name ?: "Card"
                            return "$cardName doesn't match the required filter: ${additionalCost.filter.description}"
                        }
                    }
                }
                is AdditionalCost.ExileCards -> {
                    val exiled = action.additionalCostPayment?.exiledCards ?: emptyList()
                    if (exiled.size < additionalCost.count) {
                        return "You must exile ${additionalCost.count} ${additionalCost.filter.description}(s) from your ${additionalCost.fromZone.description}"
                    }
                    val zone = when (additionalCost.fromZone) {
                        com.wingedsheep.sdk.scripting.CostZone.GRAVEYARD -> Zone.GRAVEYARD
                        com.wingedsheep.sdk.scripting.CostZone.HAND -> Zone.HAND
                        com.wingedsheep.sdk.scripting.CostZone.LIBRARY -> Zone.LIBRARY
                        com.wingedsheep.sdk.scripting.CostZone.BATTLEFIELD -> Zone.BATTLEFIELD
                    }
                    val zoneKey = ZoneKey(action.playerId, zone)
                    val zoneCards = state.getZone(zoneKey)
                    val context = PredicateContext(controllerId = action.playerId)
                    for (cardId in exiled) {
                        if (cardId !in zoneCards) {
                            return "Card to exile is not in your ${additionalCost.fromZone.description}"
                        }
                        if (!predicateEvaluator.matchesWithProjection(state, projected, cardId, additionalCost.filter, context)) {
                            val cardName = state.getEntity(cardId)?.get<CardComponent>()?.name ?: "Card"
                            return "$cardName doesn't match the required filter: ${additionalCost.filter.description}"
                        }
                    }
                }
                is AdditionalCost.DiscardCards -> {
                    val discarded = action.additionalCostPayment?.discardedCards ?: emptyList()
                    if (discarded.size < additionalCost.count) {
                        return "You must discard ${additionalCost.count} card(s) to cast this spell"
                    }
                    val handZone = ZoneKey(action.playerId, Zone.HAND)
                    val handCards = state.getZone(handZone)
                    val context = PredicateContext(controllerId = action.playerId)
                    for (cardId in discarded) {
                        if (cardId !in handCards) {
                            return "Card to discard is not in your hand"
                        }
                        if (cardId == action.cardId) {
                            return "Cannot discard the spell being cast"
                        }
                        if (additionalCost.filter != com.wingedsheep.sdk.scripting.GameObjectFilter.Any) {
                            if (!predicateEvaluator.matches(state, cardId, additionalCost.filter, context)) {
                                val cardName = state.getEntity(cardId)?.get<CardComponent>()?.name ?: "Card"
                                return "$cardName doesn't match the required filter: ${additionalCost.filter.description}"
                            }
                        }
                    }
                }
                is AdditionalCost.SacrificeCreaturesForCostReduction -> {
                    // Sacrificing 0 creatures is valid (optional sacrifice)
                    val sacrificed = action.additionalCostPayment?.sacrificedPermanents ?: emptyList()
                    for (permId in sacrificed) {
                        val permContainer = state.getEntity(permId)
                            ?: return "Sacrificed permanent not found: $permId"
                        val permCard = permContainer.get<CardComponent>()
                            ?: return "Sacrificed entity is not a card: $permId"
                        val permController = projected.getController(permId)
                        if (permController != action.playerId) {
                            return "You can only sacrifice permanents you control"
                        }
                        if (permId !in state.getBattlefield()) {
                            return "Sacrificed permanent is not on the battlefield: $permId"
                        }
                        val context = PredicateContext(controllerId = action.playerId)
                        val matches = predicateEvaluator.matchesWithProjection(state, projected, permId, additionalCost.filter, context)
                        if (!matches) {
                            return "${permCard.name} doesn't match the required filter: ${additionalCost.filter.description}"
                        }
                    }
                }
                else -> {}
            }
        }
        return null
    }

    override fun execute(state: GameState, action: CastSpell): ExecutionResult {
        var currentState = state
        val events = mutableListOf<GameEvent>()

        val cardComponent = state.getEntity(action.cardId)?.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Card not found")

        val xValue = action.xValue ?: 0
        val cardDef = cardRegistry?.getCard(cardComponent.cardDefinitionId)

        // Calculate effective cost (free if PlayWithoutPayingCostComponent is present)
        val playForFreeInExecute = hasPlayWithoutPayingCost(currentState, action.playerId, action.cardId)
        var effectiveCost = if (playForFreeInExecute) {
            ManaCost.ZERO
        } else if (action.useAlternativeCost && cardDef != null) {
            val altCosts = costCalculator.findAlternativeCastingCosts(currentState, action.playerId)
            if (altCosts.isNotEmpty()) {
                costCalculator.calculateEffectiveCostWithAlternativeBase(currentState, cardDef, altCosts.first())
            } else {
                cardComponent.manaCost
            }
        } else if (action.castFaceDown) {
            costCalculator.calculateFaceDownCost(currentState, action.playerId)
        } else if (cardDef != null) {
            costCalculator.calculateEffectiveCost(currentState, cardDef, action.playerId)
        } else {
            cardComponent.manaCost
        }

        // Add kicker cost if kicked (not applicable with alternative costs)
        if (action.wasKicked && !playForFreeInExecute && !action.useAlternativeCost && cardDef != null) {
            val kickerAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Kicker>().firstOrNull()
            if (kickerAbility != null) {
                effectiveCost = ManaCost(effectiveCost.symbols + kickerAbility.cost.symbols)
            }
        }

        // Process additional costs (sacrifice, exile, etc.)
        val sacrificedPermanentIds = mutableListOf<EntityId>()
        val sacrificedPermanentSubtypes = mutableMapOf<EntityId, Set<String>>()
        var exiledCardCount = 0

        // Collect all additional costs: script costs + kicker additional cost (if kicked)
        val allAdditionalCosts = buildList {
            if (cardDef != null) addAll(cardDef.script.additionalCosts)
            if (action.wasKicked && cardDef != null) {
                val kickerAdditionalCost = cardDef.keywordAbilities
                    .filterIsInstance<KeywordAbility.KickerWithAdditionalCost>()
                    .firstOrNull()
                if (kickerAdditionalCost != null) add(kickerAdditionalCost.cost)
            }
        }

        if (allAdditionalCosts.isNotEmpty() && action.additionalCostPayment != null) {
            for (additionalCost in allAdditionalCosts) {
                when (additionalCost) {
                    is AdditionalCost.SacrificePermanent -> {
                        // Project state to capture text-changed subtypes before sacrifice
                        val projectedBeforeSacrifice = currentState.projectedState
                        for (permId in action.additionalCostPayment.sacrificedPermanents) {
                            val permContainer = currentState.getEntity(permId) ?: continue
                            val permCard = permContainer.get<CardComponent>() ?: continue
                            val controllerId = permContainer.get<ControllerComponent>()?.playerId ?: action.playerId
                            val ownerId = permCard.ownerId ?: action.playerId
                            val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
                            val graveyardZone = ZoneKey(ownerId, Zone.GRAVEYARD)

                            // Snapshot projected subtypes before zone change
                            val projectedSubtypes = projectedBeforeSacrifice.getSubtypes(permId)
                            if (projectedSubtypes.isNotEmpty()) {
                                sacrificedPermanentSubtypes[permId] = projectedSubtypes
                            }

                            currentState = currentState.removeFromZone(battlefieldZone, permId)
                            currentState = currentState.addToZone(graveyardZone, permId)

                            sacrificedPermanentIds.add(permId)

                            events.add(PermanentsSacrificedEvent(action.playerId, listOf(permId), listOf(permCard.name)))
                            events.add(ZoneChangeEvent(
                                entityId = permId,
                                entityName = permCard.name,
                                fromZone = Zone.BATTLEFIELD,
                                toZone = Zone.GRAVEYARD,
                                ownerId = ownerId
                            ))
                        }
                    }
                    is AdditionalCost.DiscardCards -> {
                        val discardedCards = action.additionalCostPayment.discardedCards
                        for (cardId in discardedCards) {
                            val cardContainer = currentState.getEntity(cardId) ?: continue
                            val card = cardContainer.get<CardComponent>() ?: continue
                            val handZone = ZoneKey(action.playerId, Zone.HAND)
                            val graveyardZone = ZoneKey(action.playerId, Zone.GRAVEYARD)

                            currentState = currentState.removeFromZone(handZone, cardId)
                            currentState = currentState.addToZone(graveyardZone, cardId)

                            events.add(ZoneChangeEvent(
                                entityId = cardId,
                                entityName = card.name,
                                fromZone = Zone.HAND,
                                toZone = Zone.GRAVEYARD,
                                ownerId = action.playerId
                            ))
                        }
                        val discardNames = discardedCards.map { currentState.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
                        events.add(CardsDiscardedEvent(action.playerId, discardedCards, discardNames))
                    }
                    is AdditionalCost.ExileVariableCards, is AdditionalCost.ExileCards -> {
                        val exiledCards = action.additionalCostPayment.exiledCards
                        val fromZone = when (additionalCost) {
                            is AdditionalCost.ExileVariableCards -> additionalCost.fromZone
                            is AdditionalCost.ExileCards -> additionalCost.fromZone
                            else -> com.wingedsheep.sdk.scripting.CostZone.GRAVEYARD
                        }
                        val zone = when (fromZone) {
                            com.wingedsheep.sdk.scripting.CostZone.GRAVEYARD -> Zone.GRAVEYARD
                            com.wingedsheep.sdk.scripting.CostZone.HAND -> Zone.HAND
                            com.wingedsheep.sdk.scripting.CostZone.LIBRARY -> Zone.LIBRARY
                            com.wingedsheep.sdk.scripting.CostZone.BATTLEFIELD -> Zone.BATTLEFIELD
                        }
                        for (cardId in exiledCards) {
                            val cardContainer = currentState.getEntity(cardId) ?: continue
                            val card = cardContainer.get<CardComponent>() ?: continue
                            val sourceZone = ZoneKey(action.playerId, zone)
                            val exileZone = ZoneKey(action.playerId, Zone.EXILE)

                            currentState = currentState.removeFromZone(sourceZone, cardId)
                            currentState = currentState.addToZone(exileZone, cardId)

                            events.add(ZoneChangeEvent(
                                entityId = cardId,
                                entityName = card.name,
                                fromZone = zone,
                                toZone = Zone.EXILE,
                                ownerId = action.playerId
                            ))
                        }
                        exiledCardCount = exiledCards.size
                    }
                    is AdditionalCost.SacrificeCreaturesForCostReduction -> {
                        // Process sacrifices for cost reduction (e.g., Torgaar)
                        val projectedBeforeSacrifice = currentState.projectedState
                        for (permId in action.additionalCostPayment.sacrificedPermanents) {
                            val permContainer = currentState.getEntity(permId) ?: continue
                            val permCard = permContainer.get<CardComponent>() ?: continue
                            val controllerId = permContainer.get<ControllerComponent>()?.playerId ?: action.playerId
                            val ownerId = permCard.ownerId ?: action.playerId
                            val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
                            val graveyardZone = ZoneKey(ownerId, Zone.GRAVEYARD)

                            val projectedSubtypes = projectedBeforeSacrifice.getSubtypes(permId)
                            if (projectedSubtypes.isNotEmpty()) {
                                sacrificedPermanentSubtypes[permId] = projectedSubtypes
                            }

                            currentState = currentState.removeFromZone(battlefieldZone, permId)
                            currentState = currentState.addToZone(graveyardZone, permId)

                            sacrificedPermanentIds.add(permId)

                            events.add(PermanentsSacrificedEvent(action.playerId, listOf(permId), listOf(permCard.name)))
                            events.add(ZoneChangeEvent(
                                entityId = permId,
                                entityName = permCard.name,
                                fromZone = Zone.BATTLEFIELD,
                                toZone = Zone.GRAVEYARD,
                                ownerId = ownerId
                            ))
                        }
                        // Apply cost reduction based on number of creatures sacrificed
                        val reduction = action.additionalCostPayment.sacrificedPermanents.size * additionalCost.costReductionPerCreature
                        if (reduction > 0) {
                            effectiveCost = effectiveCost.reduceGeneric(reduction)
                        }
                    }
                    else -> {}
                }
            }
        }

        // Apply alternative payment (Delve/Convoke)
        if (action.alternativePayment != null && !action.alternativePayment.isEmpty && cardDef != null) {
            val altPaymentResult = alternativePaymentHandler.apply(
                currentState,
                effectiveCost,
                action.alternativePayment,
                action.playerId,
                cardDef
            )
            effectiveCost = altPaymentResult.reducedCost
            currentState = altPaymentResult.newState
            events.addAll(altPaymentResult.events)
        }

        // Handle mana payment based on strategy
        val paymentResult = processPayment(currentState, action, effectiveCost, cardComponent.name, xValue)
        if (paymentResult.error != null) {
            return ExecutionResult.error(currentState, paymentResult.error)
        }
        currentState = paymentResult.state
        events.addAll(paymentResult.events)

        // Compute target requirements for resolution-time re-validation (Rule 608.2b)
        // Use kickerTargetRequirements when spell is kicked and alternate targets are defined
        val spellTargetRequirements = if (cardDef != null) {
            val baseTargetReqs = if (action.wasKicked && cardDef.script.kickerTargetRequirements.isNotEmpty()) {
                cardDef.script.kickerTargetRequirements
            } else {
                cardDef.script.targetRequirements
            }
            buildList {
                addAll(baseTargetReqs)
                cardDef.script.auraTarget?.let { add(it) }
            }
        } else {
            emptyList()
        }

        // Check if spell requires a creature type choice during casting (e.g., Aphetto Dredging)
        val castTimeChoice = cardDef?.script?.castTimeCreatureTypeChoice
        if (castTimeChoice != null) {
            val pauseResult = pauseForCreatureTypeChoice(
                currentState, action, castTimeChoice, sacrificedPermanentIds, spellTargetRequirements, events
            )
            if (pauseResult != null) return pauseResult
        }

        // Capture storm count before incrementing (spells cast before this one)
        val stormCount = currentState.spellsCastThisTurn

        // Increment spell count for this turn (global and per-player)
        val playerCount = currentState.playerSpellsCastThisTurn[action.playerId] ?: 0
        currentState = currentState.copy(
            spellsCastThisTurn = stormCount + 1,
            playerSpellsCastThisTurn = currentState.playerSpellsCastThisTurn +
                (action.playerId to playerCount + 1)
        )

        // Track spell types cast this turn (for conditional evasion like Relic Runner)
        if (!action.castFaceDown) {
            val spellTypes = buildSet {
                add("ANY")
                if (cardComponent.typeLine.isCreature) add("CREATURE")
                if (!cardComponent.typeLine.isCreature) add("NONCREATURE")
                if (cardComponent.typeLine.isInstant || cardComponent.typeLine.isSorcery) add("INSTANT_OR_SORCERY")
                if (cardComponent.typeLine.isEnchantment) add("ENCHANTMENT")
                if (cardComponent.typeLine.isArtifact || cardComponent.typeLine.isLegendary) add("HISTORIC")
            }
            val existingTypes = currentState.spellTypesCastThisTurn[action.playerId] ?: emptySet()
            currentState = currentState.copy(
                spellTypesCastThisTurn = currentState.spellTypesCastThisTurn +
                    (action.playerId to existingTypes + spellTypes)
            )
        }

        // Cast the spell
        val castResult = stackResolver.castSpell(
            currentState,
            action.cardId,
            action.playerId,
            action.targets,
            action.xValue,
            sacrificedPermanentIds,
            sacrificedPermanentSubtypes,
            action.castFaceDown,
            action.damageDistribution,
            spellTargetRequirements,
            exiledCardCount = exiledCardCount,
            wasKicked = action.wasKicked
        )

        if (!castResult.isSuccess) {
            return castResult
        }

        var currentCastState = castResult.newState
        var allEvents = events + castResult.events

        // Handle Storm keyword: create a Storm triggered ability on the stack
        if (!action.castFaceDown && stormCount > 0 && cardDef != null && cardDef.hasKeyword(Keyword.STORM)) {
            val spellEffect = cardDef.script.spellEffect
            if (spellEffect != null) {
                val stormEffect = StormCopyEffect(
                    copyCount = stormCount,
                    spellEffect = spellEffect,
                    spellTargetRequirements = spellTargetRequirements,
                    spellName = cardComponent.name
                )
                val stormAbility = TriggeredAbilityOnStackComponent(
                    sourceId = action.cardId,
                    sourceName = cardComponent.name,
                    controllerId = action.playerId,
                    effect = stormEffect,
                    description = "Storm — copy ${cardComponent.name} $stormCount time(s)"
                )
                val stormResult = stackResolver.putTriggeredAbility(currentCastState, stormAbility)
                if (!stormResult.isSuccess) return stormResult
                currentCastState = stormResult.newState
                allEvents = allEvents + stormResult.events
            }
        }

        // Handle pending spell copies (e.g., Howl of the Horde) — copy next instant/sorcery
        if (!action.castFaceDown && cardComponent.typeLine.let { it.isInstant || it.isSorcery }) {
            val matchingCopies = currentCastState.pendingSpellCopies.filter { it.controllerId == action.playerId }
            if (matchingCopies.isNotEmpty()) {
                val totalCopies = matchingCopies.sumOf { it.copies }
                // Remove consumed pending copies
                val remainingPending = currentCastState.pendingSpellCopies.filter { it.controllerId != action.playerId }
                currentCastState = currentCastState.copy(pendingSpellCopies = remainingPending)

                // Create copies using Storm copy infrastructure
                val spellEffect = cardDef?.script?.spellEffect
                if (spellEffect != null && totalCopies > 0) {
                    val copyEffect = StormCopyEffect(
                        copyCount = totalCopies,
                        spellEffect = spellEffect,
                        spellTargetRequirements = spellTargetRequirements,
                        spellName = cardComponent.name
                    )
                    val copyAbility = TriggeredAbilityOnStackComponent(
                        sourceId = matchingCopies.first().sourceId,
                        sourceName = matchingCopies.first().sourceName,
                        controllerId = action.playerId,
                        effect = copyEffect,
                        description = "Copy ${cardComponent.name} $totalCopies time(s)"
                    )
                    val copyResult = stackResolver.putTriggeredAbility(currentCastState, copyAbility)
                    if (!copyResult.isSuccess) return copyResult
                    currentCastState = copyResult.newState
                    allEvents = allEvents + copyResult.events
                }
            }
        }

        // Detect and process triggers from casting (including additional cost events like sacrifice)
        val triggers = triggerDetector.detectTriggers(currentCastState, allEvents)
        if (triggers.isNotEmpty()) {
            val triggerResult = triggerProcessor.processTriggers(currentCastState, triggers)

            if (triggerResult.isPaused) {
                return ExecutionResult.paused(
                    triggerResult.state.withPriority(action.playerId),
                    triggerResult.pendingDecision!!,
                    allEvents + triggerResult.events
                )
            }

            allEvents = allEvents + triggerResult.events
            return ExecutionResult.success(
                triggerResult.newState.withPriority(action.playerId),
                allEvents
            )
        }

        return ExecutionResult.success(
            currentCastState.withPriority(action.playerId),
            allEvents
        )
    }

    private data class PaymentResult(
        val state: GameState,
        val events: List<GameEvent>,
        val error: String?
    )

    private fun processPayment(
        state: GameState,
        action: CastSpell,
        effectiveCost: ManaCost,
        cardName: String,
        xValue: Int
    ): PaymentResult {
        return when (action.paymentStrategy) {
            is PaymentStrategy.FromPool -> payFromPool(state, action.playerId, effectiveCost, cardName, xValue)
            is PaymentStrategy.AutoPay -> autoPay(state, action.playerId, effectiveCost, cardName, xValue)
            is PaymentStrategy.Explicit -> explicitPay(state, action.paymentStrategy, cardName)
        }
    }

    private fun payFromPool(
        state: GameState,
        playerId: EntityId,
        cost: ManaCost,
        cardName: String,
        xValue: Int
    ): PaymentResult {
        val poolComponent = state.getEntity(playerId)?.get<ManaPoolComponent>()
            ?: ManaPoolComponent()
        val pool = ManaPool(
            white = poolComponent.white,
            blue = poolComponent.blue,
            black = poolComponent.black,
            red = poolComponent.red,
            green = poolComponent.green,
            colorless = poolComponent.colorless
        )

        // Pay base cost first
        var poolAfterPayment = costHandler.payManaCost(pool, cost)
            ?: return PaymentResult(state, emptyList(), "Insufficient mana in pool")

        // Track mana spent for the event
        var whiteSpent = poolComponent.white - poolAfterPayment.white
        var blueSpent = poolComponent.blue - poolAfterPayment.blue
        var blackSpent = poolComponent.black - poolAfterPayment.black
        var redSpent = poolComponent.red - poolAfterPayment.red
        var greenSpent = poolComponent.green - poolAfterPayment.green
        var colorlessSpent = poolComponent.colorless - poolAfterPayment.colorless

        // Pay for X from remaining pool (multiply by X symbol count for XX costs)
        val xSymbolCount = cost.xCount.coerceAtLeast(1)
        var xRemainingToPay = xValue * xSymbolCount

        // Spend colorless first for X
        while (xRemainingToPay > 0 && poolAfterPayment.colorless > 0) {
            poolAfterPayment = poolAfterPayment.spendColorless()!!
            colorlessSpent++
            xRemainingToPay--
        }

        // Spend colored mana for remaining X
        for (color in Color.entries) {
            while (xRemainingToPay > 0 && poolAfterPayment.get(color) > 0) {
                poolAfterPayment = poolAfterPayment.spend(color)!!
                when (color) {
                    Color.WHITE -> whiteSpent++
                    Color.BLUE -> blueSpent++
                    Color.BLACK -> blackSpent++
                    Color.RED -> redSpent++
                    Color.GREEN -> greenSpent++
                }
                xRemainingToPay--
            }
        }

        // Check if we could pay for all of X
        if (xRemainingToPay > 0) {
            return PaymentResult(state, emptyList(), "Insufficient mana in pool for X cost")
        }

        val newState = state.updateEntity(playerId) { container ->
            container.with(
                ManaPoolComponent(
                    white = poolAfterPayment.white,
                    blue = poolAfterPayment.blue,
                    black = poolAfterPayment.black,
                    red = poolAfterPayment.red,
                    green = poolAfterPayment.green,
                    colorless = poolAfterPayment.colorless
                )
            )
        }

        val event = ManaSpentEvent(
            playerId = playerId,
            reason = "Cast $cardName",
            white = whiteSpent,
            blue = blueSpent,
            black = blackSpent,
            red = redSpent,
            green = greenSpent,
            colorless = colorlessSpent
        )

        return PaymentResult(newState, listOf(event), null)
    }

    private fun autoPay(
        state: GameState,
        playerId: EntityId,
        cost: ManaCost,
        cardName: String,
        xValue: Int
    ): PaymentResult {
        var currentState = state
        val events = mutableListOf<GameEvent>()

        // Use floating mana first
        val poolComponent = state.getEntity(playerId)?.get<ManaPoolComponent>()
            ?: ManaPoolComponent()
        val pool = ManaPool(
            white = poolComponent.white,
            blue = poolComponent.blue,
            black = poolComponent.black,
            red = poolComponent.red,
            green = poolComponent.green,
            colorless = poolComponent.colorless
        )

        val partialResult = pool.payPartial(cost)
        var poolAfterPayment = partialResult.newPool
        val remainingCost = partialResult.remainingCost
        val manaSpentFromPool = partialResult.manaSpent

        var whiteSpent = manaSpentFromPool.white
        var blueSpent = manaSpentFromPool.blue
        var blackSpent = manaSpentFromPool.black
        var redSpent = manaSpentFromPool.red
        var greenSpent = manaSpentFromPool.green
        var colorlessSpent = manaSpentFromPool.colorless

        // Use remaining floating mana for X cost (multiply by X symbol count for XX costs)
        val xSymbolCount = cost.xCount.coerceAtLeast(1)
        var xRemainingToPay = xValue * xSymbolCount

        // Spend colorless first for X
        while (xRemainingToPay > 0 && poolAfterPayment.colorless > 0) {
            poolAfterPayment = poolAfterPayment.spendColorless()!!
            colorlessSpent++
            xRemainingToPay--
        }

        // Spend colored mana for remaining X
        for (color in Color.entries) {
            while (xRemainingToPay > 0 && poolAfterPayment.get(color) > 0) {
                poolAfterPayment = poolAfterPayment.spend(color)!!
                when (color) {
                    Color.WHITE -> whiteSpent++
                    Color.BLUE -> blueSpent++
                    Color.BLACK -> blackSpent++
                    Color.RED -> redSpent++
                    Color.GREEN -> greenSpent++
                }
                xRemainingToPay--
            }
        }

        currentState = currentState.updateEntity(playerId) { container ->
            container.with(
                ManaPoolComponent(
                    white = poolAfterPayment.white,
                    blue = poolAfterPayment.blue,
                    black = poolAfterPayment.black,
                    red = poolAfterPayment.red,
                    green = poolAfterPayment.green,
                    colorless = poolAfterPayment.colorless
                )
            )
        }

        // Tap lands for remaining cost (using xRemainingToPay instead of full xValue)
        if (!remainingCost.isEmpty() || xRemainingToPay > 0) {
            val solution = manaSolver.solve(currentState, playerId, remainingCost, xRemainingToPay)
                ?: return PaymentResult(currentState, events, "Not enough mana to auto-pay")

            for (source in solution.sources) {
                currentState = currentState.updateEntity(source.entityId) { container ->
                    container.with(TappedComponent)
                }
                events.add(TappedEvent(source.entityId, source.name))
            }

            for ((_, production) in solution.manaProduced) {
                when (production.color) {
                    Color.WHITE -> whiteSpent += production.amount
                    Color.BLUE -> blueSpent += production.amount
                    Color.BLACK -> blackSpent += production.amount
                    Color.RED -> redSpent += production.amount
                    Color.GREEN -> greenSpent += production.amount
                    null -> colorlessSpent += production.colorless
                }
            }

            // Add only the bonus mana that wasn't consumed by the solver to the floating pool
            if (solution.remainingBonusMana.isNotEmpty()) {
                currentState = currentState.updateEntity(playerId) { container ->
                    var pool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
                    for ((color, amount) in solution.remainingBonusMana) {
                        pool = pool.add(color, amount)
                    }
                    container.with(pool)
                }
            }
        }

        events.add(
            ManaSpentEvent(
                playerId = playerId,
                reason = "Cast $cardName",
                white = whiteSpent,
                blue = blueSpent,
                black = blackSpent,
                red = redSpent,
                green = greenSpent,
                colorless = colorlessSpent
            )
        )

        return PaymentResult(currentState, events, null)
    }

    private fun explicitPay(
        state: GameState,
        strategy: PaymentStrategy.Explicit,
        cardName: String
    ): PaymentResult {
        var currentState = state
        val events = mutableListOf<GameEvent>()

        for (sourceId in strategy.manaAbilitiesToActivate) {
            val sourceName = currentState.getEntity(sourceId)
                ?.get<CardComponent>()?.name ?: "Unknown"

            currentState = currentState.updateEntity(sourceId) { container ->
                container.with(TappedComponent)
            }
            events.add(TappedEvent(sourceId, sourceName))
        }

        return PaymentResult(currentState, events, null)
    }

    /**
     * Check if the spell needs a creature type choice during casting (e.g., Aphetto Dredging).
     * If so, scan the appropriate zone for creature types and pause for the choice.
     * Returns null if no pause is needed (e.g., no creature types found).
     */
    private fun pauseForCreatureTypeChoice(
        currentState: GameState,
        action: CastSpell,
        source: com.wingedsheep.sdk.model.CastTimeCreatureTypeSource,
        sacrificedPermanentIds: List<EntityId>,
        spellTargetRequirements: List<com.wingedsheep.sdk.scripting.targets.TargetRequirement>,
        priorEvents: List<GameEvent>
    ): ExecutionResult? {
        // Determine which zone to scan based on source
        val zone = when (source) {
            com.wingedsheep.sdk.model.CastTimeCreatureTypeSource.GRAVEYARD ->
                ZoneKey(action.playerId, Zone.GRAVEYARD)
        }
        val zoneCards = currentState.getZone(zone)

        // Collect creature subtypes and which cards have each type
        val typeToCardIds = mutableMapOf<String, MutableList<EntityId>>()
        for (cardId in zoneCards) {
            val cc = currentState.getEntity(cardId)?.get<CardComponent>() ?: continue
            val typeLine = cc.typeLine ?: continue
            if (typeLine.isCreature) {
                for (subtype in typeLine.subtypes) {
                    typeToCardIds.getOrPut(subtype.value) { mutableListOf() }.add(cardId)
                }
            }
        }

        // If no creature types found, skip the decision — casting proceeds normally
        if (typeToCardIds.isEmpty()) return null

        val sortedTypes = typeToCardIds.keys.sorted()
        val cardComponent = currentState.getEntity(action.cardId)?.get<CardComponent>()
        val sourceName = cardComponent?.name

        // Build option index → card IDs mapping for client preview
        val optionCardIds = sortedTypes.mapIndexed { index, type ->
            index to typeToCardIds[type]!!.toList()
        }.toMap()

        val decisionId = java.util.UUID.randomUUID().toString()
        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = action.playerId,
            prompt = "Choose a creature type",
            context = DecisionContext(
                sourceId = action.cardId,
                sourceName = sourceName,
                phase = DecisionPhase.CASTING
            ),
            options = sortedTypes,
            optionCardIds = optionCardIds
        )

        val continuation = CastWithCreatureTypeContinuation(
            decisionId = decisionId,
            cardId = action.cardId,
            casterId = action.playerId,
            targets = action.targets,
            xValue = action.xValue,
            sacrificedPermanents = sacrificedPermanentIds,
            targetRequirements = spellTargetRequirements,
            count = 0,
            creatureTypes = sortedTypes
        )

        val pausedState = currentState
            .pushContinuation(continuation)
            .withPendingDecision(decision)

        return ExecutionResult.paused(
            pausedState.withPriority(action.playerId),
            decision,
            priorEvents + DecisionRequestedEvent(
                decisionId = decisionId,
                playerId = action.playerId,
                decisionType = "CHOOSE_OPTION",
                prompt = decision.prompt
            )
        )
    }

    /**
     * Check if a card is on top of the player's library and the player controls
     * a permanent with PlayFromTopOfLibrary (e.g., Future Sight) or
     * CastSpellTypesFromTopOfLibrary (e.g., Precognition Field).
     */
    private fun isOnTopOfLibraryWithPermission(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): Boolean {
        val library = state.getLibrary(playerId)
        if (library.isEmpty() || library.first() != cardId) return false
        if (hasPlayFromTopOfLibrary(state, playerId)) return true
        return hasCastFromTopOfLibraryPermission(state, playerId, cardId)
    }

    /**
     * Check if a card on top of library can be cast via CastSpellTypesFromTopOfLibrary.
     * Validates the card matches the ability's filter.
     */
    private fun hasCastFromTopOfLibraryPermission(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): Boolean {
        val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: return false
        for (entityId in state.getBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry?.getCard(card.cardDefinitionId) ?: continue
            for (ability in cardDef.script.staticAbilities) {
                if (ability is CastSpellTypesFromTopOfLibrary) {
                    if (matchesCardFilter(cardComponent, ability.filter)) return true
                }
            }
        }
        return false
    }

    /**
     * Simple card type check against a GameObjectFilter for non-battlefield cards.
     */
    private fun matchesCardFilter(card: CardComponent, filter: GameObjectFilter): Boolean {
        // Check card predicates from the filter
        for (predicate in filter.cardPredicates) {
            if (!matchesCardPredicate(card, predicate)) return false
        }
        return true
    }

    private fun matchesCardPredicate(card: CardComponent, predicate: CardPredicate): Boolean {
        return when (predicate) {
            is CardPredicate.IsInstant -> card.typeLine.isInstant
            is CardPredicate.IsSorcery -> card.typeLine.isSorcery
            is CardPredicate.IsCreature -> card.typeLine.isCreature
            is CardPredicate.IsEnchantment -> card.typeLine.isEnchantment
            is CardPredicate.IsArtifact -> card.typeLine.isArtifact
            is CardPredicate.IsLand -> card.typeLine.isLand
            is CardPredicate.Or -> predicate.predicates.any { matchesCardPredicate(card, it) }
            is CardPredicate.And -> predicate.predicates.all { matchesCardPredicate(card, it) }
            is CardPredicate.Not -> !matchesCardPredicate(card, predicate.predicate)
            else -> true // Conservative: allow unknown predicates
        }
    }

    /**
     * Check if a card is in exile and has MayPlayFromExileComponent granting
     * the player permission to play it from exile. Checks all players' exile zones
     * because cards like Villainous Wealth exile from an opponent's library (cards
     * remain in their owner's exile zone but are castable by the spell's controller).
     *
     * Also checks for permanents with GrantMayCastFromLinkedExile static ability
     * (e.g., Rona, Disciple of Gix) that link exiled cards via LinkedExileComponent.
     */
    private fun isInExileWithPlayPermission(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): Boolean {
        val inAnyExile = state.turnOrder.any { pid ->
            cardId in state.getZone(ZoneKey(pid, Zone.EXILE))
        }
        if (!inAnyExile) return false

        // Check direct MayPlayFromExileComponent grant
        val component = state.getEntity(cardId)?.get<MayPlayFromExileComponent>()
        if (component?.controllerId == playerId) return true

        // Check for GrantMayCastFromLinkedExile static abilities on battlefield permanents
        return hasLinkedExileCastPermission(state, playerId, cardId)
    }

    /**
     * Check if any permanent controlled by the player has a GrantMayCastFromLinkedExile
     * static ability and the card is in that permanent's LinkedExileComponent.
     */
    private fun hasLinkedExileCastPermission(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): Boolean {
        val cardContainer = state.getEntity(cardId) ?: return false
        val cardComponent = cardContainer.get<CardComponent>() ?: return false

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val controller = container.get<ControllerComponent>()?.playerId ?: continue
            if (controller != playerId) continue

            val linked = container.get<LinkedExileComponent>() ?: continue
            if (cardId !in linked.exiledIds) continue

            // Check if this permanent has a GrantMayCastFromLinkedExile static ability
            val entityCardComponent = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry?.getCard(entityCardComponent.cardDefinitionId) ?: continue
            val grantAbility = cardDef.script.staticAbilities
                .filterIsInstance<GrantMayCastFromLinkedExile>()
                .firstOrNull() ?: continue

            // Check if the exiled card passes the filter (e.g., nonland)
            if (matchesCardFilter(cardComponent, grantAbility.filter)) {
                return true
            }
        }
        return false
    }

    /**
     * Check if a card has an intrinsic MayCastSelfFromZones static ability
     * that permits casting from its current zone (e.g., Squee, the Immortal).
     */
    private fun hasMayCastSelfFromZonePermission(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): Boolean {
        val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: return false
        val cardDef = cardRegistry?.getCard(cardComponent.cardDefinitionId) ?: return false
        val mayCastAbility = cardDef.script.staticAbilities
            .filterIsInstance<MayCastSelfFromZones>()
            .firstOrNull() ?: return false

        // Find what zone the card is in for this player
        for (zone in mayCastAbility.zones) {
            val zoneKey = ZoneKey(playerId, zone)
            if (cardId in state.getZone(zoneKey)) return true
        }
        return false
    }

    /**
     * Check if a card has PlayWithoutPayingCostComponent granting
     * the player permission to play it without paying its mana cost.
     */
    private fun hasPlayWithoutPayingCost(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): Boolean {
        val component = state.getEntity(cardId)?.get<PlayWithoutPayingCostComponent>()
        return component?.controllerId == playerId
    }

    private fun hasPlayFromTopOfLibrary(state: GameState, playerId: EntityId): Boolean {
        for (entityId in state.getBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry?.getCard(card.cardDefinitionId) ?: continue
            if (cardDef.script.staticAbilities.any { it is PlayFromTopOfLibrary }) {
                return true
            }
        }
        return false
    }

    /**
     * Check if a card has been granted flash by a GrantFlashToSpellType static ability
     * on any permanent on the battlefield (any player's battlefield), or by its own
     * conditionalFlash condition.
     */
    private fun hasGrantedFlash(state: GameState, spellCardId: EntityId): Boolean {
        val spellOwner = state.getEntity(spellCardId)?.get<ControllerComponent>()?.playerId
            ?: return false

        // Check the card's own conditionalFlash (e.g., Ferocious)
        val spellCard = state.getEntity(spellCardId)?.get<CardComponent>()
        val spellDef = spellCard?.let { cardRegistry?.getCard(it.cardDefinitionId) }
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
                val def = cardRegistry?.getCard(card.cardDefinitionId) ?: continue
                for (ability in def.script.staticAbilities) {
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

    companion object {
        fun create(context: ActionContext): CastSpellHandler {
            return CastSpellHandler(
                context.cardRegistry,
                context.turnManager,
                context.manaSolver,
                context.costCalculator,
                context.alternativePaymentHandler,
                context.costHandler,
                context.stackResolver,
                context.targetValidator,
                context.conditionEvaluator,
                context.triggerDetector,
                context.triggerProcessor
            )
        }
    }
}
