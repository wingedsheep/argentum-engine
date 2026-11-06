package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.UntapLimitPerStep
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Restrictions on the simultaneous untap, evaluated before any permanent untaps. Caps are read by
 * [standingStaticResults], so they work printed on a permanent, granted, or held on an emblem
 * (Dovin Baan); each one binds only the players its [UntapLimitPerStep.players] names, relative to
 * its source's controller.
 */
internal fun untapLimitChoices(
    state: GameState,
    registry: CardRegistry,
    predicates: PredicateEvaluator,
    wouldUntap: List<EntityId>,
): List<UntapLimitChoice> {
    if (wouldUntap.isEmpty()) return emptyList()
    val projected = state.projectedState
    val byController = wouldUntap.groupBy { projected.getController(it) }
    val candidates = standingStaticResults(state, registry, predicates, UntapLimitPerStep::class) { ability, source, controller ->
        val bound = if (ability.players == Player.Each) null else TargetResolutionUtils.resolvePlayerTargets(
            EffectTarget.PlayerRef(ability.players), state, EffectContext(sourceId = source, controllerId = controller)
        ).toSet()
        val context = PredicateContext(controllerId = controller, sourceId = source)
        // The cap is per player, including when teammates untap in a shared turn.
        byController.mapNotNull { (player, permanents) ->
            if (bound != null && player !in bound) return@mapNotNull null
            val matching = permanents.filter { predicates.matches(state, projected, it, ability.filter, context) }
            if (matching.size <= ability.max) null else UntapLimitChoice(matching, ability.max)
        }.ifEmpty { null }
    }
    val choices = LinkedHashMap<Set<EntityId>, UntapLimitChoice>()
    for (choice in candidates.flatten()) {
        val key = choice.matchingPermanents.toSet()
        val old = choices[key]
        if (old == null || choice.max < old.max) choices[key] = choice
    }
    return choices.values.toList()
}
