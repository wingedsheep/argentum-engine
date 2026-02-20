package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.destroyPermanent
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.scripting.effects.DestroyAndChainCopyEffect
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for DestroyAndChainCopyEffect.
 *
 * Destroys the target noncreature permanent, then offers its controller a copy
 * of the spell that can target a new noncreature permanent. The copy itself has
 * the same effect, enabling recursive chaining.
 */
class DestroyAndChainCopyExecutor(
    private val targetFinder: TargetFinder = TargetFinder()
) : EffectExecutor<DestroyAndChainCopyEffect> {

    override val effectType: KClass<DestroyAndChainCopyEffect> = DestroyAndChainCopyEffect::class

    override fun execute(
        state: GameState,
        effect: DestroyAndChainCopyEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.success(state)

        val container = state.getEntity(targetId)
            ?: return ExecutionResult.success(state)

        // Capture the controller BEFORE destruction (destruction strips ControllerComponent)
        val targetControllerId = container.get<ControllerComponent>()?.playerId
            ?: container.get<CardComponent>()?.ownerId
            ?: return ExecutionResult.success(state)

        // Destroy the permanent
        val destroyResult = destroyPermanent(state, targetId)
        if (!destroyResult.isSuccess) {
            return destroyResult
        }

        var newState = destroyResult.state
        val events = destroyResult.events.toMutableList()

        // Check if there are valid noncreature permanent targets for the chain copy
        val requirement = TargetPermanent(filter = effect.targetFilter)
        val legalTargets = targetFinder.findLegalTargets(
            newState, requirement, targetControllerId, context.sourceId
        )

        if (legalTargets.isEmpty()) {
            // No valid targets for a copy — chain ends
            return ExecutionResult.success(newState, events)
        }

        // Offer the destroyed permanent's controller a copy
        val decisionId = UUID.randomUUID().toString()
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        } ?: effect.spellName

        val decision = YesNoDecision(
            id = decisionId,
            playerId = targetControllerId,
            prompt = "Copy ${effect.spellName} and choose a new target?",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            yesText = "Copy",
            noText = "Decline"
        )

        val continuation = ChainCopyDecisionContinuation(
            decisionId = decisionId,
            targetControllerId = targetControllerId,
            targetFilter = effect.targetFilter,
            spellName = effect.spellName,
            sourceId = context.sourceId
        )

        newState = newState.withPendingDecision(decision)
        newState = newState.pushContinuation(continuation)

        return ExecutionResult.paused(
            newState,
            decision,
            events + listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = targetControllerId,
                    decisionType = "YES_NO",
                    prompt = decision.prompt
                )
            )
        )
    }
}
