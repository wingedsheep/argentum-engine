package com.wingedsheep.engine.handlers.actions.room

import com.wingedsheep.engine.core.DoorUnlockedEvent
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ManaSpentEvent
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.RoomFullyUnlockedEvent
import com.wingedsheep.engine.core.tap
import com.wingedsheep.engine.core.UnlockRoomDoor
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.event.TriggerProcessor
import com.wingedsheep.engine.handlers.CostHandler
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.mechanics.mana.ManaAbilitySideEffectExecutor
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.mechanics.mana.UnlockCostReducer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.RoomComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Color
import kotlin.reflect.KClass

/**
 * Handler for [UnlockRoomDoor] — the door-unlock special action (CR 709.5e + rule 116).
 *
 * Special-action semantics: doesn't use the stack, can't be responded to between
 * declaration and effect, can't be countered. Timing: any time the controller has
 * priority, the stack is empty, and it's a main phase of their turn.
 *
 * Effect: pay the locked face's printed mana cost (the *only* cost — additional costs
 * from the locked half's text don't apply, per 709.5e), then add that face's id to the
 * Room's [RoomComponent.unlocked] set. Emits a [DoorUnlockedEvent], and if this transition
 * fully unlocks the Room, also a [RoomFullyUnlockedEvent].
 */
