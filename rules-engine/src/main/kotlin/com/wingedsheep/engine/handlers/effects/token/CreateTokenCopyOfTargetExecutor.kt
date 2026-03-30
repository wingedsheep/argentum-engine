package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfTargetEffect
import kotlin.reflect.KClass

/**
 * Executor for CreateTokenCopyOfTargetEffect.
 *
 * Creates N token copies of a targeted permanent (resolved via EffectTarget).
 * Used for "Create X tokens that are copies of target token you control."
 */
class CreateTokenCopyOfTargetExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator(),
    private val staticAbilityHandler: StaticAbilityHandler? = null
) : EffectExecutor<CreateTokenCopyOfTargetEffect> {

    override val effectType: KClass<CreateTokenCopyOfTargetEffect> = CreateTokenCopyOfTargetEffect::class

    override fun execute(
        state: GameState,
        effect: CreateTokenCopyOfTargetEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = TargetResolutionUtils.resolveTarget(effect.target, context, state)
            ?: return ExecutionResult.success(state)

        val targetContainer = state.getEntity(targetId)
            ?: return ExecutionResult.success(state)

        val targetCard = targetContainer.get<CardComponent>()
            ?: return ExecutionResult.success(state)

        val count = amountEvaluator.evaluate(state, effect.count, context)
        if (count <= 0) return ExecutionResult.success(state)

        val controllerId = context.controllerId
        var newState = state
        val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()

        repeat(count) {
            val tokenId = EntityId.generate()
            val tokenCard = targetCard.copy(ownerId = controllerId)

            var container = ComponentContainer.of(
                tokenCard,
                TokenComponent,
                ControllerComponent(controllerId),
                SummoningSicknessComponent
            )

            if (staticAbilityHandler != null) {
                container = staticAbilityHandler.addContinuousEffectComponent(container)
                container = staticAbilityHandler.addReplacementEffectComponent(container)
            }

            newState = newState.withEntity(tokenId, container)
            val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
            newState = newState.addToZone(battlefieldZone, tokenId)

            events.add(
                ZoneChangeEvent(
                    entityId = tokenId,
                    entityName = tokenCard.name,
                    fromZone = null,
                    toZone = Zone.BATTLEFIELD,
                    ownerId = controllerId
                )
            )
        }

        return ExecutionResult.success(newState, events)
    }
}
