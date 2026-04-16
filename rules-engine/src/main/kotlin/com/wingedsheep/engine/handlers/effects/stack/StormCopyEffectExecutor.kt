package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.StormCopyEffect
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import kotlin.reflect.KClass

/**
 * Executor for StormCopyEffect.
 * Creates copies of a spell on the stack for the Storm mechanic.
 *
 * If the spell has no targets, all copies are created immediately.
 * If the spell has targets, pauses for target selection for the first copy,
 * then uses StormCopyTargetContinuation for remaining copies.
 */
class StormCopyEffectExecutor(
    private val cardRegistry: com.wingedsheep.engine.registry.CardRegistry,
    private val targetFinder: TargetFinder = TargetFinder()
) : EffectExecutor<StormCopyEffect> {

    override val effectType: KClass<StormCopyEffect> = StormCopyEffect::class

    override fun execute(
        state: GameState,
        effect: StormCopyEffect,
        context: EffectContext
    ): EffectResult {
        if (effect.copyCount <= 0) {
            return EffectResult.success(state)
        }

        val stackResolver = StackResolver(cardRegistry = cardRegistry)

        // Modal source (700.2g): targets live per-mode on the original's
        // [SpellOnStackComponent], not as a flat TargetsComponent. Inherit modes
        // and per-mode targets onto each copy directly — no flat re-targeting.
        val sourceSpell = context.sourceId?.let { state.getEntity(it)?.get<SpellOnStackComponent>() }
        if (sourceSpell != null && sourceSpell.chosenModes.isNotEmpty()) {
            return createAllCopiesNoTargets(state, effect, context, stackResolver)
        }

        // If spell has no targets, create all copies immediately
        if (effect.spellTargetRequirements.isEmpty()) {
            return createAllCopiesNoTargets(state, effect, context, stackResolver)
        }

        // Spell has targets — need to ask for target selection for each copy
        return promptForNextCopyTarget(state, effect, context, effect.copyCount)
    }

    private fun createAllCopiesNoTargets(
        state: GameState,
        effect: StormCopyEffect,
        context: EffectContext,
        stackResolver: StackResolver
    ): EffectResult {
        val sourceId = context.sourceId
            ?: return EffectResult.error(state, "Storm copy has no source spell to copy")

        var currentState = state
        val allEvents = mutableListOf<GameEvent>()

        for (i in 1..effect.copyCount) {
            // Put the copy on the stack as a spell (707.12). Modes/targets default to the
            // source's — putSpellCopy reads them off the source SpellOnStackComponent.
            val result = EffectResult.from(
                stackResolver.putSpellCopy(
                    state = currentState,
                    sourceSpellId = sourceId,
                    copyIndex = i,
                    copyTotal = effect.copyCount,
                    controllerId = context.controllerId
                )
            )
            if (!result.isSuccess) return result
            currentState = result.newState
            allEvents.addAll(result.events)
        }

        return EffectResult.success(currentState, allEvents)
    }

    private fun promptForNextCopyTarget(
        state: GameState,
        effect: StormCopyEffect,
        context: EffectContext,
        remainingCopies: Int
    ): EffectResult {
        val decisionId = "storm-copy-target-${System.nanoTime()}"

        // Find legal targets for the copy
        val legalTargetsMap = mutableMapOf<Int, List<EntityId>>()
        for ((index, requirement) in effect.spellTargetRequirements.withIndex()) {
            val legalTargets = targetFinder.findLegalTargets(
                state, requirement, context.controllerId, context.sourceId
            )
            legalTargetsMap[index] = legalTargets
        }

        // If no legal targets for any requirement, skip this copy and remaining copies
        val hasNoLegalTargets = legalTargetsMap.any { (_, targets) -> targets.isEmpty() }
        if (hasNoLegalTargets) {
            return EffectResult.success(state)
        }

        // Push continuation for resuming after target selection
        val continuation = StormCopyTargetContinuation(
            decisionId = decisionId,
            remainingCopies = remainingCopies,
            spellEffect = effect.spellEffect,
            spellTargetRequirements = effect.spellTargetRequirements,
            spellName = effect.spellName,
            controllerId = context.controllerId,
            sourceId = context.sourceId ?: EntityId.generate(),
            totalCopies = effect.copyCount
        )
        val targetReqInfos = effect.spellTargetRequirements.mapIndexed { index, req ->
            TargetRequirementInfo(
                index = index,
                description = req.description
            )
        }

        val copyNumber = effect.copyCount - remainingCopies + 1
        val decision = ChooseTargetsDecision(
            id = decisionId,
            playerId = context.controllerId,
            prompt = "Choose targets for Storm copy $copyNumber of ${effect.spellName}",
            context = DecisionContext(
                phase = DecisionPhase.CASTING,
                sourceName = effect.spellName
            ),
            targetRequirements = targetReqInfos,
            legalTargets = legalTargetsMap
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return EffectResult.paused(stateWithContinuation, decision)
    }
}
