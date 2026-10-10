package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * "Does this activation find anything?" — the question behind
 * [AiProfile.refuseDeadSearches][com.wingedsheep.ai.engine.AiProfile.refuseDeadSearches].
 *
 * A fetch land's whole payoff is the card it finds. When our own library holds no card the search
 * can match, the activation is a sacrificed land and a shuffle — strictly worse than not activating.
 * The leaf score already prices the lost land (Seething Landscape, 2026-10-10 log game 3 turn 17:
 * about −1.6 against passing), yet the agent cracked it — most likely rollout noise: the shuffle
 * reorders the library, so the activation's playouts draw different cards from the pass line's and
 * the paired comparison stops cancelling noise. Whatever lifted it, "finds nothing" is the
 * structural certainty [TimingVerdict.NoWindow] is reserved for, so no score should outvote it.
 *
 * Reading the library's *contents* is honest: a player knows their decklist and what has left the
 * library, just not the order. The order is never read here.
 *
 * Deliberately narrow — it fires only when "does nothing" is certain:
 *  - every leaf of the effect is part of a library-search pipeline over **our own** library —
 *    the search itself, the choice, the move of what was found, a shuffle of our library and the
 *    "searched" event. Anything else (cycling's draw, a life gain rider, a second zone) declines;
 *  - every cost is tap, mana, life or sacrificing the source itself — a cost that could be a payoff
 *    of its own (a discard for madness, sacrificing another permanent) declines;
 *  - the source is not a creature, whose death can feed "whenever a creature dies" payoffs.
 */
object DeadSearch {

    /** Whether [ability], activated as [activation], searches for a card our library does not hold. */
    fun holds(
        state: GameState,
        playerId: EntityId,
        ability: ActivatedAbility,
        activation: ActivateAbility,
        predicates: PredicateEvaluator,
    ): Boolean {
        if (!costsOnlyResources(ability.cost)) return false
        if (state.projectedState.isCreature(activation.sourceId)) return false

        val leaves = EffectWalker.leaves(ability.effect)
        val filters = leaves.mapNotNull { searchFilter(it) }
        if (filters.isEmpty()) return false
        if (!leaves.all { searchFilter(it) != null || isSearchPlumbing(it) }) return false

        val library = state.getZone(ZoneKey(playerId, Zone.LIBRARY))
        val context = PredicateContext(controllerId = playerId, sourceId = activation.sourceId)
        val projected = state.projectedState
        return filters.none { filter ->
            library.any { cardId ->
                // An unreadable filter is "it might match", never a veto.
                runCatching { predicates.matches(state, projected, cardId, filter, context) }.getOrDefault(true)
            }
        }
    }

    /** The filter of a search through our own library, or null when [effect] is not one. */
    private fun searchFilter(effect: Any): GameObjectFilter? {
        if (effect !is GatherCardsEffect || !effect.search) return null
        val source = effect.source as? CardSource.FromZone ?: return null
        if (source.zone != Zone.LIBRARY || source.player != Player.You) return null
        return source.filter
    }

    /** The rest of `Patterns.Library.searchLibrary`: choose, move what was found, shuffle, announce. */
    private fun isSearchPlumbing(effect: Any): Boolean = when (effect) {
        is SelectFromCollectionEffect, is MoveCollectionEffect, EmitLibrarySearchedEventEffect -> true
        is ShuffleLibraryEffect -> effect.target == EffectTarget.Controller
        else -> false
    }

    private fun costsOnlyResources(cost: AbilityCost): Boolean = when (cost) {
        AbilityCost.Free, AbilityCost.Tap, AbilityCost.SacrificeSelf -> true
        is AbilityCost.Atom -> cost.atom is CostAtom.Mana || cost.atom is CostAtom.PayLife
        is AbilityCost.Composite -> cost.costs.all(::costsOnlyResources)
        else -> false
    }
}
