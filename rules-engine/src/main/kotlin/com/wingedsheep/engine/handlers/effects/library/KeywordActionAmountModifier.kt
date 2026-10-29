package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
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
 * branch for the flagged gather, the mill cost atom, the pay-or-suffer mill, the damage-to-mill
 * replacements, each spell-copy executor), NOT per moved card or per copy, so a paused-and-resumed
 * pipeline never double-modifies.
 *
 * The keyword-action twin of `DrawReplacementDispatcher.applyDrawAmountModifier`: it scans every
 * battlefield permanent's replacement effects for the matching action's pattern, gates each by that
 * pattern's `player` filter relative to the acting player and the source's projected controller, and
 * checks `restrictions`. Each applicable replacement is the affine map `x -> x * multiplier + modifier`
 * and applies once (CR 616.1). When every pair commutes (any stack of "+N"s, any stack of doublers)
 * the order can't matter and [outcomes] returns the single result; otherwise it returns one
 * [AmountOutcome] per distinct result, each carrying an order that produces it, and the acting player
 * chooses (CR 616.1 — the affected player picks which replacement applies first). The result is
 * clamped to `≥ 0`. A base count of 0 is left untouched — a mill, scry or surveil of 0 is no event —
 * and so is the count of a player who has lost or left the game (CR 800.4a), who has nobody to ask.
 *
 * Also home to [librarySearchDepth], the [LimitLibrarySearch] lookup for the search keyword action
 * (CR 701.23f): a search announces no count, but it shares the same scope-and-restriction scan.
 */
object KeywordActionAmountModifier {

    /**
     * One way the applicable replacements can resize the count: the result and the names of the
     * replacement sources in an order that gives it (empty when there was no order to choose).
     */
    data class AmountOutcome(val count: Int, val orderSourceNames: List<String>)

    /** The keyword actions whose count [ModifyKeywordActionAmount] resizes. */
    enum class Action(val verb: String) { MILL("Mill"), SCRY("Scry"), SURVEIL("Surveil") }

    /**
     * Every distinct count [playerId]'s [action] of [originalCount] can end up as — a single entry
     * when the applicable replacements commute (no choice to make), several when their order matters.
     */
    fun outcomes(
        state: GameState,
        playerId: EntityId,
        originalCount: Int,
        predicateEvaluator: PredicateEvaluator,
        action: Action
    ): List<AmountOutcome> =
        amountOutcomes(state, playerId, originalCount, predicateEvaluator) { pattern ->
            when (action) {
                Action.MILL -> (pattern as? EventPattern.MillEvent)?.player
                Action.SCRY -> (pattern as? EventPattern.ScryEvent)?.player
                Action.SURVEIL -> (pattern as? EventPattern.SurveilEvent)?.player
            }
        }

    fun millOutcomes(state: GameState, playerId: EntityId, originalCount: Int, predicateEvaluator: PredicateEvaluator): List<AmountOutcome> =
        outcomes(state, playerId, originalCount, predicateEvaluator, Action.MILL)

    /**
     * The mill count when no choice is needed, or null when [playerId] must choose the order of
     * their mill replacements first ([millOutcomes] has more than one entry).
     */
    fun millIfUnambiguous(state: GameState, playerId: EntityId, originalCount: Int, predicateEvaluator: PredicateEvaluator): Int? =
        millOutcomes(state, playerId, originalCount, predicateEvaluator).singleOrNull()?.count

    /**
     * Option labels for choosing among [outcomes] — "Mill 12 (The Water Crystal, then Bruvac the
     * Grandiloquent)" — aligned with [outcomes].
     */
    fun optionLabels(action: Action, outcomes: List<AmountOutcome>): List<String> =
        outcomes.map { "${action.verb} ${it.count} (${it.orderSourceNames.joinToString(", then ")})" }

    /** The prompt the acting player sees when the order of their [action] replacements matters. */
    fun prompt(action: Action): String =
        "Choose the order your ${action.verb.lowercase()} replacement effects apply in"

    /**
     * How many copies [copierId] makes when an effect would copy [spellId] [originalCount] times —
     * "instead copy it that many times plus an additional time" (Twinning Staff). Each pattern's
     * `spellFilter` is matched against the spell being copied; zero copies is no copy and is never
     * resized ("one or more times"). Spell-copy replacements are additive only (the SDK rejects a
     * `multiplier` on [EventPattern.SpellCopiedEvent]), so they always commute and there is never an
     * order to choose.
     */
    fun copySpell(
        state: GameState,
        copierId: EntityId,
        spellId: EntityId,
        originalCount: Int,
        predicateEvaluator: PredicateEvaluator
    ): Int =
        amountOutcomes(state, copierId, originalCount, predicateEvaluator) { pattern ->
            (pattern as? EventPattern.SpellCopiedEvent)
                ?.takeIf {
                    it.spellFilter == GameObjectFilter.Any ||
                        predicateEvaluator.matches(
                            state, state.projectedState, spellId, it.spellFilter,
                            PredicateContext(controllerId = copierId)
                        )
                }
                ?.player
        }.single().count

