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
 * (via ChooseOptionPipelineContinuation with storeAs="chosenCreatureType")
 * for subsequent effects.
 */
class ChooseCreatureTypePipelineExecutor : EffectExecutor<ChooseCreatureTypeEffect> {

    override val effectType: KClass<ChooseCreatureTypeEffect> = ChooseCreatureTypeEffect::class

    override fun execute(
        state: GameState,
        effect: ChooseCreatureTypeEffect,
        context: EffectContext
    ): EffectResult {
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

        val continuation = ChooseOptionPipelineContinuation(
            decisionId = decisionId,
            controllerId = controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            storeAs = CHOSEN_CREATURE_TYPE_KEY,
            options = allCreatureTypes
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return EffectResult.paused(
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

    companion object {
        /** Key used to store chosen creature type — triggers special handling in the resumer */
        const val CHOSEN_CREATURE_TYPE_KEY = "chosenCreatureType"
    }
}
