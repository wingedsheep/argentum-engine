package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.mechanics.stack.StackPlacement
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CantBeCopiedComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CopyForEachOtherPossibleTargetEffect
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import kotlin.reflect.KClass

/**
 * Executor for [CopyForEachOtherPossibleTargetEffect] — CR 707.10d, the Zada / Mirrorwing Dragon /
 * Agrus Kos shape: copy a spell or ability once for each *other* object it could target, each copy
 * auto-assigned a distinct one of those objects.
 *
 * Unlike [CopyTargetSpellExecutor], nothing here pauses for a decision. The candidate set and each
 * copy's target both fall out of the board:
 *
 *  1. The effect's **copier** is the reference player throughout — it controls the copies, and the
 *     effect's `candidates` filter and each candidate's legality are evaluated relative to it. "You
 *     copy" (Zada, Agrus Kos) and "that player copies … they control" (Mirrorwing Dragon, which
 *     watches every seat) differ only in which player that is.
 *  2. A candidate must be a legal target for **every** instance of the word "target" on the copied
 *     object (707.10d) — so the legal-target sets of all its target requirements are intersected,
 *     which also folds in hexproof, shroud, protection and per-requirement filters for free.
 *  3. The objects the original already targets are removed — the "each **other** …" of the card text.
 *  4. Each surviving candidate gets one copy, filling every target slot of the original (707.10d: "if
 *     the spell or ability has more than one target, each of its targets must be the same player or
 *     object"). A modal original's per-mode targets are rewritten the same way (700.2g).
 *
 * 707.10d puts the copies on the stack "in the order of their controller's choice". No card in the
 * family cares — the copies are independent and the order only decides resolution order among
 * simultaneously-created copies — so they go on in battlefield order rather than costing the player a
 * decision.
 *
 * An object flagged can't-be-copied yields no copies at all.
 */
