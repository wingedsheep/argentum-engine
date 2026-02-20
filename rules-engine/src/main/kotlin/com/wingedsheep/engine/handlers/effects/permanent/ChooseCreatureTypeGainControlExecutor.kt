package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ChooseCreatureTypeGainControlEffect
import com.wingedsheep.sdk.scripting.Duration
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for ChooseCreatureTypeGainControlEffect.
 *
 * "Choose a creature type. If you control more creatures of that type than each
 * other player, you gain control of all creatures of that type."
 *
 * Used by Peer Pressure.
 */
class ChooseCreatureTypeGainControlExecutor : EffectExecutor<ChooseCreatureTypeGainControlEffect> {

    override val effectType: KClass<ChooseCreatureTypeGainControlEffect> =
        ChooseCreatureTypeGainControlEffect::class

    override fun execute(
        state: GameState,
        effect: ChooseCreatureTypeGainControlEffect,
        context: EffectContext
    ): ExecutionResult {
        val controllerId = context.controllerId
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        val allCreatureTypes = Subtype.ALL_CREATURE_TYPES
        val decisionId = UUID.randomUUID().toString()
        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = "Choose a creature type",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = allCreatureTypes
        )

        val continuation = ChooseCreatureTypeGainControlContinuation(
            decisionId = decisionId,
            controllerId = controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            creatureTypes = allCreatureTypes,
            duration = effect.duration
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = controllerId,
                    decisionType = "CHOOSE_OPTION",
                    prompt = decision.prompt
                )
            )
        )
    }

    companion object {
        private val stateProjector = StateProjector()

        fun applyChooseCreatureTypeGainControl(
            state: GameState,
            chosenType: String,
            controllerId: EntityId,
            sourceId: EntityId?,
            sourceName: String?,
            duration: Duration
        ): ExecutionResult {
            val projected = stateProjector.project(state)

            // Count creatures of the chosen type per player
            val counts = state.turnOrder.associateWith { playerId ->
                state.getBattlefield().count { entityId ->
                    val container = state.getEntity(entityId) ?: return@count false
                    val cardComponent = container.get<CardComponent>() ?: return@count false
                    val isCreature = cardComponent.typeLine.isCreature || container.has<FaceDownComponent>()
                    if (!isCreature) return@count false
                    val controlledBy = container.get<ControllerComponent>()?.playerId
                    controlledBy == playerId && projected.hasSubtype(entityId, chosenType)
                }
            }

            val controllerCount = counts[controllerId] ?: 0
            if (controllerCount == 0) {
                return ExecutionResult.success(state, emptyList())
            }

            // Check if controller has strictly more than EACH other player
            val otherPlayerCounts = counts.filter { it.key != controllerId }
            val controllerHasMore = otherPlayerCounts.all { controllerCount > it.value }

            if (!controllerHasMore) {
                return ExecutionResult.success(state, emptyList())
            }

            // Gain control of all creatures of that type not already controlled by the controller
            var newState = state
            val events = mutableListOf<GameEvent>()

            for (entityId in state.getBattlefield()) {
                val container = newState.getEntity(entityId) ?: continue
                val cardComponent = container.get<CardComponent>() ?: continue
                val isCreature = cardComponent.typeLine.isCreature || container.has<FaceDownComponent>()
                if (!isCreature) continue
                if (!projected.hasSubtype(entityId, chosenType)) continue

                val currentControllerId = container.get<ControllerComponent>()?.playerId
                if (currentControllerId == controllerId) continue

                // Remove any previous Layer.CONTROL floating effects from the same source on the same target
                val filteredEffects = newState.floatingEffects.filter { floating ->
                    !(floating.sourceId == sourceId &&
                      floating.effect.layer == Layer.CONTROL &&
                      entityId in floating.effect.affectedEntities)
                }

                val floatingEffect = ActiveFloatingEffect(
                    id = EntityId.generate(),
                    effect = FloatingEffectData(
                        layer = Layer.CONTROL,
                        sublayer = null,
                        modification = SerializableModification.ChangeController(controllerId),
                        affectedEntities = setOf(entityId)
                    ),
                    duration = duration,
                    sourceId = sourceId,
                    sourceName = sourceName,
                    controllerId = controllerId,
                    timestamp = System.currentTimeMillis()
                )

                newState = newState.copy(
                    floatingEffects = filteredEffects + floatingEffect
                )

                events.add(
                    ControlChangedEvent(
                        permanentId = entityId,
                        permanentName = cardComponent.name,
                        oldControllerId = currentControllerId ?: controllerId,
                        newControllerId = controllerId
                    )
                )
            }

            return ExecutionResult.success(newState, events)
        }
    }
}
