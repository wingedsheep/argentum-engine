package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CantBeCopiedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CopyEachTargetSpellEffect
import kotlin.reflect.KClass

/**
 * Executor for [CopyEachTargetSpellEffect] (CR 707.10).
 *
 * Reads every spell target chosen for this spell/ability (all
 * [ChosenTarget.Spell] entries in the resolution context) and creates one copy of each.
 * For each copied spell that has its own targets, pauses with a [ChooseTargetsDecision]
 * so the controller may pick new targets for that copy; copies whose source has no
 * (flat) targets — including modal spells, whose copies inherit their chosen modes and
 * targets verbatim — are created immediately. Spells flagged "can't be copied" are
 * skipped.
 *
 * Models "Copy any number of target instant and/or sorcery spells. You may choose new
 * targets for the copies." (Display of Power).
 */
class CopyEachTargetSpellExecutor(
    private val cardRegistry: com.wingedsheep.engine.registry.CardRegistry,
    private val targetFinder: TargetFinder = TargetFinder()
) : EffectExecutor<CopyEachTargetSpellEffect> {

    override val effectType: KClass<CopyEachTargetSpellEffect> = CopyEachTargetSpellEffect::class

    override fun execute(
        state: GameState,
        effect: CopyEachTargetSpellEffect,
        context: EffectContext
    ): EffectResult {
        // The spells to copy are every Spell target chosen for this effect.
        val spellIds = context.targets
            .filterIsInstance<ChosenTarget.Spell>()
            .map { it.spellEntityId }
            .distinct()

        val stackResolver = StackResolver(cardRegistry = cardRegistry)
        return EffectResult.from(
            driveCopyEachSpell(
                state = state,
                stackResolver = stackResolver,
                targetFinder = targetFinder,
                controllerId = context.controllerId,
                remainingSpellIds = spellIds,
                keywordsForCopy = effect.keywordsForCopy.toSet(),
                removeLegendary = effect.removeLegendary,
                priorEvents = emptyList()
            )
        )
    }

    companion object {
        /**
         * Process the queue of spells to copy, one at a time. Creates copies that need
         * no retargeting immediately; pauses (pushing a [CopyEachSpellContinuation] with
         * the still-unprocessed queue, head first) when the current copy can be retargeted.
         */
        fun driveCopyEachSpell(
            state: GameState,
            stackResolver: StackResolver,
            targetFinder: TargetFinder,
            controllerId: EntityId,
            remainingSpellIds: List<EntityId>,
            keywordsForCopy: Set<String>,
            removeLegendary: Boolean,
            priorEvents: List<GameEvent>
        ): ExecutionResult {
            var currentState = state
            val allEvents = priorEvents.toMutableList()
            var queue = remainingSpellIds

            while (queue.isNotEmpty()) {
                val spellId = queue.first()
                val rest = queue.drop(1)
                val container = currentState.getEntity(spellId)

                // Source already left the stack, or can't be copied — skip it.
                if (container == null || container.has<CantBeCopiedComponent>()) {
                    queue = rest
                    continue
                }

                val cardComponent = container.get<CardComponent>()
                val targetReqs = container.get<TargetsComponent>()?.targetRequirements ?: emptyList()

                // No flat targets (untargeted or modal spell): copy verbatim now.
                if (targetReqs.isEmpty()) {
                    val copyResult = stackResolver.putSpellCopy(
                        state = currentState,
                        sourceSpellId = spellId,
                        controllerId = controllerId
                    )
                    if (!copyResult.isSuccess) return copyResult
                    currentState = StormCopyEffectExecutor.applyCopyMutations(
                        copyResult.newState, copyResult.events, keywordsForCopy, removeLegendary
                    )
                    allEvents.addAll(copyResult.events)
                    queue = rest
                    continue
                }

                // Targeted spell — offer new targets for the copy.
                val legalTargetsMap = mutableMapOf<Int, List<EntityId>>()
                for ((index, requirement) in targetReqs.withIndex()) {
                    legalTargetsMap[index] = targetFinder.findLegalTargets(
                        currentState, requirement, controllerId, spellId
                    )
                }

                // 707.10c: no legal replacement — copy inherits the source's (now-illegal)
                // targets and fizzles on resolution per 608.2b / 112.3b.
                if (legalTargetsMap.any { (_, t) -> t.isEmpty() }) {
                    val copyResult = stackResolver.putSpellCopy(
                        state = currentState,
                        sourceSpellId = spellId,
                        controllerId = controllerId
                    )
                    if (!copyResult.isSuccess) return copyResult
                    currentState = StormCopyEffectExecutor.applyCopyMutations(
                        copyResult.newState, copyResult.events, keywordsForCopy, removeLegendary
                    )
                    allEvents.addAll(copyResult.events)
                    queue = rest
                    continue
                }

                val spellName = cardComponent?.name ?: "spell"
                val decisionId = "copy-each-spell-target-${System.nanoTime()}"
                val continuation = CopyEachSpellContinuation(
                    decisionId = decisionId,
                    remainingSpellIds = queue,
                    controllerId = controllerId,
                    targetRequirements = targetReqs,
                    keywordsForCopy = keywordsForCopy,
                    removeLegendary = removeLegendary
                )
                val decision = ChooseTargetsDecision(
                    id = decisionId,
                    playerId = controllerId,
                    prompt = "Choose new targets for copy of $spellName",
                    context = DecisionContext(
                        phase = DecisionPhase.CASTING,
                        sourceName = spellName,
                        effectHint = "Copy of $spellName"
                    ),
                    targetRequirements = targetReqs.mapIndexed { index, req ->
                        TargetRequirementInfo(index = index, description = req.description)
                    },
                    legalTargets = legalTargetsMap
                )

                val paused = currentState.withPendingDecision(decision).pushContinuation(continuation)
                return ExecutionResult.paused(paused, decision, allEvents)
            }

            return ExecutionResult.success(currentState, allEvents)
        }
    }
}
