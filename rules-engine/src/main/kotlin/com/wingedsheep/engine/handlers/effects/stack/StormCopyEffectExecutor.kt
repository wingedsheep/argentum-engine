package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ChosenTarget
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
        // [SpellOnStackComponent], not as a flat TargetsComponent. Modes are fixed
        // for every copy, but per 702.40a the copy controller may pick new targets
        // for each mode — iterate per-mode / per-copy via StormCopyModalTargetContinuation.
        val sourceSpell = context.sourceId?.let { state.getEntity(it)?.get<SpellOnStackComponent>() }
        if (sourceSpell != null && sourceSpell.chosenModes.isNotEmpty()) {
            val sourceId = context.sourceId
            val hasAnyTargetedMode = sourceSpell.chosenModes.any { modeIdx ->
                sourceSpell.modeTargetRequirements[modeIdx]?.isNotEmpty() == true
            }
            if (!hasAnyTargetedMode) {
                return createAllCopiesNoTargets(state, effect, context, stackResolver)
            }
            return EffectResult.from(driveStormModalCopies(
                state = state,
                stackResolver = stackResolver,
                targetFinder = targetFinder,
                sourceId = sourceId,
                controllerId = context.controllerId,
                spellName = effect.spellName,
                chosenModes = sourceSpell.chosenModes,
                modeTargetRequirements = sourceSpell.modeTargetRequirements,
                accumulatedOrdinalTargets = emptyList(),
                currentOrdinal = 0,
                remainingCopies = effect.copyCount,
                totalCopies = effect.copyCount,
                priorEvents = emptyList()
            ))
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
        val sourceId = context.sourceId
            ?: return EffectResult.error(state, "Storm copy has no source spell to copy")
        val stackResolver = StackResolver(cardRegistry = cardRegistry)

        var currentState = state
        val allEvents = mutableListOf<GameEvent>()
        var copiesLeft = remainingCopies

        // Walk copies one at a time. If the copy can be retargeted legally, pause
        // with a ChooseTargetsDecision; otherwise put it on the stack inheriting
        // the source's (now-illegal) targets per 707.7b — it fizzles on resolution
        // per 608.2b / 112.3b.
        while (copiesLeft > 0) {
            val legalTargetsMap = mutableMapOf<Int, List<EntityId>>()
            for ((index, requirement) in effect.spellTargetRequirements.withIndex()) {
                val legalTargets = targetFinder.findLegalTargets(
                    currentState, requirement, context.controllerId, sourceId
                )
                legalTargetsMap[index] = legalTargets
            }

            val hasNoLegalTargets = legalTargetsMap.any { (_, targets) -> targets.isEmpty() }
            if (hasNoLegalTargets) {
                val copyIndex = effect.copyCount - copiesLeft + 1
                val copyResult = stackResolver.putSpellCopy(
                    state = currentState,
                    sourceSpellId = sourceId,
                    copyIndex = copyIndex,
                    copyTotal = effect.copyCount,
                    controllerId = context.controllerId
                )
                if (!copyResult.isSuccess) return EffectResult.from(copyResult)
                currentState = copyResult.newState
                allEvents.addAll(copyResult.events)
                copiesLeft--
                continue
            }

            val decisionId = "storm-copy-target-${System.nanoTime()}"
            val continuation = StormCopyTargetContinuation(
                decisionId = decisionId,
                remainingCopies = copiesLeft,
                spellEffect = effect.spellEffect,
                spellTargetRequirements = effect.spellTargetRequirements,
                spellName = effect.spellName,
                controllerId = context.controllerId,
                sourceId = sourceId,
                totalCopies = effect.copyCount
            )
            val targetReqInfos = effect.spellTargetRequirements.mapIndexed { index, req ->
                TargetRequirementInfo(
                    index = index,
                    description = req.description
                )
            }

            val copyNumber = effect.copyCount - copiesLeft + 1
            val copyLabel = if (effect.copyCount > 1)
                "copy $copyNumber of ${effect.copyCount} of ${effect.spellName}"
                else "copy of ${effect.spellName}"
            val decision = ChooseTargetsDecision(
                id = decisionId,
                playerId = context.controllerId,
                prompt = "Choose new targets for $copyLabel",
                context = DecisionContext(
                    phase = DecisionPhase.CASTING,
                    sourceName = effect.spellName,
                    effectHint = "Copy of ${effect.spellName}"
                ),
                targetRequirements = targetReqInfos,
                legalTargets = legalTargetsMap
            )

            val stateWithDecision = currentState.withPendingDecision(decision)
            val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

            return EffectResult.paused(stateWithContinuation, decision, allEvents)
        }

        return EffectResult.success(currentState, allEvents)
    }

    companion object {
        /**
         * Drive per-copy / per-mode retargeting for a modal Storm source.
         *
         * Loops through [chosenModes] for the current copy; for each mode with
         * target requirements it pauses with a [ChooseTargetsDecision] and pushes a
         * [StormCopyModalTargetContinuation]; modes without requirements inherit
         * an empty target slot. When all ordinals are collected the copy is
         * pushed onto the stack via [StackResolver.putSpellCopy] and the loop
         * restarts for the next copy.
         *
         * Called from [execute] on first entry and from the resumer after each
         * TargetsResponse appends a mode's targets.
         */
        fun driveStormModalCopies(
            state: GameState,
            stackResolver: StackResolver,
            targetFinder: TargetFinder,
            sourceId: EntityId,
            controllerId: EntityId,
            spellName: String,
            chosenModes: List<Int>,
            modeTargetRequirements: Map<Int, List<TargetRequirement>>,
            accumulatedOrdinalTargets: List<List<ChosenTarget>>,
            currentOrdinal: Int,
            remainingCopies: Int,
            totalCopies: Int,
            priorEvents: List<GameEvent>
        ): ExecutionResult {
            var currentState = state
            val allEvents = priorEvents.toMutableList()
            var accumulated = accumulatedOrdinalTargets
            var ordinal = currentOrdinal
            var copiesLeft = remainingCopies

            val sourceSpellComp = currentState.getEntity(sourceId)?.get<SpellOnStackComponent>()
                ?: return ExecutionResult.error(currentState, "Storm source spell not found: $sourceId")
            val sourceModeTargetsOrdered = sourceSpellComp.modeTargetsOrdered

            while (copiesLeft > 0) {
                while (ordinal < chosenModes.size) {
                    val modeIndex = chosenModes[ordinal]
                    val reqs = modeTargetRequirements[modeIndex] ?: emptyList()

                    if (reqs.isEmpty()) {
                        accumulated = accumulated + listOf(emptyList())
                        ordinal++
                        continue
                    }

                    val legalTargetsMap = mutableMapOf<Int, List<EntityId>>()
                    for ((reqIndex, requirement) in reqs.withIndex()) {
                        legalTargetsMap[reqIndex] = targetFinder.findLegalTargets(
                            currentState, requirement, controllerId, sourceId
                        )
                    }

                    val hasNoLegalTargets = legalTargetsMap.any { (_, t) -> t.isEmpty() }
                    if (hasNoLegalTargets) {
                        // 707.7b: no legal replacement — the copy still goes on the stack
                        // with the source's (now-illegal) targets for this mode and fizzles
                        // on resolution per 608.2b / 112.3b.
                        accumulated = accumulated + listOf(sourceModeTargetsOrdered.getOrNull(ordinal) ?: emptyList())
                        ordinal++
                        continue
                    }

                    val decisionId = "storm-copy-modal-target-${System.nanoTime()}"
                    val copyNumber = totalCopies - copiesLeft + 1
                    val copyLabel = if (totalCopies > 1) "copy $copyNumber of $totalCopies of $spellName"
                        else "copy of $spellName"
                    val modeLabel = if (chosenModes.size > 1) " — mode ${ordinal + 1} of ${chosenModes.size}"
                        else ""
                    val decision = ChooseTargetsDecision(
                        id = decisionId,
                        playerId = controllerId,
                        prompt = "Choose new targets for $copyLabel$modeLabel",
                        context = DecisionContext(
                            phase = DecisionPhase.CASTING,
                            sourceName = spellName,
                            effectHint = "Copy of $spellName$modeLabel"
                        ),
                        targetRequirements = reqs.mapIndexed { index, req ->
                            TargetRequirementInfo(
                                index = index,
                                description = req.description
                            )
                        },
                        legalTargets = legalTargetsMap
                    )

                    val continuation = StormCopyModalTargetContinuation(
                        decisionId = decisionId,
                        remainingCopies = copiesLeft,
                        totalCopies = totalCopies,
                        spellName = spellName,
                        controllerId = controllerId,
                        sourceId = sourceId,
                        chosenModes = chosenModes,
                        modeTargetRequirements = modeTargetRequirements,
                        accumulatedOrdinalTargets = accumulated,
                        currentOrdinal = ordinal
                    )

                    val pausedState = currentState
                        .withPendingDecision(decision)
                        .pushContinuation(continuation)
                    return ExecutionResult.paused(pausedState, decision, allEvents)
                }

                val copyIndex = totalCopies - copiesLeft + 1
                val copyResult = stackResolver.putSpellCopy(
                    state = currentState,
                    sourceSpellId = sourceId,
                    chosenModes = chosenModes,
                    modeTargetsOrdered = accumulated,
                    modeTargetRequirements = modeTargetRequirements,
                    copyIndex = copyIndex,
                    copyTotal = totalCopies,
                    controllerId = controllerId
                )
                if (!copyResult.isSuccess) return copyResult
                currentState = copyResult.newState
                allEvents.addAll(copyResult.events)

                copiesLeft--
                accumulated = emptyList()
                ordinal = 0
            }

            return ExecutionResult.success(currentState, allEvents)
        }
    }
}
