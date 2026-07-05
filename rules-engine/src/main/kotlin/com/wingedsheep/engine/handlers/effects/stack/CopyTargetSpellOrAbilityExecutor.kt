package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CantBeCopiedComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CopyTargetSpellEffect
import com.wingedsheep.sdk.scripting.effects.CopyTargetSpellOrAbilityEffect
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import kotlin.reflect.KClass

/**
 * Executor for [CopyTargetSpellOrAbilityEffect] — "copy target instant/sorcery spell, activated
 * ability, or triggered ability. You may choose new targets for the copy." (Return the Favor), plus
 * the multi-copy form "copy target activated or triggered ability you control X times" (Gogo, Master
 * of Mimicry).
 *
 * The single target requirement can resolve to any of three stack-object kinds, so this executor
 * dispatches at resolution on the chosen entity's stack component (CR 707.10 — each copy is created
 * on the stack, controlled by the copier, inheriting cast-time choices; CR 707.10c lets the copier
 * pick new targets):
 *
 *  - **Spell** (instant/sorcery, has no ability component) → delegate to [CopyTargetSpellExecutor]
 *    (always a single copy; the spell branch does not honor [CopyTargetSpellOrAbilityEffect.copies]).
 *  - **Activated or triggered ability** → [driveAbilityCopies] makes `copies` independent copies,
 *    pausing per copy that has targets so the copier can retarget it. A no-target ability is copied
 *    just the same (CR — Gogo can copy any ability on the stack, not only targeted ones).
 *
 * CR 707.10e: an ability tagged [CantBeCopiedComponent] ("This ability can't be copied") produces no
 * copies at all.
 */
