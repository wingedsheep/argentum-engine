package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.effects.BecomeChosenTypeAllCreaturesEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for BecomeChosenTypeAllCreaturesEffect.
 *
 * "Choose a creature type other than Wall. Each creature becomes that type until end of turn."
 *
 * This executor:
 * 1. Presents a ChooseOptionDecision with creature types (excluding any in excludedTypes)
 * 2. Pushes a BecomeChosenTypeAllCreaturesContinuation for the next step
 */
class BecomeChosenTypeAllCreaturesExecutor : EffectExecutor<BecomeChosenTypeAllCreaturesEffect> {

    override val effectType: KClass<BecomeChosenTypeAllCreaturesEffect> =
        BecomeChosenTypeAllCreaturesEffect::class

    override fun execute(
        state: GameState,
        effect: BecomeChosenTypeAllCreaturesEffect,
        context: EffectContext
    ): ExecutionResult {
        val controllerId = context.controllerId
        val allCreatureTypes = if (effect.excludedTypes.isNotEmpty()) {
            Subtype.ALL_CREATURE_TYPES.filter { it !in effect.excludedTypes }
        } else {
            Subtype.ALL_CREATURE_TYPES
        }
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

        val continuation = BecomeChosenTypeAllCreaturesContinuation(
            decisionId = decisionId,
            controllerId = controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            creatureTypes = allCreatureTypes,
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
