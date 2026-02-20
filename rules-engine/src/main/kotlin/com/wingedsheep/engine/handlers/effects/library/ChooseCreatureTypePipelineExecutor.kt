package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.effects.ChooseCreatureTypeEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for ChooseCreatureTypeEffect.
 *
 * Presents a ChooseOptionDecision with all creature types. When the player
 * responds, the chosen type is stored in the pipeline's EffectContext
 * (via ChooseCreatureTypePipelineContinuation) for subsequent effects.
 */
class ChooseCreatureTypePipelineExecutor : EffectExecutor<ChooseCreatureTypeEffect> {

    override val effectType: KClass<ChooseCreatureTypeEffect> = ChooseCreatureTypeEffect::class

    override fun execute(
        state: GameState,
        effect: ChooseCreatureTypeEffect,
        context: EffectContext
    ): ExecutionResult {
        val controllerId = context.controllerId
        val allCreatureTypes = Subtype.ALL_CREATURE_TYPES
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

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

        val continuation = ChooseCreatureTypePipelineContinuation(
            decisionId = decisionId,
            controllerId = controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            creatureTypes = allCreatureTypes
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
