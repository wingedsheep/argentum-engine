package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.event.DelayedTriggeredAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.moveCardToZone
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.ExileUntilEndStepEffect
import com.wingedsheep.sdk.scripting.MoveToZoneEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for ExileUntilEndStepEffect.
 *
 * Exiles a creature and creates a delayed triggered ability that returns it
 * to the battlefield under its owner's control at the beginning of the next end step.
 */
class ExileUntilEndStepExecutor : EffectExecutor<ExileUntilEndStepEffect> {

    override val effectType: KClass<ExileUntilEndStepEffect> = ExileUntilEndStepEffect::class

    override fun execute(
        state: GameState,
        effect: ExileUntilEndStepEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for exile until end step")

        // Check entity still exists
        state.getEntity(targetId)
            ?: return ExecutionResult.error(state, "Target entity not found: $targetId")

        // Move the creature to exile
        val exileResult = moveCardToZone(state, targetId, Zone.EXILE)
        if (!exileResult.isSuccess) {
            return exileResult
        }

        // Create a delayed trigger to return it at the beginning of the next end step
        val returnEffect = MoveToZoneEffect(
            target = EffectTarget.SpecificEntity(targetId),
            destination = Zone.BATTLEFIELD
        )

        val delayedTrigger = DelayedTriggeredAbility(
            id = UUID.randomUUID().toString(),
            effect = returnEffect,
            fireAtStep = Step.END,
            sourceId = context.sourceId ?: targetId,
            sourceName = "Astral Slide",
            controllerId = context.controllerId
        )

        val newState = exileResult.newState.addDelayedTrigger(delayedTrigger)

        return ExecutionResult.success(newState, exileResult.events)
    }
}
