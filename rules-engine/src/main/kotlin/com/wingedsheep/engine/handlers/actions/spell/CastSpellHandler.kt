package com.wingedsheep.engine.handlers.actions.spell

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.CastWithCreatureTypeContinuation
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.ExecutionResult
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
import com.wingedsheep.engine.mechanics.layers.StateProjector
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
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.CastRestriction
import com.wingedsheep.sdk.scripting.effects.ChooseCreatureTypeModifyStatsEffect
import com.wingedsheep.sdk.scripting.effects.DividedDamageEffect
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.PlayFromTopOfLibrary
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.toEntityId
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
    private val stateProjector: StateProjector = StateProjector()
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
        if (!inHand && !onTopOfLibrary) {
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
            if (!turnManager.canPlaySorcerySpeed(state, action.playerId)) {
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

        // Calculate effective cost
        val effectiveCost = if (cardDef != null) {
            costCalculator.calculateEffectiveCost(state, cardDef, action.playerId)
        } else {
            cardComponent.manaCost
        }

        // Validate payment
        val paymentError = validatePayment(state, action, effectiveCost)
        if (paymentError != null) {
            return paymentError
        }

        // Validate targets (include auraTarget as a target requirement for aura spells)
        if (cardDef != null) {
            val targetRequirements = buildList {
                addAll(cardDef.script.targetRequirements)
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
        val spellEffect = cardDef?.script?.spellEffect
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
        val projected = stateProjector.project(state)
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

        // Calculate effective cost
        var effectiveCost = if (action.castFaceDown) {
            costCalculator.calculateFaceDownCost(currentState, action.playerId)
        } else if (cardDef != null) {
            costCalculator.calculateEffectiveCost(currentState, cardDef, action.playerId)
        } else {
            cardComponent.manaCost
        }

        // Process additional costs (sacrifice, exile, etc.)
        val sacrificedPermanentIds = mutableListOf<EntityId>()
        val sacrificedPermanentSubtypes = mutableMapOf<EntityId, Set<String>>()
        var exiledCardCount = 0
        if (cardDef != null && action.additionalCostPayment != null) {
            for (additionalCost in cardDef.script.additionalCosts) {
                when (additionalCost) {
                    is AdditionalCost.SacrificePermanent -> {
                        // Project state to capture text-changed subtypes before sacrifice
                        val projectedBeforeSacrifice = stateProjector.project(currentState)
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
        val spellTargetRequirements = if (cardDef != null) {
            buildList {
                addAll(cardDef.script.targetRequirements)
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

        val spellEffect = cardDef?.script?.spellEffect
        if (spellEffect is ChooseCreatureTypeModifyStatsEffect) {
            return pauseForCreatureTypeChoiceForStats(
                currentState, action, sacrificedPermanentIds, spellTargetRequirements, events
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
            exiledCardCount = exiledCardCount
        )

        if (!castResult.isSuccess) {
            return castResult
        }

        var allEvents = events + castResult.events

        // Detect and process triggers from casting (including additional cost events like sacrifice)
        val triggers = triggerDetector.detectTriggers(castResult.newState, allEvents)
        if (triggers.isNotEmpty()) {
            val triggerResult = triggerProcessor.processTriggers(castResult.newState, triggers)

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
            castResult.newState.withPriority(action.playerId),
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
     * Pause for creature type choice during casting for ChooseCreatureTypeModifyStatsEffect
     * (e.g., Tribal Unity, Defensive Maneuvers). Presents all creature types as options.
     */
    private fun pauseForCreatureTypeChoiceForStats(
        currentState: GameState,
        action: CastSpell,
        sacrificedPermanentIds: List<EntityId>,
        spellTargetRequirements: List<com.wingedsheep.sdk.scripting.targets.TargetRequirement>,
        priorEvents: List<GameEvent>
    ): ExecutionResult {
        val allCreatureTypes = com.wingedsheep.sdk.core.Subtype.ALL_CREATURE_TYPES
        val sourceName = currentState.getEntity(action.cardId)?.get<CardComponent>()?.name
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
            options = allCreatureTypes
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
            creatureTypes = allCreatureTypes
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
     * a permanent with PlayFromTopOfLibrary (e.g., Future Sight).
     */
    private fun isOnTopOfLibraryWithPermission(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): Boolean {
        val library = state.getLibrary(playerId)
        if (library.isEmpty() || library.first() != cardId) return false
        return hasPlayFromTopOfLibrary(state, playerId)
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
                context.triggerProcessor,
                context.stateProjector
            )
        }
    }
}
