package com.wingedsheep.engine.handlers.actions.land

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.event.TriggerProcessor
import com.wingedsheep.engine.handlers.actions.ActionContext
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.sdk.core.Zone
import kotlin.reflect.KClass

/**
 * Handler for the PlayLand action.
 *
 * Playing a land is a special action that doesn't use the stack.
 * It moves the land from hand to battlefield and uses up a land drop.
 */
class PlayLandHandler(
    private val triggerDetector: TriggerDetector,
    private val triggerProcessor: TriggerProcessor
) : ActionHandler<PlayLand> {
    override val actionType: KClass<PlayLand> = PlayLand::class

    override fun validate(state: GameState, action: PlayLand): String? {
        if (state.activePlayerId != action.playerId) {
            return "You can only play lands on your turn"
        }
        if (!state.step.isMainPhase) {
            return "You can only play lands during a main phase"
        }
        if (state.stack.isNotEmpty()) {
            return "You can only play lands when the stack is empty"
        }

        // Check land drop availability
        val landDrops = state.getEntity(action.playerId)?.get<LandDropsComponent>()
            ?: LandDropsComponent()
        if (!landDrops.canPlayLand) {
            return "You have already played a land this turn"
        }

        // Check card exists and is a land
        val container = state.getEntity(action.cardId)
            ?: return "Card not found: ${action.cardId}"

        val cardComponent = container.get<CardComponent>()
            ?: return "Not a card: ${action.cardId}"

        if (!cardComponent.typeLine.isLand) {
            return "You can only play land cards as lands"
        }

        // Check card is in hand
        val handZone = ZoneKey(action.playerId, Zone.HAND)
        if (action.cardId !in state.getZone(handZone)) {
            return "Land is not in your hand"
        }

        return null
    }

    override fun execute(state: GameState, action: PlayLand): ExecutionResult {
        val container = state.getEntity(action.cardId)
            ?: return ExecutionResult.error(state, "Card not found")

        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Not a card")

        var newState = state

        // Remove from hand
        val handZone = ZoneKey(action.playerId, Zone.HAND)
        newState = newState.removeFromZone(handZone, action.cardId)

        // Add to battlefield
        val battlefieldZone = ZoneKey(action.playerId, Zone.BATTLEFIELD)
        newState = newState.addToZone(battlefieldZone, action.cardId)

        // Add controller component
        newState = newState.updateEntity(action.cardId) { c ->
            c.with(ControllerComponent(action.playerId))
        }

        // Use up a land drop
        newState = newState.updateEntity(action.playerId) { c ->
            val landDrops = c.get<LandDropsComponent>() ?: LandDropsComponent()
            c.with(landDrops.use())
        }

        val zoneChangeEvent = ZoneChangeEvent(
            action.cardId,
            cardComponent.name,
            Zone.HAND,
            Zone.BATTLEFIELD,
            action.playerId
        )

        val events = listOf(zoneChangeEvent)
        newState = newState.tick()

        // Detect and process any triggers from the land entering (e.g., landfall)
        val triggers = triggerDetector.detectTriggers(newState, events)
        if (triggers.isNotEmpty()) {
            val triggerResult = triggerProcessor.processTriggers(newState, triggers)

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

        return ExecutionResult.success(newState, events)
    }

    companion object {
        fun create(context: ActionContext): PlayLandHandler {
            return PlayLandHandler(context.triggerDetector, context.triggerProcessor)
        }
    }
}
