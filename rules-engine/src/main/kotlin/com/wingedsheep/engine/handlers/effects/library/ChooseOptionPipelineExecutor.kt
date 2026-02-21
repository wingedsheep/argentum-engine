package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.effects.ChooseOptionEffect
import com.wingedsheep.sdk.scripting.effects.OptionType
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for ChooseOptionEffect.
 *
 * Presents a ChooseOptionDecision with options determined by the OptionType.
 * When the player responds, the chosen value is stored in the pipeline's
 * EffectContext.chosenValues (via ChooseOptionPipelineContinuation) for
 * subsequent effects.
 */
class ChooseOptionPipelineExecutor : EffectExecutor<ChooseOptionEffect> {

    override val effectType: KClass<ChooseOptionEffect> = ChooseOptionEffect::class

    override fun execute(
        state: GameState,
        effect: ChooseOptionEffect,
        context: EffectContext
    ): ExecutionResult {
        val controllerId = context.controllerId
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        val allOptions = when (effect.optionType) {
            OptionType.CREATURE_TYPE -> Subtype.ALL_CREATURE_TYPES
            OptionType.COLOR -> listOf("White", "Blue", "Black", "Red", "Green")
        }

        val excludedLower = effect.excludedOptions.map { it.lowercase() }.toSet()
        val options = allOptions.filter { it.lowercase() !in excludedLower }

        val prompt = effect.prompt ?: when (effect.optionType) {
            OptionType.CREATURE_TYPE -> "Choose a creature type"
            OptionType.COLOR -> "Choose a color"
        }

        val decisionId = UUID.randomUUID().toString()
        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = prompt,
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = options
        )

        val continuation = ChooseOptionPipelineContinuation(
            decisionId = decisionId,
            controllerId = controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            storeAs = effect.storeAs,
            options = options
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
