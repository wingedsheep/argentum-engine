package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DeflectDamageSourceChoiceContinuation
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.DeflectNextDamageFromChosenSourceEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for DeflectNextDamageFromChosenSourceEffect.
 *
 * Presents the controller with a choice of all game objects that could be damage sources
 * (permanents on the battlefield + spells on the stack), then creates a floating effect
 * that prevents the next damage from the chosen source and deals that much to the
 * source's controller.
 */
class DeflectNextDamageFromChosenSourceExecutor : EffectExecutor<DeflectNextDamageFromChosenSourceEffect> {

    override val effectType: KClass<DeflectNextDamageFromChosenSourceEffect> =
        DeflectNextDamageFromChosenSourceEffect::class

    override fun execute(
        state: GameState,
        effect: DeflectNextDamageFromChosenSourceEffect,
        context: EffectContext
    ): ExecutionResult {
        val controllerId = context.controllerId

        // Gather all possible sources: permanents on battlefield + spells on stack
        val sources = mutableListOf<Pair<EntityId, String>>()

        // Add all permanents on the battlefield
        for (entityId in state.getBattlefield()) {
            val entity = state.getEntity(entityId) ?: continue
            val faceDown = entity.get<FaceDownComponent>()
            val card = entity.get<CardComponent>()
            val name = if (faceDown != null) "Face-down creature" else card?.name ?: continue
            sources.add(entityId to name)
        }

        // Add all spells on the stack
        for (entityId in state.stack) {
            val entity = state.getEntity(entityId) ?: continue
            val card = entity.get<CardComponent>() ?: continue
            sources.add(entityId to "${card.name} (on stack)")
        }

        if (sources.isEmpty()) {
            // No sources to choose from — effect does nothing
            return ExecutionResult.success(state)
        }

        val decisionId = UUID.randomUUID().toString()
        val options = sources.map { it.second }
        val sourceIds = sources.map { it.first }
        val optionCardIds = sources.mapIndexed { index, (id, _) -> index to listOf(id) }.toMap()

        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = "Choose a source of damage",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
            ),
            options = options,
            optionCardIds = optionCardIds
        )

        val continuation = DeflectDamageSourceChoiceContinuation(
            decisionId = decisionId,
            controllerId = controllerId,
            sourceId = context.sourceId,
            sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name },
            sourceOptions = sourceIds
        )

        val newState = state.withPendingDecision(decision).pushContinuation(continuation)
        return ExecutionResult.paused(newState, decision)
    }
}
