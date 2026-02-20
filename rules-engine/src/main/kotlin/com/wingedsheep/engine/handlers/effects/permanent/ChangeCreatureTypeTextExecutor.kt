package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.effects.ChangeCreatureTypeTextEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for ChangeCreatureTypeTextEffect.
 *
 * "Change the text of target spell or permanent by replacing all instances of one
 * creature type with another."
 *
 * This executor:
 * 1. Resolves the target entity
 * 2. Presents a ChooseOptionDecision with all creature types for the FROM type
 * 3. Pushes a ChooseFromCreatureTypeContinuation (with excludedTypes) for the next step
 *
 * The ContinuationHandler handles steps 2 and 3:
 * - ChooseFromCreatureTypeContinuation: presents TO type choice (excluding types from effect)
 * - ChooseToCreatureTypeContinuation: applies the TextReplacementComponent
 */
class ChangeCreatureTypeTextExecutor(
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<ChangeCreatureTypeTextEffect> {

    override val effectType: KClass<ChangeCreatureTypeTextEffect> =
        ChangeCreatureTypeTextEffect::class

    override fun execute(
        state: GameState,
        effect: ChangeCreatureTypeTextEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = EffectExecutorUtils.resolveTarget(effect.target, context)
            ?: return ExecutionResult.success(state.tick())

        // Target must still exist (on battlefield or stack)
        if (targetId !in state.getBattlefield() && targetId !in state.stack) {
            return ExecutionResult.success(state.tick())
        }

        val allCreatureTypes = Subtype.ALL_CREATURE_TYPES
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        // Prefill search with the target's first creature subtype (if it's a creature)
        val targetCard = state.getEntity(targetId)?.get<CardComponent>()
        val defaultSearch = targetCard?.typeLine?.subtypes
            ?.firstOrNull { it.value in allCreatureTypes }
            ?.value

        val decisionId = UUID.randomUUID().toString()
        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = context.controllerId,
            prompt = "Choose the creature type to replace",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = allCreatureTypes,
            defaultSearch = defaultSearch
        )

        val continuation = ChooseFromCreatureTypeContinuation(
            decisionId = decisionId,
            controllerId = context.controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            targetId = targetId,
            creatureTypes = allCreatureTypes,
            excludedTypes = effect.excludedTypes
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
