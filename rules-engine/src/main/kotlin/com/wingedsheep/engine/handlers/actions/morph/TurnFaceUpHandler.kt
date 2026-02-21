package com.wingedsheep.engine.handlers.actions.morph

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.ManaSpentEvent
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.core.TurnFaceUpEvent
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.event.TriggerProcessor
import com.wingedsheep.engine.handlers.CostHandler
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.actions.ActionContext
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.costs.PayCost
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
    private val triggerProcessor: TriggerProcessor,
    private val stateProjector: StateProjector
) : ActionHandler<TurnFaceUp> {
    override val actionType: KClass<TurnFaceUp> = TurnFaceUp::class

    private val predicateEvaluator = PredicateEvaluator()

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

        // Validate cost payment based on morph cost type
        when (morphData.morphCost) {
            is PayCost.Mana -> {
                val manaCost = morphData.morphCost.cost
                when (action.paymentStrategy) {
                    is PaymentStrategy.AutoPay -> {
                        if (!manaSolver.canPay(state, action.playerId, manaCost)) {
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
                    }
                }
            }
            is PayCost.PayLife -> {
                val lifeTotal = state.getEntity(action.playerId)
                    ?.get<LifeTotalComponent>()?.life ?: 0
                if (lifeTotal < 1) {
                    return "You don't have enough life to turn this creature face up"
                }
                // Note: paying life doesn't require a minimum — you can pay life
                // even if it would reduce you to 0 or below (you lose as SBA)
            }
            is PayCost.ReturnToHand -> {
                val validTargets = findReturnToHandTargets(state, action.playerId, morphData.morphCost, action.sourceId)
                if (validTargets.size < morphData.morphCost.count) {
                    return "Not enough permanents to return to pay morph cost"
                }
                if (action.costTargetIds.size != morphData.morphCost.count) {
                    return "Must return exactly ${morphData.morphCost.count} permanent(s) to pay morph cost"
                }
                for (targetId in action.costTargetIds) {
                    if (targetId !in validTargets) {
                        return "Invalid target for return-to-hand morph cost: $targetId"
                    }
                }
            }
            else -> return "Unsupported morph cost type: ${morphData.morphCost::class.simpleName}"
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
        when (morphData.morphCost) {
            is PayCost.PayLife -> {
                val lifeCost = morphData.morphCost.amount
                val currentLife = currentState.getEntity(action.playerId)
                    ?.get<LifeTotalComponent>()?.life ?: 0
                val newLife = currentLife - lifeCost
                currentState = currentState.updateEntity(action.playerId) { c ->
                    c.with(LifeTotalComponent(newLife))
                }
                events.add(LifeChangedEvent(action.playerId, currentLife, newLife, LifeChangeReason.LIFE_LOSS))
            }
            is PayCost.Mana -> {
                val manaCost = morphData.morphCost.cost
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
                        val poolAfterPayment = partialResult.newPool
                        val remainingCost = partialResult.remainingCost
                        val manaSpentFromPool = partialResult.manaSpent

                        var whiteSpent = manaSpentFromPool.white
                        var blueSpent = manaSpentFromPool.blue
                        var blackSpent = manaSpentFromPool.black
                        var redSpent = manaSpentFromPool.red
                        var greenSpent = manaSpentFromPool.green
                        var colorlessSpent = manaSpentFromPool.colorless

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

                        // Tap lands for remaining cost
                        if (!remainingCost.isEmpty()) {
                            val solution = manaSolver.solve(currentState, action.playerId, remainingCost, 0)
                                ?: return ExecutionResult.error(currentState, "Not enough mana to turn face up")

                            for (source in solution.sources) {
                                currentState = currentState.updateEntity(source.entityId) { c ->
                                    c.with(TappedComponent)
                                }
                                events.add(TappedEvent(source.entityId, source.name))
                            }

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
            is PayCost.ReturnToHand -> {
                for (targetId in action.costTargetIds) {
                    val result = EffectExecutorUtils.movePermanentToZone(currentState, targetId, Zone.HAND)
                    if (result.error != null) {
                        return ExecutionResult.error(currentState, result.error!!)
                    }
                    currentState = result.newState
                    events.addAll(result.events)
                }
            }
            else -> return ExecutionResult.error(state, "Unsupported morph cost type: ${morphData.morphCost::class.simpleName}")
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

    /**
     * Find valid permanents on the battlefield that can be returned to hand to pay a morph cost.
     * Uses projected state to account for type-changing effects (e.g., Artificial Evolution).
     *
     * @param excludeEntityId The face-down creature being turned up (cannot return itself)
     */
    fun findReturnToHandTargets(
        state: GameState,
        playerId: EntityId,
        cost: PayCost.ReturnToHand,
        excludeEntityId: EntityId
    ): List<EntityId> {
        val projected = stateProjector.project(state)
        val playerBattlefield = ZoneKey(playerId, Zone.BATTLEFIELD)
        val predicateContext = PredicateContext(controllerId = playerId)

        return state.getZone(playerBattlefield).filter { entityId ->
            if (entityId == excludeEntityId) return@filter false
            val container = state.getEntity(entityId) ?: return@filter false
            container.get<CardComponent>() ?: return@filter false
            val controllerId = container.get<ControllerComponent>()?.playerId
            if (controllerId != playerId) return@filter false

            predicateEvaluator.matchesWithProjection(state, projected, entityId, cost.filter, predicateContext)
        }
    }

    companion object {
        fun create(context: ActionContext): TurnFaceUpHandler {
            return TurnFaceUpHandler(
                context.cardRegistry,
                context.manaSolver,
                context.costHandler,
                context.triggerDetector,
                context.triggerProcessor,
                context.stateProjector
            )
        }
    }
}
