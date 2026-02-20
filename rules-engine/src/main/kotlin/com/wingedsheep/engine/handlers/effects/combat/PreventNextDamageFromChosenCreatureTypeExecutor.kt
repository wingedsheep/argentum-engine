package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ChosenCreatureTypeComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.PreventNextDamageFromChosenCreatureTypeEffect
import kotlin.reflect.KClass

/**
 * Executor for PreventNextDamageFromChosenCreatureTypeEffect.
 * "{1}{W}: The next time a creature of the chosen type would deal damage to you this turn, prevent that damage."
 *
 * Reads the chosen creature type from the source permanent's ChosenCreatureTypeComponent
 * and creates a floating prevention shield on the controller.
 */
class PreventNextDamageFromChosenCreatureTypeExecutor : EffectExecutor<PreventNextDamageFromChosenCreatureTypeEffect> {

    override val effectType: KClass<PreventNextDamageFromChosenCreatureTypeEffect> =
        PreventNextDamageFromChosenCreatureTypeEffect::class

    override fun execute(
        state: GameState,
        effect: PreventNextDamageFromChosenCreatureTypeEffect,
        context: EffectContext
    ): ExecutionResult {
        val sourceId = context.sourceId
            ?: return ExecutionResult.error(state, "No source for PreventNextDamageFromChosenCreatureTypeEffect")

        val sourceEntity = state.getEntity(sourceId)
            ?: return ExecutionResult.error(state, "Source entity not found: $sourceId")

        val chosenType = sourceEntity.get<ChosenCreatureTypeComponent>()?.creatureType
            ?: return ExecutionResult.error(state, "No chosen creature type on source: $sourceId")

        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.ABILITY,
                sublayer = null,
                modification = SerializableModification.PreventNextDamageFromCreatureType(chosenType),
                affectedEntities = setOf(context.controllerId)
            ),
            duration = Duration.EndOfTurn,
            sourceId = sourceId,
            sourceName = sourceEntity.get<CardComponent>()?.name,
            controllerId = context.controllerId,
            timestamp = System.currentTimeMillis()
        )

        val newState = state.copy(
            floatingEffects = state.floatingEffects + floatingEffect
        )

        return ExecutionResult.success(newState)
    }
}
