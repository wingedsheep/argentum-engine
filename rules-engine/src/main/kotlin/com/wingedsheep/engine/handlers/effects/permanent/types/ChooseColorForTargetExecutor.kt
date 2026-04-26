package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.ChooseColorForTargetContinuation
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.ChooseColorForTargetEffect
import kotlin.reflect.KClass

class ChooseColorForTargetExecutor(
    private val decisionHandler: DecisionHandler
) : EffectExecutor<ChooseColorForTargetEffect> {

    override val effectType: KClass<ChooseColorForTargetEffect> = ChooseColorForTargetEffect::class

    override fun execute(
        state: GameState,
        effect: ChooseColorForTargetEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state) ?: return EffectResult.success(state)
        if (!state.getBattlefield().contains(targetId)) return EffectResult.success(state)

        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
        val decisionResult = decisionHandler.createColorDecision(
            state = state,
            playerId = context.controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            prompt = effect.prompt,
            phase = DecisionPhase.RESOLUTION
        )
        val decision = decisionResult.pendingDecision ?: return EffectResult.success(decisionResult.state)

        val continuation = ChooseColorForTargetContinuation(
            decisionId = decision.id,
            controllerId = context.controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            targetEntityId = targetId
        )

        return EffectResult.paused(
            decisionResult.state.pushContinuation(continuation),
            decision,
            decisionResult.events
        )
    }
}
