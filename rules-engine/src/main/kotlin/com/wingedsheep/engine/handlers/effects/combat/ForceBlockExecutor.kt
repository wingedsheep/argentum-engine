package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils.resolveTarget
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.ForceBlockEffect
import kotlin.reflect.KClass

/**
 * Executor for ForceBlockEffect.
 * "Target creature blocks this creature this combat if able."
 *
 * Creates a floating effect forcing the target to block the source attacker.
 * Unlike ProvokeExecutor, does NOT untap the target creature.
 */
class ForceBlockExecutor : EffectExecutor<ForceBlockEffect> {

    override val effectType: KClass<ForceBlockEffect> = ForceBlockEffect::class

    override fun execute(
        state: GameState,
        effect: ForceBlockEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for force block effect")

        val targetContainer = state.getEntity(targetId)
            ?: return ExecutionResult.error(state, "Target creature no longer exists")
        val cardComponent = targetContainer.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Target is not a card")
        if (!cardComponent.typeLine.isCreature) {
            return ExecutionResult.error(state, "Target is not a creature")
        }

        // The source is the creature that must be blocked (the attacker)
        val sourceId = context.sourceId
            ?: return ExecutionResult.error(state, "No source for force block effect")

        // Verify source is attacking
        val sourceContainer = state.getEntity(sourceId)
            ?: return ExecutionResult.error(state, "Source creature no longer exists")
        if (!sourceContainer.has<AttackingComponent>()) {
            return ExecutionResult.error(state, "Source creature is not attacking")
        }

        // Create a floating effect forcing the target to block the source
        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.MustBlockSpecificAttacker(sourceId),
            affectedEntities = setOf(targetId),
            duration = Duration.EndOfTurn,
            context = context
        )

        return ExecutionResult.success(newState)
    }
}