class UnlockRoomDoorHandler(
    private val manaSolver: ManaSolver,
    private val costHandler: CostHandler,
    private val triggerDetector: TriggerDetector,
    private val triggerProcessor: TriggerProcessor,
    private val manaAbilitySideEffectExecutor: ManaAbilitySideEffectExecutor,
    cardRegistry: com.wingedsheep.engine.registry.CardRegistry,
) : ActionHandler<UnlockRoomDoor> {
    override val actionType: KClass<UnlockRoomDoor> = UnlockRoomDoor::class

    // Inquisitive-Glimmer-style "Unlock costs you pay cost {N} less" reductions. The enumerator
    // applies the same reducer so affordability and payment agree (CR 709.5e).
    private val unlockCostReducer = UnlockCostReducer(cardRegistry)

    // Lets restricted mana tagged "spend only to ... unlock a door" (Creeping Peeper) be
    // recognized when paying an unlock cost.
    private val unlockContext = SpellPaymentContext(isUnlockDoorAction = true)

    // Re-bakes the Room's continuous-effect component when a door unlocks a continuous static.
    private val staticAbilityHandler = StaticAbilityHandler(cardRegistry)

    override fun validate(state: GameState, action: UnlockRoomDoor): String? {
        if (state.priorityPlayerId != action.playerId) {
            return "You don't have priority"
        }
        if (!state.isActiveTurnFor(action.playerId)) {
            return "You can only unlock doors during your own turn"
        }
        if (!state.step.isMainPhase) {
            return "You can only unlock doors during a main phase"
        }
        if (state.stack.isNotEmpty()) {
            return "You can only unlock doors while the stack is empty"
        }

        val container = state.getEntity(action.roomId)
            ?: return "Room not found: ${action.roomId}"

        if (action.roomId !in state.getBattlefield()) {
            return "Room is not on the battlefield"
        }

        val controller = container.get<ControllerComponent>()?.playerId
        if (controller != action.playerId) {
            return "You don't control this Room"
        }

        val room = container.get<RoomComponent>()
            ?: return "This permanent is not a Room"

        val face = room.faces.find { it.id == action.faceId }
            ?: return "Unknown face: ${action.faceId.value}"

        if (room.isUnlocked(action.faceId)) {
            return "This door is already unlocked"
        }

        val cost = unlockCostReducer.effectiveUnlockCost(state, action.playerId, face.manaCost)
        when (action.paymentStrategy) {
            is PaymentStrategy.AutoPay -> {
                if (!manaSolver.canPay(state, action.playerId, cost, 0, spellContext = unlockContext)) {
                    return "Not enough mana to unlock ${face.name}"
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
                    colorless = poolComponent.colorless,
                    restrictedMana = poolComponent.restrictedMana
                )
                if (!costHandler.canPayManaCost(pool, cost, unlockContext)) {
                    return "Insufficient mana in pool to unlock ${face.name}"
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
                val chosen = action.paymentStrategy.manaAbilitiesToActivate.toSet()
                val excluded = manaSolver.findAvailableManaSources(state, action.playerId)
                    .map { it.entityId }
                    .filter { it !in chosen }
                    .toSet()
                if (manaSolver.solve(state, action.playerId, cost, 0, excludeSources = excluded, spellContext = unlockContext) == null) {
                    return "Selected mana sources cannot pay the unlock cost"
                }
            }
        }

        return null
    }

    override fun execute(state: GameState, action: UnlockRoomDoor): ExecutionResult {
        var currentState = state
        val events = mutableListOf<GameEvent>()

        val container = state.getEntity(action.roomId)
            ?: return ExecutionResult.error(state, "Room not found")
        val room = container.get<RoomComponent>()
            ?: return ExecutionResult.error(state, "Not a Room")
        val face = room.faces.find { it.id == action.faceId }
            ?: return ExecutionResult.error(state, "Unknown face")

        // Pay the face's mana cost (the unlock cost, per CR 709.5e), after any
        // "Unlock costs you pay cost {N} less" reductions (Inquisitive Glimmer).
        val cost = unlockCostReducer.effectiveUnlockCost(currentState, action.playerId, face.manaCost)
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
                    colorless = poolComponent.colorless,
                    restrictedMana = poolComponent.restrictedMana
                )
                val newPool = costHandler.payManaCost(pool, cost, unlockContext)
                    ?: return ExecutionResult.error(currentState, "Insufficient mana in pool")
                currentState = currentState.updateEntity(action.playerId) { c ->
                    c.with(
                        ManaPoolComponent(
                            white = newPool.white,
                            blue = newPool.blue,
                            black = newPool.black,
                            red = newPool.red,
                            green = newPool.green,
                            colorless = newPool.colorless,
                            restrictedMana = newPool.restrictedMana
                        )
                    )
                }
                events.add(
                    ManaSpentEvent(
                        playerId = action.playerId,
                        reason = "Unlock ${face.name}",
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
                val poolComponent = currentState.getEntity(action.playerId)?.get<ManaPoolComponent>()
                    ?: ManaPoolComponent()
                val pool = ManaPool(
                    white = poolComponent.white,
                    blue = poolComponent.blue,
                    black = poolComponent.black,
                    red = poolComponent.red,
                    green = poolComponent.green,
                    colorless = poolComponent.colorless,
                    restrictedMana = poolComponent.restrictedMana
                )
                val partialResult = pool.payPartial(cost, unlockContext)
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
                            colorless = poolAfterPayment.colorless,
                            restrictedMana = poolAfterPayment.restrictedMana
                        )
                    )
                }

                if (!remainingCost.isEmpty()) {
                    val solution = manaSolver.solve(currentState, action.playerId, remainingCost, 0, spellContext = unlockContext)
                        ?: return ExecutionResult.error(currentState, "Not enough mana to unlock ${face.name}")

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
                        reason = "Unlock ${face.name}",
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
                    val (tappedState, tapEvent) = tap(currentState, sourceId)
                    currentState = tappedState
                    tapEvent?.let(events::add)
                }
            }
        }

        // Apply the unlock to the RoomComponent via the shared primitive, so the unlock-cost
        // special action and the resolution-time "unlock a door" effect emit identical events.
        val (stateAfterUnlock, unlockEvents) = RoomDoorUnlocker.unlock(
            currentState, action.roomId, action.faceId, action.playerId, staticAbilityHandler
        )
        currentState = stateAfterUnlock
        events.addAll(unlockEvents)

        // Detect and process triggers from the door-unlock events.
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

        // Player retains priority after the special action; clear priorityPassedBy so
        // the opponent's prior pass doesn't carry over.
        return ExecutionResult.success(currentState.withPriority(action.playerId), events)
    }

    companion object {
        fun create(services: EngineServices): UnlockRoomDoorHandler =
            UnlockRoomDoorHandler(
                manaSolver = services.manaSolver,
                costHandler = services.costHandler,
                triggerDetector = services.triggerDetector,
                triggerProcessor = services.triggerProcessor,
                manaAbilitySideEffectExecutor = services.manaAbilitySideEffectExecutor,
                cardRegistry = services.cardRegistry,
            )
    }
}