    /**
     * How deep [searcherId] may search each library — the smallest `topCount` of every
     * battlefield [LimitLibrarySearch] whose searcher scope covers them ("If an opponent would
     * search a library, that player searches the top four cards of that library instead", CR
     * 701.23f) — or null when no limit applies. Read once per search, at its gather, so the top
     * N are fixed for the whole search.
     */
    fun librarySearchDepth(state: GameState, searcherId: EntityId, predicateEvaluator: PredicateEvaluator): Int? {
        var depth: Int? = null
        forEachApplicable<LimitLibrarySearch>(state, searcherId, predicateEvaluator, { it.appliesTo.player }) { _, limit ->
            depth = minOf(depth ?: limit.topCount, limit.topCount)
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
    action: (sourceId: EntityId, effect: T) -> Unit
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
            if (effect.restrictions.all { conditionEvaluator.evaluate(state, it, effectContext) }) action(entityId, effect)
        }
    }
}

/**
 * Every distinct result of applying each battlefield [ModifyKeywordActionAmount] whose `appliesTo`
 * is the wanted action — [playerOf] returns that pattern's `player`, or null for another action — to
 * [originalCount], gated by that `player` filter relative to [playerId] and the source's controller
 * and by its `restrictions`, clamped to `≥ 0`. A count of 0 is no event and is never modified.
 *
 * Two maps `x -> a1*x + b1` and `x -> a2*x + b2` commute exactly when `b1*(a2-1) == b2*(a1-1)`
 * (the same test as `LifeGainModifiers.orderMatters`); when every pair does, the single fold is the
 * answer. Otherwise each permutation is tried and the results deduplicated — a handful of
 * replacements at most, so the factorial stays tiny.
 */
private fun amountOutcomes(
    state: GameState,
    playerId: EntityId,
    originalCount: Int,
    predicateEvaluator: PredicateEvaluator,
    playerOf: (EventPattern) -> Player?
): List<KeywordActionAmountModifier.AmountOutcome> {
    if (originalCount <= 0) return listOf(KeywordActionAmountModifier.AmountOutcome(originalCount, emptyList()))
    // A player who has lost or left the game (CR 800.4a) is no longer an affected player who could
    // choose an order; their announced count stands unmodified. Keeps every site's "who is asked"
    // list (players still in the game, APNAP) and "who has a choice" in agreement.
    if (state.getEntity(playerId)?.has<PlayerLostComponent>() == true) {
        return listOf(KeywordActionAmountModifier.AmountOutcome(originalCount, emptyList()))
    }
    var applicable: MutableList<Pair<EntityId, ModifyKeywordActionAmount>>? = null
    forEachApplicable<ModifyKeywordActionAmount>(state, playerId, predicateEvaluator, { playerOf(it.appliesTo) }) { sourceId, effect ->
        (applicable ?: mutableListOf<Pair<EntityId, ModifyKeywordActionAmount>>().also { applicable = it }) += sourceId to effect
    }
    val found = applicable ?: return listOf(KeywordActionAmountModifier.AmountOutcome(originalCount, emptyList()))

    fun fold(order: List<Pair<EntityId, ModifyKeywordActionAmount>>): Int =
        order.fold(originalCount) { x, (_, e) -> x * e.multiplier + e.modifier }.coerceAtLeast(0)
    fun names(order: List<Pair<EntityId, ModifyKeywordActionAmount>>): List<String> =
        order.map { (id, _) -> state.getEntity(id)?.get<CardComponent>()?.name ?: "Unknown" }

    val orderMatters = found.indices.any { i ->
        (i + 1 until found.size).any { j ->
            val f = found[i].second
            val g = found[j].second
            f.modifier * (g.multiplier - 1) != g.modifier * (f.multiplier - 1)
        }
    }
    // Order is irrelevant here, so no labels are needed — nobody is asked.
    if (!orderMatters) return listOf(KeywordActionAmountModifier.AmountOutcome(fold(found), emptyList()))

    val byCount = LinkedHashMap<Int, List<Pair<EntityId, ModifyKeywordActionAmount>>>()
    permutations(found) { order -> byCount.putIfAbsent(fold(order), order) }
    return byCount.entries
        .sortedByDescending { it.key }
        .map { (count, order) -> KeywordActionAmountModifier.AmountOutcome(count, names(order)) }
}

/** Visit every ordering of [items] (Heap's algorithm over a working copy). */
private fun <T> permutations(items: List<T>, visit: (List<T>) -> Unit) {
    val a = items.toMutableList()
    val c = IntArray(a.size)
    visit(a.toList())
    var i = 0
    while (i < a.size) {
        if (c[i] < i) {
            val swapWith = if (i % 2 == 0) 0 else c[i]
            a[swapWith] = a[i].also { a[i] = a[swapWith] }
            visit(a.toList())
            c[i]++
            i = 0
        } else {
            c[i] = 0
            i++
        }
    }
}