class CopyTargetSpellOrAbilityExecutor(
    private val cardRegistry: CardRegistry,
    private val targetFinder: TargetFinder = TargetFinder()
) : EffectExecutor<CopyTargetSpellOrAbilityEffect> {

    override val effectType: KClass<CopyTargetSpellOrAbilityEffect> =
        CopyTargetSpellOrAbilityEffect::class

    private val spellExecutor = CopyTargetSpellExecutor(cardRegistry, targetFinder)
    private val dynamicAmountEvaluator = DynamicAmountEvaluator()

    override fun execute(
        state: GameState,
        effect: CopyTargetSpellOrAbilityEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.error(state, "No target spell or ability to copy")

        val container = state.getEntity(targetId)
            ?: return EffectResult.error(state, "Target spell or ability entity not found on stack")

        val isAbility = container.has<TriggeredAbilityOnStackComponent>() ||
            container.has<ActivatedAbilityOnStackComponent>()

        // CR 707.10e — "This ability can't be copied": make no copy of a flagged ability.
        if (isAbility && container.has<CantBeCopiedComponent>()) {
            return EffectResult.success(state)
        }

        return when {
            isAbility -> {
                val copyCount = dynamicAmountEvaluator.evaluate(state, effect.copies, context)
                if (copyCount <= 0) return EffectResult.success(state)
                EffectResult.from(
                    driveAbilityCopies(
                        state = state,
                        stackResolver = StackResolver(cardRegistry = cardRegistry),
                        targetFinder = targetFinder,
                        abilityEntityId = targetId,
                        controllerId = context.controllerId,
                        copierSourceId = context.sourceId,
                        remainingCopies = copyCount,
                        totalCopies = copyCount,
                        priorEvents = emptyList()
                    )
                )
            }

            // Otherwise it's a spell on the stack (instant/sorcery). The spell executor handles
            // modal / targeted / no-target paths and the choose-new-targets prompt. The spell branch
            // always makes a single copy (Return the Favor); `copies` applies only to abilities.
            else -> spellExecutor.execute(state, CopyTargetSpellEffect(effect.target), context)
        }
    }

    companion object {
        /**
         * Drive the "copy this ability [remainingCopies] times" loop. Each iteration clones the
         * source ability on the stack (activated or triggered) into a fresh copy controlled by
         * [controllerId] and pushes it. When the source has targets and legal replacements exist, it
         * pauses with a [ChooseTargetsDecision] + [CopyAbilityTargetContinuation] so the copier picks
         * new targets for that copy (CR 707.10c); the loop resumes for the next copy from the
         * resumer. A no-target ability, or one whose targets have no legal replacement, is copied
         * without a prompt (the latter inheriting the source's — possibly now-illegal — targets, per
         * CR 707.10c). Copies are pushed one at a time, so they end up above the source and resolve
         * before it.
         *
         * Called from [execute] on first entry and from the resumer after each copy's targets are
         * chosen.
         */
        fun driveAbilityCopies(
            state: GameState,
            stackResolver: StackResolver,
            targetFinder: TargetFinder,
            abilityEntityId: EntityId,
            controllerId: EntityId,
            copierSourceId: EntityId?,
            remainingCopies: Int,
            totalCopies: Int,
            priorEvents: List<GameEvent>
        ): ExecutionResult {
            var currentState = state
            val allEvents = priorEvents.toMutableList()
            var copiesLeft = remainingCopies

            while (copiesLeft > 0) {
                val container = currentState.getEntity(abilityEntityId)
                    ?: return ExecutionResult.success(currentState, allEvents) // source gone — stop
                val targetRequirements = container.get<TargetsComponent>()?.targetRequirements
                    ?: emptyList()

                // No targets — clone and push directly (CR: any ability may be copied, not just
                // targeted ones).
                if (targetRequirements.isEmpty()) {
                    val push = cloneAndPush(currentState, stackResolver, abilityEntityId, controllerId)
                    if (!push.isSuccess) return push
                    currentState = push.newState
                    allEvents.addAll(push.events)
                    copiesLeft--
                    continue
                }

                // Legal targets are found from the *copier's* perspective (its source), matching
                // how new targets for a copy are validated — not from the copied ability's entity.
                val legalTargetsMap = legalTargetsFor(
                    currentState, targetFinder, targetRequirements, controllerId, copierSourceId
                )

                // CR 707.10c: no legal replacement for some requirement — the copy is still made,
                // inheriting the source's targets (which may be illegal, so it's removed on
                // resolution per CR 608.2b). The board doesn't change between copies, so this holds
                // for every remaining copy.
                if (legalTargetsMap.any { (_, targets) -> targets.isEmpty() }) {
                    val inherited = container.get<TargetsComponent>()?.targets ?: emptyList()
                    val push = cloneAndPush(
                        currentState, stackResolver, abilityEntityId, controllerId,
                        inherited, targetRequirements
                    )
                    if (!push.isSuccess) return push
                    currentState = push.newState
                    allEvents.addAll(push.events)
                    copiesLeft--
                    continue
                }

                // Legal replacements exist — pause so the copier chooses new targets for this copy.
                val sourceName = sourceNameOf(container)
                val copyNumber = totalCopies - copiesLeft + 1
                val copyLabel = if (totalCopies > 1)
                    "copy $copyNumber of $totalCopies of $sourceName's ability"
                else "copy of $sourceName's ability"
                val decisionId = "copy-ability-target-${System.nanoTime()}"
                val decision = ChooseTargetsDecision(
                    id = decisionId,
                    playerId = controllerId,
                    prompt = "Choose new targets for $copyLabel",
                    context = DecisionContext(
                        phase = DecisionPhase.CASTING,
                        sourceName = sourceName,
                        effectHint = "Copy of $sourceName's ability"
                    ),
                    targetRequirements = targetRequirements.mapIndexed { index, req ->
                        TargetRequirementInfo(index = index, description = req.description)
                    },
                    legalTargets = legalTargetsMap
                )
                val continuation = CopyAbilityTargetContinuation(
                    decisionId = decisionId,
                    abilityEntityId = abilityEntityId,
                    controllerId = controllerId,
                    copierSourceId = copierSourceId,
                    targetRequirements = targetRequirements,
                    remainingCopies = copiesLeft,
                    totalCopies = totalCopies
                )
                val paused = currentState
                    .withPendingDecision(decision)
                    .pushContinuation(continuation)
                return ExecutionResult.paused(paused, decision, allEvents)
            }

            return ExecutionResult.success(currentState, allEvents)
        }

        /**
         * Clone the ability at [abilityEntityId] (activated or triggered) into a fresh copy
         * controlled by [controllerId] and push it onto the stack with [targets]. The copy inherits
         * every cast-time value (X, sacrificed/tapped permanents, modal choices) per CR 707.10.
         */
        fun cloneAndPush(
            state: GameState,
            stackResolver: StackResolver,
            abilityEntityId: EntityId,
            controllerId: EntityId,
            targets: List<ChosenTarget> = emptyList(),
            targetRequirements: List<TargetRequirement> = emptyList()
        ): ExecutionResult {
            val container = state.getEntity(abilityEntityId)
                ?: return ExecutionResult.error(state, "Ability to copy no longer on stack")

            container.get<TriggeredAbilityOnStackComponent>()?.let { triggered ->
                val copy = CopyTargetTriggeredAbilityExecutor.cloneAbility(triggered, controllerId)
                return stackResolver.putTriggeredAbility(state, copy, targets, targetRequirements)
            }

            val activated = container.get<ActivatedAbilityOnStackComponent>()
                ?: return ExecutionResult.error(state, "Target entity is not an ability on the stack")
            // CR 707.10: the copy inherits cast-time values and is controlled by the copier. It
            // isn't "activated", so suppress the AbilityActivatedEvent (it would re-fire "whenever
            // you activate an ability" triggers off the copy).
            val copy = activated.copy(controllerId = controllerId)
            return stackResolver.putActivatedAbility(
                state, copy, targets, targetRequirements, emitActivationEvent = false
            )
        }

        private fun legalTargetsFor(
            state: GameState,
            targetFinder: TargetFinder,
            targetRequirements: List<TargetRequirement>,
            controllerId: EntityId,
            sourceId: EntityId?
        ): Map<Int, List<EntityId>> {
            val map = mutableMapOf<Int, List<EntityId>>()
            for ((index, requirement) in targetRequirements.withIndex()) {
                map[index] = targetFinder.findLegalTargets(state, requirement, controllerId, sourceId)
            }
            return map
        }

        private fun sourceNameOf(container: ComponentContainer): String =
            container.get<TriggeredAbilityOnStackComponent>()?.sourceName
                ?: container.get<ActivatedAbilityOnStackComponent>()?.sourceName
                ?: "ability"
    }
}
