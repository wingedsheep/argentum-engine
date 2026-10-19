package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.targets.withCount

/**
 * Select single targets in order when a filter reads an earlier target — an object's
 * characteristics ("power less than that creature's") or the player it names ("target creature
 * that player controls", Ravager of the Fells). A slot may be optional only when every slot after
 * it is optional too, so declining one ends the selection without shifting a later target.
 *
 * Every slot picks one target except the last, which may pick several ("tap up to five target
 * permanents that player controls", Yosei, the Morning Star): nothing after it reads its choices,
 * so it needs no completion search of its own. Chosen targets travel as one list per slot.
 */
object DependentTargetSelection {
    fun isRequired(requirements: List<TargetRequirement>): Boolean = requirements.any {
        it is TargetObject && referencesTarget(it.filter.baseFilter)
    }

    private fun referencesTarget(filter: GameObjectFilter): Boolean =
        filter.cardPredicates.any(::referencesTarget) ||
            filter.statePredicates.any(::referencesTarget) ||
            filter.controllerPredicate?.let(::referencesTarget) == true ||
            filter.anyOf.any(::referencesTarget)

    private fun referencesTarget(predicate: ControllerPredicate): Boolean = when (predicate) {
        is ControllerPredicate.And -> predicate.predicates.any(::referencesTarget)
        is ControllerPredicate.Or -> predicate.predicates.any(::referencesTarget)
        is ControllerPredicate.Not -> referencesTarget(predicate.predicate)
        is ControllerPredicate.ControlledByReferencedPlayer -> when (predicate.target) {
            is EffectTarget.ContextTarget, is EffectTarget.BoundVariable, EffectTarget.TargetController -> true
            else -> false
        }
        else -> false
    }

    /**
     * A state predicate reads an earlier target when it relates the candidate to it in combat —
     * "target creature that target Wall blocked this turn" (Glyph of Delusion).
     */
    private fun referencesTarget(predicate: StatePredicate): Boolean = when (predicate) {
        is StatePredicate.And -> predicate.predicates.any(::referencesTarget)
        is StatePredicate.Or -> predicate.predicates.any(::referencesTarget)
        is StatePredicate.Not -> referencesTarget(predicate.predicate)
        else -> isTargetReference(when (predicate) {
            is StatePredicate.BlockPairedWithEntityThisTurn -> predicate.reference
            is StatePredicate.IsBlockingEntity -> predicate.reference
            is StatePredicate.IsAttackingDefenderOf -> predicate.reference
            is StatePredicate.IsAttackingPlayer -> predicate.player
            else -> null
        })
    }

    private fun isTargetReference(target: EffectTarget?): Boolean =
        target is EffectTarget.ContextTarget || target is EffectTarget.BoundVariable

    /** Whether selection may stop before slot [index]: it and every later slot are "up to one". */
    fun canStopAt(requirements: List<TargetRequirement>, index: Int): Boolean =
        requirements.drop(index).all { it.effectiveMinCount == 0 }

    private fun referencesTarget(predicate: CardPredicate): Boolean = when (predicate) {
        is CardPredicate.And -> predicate.predicates.any(::referencesTarget)
        is CardPredicate.Or -> predicate.predicates.any(::referencesTarget)
        is CardPredicate.Not -> referencesTarget(predicate.predicate)
        else -> (when (predicate) {
            is CardPredicate.PowerAtMostEntity -> predicate.reference
            is CardPredicate.PowerLessThanEntity -> predicate.reference
            is CardPredicate.PowerGreaterThanEntity -> predicate.reference
            is CardPredicate.ManaValueAtMostEntity -> predicate.reference
            is CardPredicate.SharesColorWith -> predicate.entity
            is CardPredicate.SharesCardTypeWith -> predicate.entity
            is CardPredicate.SharesCreatureTypeWith -> predicate.entity
            is CardPredicate.SharesManaValueWith -> predicate.entity
            is CardPredicate.SharesNameWith -> predicate.entity
            else -> null
        }).let(::isTargetReference)
    }

    /**
     * Exclude choices that cannot complete the remaining requirements. Search stops at the first
     * completion; no Cartesian product is allocated. Ordinary independent targets never use this path.
     * No priority passes between these decisions, so the prefix remains on the same battlefield.
     */
    fun legalNext(
        state: GameState,
        requirements: List<TargetRequirement>,
        chosen: List<List<EntityId>>,
        context: PredicateContext,
        targetFinder: TargetFinder,
        targetingSourceType: TargetingSourceType = TargetingSourceType.TRIGGERED_ABILITY
    ): List<EntityId> {
        require(requirements.all { req -> req !is TargetObject || req.filter.zone == Zone.BATTLEFIELD } &&
            requirements.dropLast(1).all { req -> req.count == 1 && !req.unlimited }) {
            "Dependent target selection requires players or permanents, single-target except the last slot"
        }
        fun candidates(prefix: List<List<EntityId>>): List<EntityId> {
            val prefixTargets = prefix.flatten().map { entityIdToChosenTarget(state, it) }
            val prefixRequirements = requirements.take(prefix.size)
                .mapIndexed { index, req -> req.withCount(prefix[index].size) }
            return targetFinder.findLegalTargets(
                state, requirements[prefix.size], context.controllerId,
                sourceId = context.sourceId,
                targetingSourceType = targetingSourceType,
                triggeringEntityId = context.triggeringEntityId,
                pipelineContext = context.copy(
                    targets = prefixTargets,
                    namedTargets = EffectContext.buildNamedTargets(prefixRequirements, prefixTargets),
                ),
            )
        }
        fun canComplete(prefix: List<List<EntityId>>): Boolean {
            if (canStopAt(requirements, prefix.size)) return true
            val next = requirements[prefix.size]
            // The multi-target last slot completes with enough legal targets for its minimum.
            if (prefix.size == requirements.lastIndex) return com.wingedsheep.engine.mechanics.targeting.NumericTargetEquality
                .eligible(state, candidates(prefix), next).size >= next.effectiveMinCount
            return candidates(prefix).any { canComplete(prefix + listOf(listOf(it))) }
        }
        val legal = com.wingedsheep.engine.mechanics.targeting.NumericTargetEquality.eligible(state, candidates(chosen), requirements[chosen.size])
        return legal.filter { canComplete(chosen + listOf(listOf(it))) }
    }

    /**
     * Every slot's [legalNext] choices for every prefix the earlier slots can produce, keyed by the
     * comma-joined earlier picks — the table a client walks slot by slot without a round trip.
     */
    fun prefixTables(
        state: GameState,
        requirements: List<TargetRequirement>,
        context: PredicateContext,
        targetFinder: TargetFinder,
        targetingSourceType: TargetingSourceType = TargetingSourceType.TRIGGERED_ABILITY
    ): List<Map<String, List<EntityId>>> {
        val tables = requirements.map { linkedMapOf<String, List<EntityId>>() }
        fun visit(prefix: List<List<EntityId>>) {
            val legal = legalNext(state, requirements, prefix, context, targetFinder, targetingSourceType)
            tables[prefix.size][prefix.flatten().joinToString(",")] = legal
            if (prefix.size < requirements.lastIndex) legal.forEach { visit(prefix + listOf(listOf(it))) }
        }
        visit(emptyList())
        return tables
    }
}
