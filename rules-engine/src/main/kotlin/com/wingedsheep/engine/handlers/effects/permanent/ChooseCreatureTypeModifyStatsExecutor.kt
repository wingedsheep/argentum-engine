package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.ChooseCreatureTypeModifyStatsEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for ChooseCreatureTypeModifyStatsEffect.
 *
 * "Creatures of the creature type of your choice get +X/+Y until end of turn."
 *
 * This executor:
 * 1. Evaluates dynamic amounts (e.g., X value) at resolution time
 * 2. Presents a ChooseOptionDecision with all creature types
 * 3. Pushes a ChooseCreatureTypeModifyStatsContinuation with resolved integer values
 */
class ChooseCreatureTypeModifyStatsExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<ChooseCreatureTypeModifyStatsEffect> {

    override val effectType: KClass<ChooseCreatureTypeModifyStatsEffect> =
        ChooseCreatureTypeModifyStatsEffect::class

    override fun execute(
        state: GameState,
        effect: ChooseCreatureTypeModifyStatsEffect,
        context: EffectContext
    ): ExecutionResult {
        val controllerId = context.controllerId
        val allCreatureTypes = Subtype.ALL_CREATURE_TYPES
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        // Evaluate dynamic amounts at resolution time
        val resolvedPower = amountEvaluator.evaluate(state, effect.powerModifier, context)
        val resolvedToughness = amountEvaluator.evaluate(state, effect.toughnessModifier, context)

        val decisionId = UUID.randomUUID().toString()
        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = "Choose a creature type",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = allCreatureTypes
        )

        val continuation = ChooseCreatureTypeModifyStatsContinuation(
            decisionId = decisionId,
            controllerId = controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            creatureTypes = allCreatureTypes,
            powerModifier = resolvedPower,
            toughnessModifier = resolvedToughness,
            duration = effect.duration
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = controllerId,
                    decisionType = "CHOOSE_OPTION",
                    prompt = decision.prompt
                )
            )
        )
    }
}
