package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.ChooseNumberForSourceContinuation
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.ChooseNumberForSourceEffect
import kotlin.reflect.KClass

/**
 * Executor for [ChooseNumberForSourceEffect].
 *
 * Pauses for a [com.wingedsheep.engine.core.ChooseNumberDecision] in [[minValue], [maxValue]] and
 * pushes a [ChooseNumberForSourceContinuation]; the resumer writes the chosen number durably onto
 * the source permanent's cast-choices bag under the effect's slot (replacing any prior value), so
 * a characteristic-defining ability reads the latest choice. With no source (e.g. a spell with no
 * permanent) the effect is a no-op.
 */
class ChooseNumberForSourceExecutor(
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<ChooseNumberForSourceEffect> {

    override val effectType: KClass<ChooseNumberForSourceEffect> = ChooseNumberForSourceEffect::class

    override fun execute(
        state: GameState,
        effect: ChooseNumberForSourceEffect,
        context: EffectContext
    ): EffectResult {
        val sourceId = context.sourceId ?: return EffectResult.success(state)
        if (state.getEntity(sourceId) == null) return EffectResult.success(state)

        val sourceName = state.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Unknown"

        val decisionResult = decisionHandler.createNumberDecision(
            state = state,
            playerId = context.controllerId,
            sourceId = sourceId,
            sourceName = sourceName,
            prompt = effect.prompt,
            minValue = effect.minValue,
            maxValue = effect.maxValue,
            phase = DecisionPhase.RESOLUTION
        )

        val decision = decisionResult.pendingDecision!!

        val continuation = ChooseNumberForSourceContinuation(
            decisionId = decision.id,
            sourceId = sourceId,
            controllerId = context.controllerId,
            slot = effect.slot
        )

        return EffectResult.paused(
            decisionResult.state.pushContinuation(continuation),
            decision,
            decisionResult.events
        )
    }
}
