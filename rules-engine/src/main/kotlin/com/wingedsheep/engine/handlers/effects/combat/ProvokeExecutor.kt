package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.untapOrConsumeStun
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.ProvokeEffect
import kotlin.reflect.KClass

/**
 * Executor for ProvokeEffect.
 * "Target creature defending player controls untaps and blocks this creature if able."
 *
 * 1. Untaps the target creature
 * 2. Creates a floating effect forcing the target to block the source attacker
 */
class ProvokeExecutor : EffectExecutor<ProvokeEffect> {

    override val effectType: KClass<ProvokeEffect> = ProvokeEffect::class

    override fun execute(
        state: GameState,
        effect: ProvokeEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.error(state, "No valid target for provoke effect")

        val targetContainer = state.getEntity(targetId)
            ?: return EffectResult.error(state, "Target creature no longer exists")
        targetContainer.get<CardComponent>()
            ?: return EffectResult.error(state, "Target is not a card")
        // Projected, not printed: an animated land is a creature, a de-animated creature is not.
        if (!state.projectedState.isCreature(targetId)) {
            return EffectResult.error(state, "Target is not a creature")
        }

        // The source is the creature with provoke (the attacker)
        val sourceId = context.sourceId
            ?: return EffectResult.error(state, "No source for provoke effect")

        // Verify source is attacking
        val sourceContainer = state.getEntity(sourceId)
            ?: return EffectResult.error(state, "Source creature no longer exists")
        if (!sourceContainer.has<AttackingComponent>()) {
            return EffectResult.error(state, "Source creature is not attacking")
        }

        // Step 1: Untap the target creature
        val (untappedState, untapEvents) = untapOrConsumeStun(state, targetId)
        var newState = untappedState

        // Step 2: Create a floating effect forcing the target to block the source
        newState = newState.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.MustBlockSpecificAttacker(sourceId),
            affectedEntities = setOf(targetId),
            duration = Duration.EndOfTurn,
            context = context
        )

        return EffectResult.success(newState, untapEvents)
    }
}
