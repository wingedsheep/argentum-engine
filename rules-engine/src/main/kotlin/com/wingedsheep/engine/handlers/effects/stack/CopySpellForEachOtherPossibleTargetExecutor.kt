package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CantBeCopiedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CopySpellForEachOtherPossibleTargetEffect
import kotlin.reflect.KClass

/**
 * Executor for [CopySpellForEachOtherPossibleTargetEffect] — CR 707.10d, the Zada / Mirrorwing Dragon
 * shape: copy a spell once for each *other* object it could target, each copy auto-assigned a distinct
 * one of those objects.
 *
 * Unlike [CopyTargetSpellExecutor], nothing here pauses for a decision. The candidate set and each
 * copy's target both fall out of the board:
 *
 *  1. The copied spell's **controller** (its `casterId`) is the reference player throughout — it
 *     controls the copies, and the effect's `candidates` filter is evaluated relative to it. That is
 *     what makes "each other creature **they** control … **that player** copies" (Mirrorwing Dragon,
 *     which watches every seat) and "each other creature **you** control" (Zada, which only ever sees
 *     its own controller's casts) the same effect.
 *  2. A candidate must be a legal target for **every** instance of the word "target" on the spell
 *     (707.10d) — so the legal-target sets of all the spell's target requirements are intersected,
 *     which also folds in hexproof, shroud, protection and per-requirement filters for free.
 *  3. The objects the spell already targets are removed — the "each **other** …" of the card text.
 *  4. Each surviving candidate gets one copy, filling every target slot of the spell (707.10d: "if the
 *     spell or ability has more than one target, each of its targets must be the same player or
 *     object").
 *
 * 707.10d puts the copies on the stack "in the order of their controller's choice". No card in the
 * family cares — the copies are independent and the order only decides resolution order among
 * simultaneously-created copies — so they go on in battlefield order rather than costing the player a
 * decision.
 *
 * A spell flagged can't-be-copied (CR 707.10) yields no copies at all.
 */
class CopySpellForEachOtherPossibleTargetExecutor(
    private val cardRegistry: com.wingedsheep.engine.registry.CardRegistry,
    private val targetFinder: TargetFinder = TargetFinder(),
    private val predicateEvaluator: PredicateEvaluator = PredicateEvaluator()
) : EffectExecutor<CopySpellForEachOtherPossibleTargetEffect> {

    override val effectType: KClass<CopySpellForEachOtherPossibleTargetEffect> =
        CopySpellForEachOtherPossibleTargetEffect::class

    override fun execute(
        state: GameState,
        effect: CopySpellForEachOtherPossibleTargetEffect,
        context: EffectContext
    ): EffectResult {
        // The spell may have left the stack (countered, or it resolved before this trigger did), in
        // which case there is nothing to copy. That is the same known limitation every other
        // "copy that spell" trigger has — see Thousand-Year Storm's note on last-known information
        // for stack objects.
        val spellEntityId = context.resolveTarget(effect.spell)
            ?: return EffectResult.success(state)
        val container = state.getEntity(spellEntityId)
            ?: return EffectResult.success(state)
        if (container.has<CantBeCopiedComponent>()) return EffectResult.success(state)

        // CR 707.10d's reference player: the copied spell's controller, not this ability's.
        val casterId = container.get<SpellOnStackComponent>()?.casterId
            ?: return EffectResult.success(state)

        val targetsComponent = container.get<TargetsComponent>()
        val requirements = targetsComponent?.targetRequirements ?: emptyList()
        // A spell with no targets has nothing it "could target", so there is no candidate set and no
        // copies. (The Zada-family triggers all require a target anyway.)
        if (requirements.isEmpty()) return EffectResult.success(state)

        // Legal for *every* instance of "target" — intersect the per-requirement legal sets, keeping
        // the first requirement's ordering so the copies go on the stack deterministically.
        var couldTarget: List<EntityId> = targetFinder.findLegalTargets(
            state, requirements.first(), controllerId = casterId, sourceId = spellEntityId
        )
        for (requirement in requirements.drop(1)) {
            if (couldTarget.isEmpty()) break
            val legal = targetFinder.findLegalTargets(
                state, requirement, controllerId = casterId, sourceId = spellEntityId
            ).toSet()
            couldTarget = couldTarget.filter { it in legal }
        }

        // "each other …" — drop what the spell already targets.
        val alreadyTargeted = targetsComponent?.targets.orEmpty()
            .filterIsInstance<ChosenTarget.Permanent>()
            .map { it.entityId }
            .toSet()

        val predicateContext = PredicateContext(controllerId = casterId, sourceId = spellEntityId)
        val projected = state.projectedState
        val candidates = couldTarget.filter { candidateId ->
            candidateId !in alreadyTargeted &&
                predicateEvaluator.matches(
                    state, projected, candidateId, effect.candidates, predicateContext
                )
        }
        if (candidates.isEmpty()) return EffectResult.success(state)

        // A modal spell keeps its chosen modes (700.2g — "the copies will have the same mode; a
        // different mode cannot be chosen"), but its targets live per-mode as well as flat. Retarget
        // both, or the copy's mode would still point at the original target.
        val sourceModeTargets = container.get<SpellOnStackComponent>()?.modeTargetsOrdered

        val stackResolver = StackResolver(cardRegistry = cardRegistry)
        var currentState = state
        val allEvents = mutableListOf<GameEvent>()
        candidates.forEachIndexed { index, candidateId ->
            // Every target slot of the copy points at this one object (707.10d).
            val copyTargets = requirements.map { ChosenTarget.Permanent(candidateId) }
            val copyModeTargets = sourceModeTargets
                ?.map { perMode -> perMode.map { ChosenTarget.Permanent(candidateId) } }
                ?.takeIf { it.isNotEmpty() }
            val copyResult: ExecutionResult = stackResolver.putSpellCopy(
                state = currentState,
                sourceSpellId = spellEntityId,
                targets = copyTargets,
                modeTargetsOrdered = copyModeTargets,
                copyIndex = index + 1,
                copyTotal = candidates.size,
                controllerId = casterId
            )
            if (copyResult.isSuccess) {
                currentState = copyResult.newState
                allEvents.addAll(copyResult.events)
            }
        }

        return EffectResult.success(currentState, allEvents)
    }
}
