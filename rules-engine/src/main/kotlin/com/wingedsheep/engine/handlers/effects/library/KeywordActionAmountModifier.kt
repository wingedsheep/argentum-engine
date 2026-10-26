package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LimitLibrarySearch
import com.wingedsheep.sdk.scripting.ModifyKeywordActionAmount
import com.wingedsheep.sdk.scripting.ReplacementEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Applies [ModifyKeywordActionAmount] replacement effects to an announced keyword-action count —
 * mill (CR 701.17), scry (CR 701.22) or surveil (CR 701.25) — before any card moves, or to the
 * number of times a spell is about to be copied (CR 707.10) before any copy is put on the stack.
 * Called once at each action's announcement site ([GatherCardsExecutor]'s `CardSource.TopOfLibrary`
 * branch for the flagged gather, the mill cost atom, each spell-copy executor), NOT per moved card
 * or per copy, so a paused-and-resumed pipeline never double-modifies.
 *
 * The keyword-action twin of `DrawReplacementDispatcher.applyDrawAmountModifier`: it scans every
 * battlefield permanent's replacement effects for the matching action's pattern, gates each by that
 * pattern's `player` filter relative to the acting player and the source's projected controller, checks
 * `restrictions`, sums the modifier, and clamps the result to `≥ 0`. A base count of 0 is left
 * untouched — a mill, scry or surveil of 0 is no event.
 *
 * Also home to [librarySearchDepth], the [LimitLibrarySearch] lookup for the search keyword action
 * (CR 701.23f): a search announces no count, but it shares the same scope-and-restriction scan.
 */
object KeywordActionAmountModifier {

    fun mill(state: GameState, playerId: EntityId, originalCount: Int, predicateEvaluator: PredicateEvaluator): Int =
        applyAmountModifiers(state, playerId, originalCount, predicateEvaluator) {
            (it as? EventPattern.MillEvent)?.player
        }

    fun scry(state: GameState, playerId: EntityId, originalCount: Int, predicateEvaluator: PredicateEvaluator): Int =
        applyAmountModifiers(state, playerId, originalCount, predicateEvaluator) {
            (it as? EventPattern.ScryEvent)?.player
        }

    fun surveil(state: GameState, playerId: EntityId, originalCount: Int, predicateEvaluator: PredicateEvaluator): Int =
        applyAmountModifiers(state, playerId, originalCount, predicateEvaluator) {
            (it as? EventPattern.SurveilEvent)?.player
        }

    /**
     * How many copies [copierId] makes when an effect would copy [spellId] [originalCount] times —
     * "instead copy it that many times plus an additional time" (Twinning Staff). Each pattern's
     * `spellFilter` is matched against the spell being copied; zero copies is no copy and is never
     * resized ("one or more times").
     */
    fun copySpell(
        state: GameState,
        copierId: EntityId,
        spellId: EntityId,
        originalCount: Int,
        predicateEvaluator: PredicateEvaluator
    ): Int =
        applyAmountModifiers(state, copierId, originalCount, predicateEvaluator) { pattern ->
            (pattern as? EventPattern.SpellCopiedEvent)
                ?.takeIf {
                    it.spellFilter == GameObjectFilter.Any ||
                        predicateEvaluator.matches(
                            state, state.projectedState, spellId, it.spellFilter,
                            PredicateContext(controllerId = copierId)
                        )
                }
                ?.player
        }

    /**
     * How deep [searcherId] may search each library — the smallest `topCount` of every
     * battlefield [LimitLibrarySearch] whose searcher scope covers them ("If an opponent would
     * search a library, that player searches the top four cards of that library instead", CR
     * 701.23f) — or null when no limit applies. Read once per search, at its gather, so the top
     * N are fixed for the whole search.
     */
    fun librarySearchDepth(state: GameState, searcherId: EntityId, predicateEvaluator: PredicateEvaluator): Int? {
        var depth: Int? = null
        forEachApplicable<LimitLibrarySearch>(state, searcherId, predicateEvaluator, { it.appliesTo.player }) {
            depth = minOf(depth ?: it.topCount, it.topCount)
        }
        return depth
    }
}

/**
 * Run [action] on every battlefield replacement of type [T] that applies to [playerId]: [playerOf]
 * names the replacement's `player` scope (null skips it), matched relative to the source's
 * projected controller, and every `restriction` must hold with [playerId] as controller.
 */
private inline fun <reified T : ReplacementEffect> forEachApplicable(
    state: GameState,
    playerId: EntityId,
    predicateEvaluator: PredicateEvaluator,
    playerOf: (T) -> Player?,
    action: (T) -> Unit
) {
    val conditionEvaluator = predicateEvaluator.conditions
    for (entityId in state.getBattlefield()) {
        val replacementSource = state.getEntity(entityId)?.get<ReplacementEffectSourceComponent>() ?: continue
        val sourceControllerId = state.projectedState.getController(entityId)

        for (effect in replacementSource.replacementEffects) {
            if (effect !is T) continue
            val player = playerOf(effect) ?: continue

            val matchesPlayer = when (player) {
                Player.Each -> true
                Player.You -> sourceControllerId != null && playerId == sourceControllerId
                Player.EachOpponent ->
                    sourceControllerId != null && state.isOpponentOf(playerId, sourceControllerId)
                else -> false
            }
            if (!matchesPlayer) continue

            val effectContext = EffectContext(
                sourceId = entityId,
                controllerId = playerId,
            )
            if (effect.restrictions.all { conditionEvaluator.evaluate(state, it, effectContext) }) action(effect)
        }
    }
}

/**
 * Sum every battlefield permanent's [ModifyKeywordActionAmount] whose `appliesTo` is the wanted
 * action — [playerOf] returns that pattern's `player`, or null for another action — onto
 * [originalCount], gated by that `player` filter relative to [playerId] and the source's controller
 * and by its `restrictions`, clamped to `≥ 0`. A count of 0 is no event and is never modified.
 */
private fun applyAmountModifiers(
    state: GameState,
    playerId: EntityId,
    originalCount: Int,
    predicateEvaluator: PredicateEvaluator,
    playerOf: (EventPattern) -> Player?
): Int {
    if (originalCount <= 0) return originalCount
    var adjusted = originalCount
    forEachApplicable<ModifyKeywordActionAmount>(state, playerId, predicateEvaluator, { playerOf(it.appliesTo) }) {
        adjusted += it.modifier
    }
    return adjusted.coerceAtLeast(0)
}
