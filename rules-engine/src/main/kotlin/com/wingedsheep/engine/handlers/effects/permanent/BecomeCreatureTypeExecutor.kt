package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.BecomeCreatureTypeEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for BecomeCreatureTypeEffect.
 *
 * "Target creature becomes the creature type of your choice until end of turn."
 *
 * This executor:
 * 1. Resolves the target creature
 * 2. Presents a ChooseOptionDecision with all creature types
 * 3. Pushes a BecomeCreatureTypeContinuation for the next step
 */
class BecomeCreatureTypeExecutor : EffectExecutor<BecomeCreatureTypeEffect> {

    override val effectType: KClass<BecomeCreatureTypeEffect> = BecomeCreatureTypeEffect::class

    override fun execute(
        state: GameState,
        effect: BecomeCreatureTypeEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.success(state.tick())

        // Target must still be on the battlefield
        if (targetId !in state.getBattlefield()) {
            return ExecutionResult.success(state.tick())
        }

        val targetCard = state.getEntity(targetId)?.get<CardComponent>()
            ?: return ExecutionResult.success(state.tick())

        if (!targetCard.typeLine.isCreature) {
            return ExecutionResult.success(state.tick())
        }

        val allCreatureTypes = Subtype.ALL_CREATURE_TYPES
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        // Prefill search with the target's first creature subtype
        val defaultSearch = targetCard.typeLine.subtypes
            .firstOrNull { it.value in allCreatureTypes }
            ?.value

        val decisionId = UUID.randomUUID().toString()
        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = context.controllerId,
            prompt = "Choose a creature type",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = allCreatureTypes,
            defaultSearch = defaultSearch
        )

        val continuation = BecomeCreatureTypeContinuation(
            decisionId = decisionId,
            controllerId = context.controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            targetId = targetId,
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
                    playerId = context.controllerId,
                    decisionType = "CHOOSE_OPTION",
                    prompt = decision.prompt
                )
            )
        )
    }
}
