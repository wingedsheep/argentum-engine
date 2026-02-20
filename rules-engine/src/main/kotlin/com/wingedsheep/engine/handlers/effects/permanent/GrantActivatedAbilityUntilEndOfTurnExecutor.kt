package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.event.GrantedActivatedAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.GrantActivatedAbilityUntilEndOfTurnEffect
import kotlin.reflect.KClass

/**
 * Executor for GrantActivatedAbilityUntilEndOfTurnEffect.
 * "Target creature gains '[activated ability]' until end of turn"
 *
 * Adds the activated ability to GameState.grantedActivatedAbilities,
 * where GameSession will find it when computing legal actions and
 * ActivateAbilityHandler will find it when validating activations.
 */
class GrantActivatedAbilityUntilEndOfTurnExecutor : EffectExecutor<GrantActivatedAbilityUntilEndOfTurnEffect> {

    override val effectType: KClass<GrantActivatedAbilityUntilEndOfTurnEffect> =
        GrantActivatedAbilityUntilEndOfTurnEffect::class

    override fun execute(
        state: GameState,
        effect: GrantActivatedAbilityUntilEndOfTurnEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for activated ability grant")

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

        val grant = GrantedActivatedAbility(
            entityId = targetId,
            ability = effect.ability,
            duration = effect.duration
        )

        val newState = state.copy(
            grantedActivatedAbilities = state.grantedActivatedAbilities + grant
        )

        return ExecutionResult.success(newState)
    }
}
