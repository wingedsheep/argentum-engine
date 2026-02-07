package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.event.GrantedTriggeredAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.GrantTriggeredAbilityUntilEndOfTurnEffect
import kotlin.reflect.KClass

/**
 * Executor for GrantTriggeredAbilityUntilEndOfTurnEffect.
 * "Target creature gains '[triggered ability]' until end of turn"
 *
 * Adds the triggered ability to GameState.grantedTriggeredAbilities,
 * where TriggerDetector will find it when checking for triggers on
 * that entity.
 */
class GrantTriggeredAbilityUntilEndOfTurnExecutor : EffectExecutor<GrantTriggeredAbilityUntilEndOfTurnEffect> {

    override val effectType: KClass<GrantTriggeredAbilityUntilEndOfTurnEffect> =
        GrantTriggeredAbilityUntilEndOfTurnEffect::class

    override fun execute(
        state: GameState,
        effect: GrantTriggeredAbilityUntilEndOfTurnEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for triggered ability grant")

        // Verify target exists and is a creature on the battlefield
        val targetContainer = state.getEntity(targetId)
            ?: return ExecutionResult.error(state, "Target creature no longer exists")
        val cardComponent = targetContainer.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Target is not a card")
        if (!cardComponent.typeLine.isCreature) {
            return ExecutionResult.error(state, "Target is not a creature")
        }
        if (!state.getBattlefield().contains(targetId)) {
            return ExecutionResult.error(state, "Target is not on the battlefield")
        }

        val grant = GrantedTriggeredAbility(
            entityId = targetId,
            ability = effect.ability,
            duration = effect.duration
        )

        val newState = state.copy(
            grantedTriggeredAbilities = state.grantedTriggeredAbilities + grant
        )

        return ExecutionResult.success(newState)
    }
}
