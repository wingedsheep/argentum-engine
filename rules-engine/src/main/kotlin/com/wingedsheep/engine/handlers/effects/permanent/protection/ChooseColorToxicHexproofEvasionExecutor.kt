package com.wingedsheep.engine.handlers.effects.permanent.protection

import com.wingedsheep.engine.core.ChooseColorToxicHexproofEvasionContinuation
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.ChooseColorGrantToxicHexproofAndCantBeBlockedByColorEffect
import kotlin.reflect.KClass

/**
 * Executor for choose-color effects that grant toxic, hexproof from that color,
 * and "can't be blocked by creatures of that color" to a target.
 */
class ChooseColorToxicHexproofEvasionExecutor(
    private val decisionHandler: DecisionHandler
) : EffectExecutor<ChooseColorGrantToxicHexproofAndCantBeBlockedByColorEffect> {

    override val effectType: KClass<ChooseColorGrantToxicHexproofAndCantBeBlockedByColorEffect> =
        ChooseColorGrantToxicHexproofAndCantBeBlockedByColorEffect::class

    override fun execute(
        state: GameState,
        effect: ChooseColorGrantToxicHexproofAndCantBeBlockedByColorEffect,
        context: EffectContext
    ): EffectResult {
        val targetEntityId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.error(state, "Could not resolve target for color evasion effect")

        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "Unknown"
        val decisionResult = decisionHandler.createColorDecision(
            state = state,
            playerId = context.controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            prompt = "Choose a color",
            phase = DecisionPhase.RESOLUTION
        )

        val continuation = ChooseColorToxicHexproofEvasionContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            controllerId = context.controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            targetEntityId = targetEntityId,
            toxicAmount = effect.toxicAmount,
            duration = effect.duration
        )

        return EffectResult.paused(
            decisionResult.state.pushContinuation(continuation),
            decisionResult.pendingDecision,
            decisionResult.events
        )
    }
}
