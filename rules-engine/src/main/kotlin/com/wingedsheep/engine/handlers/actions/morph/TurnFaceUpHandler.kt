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
import com.wingedsheep.engine.handlers.actions.ActionContext
import com.wingedsheep.engine.handlers.actions.ActionHandler
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
    private val cardRegistry: CardRegistry?,
    private val manaSolver: ManaSolver,
    private val costHandler: CostHandler,
    private val triggerDetector: TriggerDetector,
    private val triggerProcessor: TriggerProcessor
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

        // Validate mana payment
        when (action.paymentStrategy) {
            is PaymentStrategy.AutoPay -> {
                if (!manaSolver.canPay(state, action.playerId, morphData.morphCost)) {
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
                if (!costHandler.canPayManaCost(pool, morphData.morphCost)) {
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
        val cardDef = cardRegistry?.getCard(morphData.originalCardDefinitionId)
        val cardName = cardDef?.name ?: cardComponent?.name ?: "Unknown"

        // Pay the morph cost
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

                val newPool = costHandler.payManaCost(pool, morphData.morphCost)
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
                val solution = manaSolver.solve(currentState, action.playerId, morphData.morphCost, 0)
                    ?: return ExecutionResult.error(currentState, "Not enough mana to turn face up")

                for (source in solution.sources) {
                    currentState = currentState.updateEntity(source.entityId) { c ->
                        c.with(TappedComponent)
                    }
                    events.add(TappedEvent(source.entityId, source.name))
                }

                var whiteSpent = 0
                var blueSpent = 0
                var blackSpent = 0
                var redSpent = 0
                var greenSpent = 0
                var colorlessSpent = 0

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

        // Turn the creature face up
        currentState = currentState.updateEntity(action.sourceId) { c ->
            c.without<FaceDownComponent>()
        }

        val turnFaceUpEvent = TurnFaceUpEvent(
            entityId = action.sourceId,
            cardName = cardName,
            controllerId = action.playerId
        )
        events.add(turnFaceUpEvent)

        // Detect and process "when turned face up" triggers
        val triggers = triggerDetector.detectTriggers(currentState, events)
        if (triggers.isNotEmpty()) {
            val triggerResult = triggerProcessor.processTriggers(currentState, triggers)

            if (triggerResult.isPaused) {
                return ExecutionResult.paused(
                    triggerResult.state,
                    triggerResult.pendingDecision!!,
                    events + triggerResult.events
                )
            }

            return ExecutionResult.success(
                triggerResult.newState,
                events + triggerResult.events
            )
        }

        // Player retains priority after turning face up
        return ExecutionResult.success(currentState, events)
    }

    companion object {
        fun create(context: ActionContext): TurnFaceUpHandler {
            return TurnFaceUpHandler(
                context.cardRegistry,
                context.manaSolver,
                context.costHandler,
                context.triggerDetector,
                context.triggerProcessor
            )
        }
    }
}