class CopyForEachOtherPossibleTargetExecutor(
    private val targetFinder: TargetFinder,
    private val predicateEvaluator: PredicateEvaluator
) : EffectExecutor<CopyForEachOtherPossibleTargetEffect> {

    override val effectType: KClass<CopyForEachOtherPossibleTargetEffect> =
        CopyForEachOtherPossibleTargetEffect::class

    override fun execute(
        state: GameState,
        effect: CopyForEachOtherPossibleTargetEffect,
        context: EffectContext
    ): EffectResult {
        // The object may have left the stack (countered, or it resolved before this trigger did), in
        // which case there is nothing to copy. That is the same known limitation every other
        // "copy that spell" trigger has — see Thousand-Year Storm's note on last-known information
        // for stack objects.
        val originalId = context.resolveTarget(effect.target)
            ?: return EffectResult.success(state)
        val container = state.getEntity(originalId)
            ?: return EffectResult.success(state)
        if (container.has<CantBeCopiedComponent>()) return EffectResult.success(state)

        val triggered = container.get<TriggeredAbilityOnStackComponent>()
        val activated = container.get<ActivatedAbilityOnStackComponent>()
        val isAbility = triggered != null || activated != null
        if (!isAbility && !container.has<SpellOnStackComponent>()) return EffectResult.success(state)

        val copierId = TargetResolutionUtils.resolvePlayerRef(effect.copier, context, state)
            ?: return EffectResult.success(state)

        val targetsComponent = container.get<TargetsComponent>()
        val requirements = targetsComponent?.targetRequirements ?: emptyList()
        // An object with no targets has nothing it "could target", so there is no candidate set and
        // no copies. (Every trigger in the family requires a target anyway.)
        if (requirements.isEmpty()) return EffectResult.success(state)

        // Legality is judged for a copy the copier controls; the targeting source is the spell
        // itself, or the permanent an ability came from (what protection and "another target"
        // read).
        val targetingSourceId = triggered?.sourceId ?: activated?.sourceId ?: originalId
        // Protection / hexproof "from activated (or triggered) abilities" reads the kind of object
        // doing the targeting, so a copy can't be aimed past it (707.10d: "is just ignored").
        val targetingSourceType = when {
            triggered != null -> TargetingSourceType.TRIGGERED_ABILITY
            activated != null -> TargetingSourceType.ACTIVATED_ABILITY
            else -> TargetingSourceType.SPELL
        }
        val candidates = candidatesFor(
            state, effect, requirements, targetsComponent, copierId, targetingSourceId, targetingSourceType
        )
        if (candidates.isEmpty()) return EffectResult.success(state)

        var currentState = state
        val allEvents = mutableListOf<GameEvent>()
        candidates.forEachIndexed { index, candidateId ->
            val copyResult = if (isAbility) {
                pushAbilityCopy(currentState, container, originalId, copierId, candidateId, requirements)
            } else {
                pushSpellCopy(currentState, container, originalId, copierId, candidateId, requirements, index, candidates.size)
            }
            if (copyResult.outcome is Outcome.Done) {
                currentState = copyResult.newState
                allEvents.addAll(copyResult.events)
            }
        }

        return EffectResult.success(currentState, allEvents)
    }

    private fun candidatesFor(
        state: GameState,
        effect: CopyForEachOtherPossibleTargetEffect,
        requirements: List<TargetRequirement>,
        targetsComponent: TargetsComponent?,
        copierId: EntityId,
        targetingSourceId: EntityId,
        targetingSourceType: TargetingSourceType
    ): List<EntityId> {
        // Legal for *every* instance of "target" — intersect the per-requirement legal sets, keeping
        // the first requirement's ordering so the copies go on the stack deterministically.
        var couldTarget: List<EntityId> = targetFinder.findLegalTargets(
            state, requirements.first(), controllerId = copierId, sourceId = targetingSourceId,
            targetingSourceType = targetingSourceType
        )
        for (requirement in requirements.drop(1)) {
            if (couldTarget.isEmpty()) break
            val legal = targetFinder.findLegalTargets(
                state, requirement, controllerId = copierId, sourceId = targetingSourceId,
                targetingSourceType = targetingSourceType
            ).toSet()
            couldTarget = couldTarget.filter { it in legal }
        }

        // "each other …" — drop what the original already targets.
        val alreadyTargeted = targetsComponent?.targets.orEmpty()
            .filterIsInstance<ChosenTarget.Permanent>()
            .map { it.entityId }
            .toSet()

        val predicateContext = PredicateContext(controllerId = copierId, sourceId = targetingSourceId)
        val projected = state.projectedState
        return couldTarget.filter { candidateId ->
            candidateId !in alreadyTargeted &&
                predicateEvaluator.matches(state, projected, candidateId, effect.candidates, predicateContext)
        }
    }

    private fun pushSpellCopy(
        state: GameState,
        container: ComponentContainer,
        spellId: EntityId,
        copierId: EntityId,
        candidateId: EntityId,
        requirements: List<TargetRequirement>,
        index: Int,
        total: Int
    ): ExecutionResult {
        // A modal spell keeps its chosen modes (700.2g — "the copies will have the same mode; a
        // different mode cannot be chosen"), but its targets live per-mode as well as flat. Retarget
        // both, or the copy's mode would still point at the original target.
        val copyModeTargets = container.get<SpellOnStackComponent>()?.modeTargetsOrdered
            ?.map { perMode -> perMode.map { ChosenTarget.Permanent(candidateId) } }
            ?.takeIf { it.isNotEmpty() }
        return StackPlacement.putSpellCopy(
            state = state,
            sourceSpellId = spellId,
            targets = requirements.map { ChosenTarget.Permanent(candidateId) },
            modeTargetsOrdered = copyModeTargets,
            copyIndex = index + 1,
            copyTotal = total,
            controllerId = copierId
        )
    }

    private fun pushAbilityCopy(
        state: GameState,
        container: ComponentContainer,
        abilityId: EntityId,
        copierId: EntityId,
        candidateId: EntityId,
        requirements: List<TargetRequirement>
    ): ExecutionResult {
        val targets = requirements.map { ChosenTarget.Permanent(candidateId) }
        // A modal triggered ability carries per-mode targets too (700.2g) — retarget them with the
        // flat list, as for a spell.
        container.get<TriggeredAbilityOnStackComponent>()?.let { triggered ->
            val clone = CopyTargetTriggeredAbilityExecutor.cloneAbility(triggered, copierId)
            val copy = clone.copy(
                modeTargetsOrdered = clone.modeTargetsOrdered.map { perMode ->
                    perMode.map { ChosenTarget.Permanent(candidateId) }
                }
            )
            return StackPlacement.putTriggeredAbility(state, copy, targets, requirements)
        }
        return CopyTargetSpellOrAbilityExecutor.cloneAndPush(state, abilityId, copierId, targets, requirements)
    }
}
