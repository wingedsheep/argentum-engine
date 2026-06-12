package com.wingedsheep.engine.handlers.actions.morph

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ManaSpentEvent
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.core.TurnFaceUpEvent
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.event.TriggerProcessor
import com.wingedsheep.engine.handlers.CostHandler
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.mechanics.cost.CostPaymentContext
import com.wingedsheep.engine.mechanics.cost.CostPaymentService
import com.wingedsheep.engine.mechanics.cost.PaymentResult
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.TurnFaceUpEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlin.reflect.KClass

/**
 * Handler for the TurnFaceUp action.
 *
 * Turning a face-down creature face up is a special action that:
 * - Doesn't use the stack
 * - Can be done any time the player has priority
 * - Requires paying the morph cost
 */
class TurnFaceUpHandler(
    private val cardRegistry: CardRegistry,
    private val manaSolver: ManaSolver,
    private val costHandler: CostHandler,
    private val costCalculator: CostCalculator,
    private val triggerDetector: TriggerDetector,
    private val triggerProcessor: TriggerProcessor,
    private val effectExecutorRegistry: com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry,
    private val manaAbilitySideEffectExecutor: com.wingedsheep.engine.mechanics.mana.ManaAbilitySideEffectExecutor,
    private val costPaymentService: CostPaymentService,
    private val staticAbilityHandler: StaticAbilityHandler = StaticAbilityHandler(cardRegistry)
) : ActionHandler<TurnFaceUp> {
    override val actionType: KClass<TurnFaceUp> = TurnFaceUp::class

    override fun validate(state: GameState, action: TurnFaceUp): String? {
        if (state.priorityPlayerId != action.playerId) {
            return "You don't have priority"
        }

        val container = state.getEntity(action.sourceId)
            ?: return "Permanent not found: ${action.sourceId}"

        val controller = container.get<ControllerComponent>()?.playerId
        if (controller != action.playerId) {
            return "You don't control this permanent"
        }

        if (action.sourceId !in state.getBattlefield()) {
            return "Permanent is not on the battlefield"
        }

        if (!container.has<FaceDownComponent>()) {
            return "This creature is not face-down"
        }

        val morphData = container.get<MorphDataComponent>()
            ?: return "This creature cannot be turned face up (no morph cost)"

        // Validate cost payment based on morph cost type.
        // Apply morph cost increases from permanents like Exiled Doomsayer.
        val morphCostIncrease = costCalculator.calculateMorphCostIncrease(state)
        val morphCost = morphData.morphCost
        val manaMorph = (morphCost as? PayCost.Atom)?.atom as? CostAtom.Mana
        when {
            manaMorph != null -> {
                // Mana morph payment stays in this handler: it carries the rich up-front UX
                // (explicit mana-source selection, X, auto-tap preview) that the shared
                // CostPaymentService's yes/no mana path deliberately doesn't model.
                val manaCost = costCalculator.increaseGenericCost(manaMorph.cost, morphCostIncrease)
                val xValue = action.xValue ?: 0
                when (action.paymentStrategy) {
                    is PaymentStrategy.AutoPay -> {
                        if (!manaSolver.canPay(state, action.playerId, manaCost, xValue)) {
                            return "Not enough mana to turn this creature face up"
                        }
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
                        if (!costHandler.canPayManaCost(pool, manaCost)) {
                            return "Insufficient mana in pool to turn this creature face up"
                        }
                    }
                    is PaymentStrategy.Explicit -> {
                        for (sourceId in action.paymentStrategy.manaAbilitiesToActivate) {
                            val sourceContainer = state.getEntity(sourceId)
                                ?: return "Mana source not found: $sourceId"
                            if (sourceContainer.has<TappedComponent>()) {
                                return "Mana source is already tapped: $sourceId"
                            }
                        }
                        // Verify the chosen sources can actually pay the colored cost.
                        // We do this by asking the solver to pay using ONLY the chosen sources
                        // (excluding everything else from consideration).
                        val chosen = action.paymentStrategy.manaAbilitiesToActivate.toSet()
                        val excluded = manaSolver.findAvailableManaSources(state, action.playerId)
                            .map { it.entityId }
                            .filter { it !in chosen }
                            .toSet()
                        if (manaSolver.solve(state, action.playerId, manaCost, xValue, excludeSources = excluded) == null) {
                            return "Selected mana sources cannot pay the morph cost"
                        }
                    }
                }
            }
            // Every other morph cost (life, sacrifice, discard, reveal, exile, return, tap,
            // own-mana-cost, choice) is paid through CostPaymentService at resolution. The only
            // pre-check the action needs is affordability (CR 118.3) — the cost-specific selection
            // happens afterward as a decision pause, so there are no costTargetIds to validate.
            else -> {
                if (!costPaymentService.canAfford(state, action.playerId, morphCost, action.sourceId)) {
                    return "Cannot pay the morph cost to turn this creature face up"
                }
            }
        }

        return null
    }

    override fun execute(state: GameState, action: TurnFaceUp): ExecutionResult {
        var currentState = state
        val events = mutableListOf<GameEvent>()

        val container = state.getEntity(action.sourceId)
            ?: return ExecutionResult.error(state, "Permanent not found")

        val morphData = container.get<MorphDataComponent>()
            ?: return ExecutionResult.error(state, "No morph data found")

        val cardComponent = container.get<CardComponent>()
        val cardDef = cardRegistry.getCard(morphData.originalCardDefinitionId)
        val cardName = cardDef?.name ?: cardComponent?.name ?: "Unknown"

        // Pay the morph cost (including any morph cost increases)
        val morphCostIncrease = costCalculator.calculateMorphCostIncrease(currentState)
        val xValue = action.xValue ?: 0
        val morphCost = morphData.morphCost
        val manaMorph = (morphCost as? PayCost.Atom)?.atom as? CostAtom.Mana
        when {
            manaMorph != null -> {
                val manaCost = costCalculator.increaseGenericCost(manaMorph.cost, morphCostIncrease)
                when (action.paymentStrategy) {
                    is PaymentStrategy.FromPool -> {
                        val poolComponent = currentState.getEntity(action.playerId)?.get<ManaPoolComponent>()
                            ?: ManaPoolComponent()
                        val pool = ManaPool(
                            white = poolComponent.white,
                            blue = poolComponent.blue,
                            black = poolComponent.black,
                            red = poolComponent.red,
                            green = poolComponent.green,
                            colorless = poolComponent.colorless
                        )

                        val newPool = costHandler.payManaCost(pool, manaCost)
                            ?: return ExecutionResult.error(currentState, "Insufficient mana in pool")

                        currentState = currentState.updateEntity(action.playerId) { c ->
                            c.with(
                                ManaPoolComponent(
                                    white = newPool.white,
                                    blue = newPool.blue,
                                    black = newPool.black,
                                    red = newPool.red,
                                    green = newPool.green,
                                    colorless = newPool.colorless
                                )
                            )
                        }

                        events.add(
                            ManaSpentEvent(
                                playerId = action.playerId,
                                reason = "Turn face up $cardName",
                                white = poolComponent.white - newPool.white,
                                blue = poolComponent.blue - newPool.blue,
                                black = poolComponent.black - newPool.black,
                                red = poolComponent.red - newPool.red,
                                green = poolComponent.green - newPool.green,
                                colorless = poolComponent.colorless - newPool.colorless
                            )
                        )
                    }

                    is PaymentStrategy.AutoPay -> {
                        // Use floating mana first
                        val poolComponent = currentState.getEntity(action.playerId)?.get<ManaPoolComponent>()
                            ?: ManaPoolComponent()
                        val pool = ManaPool(
                            white = poolComponent.white,
                            blue = poolComponent.blue,
                            black = poolComponent.black,
                            red = poolComponent.red,
                            green = poolComponent.green,
                            colorless = poolComponent.colorless
                        )

                        val partialResult = pool.payPartial(manaCost)
                        var poolAfterPayment = partialResult.newPool
                        val remainingCost = partialResult.remainingCost
                        val manaSpentFromPool = partialResult.manaSpent

                        var whiteSpent = manaSpentFromPool.white
                        var blueSpent = manaSpentFromPool.blue
                        var blackSpent = manaSpentFromPool.black
                        var redSpent = manaSpentFromPool.red
                        var greenSpent = manaSpentFromPool.green
                        var colorlessSpent = manaSpentFromPool.colorless

                        // Pay for X from remaining pool (multiply by X symbol count for XX costs)
                        val xSymbolCount = manaCost.xCount.coerceAtLeast(1)
                        var xRemainingToPay = xValue * xSymbolCount
                        while (xRemainingToPay > 0 && poolAfterPayment.colorless > 0) {
                            poolAfterPayment = poolAfterPayment.copy(colorless = poolAfterPayment.colorless - 1)
                            colorlessSpent++
                            xRemainingToPay--
                        }
                        for (color in listOf(Color.GREEN, Color.RED, Color.BLACK, Color.BLUE, Color.WHITE)) {
                            while (xRemainingToPay > 0) {
                                val current = when (color) {
                                    Color.WHITE -> poolAfterPayment.white
                                    Color.BLUE -> poolAfterPayment.blue
                                    Color.BLACK -> poolAfterPayment.black
                                    Color.RED -> poolAfterPayment.red
                                    Color.GREEN -> poolAfterPayment.green
                                }
                                if (current <= 0) break
                                poolAfterPayment = when (color) {
                                    Color.WHITE -> poolAfterPayment.copy(white = poolAfterPayment.white - 1).also { whiteSpent++ }
                                    Color.BLUE -> poolAfterPayment.copy(blue = poolAfterPayment.blue - 1).also { blueSpent++ }
                                    Color.BLACK -> poolAfterPayment.copy(black = poolAfterPayment.black - 1).also { blackSpent++ }
                                    Color.RED -> poolAfterPayment.copy(red = poolAfterPayment.red - 1).also { redSpent++ }
                                    Color.GREEN -> poolAfterPayment.copy(green = poolAfterPayment.green - 1).also { greenSpent++ }
                                }
                                xRemainingToPay--
                            }
                        }

                        currentState = currentState.updateEntity(action.playerId) { c ->
                            c.with(
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
                            val solution = manaSolver.solve(currentState, action.playerId, remainingCost, xRemainingToPay)
                                ?: return ExecutionResult.error(currentState, "Not enough mana to turn face up")

                            val (stateAfterTaps, tapEvents) = manaAbilitySideEffectExecutor
                                .tapSourcesWithSideEffects(currentState, solution, action.playerId)
                            currentState = stateAfterTaps
                            events.addAll(tapEvents)

                            for ((_, production) in solution.manaProduced) {
                                when (production.color) {
                                    Color.WHITE -> whiteSpent++
                                    Color.BLUE -> blueSpent++
                                    Color.BLACK -> blackSpent++
                                    Color.RED -> redSpent++
                                    Color.GREEN -> greenSpent++
                                    null -> colorlessSpent += production.colorless
                                }
                            }
                        }

                        events.add(
                            ManaSpentEvent(
                                playerId = action.playerId,
                                reason = "Turn face up $cardName",
                                white = whiteSpent,
                                blue = blueSpent,
                                black = blackSpent,
                                red = redSpent,
                                green = greenSpent,
                                colorless = colorlessSpent
                            )
                        )
                    }

                    is PaymentStrategy.Explicit -> {
                        for (sourceId in action.paymentStrategy.manaAbilitiesToActivate) {
                            val sourceName = currentState.getEntity(sourceId)
                                ?.get<CardComponent>()?.name ?: "Unknown"

                            currentState = currentState.updateEntity(sourceId) { c ->
                                c.with(TappedComponent)
                            }
                            events.add(TappedEvent(sourceId, sourceName))
                        }
                    }
                }
            }
            // Every non-mana morph cost (life, sacrifice, discard, reveal, exile, return, tap,
            // own-mana-cost, choice) is handed to the shared CostPaymentService. It pauses for the
            // cost-specific decision — battlefield targeting for sacrifice/return/tap, card
            // selection for discard/reveal/exile, yes/no for life — and on payment runs `onPaid`,
            // which flips the creature face up (plus any megamorph faceUpEffect). The resulting
            // TurnFaceUpEvent is picked up by SubmitDecisionHandler, which fires the "when turned
            // face up" triggers and restores priority. A decline runs nothing, leaving the creature
            // face down — equivalent to never having taken the action.
            else -> {
                val flip: com.wingedsheep.sdk.scripting.effects.Effect =
                    morphData.faceUpEffect
                        ?.let { Effects.Composite(TurnFaceUpEffect(EffectTarget.Self), it) }
                        ?: TurnFaceUpEffect(EffectTarget.Self)
                return when (
                    val result = costPaymentService.pay(
                        currentState,
                        action.playerId,
                        morphCost,
                        action.sourceId,
                        CostPaymentContext(onPaid = flip)
                    )
                ) {
                    is PaymentResult.Pending ->
                        ExecutionResult.paused(result.state, result.pendingDecision, events + result.events)
                    is PaymentResult.Unaffordable ->
                        ExecutionResult.error(currentState, "Cannot pay the morph cost to turn this creature face up")
                    // Selection / yes-no payments never settle synchronously — they always pause first.
                    else ->
                        ExecutionResult.error(currentState, "Unexpected synchronous morph payment result")
                }
            }
        }

        // Turn the creature face up and add static ability components
        currentState = currentState.updateEntity(action.sourceId) { c ->
            var updated = c.without<FaceDownComponent>()
            updated = staticAbilityHandler.addContinuousEffectComponent(updated)
            updated = staticAbilityHandler.addReplacementEffectComponent(updated)
            updated
        }

        // Execute face-up replacement effect (e.g., "put five +1/+1 counters on it")
        if (morphData.faceUpEffect != null) {
            val effectContext = com.wingedsheep.engine.handlers.EffectContext(
                sourceId = action.sourceId,
                controllerId = action.playerId,
            )
            val effectResult = effectExecutorRegistry.execute(currentState, morphData.faceUpEffect, effectContext)
            if (effectResult.error == null) {
                currentState = effectResult.state
                events.addAll(effectResult.events)
            }
        }

        val turnFaceUpEvent = TurnFaceUpEvent(
            entityId = action.sourceId,
            cardName = cardName,
            controllerId = action.playerId,
            xValue = if (action.xValue != null && action.xValue > 0) action.xValue else null
        )
        events.add(turnFaceUpEvent)

        // Detect and process "when turned face up" triggers
        val triggers = triggerDetector.detectTriggers(currentState, events)
        if (triggers.isNotEmpty()) {
            val triggerResult = triggerProcessor.processTriggers(currentState, triggers)

            if (triggerResult.isPaused) {
                return ExecutionResult.paused(
                    triggerResult.state.withPriority(action.playerId),
                    triggerResult.pendingDecision!!,
                    events + triggerResult.events
                )
            }

            return ExecutionResult.success(
                triggerResult.newState.withPriority(action.playerId),
                events + triggerResult.events
            )
        }

        // Player retains priority after turning face up.
        // Must call withPriority to clear priorityPassedBy — otherwise the opponent's
        // earlier pass is treated as still valid, causing both players to appear "passed"
        // and the game to advance without giving the opponent priority after the flip.
        return ExecutionResult.success(currentState.withPriority(action.playerId), events)
    }

    companion object {
        fun create(services: EngineServices): TurnFaceUpHandler {
            return TurnFaceUpHandler(
                services.cardRegistry,
                services.manaSolver,
                services.costHandler,
                services.costCalculator,
                services.triggerDetector,
                services.triggerProcessor,
                services.effectExecutorRegistry,
                services.manaAbilitySideEffectExecutor,
                CostPaymentService(services)
            )
        }
    }
}
